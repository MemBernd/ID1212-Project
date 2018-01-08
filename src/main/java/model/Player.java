/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import protocol.GameSelections;

/**
 *
 * @author Bernardo
 */
public class Player {
    private final Queue<ByteBuffer> messageToSend = new ArrayDeque<>();
    
    private String name;
    private String host;
    private int port;
    private GameSelections choice;
    
    public Player(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
        choice = null;
    }
    
    public void addMessage(ByteBuffer msg) {
        getMessageToSend().add(msg);
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the choice
     */
    public GameSelections getChoice() {
        return choice;
    }

    /**
     * @param choice the choice to set
     */
    public void setChoice(GameSelections choice) {
        this.choice = choice;
    }

    /**
     * @return the messageToSend
     */
    public Queue<ByteBuffer> getMessageToSend() {
        return messageToSend;
    }
}
