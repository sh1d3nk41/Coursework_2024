package backend;

import java.io.File;

public class App {
    public static void main(String[] args) {
        InvertedIndex index = new InvertedIndex();
        ThreadPool threadPool = new ThreadPool(4);

        FileProcessor processor = new FileProcessor(index, threadPool);
        File dataFolder = new File("src/main/resources/data");
        processor.processFiles(dataFolder);

        int port = 8081;
        Server server = new Server(port, index, threadPool);
        server.start();

    }
}

