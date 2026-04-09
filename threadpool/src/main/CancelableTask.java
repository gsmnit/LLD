package main;

import abstraction.TaskHandle;

public class CancelableTask implements Runnable, TaskHandle {

    private final Runnable userTask;

    // State flags
    private volatile boolean isCancelled = false;
    private volatile Thread runnerThread = null; // Tracks who is executing this task

    public CancelableTask(Runnable userTask) {
        this.userTask = userTask;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isCancelled) {
            return false; // Already cancelled
        }

        isCancelled = true;

        // If it's currently running, and the user allowed interruption,
        // we interrupt the specific thread running it.
        if (mayInterruptIfRunning && runnerThread != null) {
            runnerThread.interrupt();
        }

        return true;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void run() {
        // 1. If cancelled while waiting in the queue, abort before starting
        if (isCancelled) {
            return;
        }

        // 2. Mark WHICH thread is running this task, so we can interrupt it later if needed
        runnerThread = Thread.currentThread();

        try {
            // 3. Check one last time before execution
            if (!isCancelled) {
                userTask.run();
            }
        } finally {
            // 4. Clean up. The task is done, so it's no longer tied to this thread.
            runnerThread = null;

            // Clear the thread's interrupt status just in case it was interrupted
            // at the exact millisecond the task finished. We don't want the Worker
            // to think it's being shut down!
            Thread.interrupted();
        }
    }
}