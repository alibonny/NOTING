package com.google.gwt.sample.noting.shared;

import java.io.Serializable;

public class User implements Serializable {
    private String username;

    // Costruttore vuoto per la serializzazione
    public User() {}

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}