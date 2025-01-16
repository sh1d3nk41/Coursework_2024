package backend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int port;
    private final InvertedIndex invertedIndex;
    private final ThreadPool threadPool;

    public Server(int port, InvertedIndex invertedIndex, ThreadPool threadPool) {
        this.port = port;
        this.invertedIndex = invertedIndex;
        this.threadPool = threadPool;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new RequestHandler(clientSocket, invertedIndex));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = 8081;
        InvertedIndex invertedIndex = new InvertedIndex();
        ThreadPool threadPool = new ThreadPool(10);
        Server server = new Server(port, invertedIndex, threadPool);
        server.start();
    }
}

