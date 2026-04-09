package abstraction;

import main.SimplePromise;

import java.util.concurrent.Callable;

public abstract class Action<T> implements Runnable, Callable<T>, Comparable<Action<?>> {
    private final SimplePromise<T> promise = new SimplePromise<>();
    private int priority; // lower number is higher priority

    @Override
    public void run() {
        try {
            promise.resolve(call());
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    public SimplePromise<T> getPromise() {
        return promise;
    }

    @Override
    public int compareTo(Action<?> other) {
        return Integer.compare(this.priority, other.priority);
    }
}
