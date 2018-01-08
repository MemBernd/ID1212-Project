/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Constants;
import protocol.MessageTypes;

/**
 *
 * @author Bernardo
 */
public class Server implements Runnable {
    private int port;
    private static final int LINGER = 3000;
    private Game game;
    private MenuChanger view;
    private final Queue<ByteBuffer> messageToSend = new ArrayDeque<>();
    private ByteBuffer messageReceived = ByteBuffer.allocateDirect(Constants.MAX_LENGTH);
    private boolean stop = false;

    
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private InetSocketAddress server;

    private Boolean join;
    
    public Server(MenuChanger view, int port, Game game) throws IOException {
        this.game = game;
        this.view = view;
        this.port = port;
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
    }
    
    public void host() {
        join = false;
    }
    
    public void join(String host, int port) {
        join = true;
        server = new InetSocketAddress(host, port);

    }
    
    @Override
    public void run() {
        try {
            
            if(join)
                connect(server);
            else
                helper();
            serve();
            
        } catch (IOException e) {
            System.out.println(e.getCause().toString());
        }
    }
    
    private void sendToNode(SelectionKey key) {
        ByteBuffer message;
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            synchronized(messageToSend) {
                //System.out.println("Head of queue in send: " + messageToSend.peek().toString());
                while((message = messageToSend.poll()) != null) {
                    
                    if(message.hasRemaining())
                        return;
                }
                message = prepareMessage((String)key.attachment());
                channel.write(message);
                key.interestOps(SelectionKey.OP_READ);  
            }
        } catch (IOException ex) {
            view.print("Couldn't send.");
        } catch(CancelledKeyException e) {
            view.print("Program quit while sending");
            //remove client from list
        }
    }
    
    private void sendBroadcast(String message) {
        synchronized(messageToSend) {
            messageToSend.add(prepareMessage(message));
            //System.out.println("Head of queue in broadcast: " + messageToSend.peek().toString());
        }
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof SocketChannel && key.isValid()) {
                key.interestOps(SelectionKey.OP_WRITE);
                key.attach(message);
            }
        }
    }

    private void receiveMessage(SelectionKey key) throws IOException {
        messageReceived.clear();
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            int readBytes = channel.read(messageReceived);
        
            if (readBytes == -1) {
                throw new IOException("Lost connection");
            }
        } catch (IOException e) {
            System.out.println(e.getCause().toString());
        }
        try {
            String[] msg = verifyMessage(extractFromBuffer(messageReceived)).split(Constants.TYPE_DELIMITER);
            
            if (msg[0].equalsIgnoreCase(MessageTypes.JOIN.toString())) {
                StringJoiner message = new StringJoiner(Constants.CONTENT_DELIMITER);
                for (SelectionKey k : selector.keys()) {
                    if (k.channel() instanceof SocketChannel && key.isValid()) {
                        SocketChannel temp = (SocketChannel) k.channel();
                        InetSocketAddress test = (InetSocketAddress) temp.getRemoteAddress();
                        //System.out.println("sending key " + test.getHostString());
                        message.add(test.getHostString());
                    }
                }
                key.attach(MessageTypes.LOBBY + Constants.TYPE_DELIMITER + message.toString());
                key.interestOps(SelectionKey.OP_WRITE);
            } else if (msg[0].equalsIgnoreCase(MessageTypes.CHOICE.toString())) {
                view.print("choice");
            } else if (msg[0].equalsIgnoreCase(MessageTypes.LEAVE.toString())) {
                key.channel().close();
                key.cancel();
            } else if (msg[0].equalsIgnoreCase(MessageTypes.LOBBY.toString())) {
                if (msg.length > 1 ) {
                    String[] players = msg[1].split(Constants.CONTENT_DELIMITER);
                    for (String player : players) {
                        //connect(new InetSocketAddress(player, Constants.listeningPort));
                        view.print(player);
                    }
                    InetSocketAddress socket = (InetSocketAddress) serverChannel.getLocalAddress();
                    StringJoiner joiner = new StringJoiner(":");
                    joiner.add(socket.getHostString());
                    joiner.add(Integer.toString(port));
                    sendBroadcast(MessageTypes.ENTER.toString() + Constants.TYPE_DELIMITER + joiner.toString());
                }
            } else if (msg[0].equalsIgnoreCase(MessageTypes.ENTER.toString())) {
                view.setInGame(true);
                if (msg.length != 2)
                    throw new IOException();
                playerJoined(key, msg[1]);
            } else {
                view.print("unknown: " + msg[0]);
            }
        } catch (IOException e) {
            view.print(e.getCause().toString());
        }
    }
    
    private void playerJoined(SelectionKey key, String data) {
        try {
            
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
     private ByteBuffer prepareMessage(String message) {
        StringJoiner joiner = new StringJoiner(Constants.LENGTH_DELIMITER);
        joiner.add(Integer.toString(message.length()));
        joiner.add(message);
        return ByteBuffer.wrap(joiner.toString().getBytes());
    }
     
    private String extractFromBuffer(ByteBuffer message) {
        message.flip();
        byte[] bytes = new byte[message.remaining()];
        message.get(bytes);
        return new String(bytes);
    }
    
    private String verifyMessage(String message) throws IOException {
        String[] msg = message.split(Constants.LENGTH_DELIMITER);
        if (msg.length != 2) 
            throw new IOException("Corrupted message");
        int length = Integer.parseInt(msg[0]);
        if (length != msg[1].length())
            throw new IOException("Length doesn't match");
        return msg[1];
    }



    
    private void startNewClient(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, null);
        clientChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER);
    }
    
    private void serve() throws IOException {
       //selector = Selector.open();
       
       while (!stop) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                SelectionKey key;
                while(iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        startNewClient(key);
                    } else if(key.isConnectable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        channel.finishConnect();
                        if (join) {
                            synchronized(messageToSend) {
                                messageToSend.add(prepareMessage(MessageTypes.JOIN.toString()));
                                //System.out.println("Head of queue in serve: " + messageToSend.peek().toString());
                            }
                            key.attach(MessageTypes.JOIN.toString());
                            key.interestOps(SelectionKey.OP_WRITE);
                            join = false;
                        }
                        
                    } else if(key.isReadable()) {
                        receiveMessage(key);
                    } else if(key.isWritable()) {
                        sendToNode(key);
                    }
                }
            }
    }
    
    private void connect(InetSocketAddress endpoint) throws IOException {
        //selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(endpoint);
        channel.register(selector, SelectionKey.OP_CONNECT);
    }
    
    private void helper() throws IOException {
       
       serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
    
    public void reset() throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof SocketChannel && key.isValid()) {
                key.channel().close();
                key.cancel();
            }
        }
        stop = true;
    }


   

}
