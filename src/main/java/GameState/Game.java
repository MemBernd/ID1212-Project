/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GameState;

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
    
    public GameSelections getChoice () {
        return choice;
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
    
    public boolean contains(String name) {
        for (Player player : players) {
            if (name.equals(player.getName()))
                return true;
        }
        return false;
    }
    
    public int pointsAwarded() {
        int scissors = 0;
        int rocks = 0;
        int papers = 0;
        int points = 0;
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
            points = 0;
        switch(choice) {
            case ROCK:
                if(papers > 0)
                    points = 0;
                points = scissors;
                break;
            case PAPER:
                if(scissors > 0)
                    points = 0;
                points = rocks;
                break;
            case SCISSOR:
               if(rocks > 0)
                    points = 0;
                points = papers;
                break;
        }
        score += points;
        return points;
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
