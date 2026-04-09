package threadpool.abstraction;


import java.util.function.Consumer;

/**
 * A lightweight interface representing an asynchronous promise.
 */
public interface Promise<T> {

    /**
     * Attaches callbacks to fire when the promise completes.
     */
    void then(Consumer<T> onSuccess, Consumer<Throwable> onError);

    /**
     * Resolves the promise with a successful result.
     */
    void resolve(T result);

    /**
     * Rejects the promise with an error.
     */
    void reject(Exception error);
}