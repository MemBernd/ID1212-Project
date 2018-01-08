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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Constants;

/**
 *
 * @author Bernardo
 */
public class Server implements Runnable {
    private int port = Constants.listeningPort;
    private static final int LINGER = 3000;
    private static final int TIMEOUT_LONG = 1200000;
    private MenuChanger view;
    
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private InetSocketAddress server;

    
    private Boolean join;
    
    public Server(MenuChanger view) {
        join = false;
        this.view = view;
        new Thread(this).start();

    }
    
    public Server(MenuChanger view, String host) {
        join = true;
        this.view = view;
        server = new InetSocketAddress(host, port);
        new Thread(this).start();

    }
    
    @Override
    public void run() {
        try {
            selector = Selector.open();
            if(join)
                join();
            serve();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void receive(SelectionKey key) throws IOException {
        Handler handler = (Handler) key.attachment();
        try {
            handler.receiveMessage();
        } catch (IOException e) {
            handler.disconnect();
            key.cancel();
        }
    }
    
    public void reply(SocketChannel client) {
        client.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }
    
    private void send(SelectionKey key) throws IOException {
        Handler handler = (Handler) key.attachment();
        try {
            handler.sendMessage();
            key.interestOps(SelectionKey.OP_READ);
        } catch(IOException e) {
            handler.disconnect();
            key.cancel();
        }
    }
    
    private void startNewClient(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, new Handler(clientChannel, this));
        clientChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER);
    }
    
    private void serve() throws IOException {
       //selector = Selector.open();
       serverChannel = ServerSocketChannel.open();
       serverChannel.configureBlocking(false);
       serverChannel.bind(new InetSocketAddress(port));
       serverChannel.register(selector, SelectionKey.OP_ACCEPT);
       while (true) {
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
                    } else if(key.isReadable()) {
                        receive(key);
                    } else if(key.isWritable()) {
                        send(key);
                    }
                }
            }
    }
    
    private void join() throws IOException {
        //selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(server);
        channel.register(selector, SelectionKey.OP_CONNECT);
        channel.finishConnect();

    }
    
        private void send(SelectionKey key) {
        ByteBuffer message;
        try {
            synchronized(messageToSend) {
                while((message = messageToSend.poll()) != null) {
                    Thread.sleep(000);
                    channel.write(message);
                    if(message.hasRemaining())
                        return;
                }
             key.interestOps(SelectionKey.OP_READ);   
            }
            
        } catch (IOException ex) {
            output.printMessage("Couldn't send.");
        } catch(CancelledKeyException e) {
            output.printMessage("Program quit while sending");
        } catch (InterruptedException ex) {
            Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    
}
