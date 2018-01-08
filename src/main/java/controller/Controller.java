/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

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
    
    public void hostGame() {
        server = new Server(view);
    }
    
    public void joinGame(String node) {
        server = new Server(view, node);
    }
}
