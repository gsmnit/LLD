package threadpool;

import threadpool.abstraction.Promise;
import java.util.function.Consumer;

/**
 * Concrete implementation of a lightweight Promise.
 */
public class SimplePromise<T> implements Promise<T> {
    private T result;
    private Throwable error;
    private boolean isComplete = false;

    // Callbacks to fire when done
    private Consumer<T> onSuccess;
    private Consumer<Throwable> onError;

    @Override
    public void then(Consumer<T> onSuccess, Consumer<Throwable> onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;

        // If the task finished before the callback was attached, fire immediately
        if (isComplete) {
            triggerCallbacks();
        }
    }

    @Override
    public void resolve(T result) {
        if (isComplete) return;
        this.result = result;
        this.isComplete = true;
        triggerCallbacks();
    }

    @Override
    public void reject(Exception error) {
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