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
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.stage.BoundedPipelineMemoizer;
import com.vectorium.core.stage.Pipeline;
import com.vectorium.core.stage.PipelineConfig;
import com.vectorium.core.stage.PipelineLogger;
import com.vectorium.core.stage.RasterMode;
import com.vectorium.core.stage.SilentPipelineLogger;
import com.vectorium.core.stage.StageException;
import com.vectorium.core.stage.StageRegistry;
import com.vectorium.core.svg.SvgCache;
public final class PipelineController {
    private final VectorCanvas canvas;
    private final StatusBar statusBar;
    private final DesktopDocumentLoader loader;
    private final SvgCache cache;
    private final BoundedPipelineMemoizer memoizer;
    private final StageRegistry registry;
    private volatile UiLogger uiLogger;
    private final ExecutorService executor;
    private final AtomicLong runSequence=new AtomicLong();
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
        memoizer=new BoundedPipelineMemoizer();
        registry=new StageRegistry(new SilentPipelineLogger(), memoizer, cache);
        uiLogger=new UiLogger(0L, registry.names().size());
        executor=Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    }
    public void load(File file) {
        ensureOpen();
        if (file==null) {
            throw new IllegalArgumentException("file must not be null");
        }
        long requestId=runSequence.incrementAndGet();
        source=null;
        result=null;
        memoizer.clear();
        cache.clear();
        statusBar.setStatus("Loading "+file.getName());
        statusBar.setBusy(true);
        notifyDocumentChanged();
        pending=executor.submit(new LoadTask(file, requestId));
    }
    public void runPipeline() {
        ensureOpen();
        if (source==null) {
            return;
        }
        statusBar.setStatus("Running pipeline");
        statusBar.setBusy(true);
        result=null;
        notifyDocumentChanged();
        long runId=runSequence.incrementAndGet();
        UiLogger logger=new UiLogger(runId, registry.names().size());
        uiLogger=logger;
        try {
            Pipeline pipeline=new StageRegistry(logger, memoizer, cache).buildPipeline(config);
            pending=executor.submit(new RunTask(pipeline, config, runId));
        } catch (RuntimeException exception) {
            fail(exception, runId);
        }
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
        notifyDocumentChanged();
        VectorDocument document=result;
        long exportId=runSequence.incrementAndGet();
        pending=executor.submit(new ExportTask(file, document, exportId));
    }
    public void setConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (!this.config.equals(config)) {
            this.config=config;
            runSequence.incrementAndGet();
            result=null;
            memoizer.clear();
            cache.clear();
            notifyDocumentChanged();
        }
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
    public boolean isBusy() {
        return statusBar.isBusy();
    }
    public void awaitIdle() throws Exception {
        Future<?> future=pending;
        while (future!=null) {
            future.get(10L, TimeUnit.SECONDS);
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                    }
                });
            }
            if (future==pending) {
                return;
            }
            future=pending;
        }
    }
    public void close() {
        closed=true;
        runSequence.incrementAndGet();
        memoizer.clear();
        cache.clear();
        executor.shutdownNow();
    }
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("pipeline controller is closed");
        }
    }
    private void fail(Throwable throwable, long operationId) {
        if (operationId!=runSequence.get()) {
            return;
        }
        String message=throwable.getMessage();
        if (message==null||message.trim().isEmpty()) {
            message=throwable.getClass().getSimpleName();
        }
        final String errorMessage=message;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (operationId!=runSequence.get()) {
                    return;
                }
                statusBar.setStatus("Error: "+errorMessage);
                statusBar.setBusy(false);
                notifyDocumentChanged();
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
        private final long requestId;
        private LoadTask(File file, long requestId) {
            this.file=file;
            this.requestId=requestId;
        }
        public VectorDocument call() throws Exception {
            try {
                VectorDocument loaded=loader.load(file);
                if (requestId!=runSequence.get()) {
                    return loaded;
                }
                source=loaded;
                result=loaded.getOrigin().isRaster()?null:loaded;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (requestId!=runSequence.get()) {
                            return;
                        }
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
                if (requestId==runSequence.get()) {
                    fail(exception, requestId);
                }
                return null;
            }
        }
    }
    private final class RunTask implements Callable<VectorDocument> {
        private final Pipeline pipeline;
        private final PipelineConfig runConfig;
        private final long runId;
        private RunTask(Pipeline pipeline, PipelineConfig runConfig, long runId) {
            this.pipeline=pipeline;
            this.runConfig=runConfig;
            this.runId=runId;
        }
        public VectorDocument call() throws Exception {
            try {
                VectorDocument input=source;
                VectorDocument output=pipeline.run(input, runConfig);
                if (runId!=runSequence.get()) {
                    return null;
                }
                result=output;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (runId!=runSequence.get()) {
                            return;
                        }
                        canvas.setVectorResult(output, runConfig.getRasterMode()==RasterMode.COLOR);
                        statusBar.setStatus("Vectorization complete");
                        statusBar.setBusy(false);
                        notifyDocumentChanged();
                    }
                });
                return output;
            } catch (StageException exception) {
                if (runId==runSequence.get()) {
                    fail(exception, runId);
                }
                return null;
            } catch (RuntimeException exception) {
                if (runId==runSequence.get()) {
                    fail(exception, runId);
                }
                return null;
            }
        }
    }
    private final class ExportTask implements Callable<String> {
        private final File file;
        private final VectorDocument document;
        private final long operationId;
        private ExportTask(File file, VectorDocument document, long operationId) {
            this.file=file;
            this.document=document;
            this.operationId=operationId;
        }
        public String call() throws Exception {
            try {
                if (operationId!=runSequence.get()) {
                    return null;
                }
                String svg=cache.get(document);
                if (operationId!=runSequence.get()) {
                    return null;
                }
                Files.write(file.toPath(), svg.getBytes(StandardCharsets.UTF_8));
                if (operationId!=runSequence.get()) {
                    return null;
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (operationId!=runSequence.get()) {
                            return;
                        }
                        statusBar.setStatus("Exported "+file.getName());
                        statusBar.setBusy(false);
                        notifyDocumentChanged();
                    }
                });
                return svg;
            } catch (IOException exception) {
                fail(exception, operationId);
                return null;
            } catch (RuntimeException exception) {
                fail(exception, operationId);
                return null;
            }
        }
    }
    private final class UiLogger implements PipelineLogger {
        private final long operationId;
        private final int maximum;
        private int completed;
        private UiLogger(long operationId, int maximum) {
            this.operationId=operationId;
            this.maximum=maximum;
        }
        public void onStage(String stageName, com.vectorium.core.stage.StageResult result, long nanos) {
            completed++;
            final int progress=Math.min(completed, maximum);
            final String message=stageName+" · "+result.name();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (operationId!=runSequence.get()) {
                        return;
                    }
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
