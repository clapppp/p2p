package project.channel;

import project.util.AddressPair;
import project.util.RawRequest;
import project.util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static project.Main.clientMap;

public class TcpChannel {
    private final UdpChannel udpChannel;

    public TcpChannel(Selector selector, UdpChannel udpChannel) throws IOException {
        this.udpChannel = udpChannel;
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress(4000));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void handle(SocketChannel channel) {
        try (channel) {
            channel.configureBlocking(true);
            String request = Util.receiveTcpToString(channel);
            RawRequest rawRequest = new RawRequest(request);
            Map<String, String> body = rawRequest.getBody();
            String type = body.get("type");

            if (type.equals("list")) {
                handleList(channel);
            } else if (type.equals("signal")) {
                handleSignal(channel, body);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleList(SocketChannel channel) throws IOException {
        StringBuilder sb = new StringBuilder();
        clientMap.forEach((name, pair) -> {
            sb.append(name).append("=").append(pair.publicAddress()).append("|").append(pair.privateAddress())
                    .append("|").append(pair.isSymmetric()).append(",");
        });
        String response = RawRequest.makeRaw(Map.of("type", "list", "list", sb.toString()));
        channel.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
    }

    private void handleSignal(SocketChannel channel, Map<String, String> body) throws IOException {
        String sender = body.get("sender");
        String target = body.get("target");
        AddressPair senderIP = clientMap.get(sender);
        AddressPair targetIP = clientMap.get(target);
        if (target == null) {
            channel.close();
            return;
        }
        udpChannel.sendSignal(sender + " " + senderIP.publicAddress() + " " + senderIP.privateAddress() + "\r\n"
                + target + " " + targetIP.publicAddress() + " " + targetIP.privateAddress(), senderIP.publicAddress());
        udpChannel.sendSignal(target + " " + targetIP.publicAddress() + " " + targetIP.privateAddress() + "\r\n"
                + sender + " " + senderIP.publicAddress() + " " + senderIP.privateAddress(), targetIP.publicAddress());
    }

}
