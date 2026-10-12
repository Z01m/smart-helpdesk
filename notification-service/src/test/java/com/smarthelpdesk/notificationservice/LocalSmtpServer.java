package com.smarthelpdesk.notificationservice;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

// A loopback-only SMTP receiver used exclusively by tests. Never relays mail.
final class LocalSmtpServer implements AutoCloseable {
    private final ServerSocket server;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    final BlockingQueue<String> received = new LinkedBlockingQueue<>();
    LocalSmtpServer() throws IOException {
        server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        executor.submit(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    var writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true);
                    writer.print("220 localhost test SMTP\r\n");writer.flush();
                    String line;boolean data=false;var body = new StringBuilder();
                    while ((line=reader.readLine())!=null) {
                        if (data) {
                            if (line.equals(".")) { received.add(body.toString()); data=false; writer.print("250 accepted\r\n"); }
                            else body.append(line.startsWith("..")?line.substring(1):line).append("\r\n");
                        } else if (line.startsWith("DATA")) { data=true; writer.print("354 continue\r\n"); }
                        else if (line.startsWith("QUIT")) { writer.print("221 bye\r\n");writer.flush();break; }
                        else writer.print("250 OK\r\n");
                        writer.flush();
                    }
                } catch (IOException ignored) { }
            }
        });
    }
    int port() { return server.getLocalPort(); }
    @Override public void close() throws IOException { server.close();executor.shutdownNow(); }
}
