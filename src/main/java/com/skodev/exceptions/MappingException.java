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

//Exception raises when no Required EANCOM field found  
public class MappingException extends Exception {

    /**
     * Creates a new instance of <code>RequiredFieldMissing</code> without
     * detail message.
     */
    public MappingException() {
    }

    /**
     * Constructs an instance of <code>RequiredFieldMissing</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public MappingException(String msg) {
        super(msg);
    }
}
