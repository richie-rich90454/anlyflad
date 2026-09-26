package com.anlyflad.cli;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import com.anlyflad.core.raster.ParallelRunner;
public final class JvmParallelRunner implements ParallelRunner {
    public void run(final int taskCount, final Task task) {
        if (System.getProperty("anlyflad.sequential")!=null) {
            ParallelRunner.SEQUENTIAL.run(taskCount, task);
            return;
        }
        int threads=Math.min(taskCount, Math.max(1, Runtime.getRuntime().availableProcessors()));
        if (threads<=1) {
            SEQUENTIAL.run(taskCount, task);
            return;
        }
        ExecutorService pool=Executors.newFixedThreadPool(threads, new DaemonThreadFactory());
        final AtomicInteger next=new AtomicInteger();
        List<Callable<Void>> workers=new ArrayList<Callable<Void>>(threads);
        for (int index=0;index<threads;index++) {
            workers.add(new Callable<Void>() {
                public Void call() {
                    int taskIndex;
                    while ((taskIndex=next.getAndIncrement())<taskCount) {
                        task.run(taskIndex);
                    }
                    return null;
                }
            });
        }
        try {
            List<Future<Void>> futures=pool.invokeAll(workers);
            for (int index=0;index<futures.size();index++) {
                try {
                    futures.get(index).get();
                } catch (ExecutionException exception) {
                    Throwable cause=exception.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException)cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error)cause;
                    }
                    throw new IllegalStateException("parallel task failed", cause);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("parallel execution interrupted", exception);
        } finally {
            pool.shutdownNow();
        }
    }
    private static final class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread thread=new Thread(runnable, "anlyflad-fit");
            thread.setDaemon(true);
            return thread;
        }
    }
}
