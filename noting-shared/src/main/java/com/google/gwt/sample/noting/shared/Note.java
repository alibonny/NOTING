package com.google.gwt.sample.noting.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Note implements Serializable {
    private String title;
    private String content;
    private String ownerUsername;
    private int id;
    private Stato stato;
    private List<String> utentiCondivisi = new ArrayList<>();

    public enum Stato {
        Privata,Condivisa,CondivisaSCR
    }

    // Costruttore vuoto necessario per la serializzazione GWT
    public Note() {}

    public Note(String title, String content, Stato stato) {
        this.title = title;
        this.content = content;
        this.stato = stato;
        this.id = 0; // ID temporaneo: sarà assegnato automaticamente lato server
    }

    public Note(String title, String content, Stato stato, List<String> utentiCondivisi, String ownerUsername) {
        this.title = title;
        this.content = content;
        this.stato = stato;
        setUtentiCondivisi(utentiCondivisi); 
        this.ownerUsername = ownerUsername;
        this.id = 0; // ID temporaneo: sarà assegnato automaticamente lato server
    }

    public Note(String title, String content, Stato stato ,String ownerUsername) {
        this.id = 0; // ID temporaneo: sarà assegnato automaticamente lato server
        this.title = title;
        this.content = content;
        this.stato = stato;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Stato getStato() {
        return stato;
    }   
    
    public void setStato(Stato stato) {
        this.stato = stato;
    }

    public List<String> getUtentiCondivisi() { return utentiCondivisi; }

    public void setUtentiCondivisi(List<String> utentiCondivisi) {
        // mai null internamente
        this.utentiCondivisi = (utentiCondivisi == null) ? new ArrayList<>() : new ArrayList<>(utentiCondivisi);
    }


}