package abstraction;

import main.SimplePromise;

import java.util.concurrent.Callable;

public interface Executor {
    /**
     * Submits a new task to the thread pool for execution.
     * * @param task The task to be executed.
     * @throws NullPointerException if the task is null.
     */
    void execute(Runnable task) throws InterruptedException;

    TaskHandle submit(Runnable task) throws InterruptedException;

    <T> SimplePromise<T> submitAndGetPromise(Callable<T> task) throws InterruptedException;
}