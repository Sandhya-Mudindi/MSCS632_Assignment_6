package MSCS632_Assignment_6;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

class SharedQueue {
    private final Queue<String> tasks = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public void addTask(String task) {
        lock.lock();
        try {
            tasks.add(task);
        } finally {
            lock.unlock();
        }
    }

    public String getTask() {
        lock.lock();
        try {
            return tasks.poll();
        } finally {
            lock.unlock();
        }
    }
}

class Worker implements Runnable {
    private final SharedQueue queue;
    private final String workerName;
    private final FileWriter writer;

    public Worker(String name, SharedQueue queue, FileWriter writer) {
        this.queue = queue;
        this.workerName = name;
        this.writer = writer;
    }

    @Override
    public void run() {
        log(workerName + " started.");

        while (true) {
            String task = queue.getTask();
            if (task == null) break;

            log(workerName + " started processing " + task + ".");

            try {
                Thread.sleep(500); // Simulate work
                

                synchronized (writer) {
                    writer.write(workerName + " processed: " + task + "\n");
                }

                log(workerName + " finished processing " + task + ".");
            } catch (InterruptedException e) {
                log(workerName + " interrupted.");
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log(workerName + " file write error: " + e.getMessage());
            }
        }

        log(workerName + " finished.");
    }

    private void log(String message) {
        System.out.println("[LOG] " + message);
    }
}

public class DataProcessingSystem {
    public static void main(String[] args) {
        SharedQueue queue = new SharedQueue();
        int numberOfWorkers = 4;

        // Add tasks
        for (int i = 1; i <= 10; i++) {
            queue.addTask("Task-" + i);
        }

        try (FileWriter writer = new FileWriter("results_java.txt")) {
            ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);

            for (int i = 0; i < numberOfWorkers; i++) {
                executor.submit(new Worker("Worker-" + (i + 1), queue, writer));
            }

            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(100);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[Main Error] " + e.getMessage());
        }

        System.out.println("[LOG] All tasks completed.");
    }
}
