package com.google.gwt.sample.stockwatcher.shared;

import java.io.Serializable;

public class User implements Serializable {
    private String username;
    // Non è una buona pratica salvare la password in chiaro, ma per il progetto può essere sufficiente.
    private String password; 

    // Costruttore vuoto necessario per la serializzazione
    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}