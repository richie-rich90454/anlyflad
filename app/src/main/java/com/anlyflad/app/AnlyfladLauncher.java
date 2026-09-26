package com.anlyflad.app;
import com.anlyflad.cli.AnlyfladCli;
import com.anlyflad.desktop.VectoriumFrame;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;
public final class AnlyfladLauncher {
    private static final int DEFAULT_PORT=8080;
    private AnlyfladLauncher() {
    }
    public static void main(String[] args) throws Exception {
        if (args.length>0&&"--desktop".equalsIgnoreCase(args[0])) {
            launchDesktop();
            return;
        }
        if (args.length>0&&"--web".equalsIgnoreCase(args[0])) {
            launchWeb(args);
            return;
        }
        String[] cliArgs=args.length==0?new String[]{"--help"}:args;
        int exitCode=new AnlyfladCli().execute(cliArgs);
        if (exitCode!=0) {
            System.exit(exitCode);
        }
    }
    private static void launchDesktop() {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("The desktop UI requires a graphical environment. Use --web or the CLI instead.");
            System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new VectoriumFrame().setVisible(true);
            }
        });
    }
    private static void launchWeb(String[] args) throws Exception {
        int port=DEFAULT_PORT;
        for (int index=1;index<args.length;index++) {
            String argument=args[index];
            if ("--port".equalsIgnoreCase(argument)&&index+1<args.length) {
                port=parsePort(args[index+1]);
                index++;
            } else if (argument.toLowerCase(java.util.Locale.ROOT).startsWith("--port=")) {
                port=parsePort(argument.substring("--port=".length()));
            }
        }
        if (port<1||port>65535) {
            System.err.println("Port must be between 1 and 65535.");
            System.exit(1);
            return;
        }
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", new StaticWebHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("Anlyflad web UI: http://127.0.0.1:"+port+"/");
        System.out.println("Documentation:  http://127.0.0.1:"+port+"/docs.html");
        System.out.println("Press Ctrl+C to stop.");
        new CountDownLatch(1).await();
    }
    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
    private static final class StaticWebHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String path=exchange.getRequestURI().getPath();
            if (path==null||path.isEmpty()||"/".equals(path)) {
                path="/index.html";
            }
            path=URLDecoder.decode(path, "UTF-8");
            if (path.indexOf("..")>=0||path.indexOf('\0')>=0) {
                send(exchange,404,"text/plain","Not found");
                return;
            }
            if (path.endsWith("/")) {
                path=path+"index.html";
            }
            InputStream stream=AnlyfladLauncher.class.getResourceAsStream("/webapp"+path);
            if (stream==null) {
                send(exchange,404,"text/plain","Not found");
                return;
            }
            try {
                byte[] body=readAll(stream);
                exchange.getResponseHeaders().set("Content-Type", contentType(path));
                exchange.getResponseHeaders().set("Cache-Control","no-cache");
                exchange.sendResponseHeaders(200,body.length);
                OutputStream output=exchange.getResponseBody();
                try {
                    output.write(body);
                } finally {
                    output.close();
                }
            } finally {
                stream.close();
            }
        }
        private static byte[] readAll(InputStream stream) throws IOException {
            java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192];
            int read;
            while ((read=stream.read(buffer))>=0) {
                output.write(buffer,0,read);
            }
            return output.toByteArray();
        }
        private static void send(HttpExchange exchange,int status,String type,String message) throws IOException {
            byte[] body=message.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");
            exchange.sendResponseHeaders(status,body.length);
            OutputStream output=exchange.getResponseBody();
            try {
                output.write(body);
            } finally {
                output.close();
            }
        }
        private static String contentType(String path) {
            String lower=path.toLowerCase(java.util.Locale.ROOT);
            if (lower.endsWith(".html")) {
                return "text/html; charset=utf-8";
            }
            if (lower.endsWith(".js")) {
                return "text/javascript; charset=utf-8";
            }
            if (lower.endsWith(".css")) {
                return "text/css; charset=utf-8";
            }
            if (lower.endsWith(".svg")) {
                return "image/svg+xml";
            }
            if (lower.endsWith(".png")) {
                return "image/png";
            }
            if (lower.endsWith(".ico")) {
                return "image/x-icon";
            }
            if (lower.endsWith(".webmanifest")||lower.endsWith(".json")) {
                return "application/json; charset=utf-8";
            }
            if (lower.endsWith(".md")) {
                return "text/markdown; charset=utf-8";
            }
            if (lower.endsWith(".woff2")) {
                return "font/woff2";
            }
            if (lower.endsWith(".ttf")) {
                return "font/ttf";
            }
            if (lower.endsWith(".txt")) {
                return "text/plain; charset=utf-8";
            }
            return "application/octet-stream";
        }
    }
}
