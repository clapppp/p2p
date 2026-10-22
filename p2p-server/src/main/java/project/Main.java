package project;

import project.channel.TcpChannel;
import project.channel.UdpChannel;
import project.util.AddressPair;
import project.util.Util;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static final ConcurrentHashMap<String, AddressPair> clientMap = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, LocalDateTime> clientTimer = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            final Selector selector = Selector.open();
            final ExecutorService executor = Executors.newFixedThreadPool(5);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

            final UdpChannel udpChannel = new UdpChannel(selector);
            final TcpChannel tcpChannel = new TcpChannel(selector, udpChannel);

            Util.manageClient();

            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    if (key.channel() instanceof ServerSocketChannel channel) {
                        SocketChannel client = channel.accept();
                        executor.execute(() -> tcpChannel.handle(client));
                    } else if (key.channel() instanceof DatagramChannel channel) {
                        buffer.clear();
                        InetSocketAddress sender = (InetSocketAddress) channel.receive(buffer);
                        buffer.flip();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        executor.execute(() -> udpChannel.handle(data, sender));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
