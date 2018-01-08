/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.IOException;
import model.MenuChanger;
import model.Server;

/**
 *
 * @author Bernardo
 */
public class Controller {
    MenuChanger view;
    Server server;
    
    public Controller (MenuChanger view) {
        this.view = view;
    }
    
    public void init(int port) {
        server = new Server(view, port);
    }
    
    public void hostGame() {
        server.host();
        new Thread(server).start();
    }
    
    public void joinGame(String node, int port) {
        server.join(node, port);
        new Thread(server).start();
    }
    
    public void reset() throws IOException {
        //cleaning
        server.reset();
    }
}
