package main;

import java.util.concurrent.*;

public class BlockingQueueFactory {
    public <T> BlockingQueue<T> getBlockingQueue(SchedulingPolicy policy, int capacity) {
        return switch (policy) {
            case FIFO -> new LinkedBlockingQueue<>(capacity);
            case LIFO -> new Lifo<>(capacity);
            case PRIORITY -> new PriorityBlockingQueue<>(capacity); // Note: This will be unbounded!
        };
    }
}

class Lifo<E> extends LinkedBlockingDeque<E> {
    public Lifo(int capacity) {
        super(capacity);
    }

    // --- Override ALL insertion methods to push to the HEAD (First) ---

    @Override
    public void put(E e) throws InterruptedException {
        this.putFirst(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return this.offerFirst(e, timeout, unit); // Fixed: offerLast -> offerFirst
    }

    @Override
    public boolean offer(E e) {
        return this.offerFirst(e); // Added to handle non-blocking inserts
    }

    @Override
    public boolean add(E e) {
        this.addFirst(e); // Added to handle exception-throwing inserts
        return true;
    }
}


class BoundedBlockingPriorityQueue<E> {
    private final PriorityBlockingQueue<E> queue;
    private final Semaphore permits;

    public BoundedBlockingPriorityQueue(int capacity) {
        // The underlying queue handles the priority sorting
        this.queue = new PriorityBlockingQueue<>();

        // The Semaphore handles the strict capacity limit
        // 'true' ensures fairness (threads get in the order they arrived)
        this.permits = new Semaphore(capacity, true);
    }

    // --- BLOCKING METHODS ---

    public void put(E e) throws InterruptedException {
        permits.acquire(); // Blocks here if the queue is at max capacity
        queue.put(e);
    }

    public E take() throws InterruptedException {
        E element = queue.take(); // Blocks here if the queue is empty
        permits.release(); // Free up a spot for producers
        return element;
    }

    // --- TIMEOUT METHODS ---

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (permits.tryAcquire(timeout, unit)) {
            queue.offer(e);
            return true;
        }
        return false; // Timed out waiting for space
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E element = queue.poll(timeout, unit);
        if (element != null) {
            permits.release(); // Only release a permit if we actually got an element
        }
        return element;
    }

    // --- NON-BLOCKING METHODS ---

    public boolean offer(E e) {
        if (permits.tryAcquire()) { // Returns false immediately if no permits available
            queue.offer(e);
            return true;
        }
        return false;
    }

    public E poll() {
        E element = queue.poll();
        if (element != null) {
            permits.release();
        }
        return element;
    }
}