package backend;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPool {
    private final BlockingQueue<Runnable> taskQueue; 
    private final Thread[] workers;
    private volatile boolean isStopped = false;

    public ThreadPool(int numThreads) {
        taskQueue = new LinkedBlockingQueue<>();
        workers = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            workers[i] = new Worker(taskQueue);
            workers[i].start();
        }
    }

    public void submit(Runnable task) {
        if (!isStopped) {
            taskQueue.offer(task);
        } else {
            throw new IllegalStateException("ThreadPool is stopped!");
        }
    }

    public void stop() {
        isStopped = true;
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }

    private static class Worker extends Thread {
        private final BlockingQueue<Runnable> taskQueue;

        public Worker(BlockingQueue<Runnable> taskQueue) {
            this.taskQueue = taskQueue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Runnable task = taskQueue.take();
                    task.run();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
