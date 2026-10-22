package project.channel;

import project.util.AddressPair;
import project.util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static project.Main.clientMap;
import static project.Main.clientTimer;

public class UdpChannel {
    public final DatagramChannel channel;
    public final DatagramChannel channel2;

    public UdpChannel(Selector selector) throws IOException {
        channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(8080));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel2 = DatagramChannel.open();
        channel2.bind(new InetSocketAddress(8081));
        channel2.configureBlocking(false);
        channel2.register(selector, SelectionKey.OP_READ);
    }

    public void sendSignal(String message, InetSocketAddress target) {
        try {
            channel.send(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)), target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handle(byte[] data, InetSocketAddress sender) {
        try {
            String message = new String(data, StandardCharsets.UTF_8);
            if (message.startsWith("quit")) { //quit (name)
                String name = message.replace("quit ", "");
                clientMap.remove(name);
                clientTimer.remove(name);
            } else if (message.equals("STUN")) {
                channel.send(ByteBuffer.wrap(sender.toString().getBytes()), sender);
            } else {
                if (clientMap.get(message.split(" ")[0]) == null) System.out.println("Udp register : " + message);
                String[] part = message.split(" ", 3);
                clientMap.put(part[0], new AddressPair(sender, Util.strToInet(part[1]), Boolean.parseBoolean(part[2])));
                clientTimer.put(part[0], LocalDateTime.now());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
