/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import GameState.Player;
import GameState.Game;
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
import protocol.GameSelections;
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
    //private final Queue<ByteBuffer> messageToSend = new ArrayDeque<>();
    private ByteBuffer messageReceived = ByteBuffer.allocateDirect(Constants.MAX_LENGTH);
    private boolean stop = false;
    private boolean timeToEnter = false;

    
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
        helper();
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
                connect(server, "helper");
            serve();
            
        } catch (IOException e) {
            System.out.println(e.getCause().toString());
        }
    }
    
    private void sendToNode(SelectionKey key) {
        ByteBuffer message;
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            Player player = (Player) key.attachment();
            
                while((message = player.getMessageToSend().poll()) != null) {
                    //System.out.println("entered while sending.");
                    channel.write(message);
                    if(message.hasRemaining()) {
                        System.out.println("send returned");
                        return;
                    }
                }
                //message = prepareMessage((String)key.attachment());
                
                key.interestOps(SelectionKey.OP_READ);  
        } catch (IOException ex) {
            view.print("Couldn't send.");
        } catch(CancelledKeyException e) {
            view.print("Program quit while sending");
            //remove client from list
        }
    }
    
    private void sendBroadcast(String message) {
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof SocketChannel && key.isValid()) {
                Player player = (Player) key.attachment();
                player.addMessage(prepareMessage(message));
                key.attach(player);
                key.interestOps(SelectionKey.OP_WRITE);
                
            }
        }
        selector.wakeup();
    }
    
    public void sendChoice() {
        sendBroadcast(MessageTypes.CHOICE + Constants.TYPE_DELIMITER + game.getChoice().toString());
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
                if (msg.length != 2)
                    throw new IOException();
                Player player = convertToPlayer(msg[1]);
                handleJoin(key, player);
            } 
            
            else if (msg[0].equalsIgnoreCase(MessageTypes.CHOICE.toString())) { //selection from player received
                //view.print("choice");
                Player player = (Player) key.attachment();
                player.setChoice(GameSelections.valueOf(msg[1].toUpperCase()));
                if (game.isRoundOver()) {
                    view.print("Round: " + game.pointsAwarded() + " Total: " + game.getScore());
                    game.newRound();
                }
            } 
            
            else if (msg[0].equalsIgnoreCase(MessageTypes.LEAVE.toString())) { //player left
                Player player = (Player) key.attachment();
                game.removePlayer(player);
                key.channel().close();
                key.cancel();
            } 
            
            else if (msg[0].equalsIgnoreCase(MessageTypes.LOBBY.toString())) { //received player in lobby
                if (msg.length > 1 ) {
                    String[] players = msg[1].split(Constants.CONTENT_DELIMITER);
                    for (String player : players) {
                        connect(convertToPlayer(player));
                    }
                    //helper();
                    //selector.wakeup();
                }
                view.setInGame(true);
            } 
            
            else if (msg[0].equalsIgnoreCase(MessageTypes.ENTER.toString())) {
                if (msg.length != 2)
                    throw new IOException();
                Player player = convertToPlayer(msg[1]);
                if (!game.contains(player.getName()))
                    playerJoined(key, player);
            } 
            
            else {
                view.print("unknown: " + msg[0]);
            }
        } catch (IOException e) {
            view.print(e.getCause().toString());
        }
    }
    
    private void handleJoin(SelectionKey key, Player player) throws IOException {
        StringJoiner message = new StringJoiner(Constants.CONTENT_DELIMITER);
        for (SelectionKey k : selector.keys()) {
            if (k.channel() instanceof SocketChannel && key.isValid() && k != key) {
                SocketChannel socket = (SocketChannel) k.channel();
                InetSocketAddress test = (InetSocketAddress) socket.getRemoteAddress();
                //System.out.println("sending key " + test.getHostString());
                Player play = (Player) k.attachment();
                play.setHost(test.getHostString());
                StringJoiner temp = new StringJoiner(Constants.FIELD_DELIMITER);
                if( player != null) {
                    temp.add(play.getName());
                    temp.add(play.getHost());
                    temp.add(Integer.toString(play.getPort()));
                }
                message.add(temp.toString());
            }
        }
        player.addMessage(prepareMessage(MessageTypes.LOBBY + Constants.TYPE_DELIMITER + message.toString()));
        playerJoined(key, player);
        key.interestOps(SelectionKey.OP_WRITE);
        
    }
    
    private Player convertToPlayer(String data) throws IOException {
        try {
            String[] fields = data.split(Constants.FIELD_DELIMITER);
            return new Player (fields[0], fields[1], Integer.parseInt(fields[2]));
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IOException("faulty message");
    }
    
    private void playerJoined(SelectionKey key, Player player) {
        try {
            key.attach(player);
            view.setInGame(true);
            game.addPlayer(player);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    private String selfAsData() throws IOException {
        InetSocketAddress socket = (InetSocketAddress) serverChannel.getLocalAddress();
                    StringJoiner joiner = new StringJoiner(Constants.FIELD_DELIMITER);
                    joiner.add(game.getName());
                    joiner.add(socket.getHostString());
                    joiner.add(Integer.toString(port));
        return joiner.toString();
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
           if(timeToEnter) {
               sendBroadcast(MessageTypes.ENTER.toString() + Constants.TYPE_DELIMITER + selfAsData());
               timeToEnter = false;
           }
           
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
                            Player player = (Player) key.attachment();
                            player.addMessage(prepareMessage(MessageTypes.JOIN.toString() +
                                    Constants.TYPE_DELIMITER + selfAsData()));
                            key.interestOps(SelectionKey.OP_WRITE);
                            join = false;
                        }else 
                        timeToEnter = true;

                        
                    } else if(key.isReadable()) {
                        receiveMessage(key);
                    } else if(key.isWritable()) {
                        sendToNode(key);
                    }
                }
            }
    }
    
    private void connect(InetSocketAddress endpoint, String name) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(endpoint);
        channel.register(selector, SelectionKey.OP_CONNECT);
        Player player = new Player(name, endpoint.getHostString(), endpoint.getPort());
        game.addPlayer(player);
        channel.keyFor(selector).attach(player);
        System.out.println("connecting to: " + player.getHost() + " port: " + player.getPort());
    }
    
    private void connect(Player player) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress("localhost", player.getPort()));
        channel.register(selector, SelectionKey.OP_CONNECT);
        game.addPlayer(player);
        channel.keyFor(selector).attach(player);
        System.out.println("connecting to: " + player.getHost() + " port: " + player.getPort());
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
