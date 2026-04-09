package threadpool;

import threadpool.abstraction.*;
import threadpool.abstraction.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FixedThreadPool implements Executor, ThreadPoolLifecycle {

    // The buffer holding waiting tasks
    private final BlockingQueue<Runnable> taskQueue;

    // Tracking the OS threads so we can interrupt them later
    private final List<Thread> workerThreads;

    // State flag to prevent new task submissions during shutdown
    private volatile boolean isShutdown = false;

    /**
     * Bootstraps the thread pool.
     * * @param poolSize The fixed number of worker threads.
     * @param queueCapacity The maximum number of tasks the queue can hold.
     */
    public FixedThreadPool(int poolSize, SchedulingPolicy policy, int queueCapacity) {
        this.taskQueue = new BlockingQueueFactory().getBlockingQueue(policy, queueCapacity);
        this.workerThreads = new ArrayList<>(poolSize);

        // Spin up the workers immediately
        for (int i = 0; i < poolSize; i++) {
            Thread thread = new Thread(new Worker(), "ThreadPool-Worker-" + i);
            thread.start();
            workerThreads.add(thread);
        }
    }

    @Override
    public void execute(Runnable task) throws InterruptedException {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        // Rejection Policy: If shut down, reject new work.
        if (isShutdown) {
            throw new IllegalStateException("ThreadPool is shut down. Cannot accept new tasks.");
        }

        // Put task into the queue. Blocks if the queue is full.
        taskQueue.put(task);
    }

    @Override
    public TaskHandle submit(Runnable task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException("Pool is shut down.");
        }

        // Wrap the raw task
        CancelableTask cancelableTask = new CancelableTask(task);

        // Put the wrapper into the queue
        taskQueue.put(cancelableTask);

        // Return the receipt to the Producer
        return cancelableTask;
    }

    @Override
    public <T> Promise<T> submitAndGetPromise(Callable<T> task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException("Pool is shut down.");
        }

        var actionWrapper = new Action<T>(new SimplePromise<>()) {
            @Override
            public T call() throws Exception {
                return task.call();
            }
        };

        // Wait for space in the queue up to the specified timeout
        boolean accepted = taskQueue.offer(actionWrapper, 5000, TimeUnit.MILLISECONDS);

        if (!accepted) {
            // The queue was full, and we waited the maximum amount of time.
            // Reject the promise immediately so the caller knows it failed.
            actionWrapper.getPromise().reject(
                    new TimeoutException("Task rejected: Thread pool queue is full.")
            );
        }

        // Return the promise. If it timed out, the caller's onError callback
        // will trigger immediately because we just rejected it above!
        return actionWrapper.getPromise();
    }

    @Override
    public void shutdown() {
        // 1. Stop accepting new tasks
        isShutdown = true;

        // 2. Interrupt all idle worker threads.
        // If they are sleeping on queue.take(), this wakes them up
        // so they can see the 'isShutdown' flag and exit gracefully.
        for (Thread thread : workerThreads) {
            thread.interrupt();
        }
    }

    /**
     * The Consumer: An internal class that continuously polls for work.
     */
    private class Worker implements Runnable {
        @Override
        public void run() {
            // Keep running until shutdown AND the queue is completely empty
            while (true) {

                // Exit condition: Pool is shutting down AND no more tasks remain
                if (isShutdown && taskQueue.isEmpty()) {
                    break;
                }

                try {
                    // Block and wait for a task.
                    // Consumes 0 CPU cycles while waiting.
                    Runnable task = taskQueue.take();

                    // Execute the task
                    task.run();

                } catch (InterruptedException e) {
                    // The thread was interrupted by the shutdown() method while sleeping.
                    // We don't need to do anything; the loop will restart, hit the
                    // 'isShutdown && taskQueue.isEmpty()' check, and break.
                } catch (RuntimeException e) {
                    // CRITICAL: We must catch RuntimeExceptions thrown by the user's task.
                    // If we don't, the exception will kill our worker thread permanently!
                    System.err.println("A task threw an exception: " + e.getMessage());
                }
            }
            System.out.println(Thread.currentThread().getName() + " has gracefully terminated.");
        }
    }
}