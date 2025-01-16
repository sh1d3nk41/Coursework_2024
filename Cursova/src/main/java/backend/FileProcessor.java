package backend;

import java.io.*;
import java.util.concurrent.ExecutorService;

public class FileProcessor {
    private final InvertedIndex index;
    private final ThreadPool threadPool;

    public FileProcessor(InvertedIndex index, ThreadPool threadPool) {
        this.index = index;
        this.threadPool = threadPool;
    }

    public void processFiles(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null) return;

        for (File file : files) {
            threadPool.submit(() -> processFile(file));
        }
    }

    private void processFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("[^\\p{L}\\p{Nd}]+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        index.add(word, file.getName());
                    }
                }
            }
            System.out.println("Indexed file: " + file.getName());
        } catch (IOException e) {
            System.err.println("Failed to process file: " + file.getName());
            e.printStackTrace();
        }
    }
}
