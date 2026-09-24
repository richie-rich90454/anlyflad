package com.vectorium.desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.stage.BoundedPipelineMemoizer;
import com.vectorium.core.stage.Pipeline;
import com.vectorium.core.stage.PipelineConfig;
import com.vectorium.core.stage.PipelineLogger;
import com.vectorium.core.stage.StageException;
import com.vectorium.core.stage.StageRegistry;
import com.vectorium.core.svg.SvgCache;
public final class PipelineController {
    private final VectorCanvas canvas;
    private final StatusBar statusBar;
    private final DesktopDocumentLoader loader;
    private final SvgCache cache;
    private final StageRegistry registry;
    private final ExecutorService executor;
    private volatile VectorDocument source;
    private volatile VectorDocument result;
    private volatile PipelineConfig config=PipelineConfig.defaults();
    private volatile Future<?> pending;
    private volatile boolean autoRun=true;
    private volatile boolean closed;
    private Runnable documentListener;
    public PipelineController(VectorCanvas canvas, StatusBar statusBar) {
        if (canvas==null||statusBar==null) {
            throw new IllegalArgumentException("canvas and statusBar must not be null");
        }
        this.canvas=canvas;
        this.statusBar=statusBar;
        loader=new DesktopDocumentLoader();
        cache=new SvgCache();
        registry=new StageRegistry(new UiLogger(), new BoundedPipelineMemoizer(), cache);
        executor=Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    }
    public void load(File file) {
        ensureOpen();
        statusBar.setStatus("Loading "+file.getName());
        statusBar.setBusy(true);
        pending=executor.submit(new LoadTask(file));
    }
    public void runPipeline() {
        ensureOpen();
        if (source==null) {
            return;
        }
        statusBar.setStatus("Running pipeline");
        statusBar.setBusy(true);
        Pipeline pipeline=registry.buildPipeline(config);
        pending=executor.submit(new RunTask(pipeline, config));
    }
    public void export(File file) {
        ensureOpen();
        if (file==null) {
            throw new IllegalArgumentException("output file must not be null");
        }
        if (result==null) {
            throw new IllegalStateException("there is no vector result to export");
        }
        statusBar.setStatus("Exporting SVG");
        statusBar.setBusy(true);
        pending=executor.submit(new ExportTask(file, result));
    }
    public void setConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config=config;
    }
    public PipelineConfig getConfig() {
        return config;
    }
    public void setAutoRun(boolean autoRun) {
        this.autoRun=autoRun;
    }
    public void setDocumentListener(Runnable listener) {
        documentListener=listener;
    }
    public VectorDocument getSource() {
        return source;
    }
    public VectorDocument getResult() {
        return result;
    }
    public Future<?> getPending() {
        return pending;
    }
    public void awaitIdle() throws Exception {
        Future<?> future=pending;
        if (future!=null) {
            future.get(10L, TimeUnit.SECONDS);
        }
    }
    public void close() {
        closed=true;
        executor.shutdownNow();
    }
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("pipeline controller is closed");
        }
    }
    private void fail(Throwable throwable) {
        String message=throwable.getMessage();
        if (message==null||message.trim().isEmpty()) {
            message=throwable.getClass().getSimpleName();
        }
        final String errorMessage=message;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusBar.setStatus("Error: "+errorMessage);
                statusBar.setBusy(false);
            }
        });
    }
    private void notifyDocumentChanged() {
        if (documentListener!=null) {
            SwingUtilities.invokeLater(documentListener);
        }
    }
    private final class LoadTask implements Callable<VectorDocument> {
        private final File file;
        private LoadTask(File file) {
            this.file=file;
        }
        public VectorDocument call() throws Exception {
            try {
                VectorDocument loaded=loader.load(file);
                source=loaded;
                result=loaded;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        canvas.setDocument(loaded);
                        statusBar.setStatus("Ready to vectorize");
                        statusBar.setBusy(false);
                        notifyDocumentChanged();
                        if (autoRun) {
                            runPipeline();
                        }
                    }
                });
                return loaded;
            } catch (Exception exception) {
                fail(exception);
                return null;
            }
        }
    }
    private final class RunTask implements Callable<VectorDocument> {
        private final Pipeline pipeline;
        private final PipelineConfig runConfig;
        private RunTask(Pipeline pipeline, PipelineConfig runConfig) {
            this.pipeline=pipeline;
            this.runConfig=runConfig;
        }
        public VectorDocument call() throws Exception {
            try {
                VectorDocument input=source;
                VectorDocument output=pipeline.run(input, runConfig);
                result=output;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        canvas.setDocument(output);
                        statusBar.setStatus("Vectorization complete");
                        statusBar.setBusy(false);
                        notifyDocumentChanged();
                    }
                });
                return output;
            } catch (StageException exception) {
                fail(exception);
                return null;
            } catch (RuntimeException exception) {
                fail(exception);
                return null;
            }
        }
    }
    private final class ExportTask implements Callable<String> {
        private final File file;
        private final VectorDocument document;
        private ExportTask(File file, VectorDocument document) {
            this.file=file;
            this.document=document;
        }
        public String call() throws Exception {
            try {
                String svg=cache.get(document);
                Files.write(file.toPath(), svg.getBytes(StandardCharsets.UTF_8));
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        statusBar.setStatus("Exported "+file.getName());
                        statusBar.setBusy(false);
                    }
                });
                return svg;
            } catch (IOException exception) {
                fail(exception);
                return null;
            }
        }
    }
    private final class UiLogger implements PipelineLogger {
        private int completed;
        public void onStage(String stageName, com.vectorium.core.stage.StageResult result, long nanos) {
            completed++;
            final int progress=completed;
            final int maximum=registry.names().size();
            final String message=stageName+" · "+result.name();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    statusBar.setStatus(message);
                    statusBar.setProgress(progress, maximum);
                }
            });
        }
    }
    private static final class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread thread=new Thread(runnable, "anlyflad-pipeline");
            thread.setDaemon(true);
            return thread;
        }
    }
}
