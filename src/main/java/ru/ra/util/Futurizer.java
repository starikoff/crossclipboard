package ru.ra.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import ru.ra.Environment.IDisposable;

public class Futurizer implements IDisposable {

    private final ExecutorService exec;

    private static final int CORE_SIZE = 1;

    private static final int MAX_SIZE = 10;

    private static final long TIMEOUT = 1000;

    public Futurizer() {
        LinkedBlockingQueue<Runnable> workQueue =
            new LinkedBlockingQueue<Runnable>();
        exec =
            new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, TIMEOUT,
                TimeUnit.MILLISECONDS, workQueue);
    }

    public <T, R> ListenableFuture<R> transform(ListenableFuture<T> f,
            Function<T, R> func) {
        return Futures.transform(f, func, exec);
    }

    public <T, R> ListenableFuture<R> transform(ListenableFuture<T> f,
            AsyncFunction<T, R> func) {
        return Futures.transform(f, func, exec);
    }

    public <T> void addCallback(ListenableFuture<T> f, FutureCallback<T> cb) {
        Futures.addCallback(f, cb, exec);
    }

    public <T> ListenableFuture<T> withFallback(ListenableFuture<T> f,
            FutureFallback<T> fallback) {
        return Futures.withFallback(f, fallback, exec);
    }

    @Override
    public void dispose() {
        exec.shutdown();
        try {
            if (!exec.awaitTermination(20, TimeUnit.SECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException e) {
            exec.shutdownNow();
        }
    }
}
