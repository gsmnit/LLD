package threadpool.abstraction;


import java.util.concurrent.Callable;

public abstract class Action<T> implements Runnable, Callable<T>, Comparable<Action<?>> {
    private final Promise<T> promise;
    private int priority; // lower number is higher priority

    public Action(Promise<T> promise) {
        this.promise = promise;
    }

    @Override
    public void run() {
        try {
            promise.resolve(call());
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    public Promise<T> getPromise() {
        return promise;
    }

    @Override
    public int compareTo(Action<?> other) {
        return Integer.compare(this.priority, other.priority);
    }
}
