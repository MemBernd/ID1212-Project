/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.util.ArrayList;
import java.util.List;
import protocol.GameSelections;

/**
 *
 * @author Bernardo
 */
public class Game {

    private List<Player> players = new ArrayList();
    private int score = 0;
    private GameSelections choice;
    private String name;
    
    public Game(String name) {
        this.name = name;
    }
    public void addPlayer(Player player) {
        players.add(player);
    }
    
    public void removePlayer(Player player) {
        players.remove(player);
    }
    
    public int getScore() {
        return score;
    }
    
    public void setChoice (GameSelections choice) {
        this.choice = choice;
    }
    
    public boolean isRoundOver() {
        if(choice == null)
            return false;
        for (Player player : players) {
            if (player.getChoice() == null)
                return false;
        }
        return true;
    }
    
    public int pointsAwarded() {
        int scissors = 0;
        int rocks = 0;
        int papers = 0;
        switch(choice) {
            case ROCK:
                rocks ++;
                break;
            case PAPER:
                papers ++;
                break;
            case SCISSOR:
                scissors ++;
                break;
                
        }
        for (Player player : players) {
            switch(player.getChoice()) {
                case ROCK:
                    rocks ++;
                    break;
                case PAPER:
                    papers ++;
                    break;
                case SCISSOR:
                    scissors ++;
                    break;
            }
        }
        if (scissors > 0 && rocks > 0 && papers > 0)
            return 0;
        switch(choice) {
            case ROCK:
                if(papers > 0)
                    return 0;
                return scissors;
            case PAPER:
                if(scissors > 0)
                    return 0;
                return rocks;
            case SCISSOR:
               if(rocks > 0)
                    return 0;
                return papers;
                
        }
        return -1;
    }
    
    public void newRound() {
        choice = null;
        for (Player player : players) {
            player.setChoice(null);
        }
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
}
