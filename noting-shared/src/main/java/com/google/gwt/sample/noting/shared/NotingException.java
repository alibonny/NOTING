package com.google.gwt.sample.noting.shared;

import java.io.Serializable;

/**
 * Eccezione personalizzata e serializzabile per la gestione degli errori
 * nell'applicazione Noting.
 */
public class NotingException extends Exception implements Serializable {

    // Costruttore vuoto necessario per la serializzazione GWT
    public NotingException() {
        super();
    }

    public NotingException(String message) {
        super(message);
    }
}