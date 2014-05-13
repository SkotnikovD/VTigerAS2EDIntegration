/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.skodev.exceptions;

/**
 *
 * @author HOME
 */
public class AS2Exception extends Exception {

    /**
     * Creates a new instance of <code>AS2Exception</code> without detail
     * message.
     */
    public AS2Exception() {
    }

    /**
     * Constructs an instance of <code>AS2Exception</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public AS2Exception(String msg) {
        super(msg);
    }

    public AS2Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public AS2Exception(Throwable cause) {
        super(cause);
    }
    
}
