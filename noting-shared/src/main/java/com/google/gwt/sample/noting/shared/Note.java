package com.google.gwt.sample.noting.shared;

import java.io.Serializable;

public class Note implements Serializable {
    private String title;
    private String content;
    private String ownerUsername;

    // Costruttore vuoto necessario per la serializzazione GWT
    public Note() {}

    public Note(String title, String content, String ownerUsername) {
        this.title = title;
        this.content = content;
        this.ownerUsername = ownerUsername;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }
}