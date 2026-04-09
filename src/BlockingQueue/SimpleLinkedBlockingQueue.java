package BlockingQueue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleLinkedBlockingQueue<E> {

    // 1. The Linked List Node
    static class Node<E> {
        E item;
        Node<E> next;

        Node(E item) {
            this.item = item;
        }
    }

    private final int capacity;
    private int count = 0; // Number of items currently in the queue

    // Pointers to the front and back of the line
    private Node<E> head;
    private Node<E> tail;

    // 2. The Synchronization Tools
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public SimpleLinkedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;

        // Use a "dummy" node to make enqueue/dequeue logic much simpler (no null checks)
        this.head = this.tail = new Node<>(null);
    }

    // ==========================================
    // PRODUCER: Adding elements to the tail
    // ==========================================
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();

        // 1. Acquire the lock (Only I can touch the queue right now)
        lock.lockInterruptibly();
        try {
            // 2. If full, go to the 'notFull' waiting room and sleep
            while (count == capacity) {
                notFull.await();
            }

            // 3. Add to the linked list
            enqueue(new Node<>(e));
            count++;

            // 4. Wake up ONE waiting consumer thread (if any are waiting in 'notEmpty')
            notEmpty.signal();

        } finally {
            // 5. Always release the lock in a finally block!
            lock.unlock();
        }
    }

    // ==========================================
    // CONSUMER: Removing elements from the head
    // ==========================================
    public E take() throws InterruptedException {
        // 1. Acquire the lock
        lock.lockInterruptibly();
        try {
            // 2. If empty, go to the 'notEmpty' waiting room and sleep
            while (count == 0) {
                notEmpty.await();
            }

            // 3. Remove from the linked list
            E item = dequeue();
            count--;

            // 4. Wake up ONE waiting producer thread (if any are waiting in 'notFull')
            notFull.signal();

            return item;

        } finally {
            // 5. Release the lock
            lock.unlock();
        }
    }

    // --- Internal Helpers (Assumes lock is held) ---

    private void enqueue(Node<E> node) {
        tail.next = node;
        tail = node;
    }

    private E dequeue() {
        // Skip the dummy head node to get the actual first element
        Node<E> h = head;
        Node<E> first = h.next;

        // Move the head pointer forward
        head = first;
        E item = first.item;

        // Clear out data to help the Garbage Collector
        first.item = null;

        return item;
    }

    static void main(String[] args) throws InterruptedException {
        // Create our custom queue with a strict capacity of 3
        var queue = new SimpleLinkedBlockingQueue<String>(3);

        // ==========================================
        // 1. THE PRODUCER THREAD (Fast)
        // ==========================================
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    String item = "Task-" + i;
                    System.out.println("[Producer] Attempting to add: " + item);

                    // This will block if the queue is full
                    queue.put(item);

                    System.out.println("[Producer] Successfully added: " + item);
                }
                System.out.println("[Producer] Finished generating all tasks.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Producer was interrupted!");
            }
        });

        // ==========================================
        // 2. THE CONSUMER THREAD (Slow)
        // ==========================================
        Thread consumer = new Thread(() -> {
            try {
                // Give the producer a half-second head start to completely fill the queue
                Thread.sleep(500);

                for (int i = 1; i <= 6; i++) {
                    System.out.println("   [Consumer] Waiting to take an item...");

                    // This will block if the queue is empty
                    String item = queue.take();

                    System.out.println("   [Consumer] Successfully processed: " + item);

                    // Simulate heavy work by sleeping for 1.5 seconds
                    Thread.sleep(1500);
                }
                System.out.println("   [Consumer] Finished processing all tasks.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Consumer was interrupted!");
            }
        });

        // Start the test
        producer.start();
        consumer.start();
    }
}