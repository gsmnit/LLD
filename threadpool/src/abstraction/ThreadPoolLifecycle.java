package abstraction;

public interface ThreadPoolLifecycle {
    /**
     * Initiates an orderly shutdown.
     * Previously submitted tasks are executed, but no new tasks will be accepted.
     */
    void shutdown();
}
