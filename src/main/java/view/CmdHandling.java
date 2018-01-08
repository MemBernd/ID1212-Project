/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

/**
 *
 * @author Bernardo
 */
public class CmdHandling {
    private Commands command;
    private String enteredCommand;
    private String[] arguments;
    private static final String DELIM = " ";
    
    CmdHandling (String line) {
        enteredCommand = line;
        parseCommand(line);
    }
    
    public Commands getCmd() {
        return command;
    }
    
    public String getEnteredCommand() {
        return enteredCommand;
    }
    
    public String getArgument(int index) {
        if (arguments == null) {
            return null;
        } else if (index >= arguments.length) {
            return null;
        } else {
            return arguments[index];
        }
    }
    
    private void parseCommand(String line) {
        String[] parts;
        try {
            parts = line.split(DELIM);
            if (parts.length >=1) {
                command = Commands.valueOf(parts[0].toUpperCase());
                arguments = new String[parts.length-1];
                for (int i = 1; i < parts.length; i++) {
                    arguments[i - 1] = parts[i];
                }
            } else {
                //System.out.println("else");
                command = Commands.INVALID;
            }
        } catch (Exception e) {
            //System.out.println("catch");
            command = Commands.INVALID;
        }
    }
    
}
