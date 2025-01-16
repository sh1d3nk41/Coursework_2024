package backend;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPool {
    private final BlockingQueue<Runnable> taskQueue; // Очередь задач
    private final Thread[] workers;                 // Потоки-работники
    private volatile boolean isStopped = false;     // Флаг остановки

    public ThreadPool(int numThreads) {
        taskQueue = new LinkedBlockingQueue<>();
        workers = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            workers[i] = new Worker(taskQueue); // Создаем и запускаем поток
            workers[i].start();
        }
    }

    public void submit(Runnable task) {
        if (!isStopped) {
            taskQueue.offer(task); // Добавляем задачу в очередь
        } else {
            throw new IllegalStateException("ThreadPool is stopped!");
        }
    }

    public void stop() {
        isStopped = true; // Ставим флаг остановки
        for (Thread worker : workers) {
            worker.interrupt(); // Прерываем потоки
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
                    Runnable task = taskQueue.take(); // Берем задачу из очереди
                    task.run();                      // Выполняем задачу
                } catch (InterruptedException e) {
                    break; // Выходим из цикла при прерывании
                }
            }
        }
    }
}
