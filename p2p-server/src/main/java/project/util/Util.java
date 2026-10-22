package project.util;

import project.Main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Util {
    public static InetSocketAddress strToInet(String msg) {
        String[] part = msg.substring(1).split(":", 2);
        return new InetSocketAddress(part[0], Integer.parseInt(part[1]));
    }

    public static String receiveTcpToString(SocketChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        channel.read(buf);
        buf.flip();
        return StandardCharsets.UTF_8.decode(buf).toString();
    }

    public static boolean readEnd(ByteBuffer buffer) {
        String message = StandardCharsets.UTF_8.decode(buffer).toString();
        int headerEnd = message.indexOf("\r\n\r\n");
        if (headerEnd == -1) return false;

        int headerStartIndex = message.indexOf("Content-Length:");
        if (headerStartIndex == -1) return true;
        int headerEndIndex = message.indexOf("\r\n", headerStartIndex);
        int byteLength = Integer.parseInt(message.substring(headerStartIndex + 15, headerEndIndex).trim());
        String body = message.substring(headerEnd + 4);
        return body.getBytes(StandardCharsets.UTF_8).length == byteLength;
    }

    public static void manageClient() {
        try (ScheduledExecutorService manager = Executors.newScheduledThreadPool(1)) {
            manager.scheduleAtFixedRate(() -> {
                LocalTime now = LocalTime.now();
                for (Map.Entry<String, LocalDateTime> t : Main.clientTimer.entrySet()) {
                    if (ChronoUnit.SECONDS.between(now, t.getValue()) > 30) {
                        Main.clientMap.remove(t.getKey());
                        Main.clientTimer.remove(t.getKey());
                    }
                }
            }, 0, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
