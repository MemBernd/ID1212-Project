/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author Bernardo
 */
public interface MenuChanger {
    void startMenu();
    void print(String message);
    void setInGame(Boolean set);
    void printPlayMessage();
}
