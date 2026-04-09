package threadpool.abstraction;

// The receipt given back to the user
public interface TaskHandle {
    // Attempts to cancel.
    // mayInterruptIfRunning determines if we should interrupt an actively running thread.
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
}