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
public class PrintUsingThread {
    //synchrozize prints

    /**
     *
     * @param output Output to print
     */
    
    public synchronized void print(String output) {
        System.out.print(output);
    }
    
    /**
     * 
     * @param output Output to print with trailing new line
     */
    synchronized void println(String output) {
        System.out.println(output);
    }
}
