package backend;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.Set;

public class RequestHandler implements Runnable {

    private final Socket socket;
    private final InvertedIndex invertedIndex;

    public RequestHandler(Socket socket, InvertedIndex invertedIndex) {
        this.socket = socket;
        this.invertedIndex = invertedIndex;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(out, "Invalid request");
                return;
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendErrorResponse(out, "Invalid request format");
                return;
            }
            String method = parts[0];
            String path = parts[1];

            int contentLength = 0;
            String contentType = null;
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.toLowerCase().startsWith("content-type:")) {
                    contentType = line.split(":")[1].trim().toLowerCase();
                }
            }

            if (method.equalsIgnoreCase("GET") && path.equals("/")) {
                handleGetRoot(out);
            } else if (method.equalsIgnoreCase("POST") && path.equals("/")) {
                handlePostRoot(in, out, contentType, contentLength);
            } else if (method.equalsIgnoreCase("POST") && path.equals("/add")) {
                handlePostAdd(in, out, contentType, contentLength);
            } else {
                sendErrorResponse(out, "Not found or not supported: " + method + " " + path);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleGetRoot(OutputStream out) throws IOException {
        String html =
                "<!DOCTYPE html>" +
                        "<html lang=\"en\">" +
                        "<head>" +
                        "    <meta charset=\"UTF-8\">" +
                        "    <title>Inverted Index Search</title>" +
                        "    <style>" +
                        "        body { font-family: Arial, sans-serif; background: #f4f4f9; padding: 20px; color: #333; }" +
                        "        h1, h2 { color: #444; }" +
                        "        form { margin-top: 20px; padding: 10px; border: 1px solid #ccc; background: #fff; border-radius: 5px; }" +
                        "        input, textarea, button { width: 100%; padding: 10px; margin: 5px 0; border: 1px solid #ccc; border-radius: 5px; }" +
                        "        button { background: #4CAF50; color: white; border: none; cursor: pointer; font-size: 16px; }" +
                        "        button:hover { background: #45a049; }" +
                        "        #results, #addFileResult { margin-top: 10px; padding: 10px; border: 1px solid #ccc; background: #f9f9f9; border-radius: 5px; }" +
                        "    </style>" +
                        "</head>" +
                        "<body>" +
                        "    <h1>Inverted Index Search</h1>" +
                        "    <form id=\"searchForm\">" +
                        "        <label for=\"query\">Search:</label>" +
                        "        <input type=\"text\" id=\"query\" placeholder=\"Enter a word\" required>" +
                        "        <button type=\"button\" id=\"submitButton\">Search</button>" +
                        "    </form>" +
                        "    <div id=\"results\"></div>" +
                        "    <h2>Add File to Index</h2>" +
                        "    <form id=\"addFileForm\">" +
                        "        <label for=\"filename\">File Name:</label>" +
                        "        <input type=\"text\" id=\"filename\" placeholder=\"example.txt\" required>" +
                        "        <label for=\"fileContent\">File Content:</label>" +
                        "        <textarea id=\"fileContent\" rows=\"6\" placeholder=\"Enter file content\" required></textarea>" +
                        "        <button type=\"button\" id=\"addFileButton\">Add File</button>" +
                        "    </form>" +
                        "    <div id=\"addFileResult\"></div>" +
                        "    <script>" +
                        "        document.addEventListener('DOMContentLoaded', () => {" +
                        "            document.getElementById('submitButton').addEventListener('click', async () => {" +
                        "                const query = document.getElementById('query').value.trim();" +
                        "                const response = await fetch('/', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query }) });" +
                        "                const json = await response.json();" +
                        "                document.getElementById('results').textContent = json.result;" +
                        "            });" +
                        "            document.getElementById('addFileButton').addEventListener('click', async () => {" +
                        "                const filename = document.getElementById('filename').value.trim();" +
                        "                const content = document.getElementById('fileContent').value.trim();" +
                        "                const response = await fetch('/add', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ filename, content }) });" +
                        "                const json = await response.json();" +
                        "                document.getElementById('addFileResult').textContent = json.message;" +
                        "            });" +
                        "        });" +
                        "    </script>" +
                        "</body>" +
                        "</html>";

        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + html.getBytes().length + "\r\n" +
                "\r\n" +
                html;

        out.write(httpResponse.getBytes());
        out.flush();
    }

    private void handlePostRoot(BufferedReader in, OutputStream out, String contentType, int contentLength) throws IOException {
        char[] buffer = new char[contentLength];
        in.read(buffer);
        String body = new String(buffer);
        JSONObject json = new JSONObject(body);
        String query = json.optString("query", null);

        if (query == null || query.isEmpty()) {
            sendErrorResponse(out, "Missing 'query' in JSON");
            return;
        }
        sendSearchResponse(out, query);
    }

    private void handlePostAdd(BufferedReader in, OutputStream out, String contentType, int contentLength) throws IOException {
        char[] buffer = new char[contentLength];
        in.read(buffer);
        String body = new String(buffer);
        JSONObject json = new JSONObject(body);

        String filename = json.optString("filename", null);
        String content = json.optString("content", null);

        if (filename == null || content == null) {
            sendErrorResponse(out, "Missing 'filename' or 'content' in JSON");
            return;
        }

        File resourcesDir = new File("src/main/resources/data");
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }

        File file = new File(resourcesDir, filename); // Файл в директории resources
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content); // Сохраняем содержимое файла
        }

        for (String word : content.split("\\W+")) {
            if (!word.isEmpty()) {
                invertedIndex.add(word.toLowerCase(), filename);
            }
        }

        JSONObject response = new JSONObject();
        response.put("message", "File '" + filename + "' added to resources and index updated.");

        String jsonResponse = response.toString();
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + jsonResponse.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                jsonResponse;

        out.write(httpResponse.getBytes("UTF-8"));
        out.flush();
    }


    private void sendSearchResponse(OutputStream out, String query) throws IOException {
        query = query.toLowerCase();
        Set<String> results = invertedIndex.search(query);

        String resultMessage;
        if (results.isEmpty()) {
            resultMessage = "No results found for: " + query;
        } else {
            resultMessage = "Results for '" + query + "':\n" + String.join("\n", results);
        }

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("result", resultMessage);

        String jsonString = jsonObj.toString();
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + jsonString.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                jsonString;

        out.write(httpResponse.getBytes("UTF-8")); // Отправляем данные в кодировке UTF-8
        out.flush();
    }


    private void sendErrorResponse(OutputStream out, String message) throws IOException {
        String html = "<html><body><h1>Error: " + message + "</h1></body></html>";
        String httpResponse = "HTTP/1.1 400 Bad Request\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + html.length() + "\r\n" +
                "\r\n" +
                html;

        out.write(httpResponse.getBytes());
        out.flush();
    }
}
