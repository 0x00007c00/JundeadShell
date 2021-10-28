package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class WebAppCheck {

    public static boolean check() {
        String target_url = FileUtil.getProperty("target_url");
        URL url = null;
        try {
            url = new URL(target_url);
        } catch (MalformedURLException ignored) {
        }
        if (url == null) {
            Logger.printErr("\"target_url\" configuration error!");
            return false;
        }
        String host = url.getHost();
        int port = url.getPort();
        if (port == -1) {
            if (target_url.startsWith("https")) {
                port = 443;
            } else {
                port = 80;
            }
        }

        boolean connected = isConnected(host, port);
        if (connected) {
            // Make an initial request.
            httpGet(host, port);
        }
        return connected;
    }

    private static boolean isConnected(String host, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private static void httpGet(String host, int port) {
        Socket socket;
        try {
            InetAddress hostAdd = InetAddress.getByName(host);
            socket = new Socket(hostAdd, port);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            byte[] bytes = ("GET / HTTP/1.1\r\n" +
                    "Host:" + host + "\r\n" +
                    "Connection:close\r\n" +
                    "User-agent:Mozilla/4.0\r\n" +
                    "Accept-language:zh-cn\r\n" +
                    "\r\n").getBytes("UTF-8");

            out.write(bytes);
            out.flush();
            byte[] bt = new byte[1024];
            int len;
            while ((len = in.read(bt)) != -1) {
                String s = new String(bt, 0, len);
                //Logger.print(s);
            }
            in.close();
            out.close();
        } catch (Exception ignored) {
        }
    }
}
