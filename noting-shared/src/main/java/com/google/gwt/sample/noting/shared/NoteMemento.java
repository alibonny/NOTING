package com.google.gwt.sample.noting.shared;

import java.io.Serializable;
import java.util.Date;

// CLASSE MEMENTO che conterrà lo stato di una nota in un determinato momento (titolo e testo)
public class NoteMemento implements Serializable {
    private String title;
    private String content;
    private Date timestamp;

    public NoteMemento() {
    }

    public NoteMemento(String title, String content) {
        this.title = title;
        this.content = content;
        this.timestamp = new Date();
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}