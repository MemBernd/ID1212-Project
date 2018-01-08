/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;


import controller.Controller;
import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import model.MenuChanger;
import model.OutputHandler;

/**
 *
 * @author Bernardo
 */
public class Interpreter extends Thread implements MenuChanger {
    private final Scanner consoleInput = new Scanner(System.in);
    private static final String PROMPT = "> ";
    private boolean exit = false;
    private final PrintUsingThread put = new PrintUsingThread();
    private final ConsoleOutput output = new ConsoleOutput();
    private String playerName;
    Controller controller = new Controller(this);
    
    private static final String help = "commands: " +
                "\nquit: quit" +
                "\nhelp: help";


    @Override
    public void run() {
        put.println(help);
        put.println("Type your player name:");
        playerName = readLine();
        put.println(playerName);
        startMenu();
    }

    @Override
    public void startMenu() {
        put.println("Start new game (new) or join existing one (join)");
        while(!exit) {
            CmdHandling cmd = new CmdHandling(readLine());
            //System.out.println(cmd.getCmd());
            switch (cmd.getCmd()) {
                case QUIT:
                    exit = true;
                    break;
                case NEW:
                    newGame();
                    break;
                case JOIN:
                    joinGame();

                    break;
                case HELP:
                    put.println(help);
                    break;
                default:    
                    System.out.println("invalid command, try again");
                    break;
            }
        }
        
    }
    
    private void newGame() {
        controller.hostGame();
    }
    
    private void joinGame() {
        put.println("address of a node playing:");
        controller.joinGame(readLine());
    }
    
    private String readLine() {
        put.print(PROMPT);
        return consoleInput.nextLine();
    }

    private class ConsoleOutput implements OutputHandler{

        @Override
        public void printMessage (String output) {
            put.println(output);
            put.print(PROMPT);
        }
        
        @Override
        public void printWithoutPrompt(String output) {
            put.println(output);
        }
    }
}
