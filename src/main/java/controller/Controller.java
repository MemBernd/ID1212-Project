/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.IOException;
import model.Game;
import model.MenuChanger;
import model.Server;
import protocol.GameSelections;

/**
 *
 * @author Bernardo
 */
public class Controller {
    MenuChanger view;
    Server server;
    Game game;
    
    public Controller (MenuChanger view) {
        this.view = view;
    }
    
    public void init(int port, String name) throws IOException {
        game = new Game(name);
        server = new Server(view, port, game);
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
    
    public void sendChoice(GameSelections choice) {
        game.setChoice(choice);
        server.sendChoice();
        if (game.isRoundOver()) {
            view.print("Round: " + game.pointsAwarded() + " Total: " + game.getScore());
            game.newRound();
        }
    }
}
