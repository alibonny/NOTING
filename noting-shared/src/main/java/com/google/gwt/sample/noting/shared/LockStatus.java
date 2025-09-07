package com.google.gwt.sample.noting.shared;

import java.io.Serializable;
import java.util.Date;


// questa classe rappresenta lo stato di lock che il server restituisce
// per dire se la nota è bloccata, da chi e fino a quando

public class LockStatus implements Serializable{
    private int noteId;
    private boolean locked; // true se esiste un lock valido
    private String proprietarioLock; // username dell'utente che ha il lock
    private Date expiresDate; // data di scadenza del lock

    public LockStatus() {
        // Costruttore vuoto necessario per la serializzazione GWT
    }

    public LockStatus(int noteId, boolean locked, String proprietarioLock, Date expiresDate) {
        this.noteId = noteId;
        this.locked = locked;
        this.proprietarioLock = proprietarioLock;
        this.expiresDate = expiresDate;
    }

    public int getNoteId() {
        return noteId;
    }
    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }
    public boolean isLocked() {
        return locked;
    }
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    public String getProprietarioLock() {
        return proprietarioLock;
    }
    public void setProprietarioLock(String proprietarioLock) {
        this.proprietarioLock = proprietarioLock;
    }
    public Date getExpiresDate() {
        return expiresDate;
    }
    public void setExpiresDate(Date expiresDate) {
        this.expiresDate = expiresDate;
    }
    @Override
    public String toString() {
        return "LockStatus [noteId=" + noteId + ", locked=" + locked + ", proprietarioLock=" + proprietarioLock
                + ", expiresDate=" + expiresDate + "]";
    }
}
