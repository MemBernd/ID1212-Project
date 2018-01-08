/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;


import controller.Controller;
import java.io.IOException;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;

import java.util.Scanner;
import model.MenuChanger;
import model.OutputHandler;
import protocol.GameSelections;

/**
 *
 * @author Bernardo
 */
public class Interpreter extends Thread implements MenuChanger {
    private final Scanner consoleInput = new Scanner(System.in);
    private static final String PROMPT = "> ";
    private boolean exit = false;
    private boolean cancel = false;
    private boolean inGame = false;
    private final PrintUsingThread put = new PrintUsingThread();
    private final ConsoleOutput output = new ConsoleOutput();
    private String playerName;
    private int port;
    Controller controller = new Controller(this);
    
    private static final String help = "commands: " +
                "\nquit: quit" +
                "\nhelp: help";
    private static final String playMessage = "Select: " +
                        "\n[rock]" +
                        "\n[paper]" +
                        "\n[scissor]" +
                        "\n[cancel] to leave";


    @Override
    public void run() {
        put.println(help);
        put.println("Type your player name:");
        playerName = readLine();
        boolean incorrect = true;
        while(incorrect) {
            put.println("Port to use to listen: ");
            try {
                port = Integer.parseInt(readLine());
                controller.init(port, playerName);
                incorrect = false;
            } catch (Exception e) {
                put.println(e.getMessage());
            }
        }
        startMenu();
    }

    @Override
    public void startMenu() {
        
        while(!exit) {
            put.println("Start new game (new) or join existing one (join)");
            CmdHandling cmd = new CmdHandling(readLine());
            //System.out.println(cmd.getCmd());
            switch (cmd.getCmd()) {
                case QUIT:
                    exit = true;
                    break;
                case NEW:
                    newGame();
                    cancel = false;
                    break;
                case JOIN:
                    joinGame();
                    cancel = false;
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
        try {
            controller.hostGame();
            put.println("waiting for player to connect. [cancel] to return to startmenu");
            while(!cancel) {
                CmdHandling cmd = new CmdHandling(readLine());
                //System.out.println(cmd.getCmd());
                switch (cmd.getCmd()) {
                    case QUIT:
                        exit = true;
                        break;
                    case CANCEL:
                        cancel = true;
                        controller.reset();
                        break;
                    case HELP:
                        put.println(help);
                        break;
                    default:
                        if (inGame) {
                            game(cmd);
                        } else
                            System.out.println("invalid command, try again");
                        break;
                }
            }
        } catch (Exception e) {
            put.println(e.getMessage());
        }
    }
    
    private void joinGame() {
        boolean incorrect = true;
        String[] text;
         while(incorrect) {
            put.println("address:port of a node playing:");
            try {
                text = readLine().split(":");
                port = Integer.parseInt(text[1]);
                incorrect = false;
                controller.joinGame(text[0], port);
                
                put.println("waiting to join. [cancel] to return to startmenu");
                while(!cancel) {
                    CmdHandling cmd = new CmdHandling(readLine());
                    //System.out.println(cmd.getCmd());
                    switch (cmd.getCmd()) {
                        case QUIT:
                            exit = true;
                            break;
                        case CANCEL:
                            cancel = true;
                            controller.reset();
                            break;
                        case HELP:
                            put.println(help);
                            break;
                        default:
                            if (inGame) {
                                
                                game(cmd);
                            } else
                                System.out.println("invalid command, try again");
                            break;
                    }
                }
            } catch (Exception e) {
                put.println("incorrect input");
            }
        }
        
    }
    
    private void game(CmdHandling cmd) throws IOException {
        while(!cancel) {
            
            //System.out.println(cmd.getCmd());
            switch (cmd.getCmd()) {
                case QUIT:
                    exit = true;
                    break;
                case CANCEL:
                    cancel = true;
                    controller.reset();
                    break;
                case HELP:
                    output.printMessage(help);
                    break;
                default:
                    try {
                        GameSelections choice = GameSelections.valueOf(cmd.getEnteredCommand().toUpperCase());
                        controller.sendChoice(choice);
                    } catch (Exception e) {
                        output.printMessage("Invalid command. Try again.");
                    }
                    break;
            }
            cmd = new CmdHandling(readLine());
        }
    }
    
    public void setInGame(Boolean value) {
        output.printMessage("Player connected.");
        if(!inGame)
            printPlayMessage();
        
        inGame = value;
    }
    
    public void printPlayMessage() {
        output.printMessage(playMessage);
    }
    
    private String readLine() {
        put.print(PROMPT);
        return consoleInput.nextLine();
    }
    
    public void print (String message) {
        put.println(message);
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
