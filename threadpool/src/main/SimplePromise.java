package main;

import java.util.function.Consumer;

/**
 * A lightweight version of CompletableFuture.
 */
public class SimplePromise<T> {
    private T result;
    private Throwable error;
    private boolean isComplete = false;

    // Callbacks to fire when done
    private Consumer<T> onSuccess;
    private Consumer<Throwable> onError;

    // --- 1. How the consumer ATTACHES callbacks ---
    public synchronized void then(Consumer<T> onSuccess, Consumer<Throwable> onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;

        // If the task finished before the callback was attached, fire immediately
        if (isComplete) {
            triggerCallbacks();
        }
    }

    // --- 2. How the worker thread RESOLVES the promise ---
    public synchronized void resolve(T result) {
        if (isComplete) return;
        this.result = result;
        this.isComplete = true;
        triggerCallbacks();
    }

    // --- 3. How the worker thread FAILS the promise ---
    public synchronized void reject(Exception error) {
        if (isComplete) return;
        this.error = error;
        this.isComplete = true;
        triggerCallbacks();
    }

    private void triggerCallbacks() {
        if (error != null && onError != null) {
            onError.accept(error);
        } else if (result != null && onSuccess != null) {
            onSuccess.accept(result);
        }
    }
}