package com.google.gwt.sample.noting.shared;

import java.io.Serializable;
import java.util.Date;

public class NoteMemento implements Serializable {
    private String title;
    private String content;
    private Date timestamp;

    public NoteMemento() {}

    public NoteMemento(String title, String content, Date timestamp) {
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Date getTimestamp() { return timestamp; }
}