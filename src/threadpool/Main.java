package threadpool;

public class Main {
    static void main() throws InterruptedException {
        var threadPool = new FixedThreadPool(10, SchedulingPolicy.FIFO, 20);
        threadPool.submitAndGetPromise( () -> "good good").then(
                (s) -> System.out.println("result: " + s),
                ( e) -> System.out.println("error: " + e.getMessage())
        );
    }
}
