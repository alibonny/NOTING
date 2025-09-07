package com.google.gwt.sample.noting.shared;

import java.io.Serializable;
import java.util.Date;

// questa classe rappresenta il token di lock che il server restituisce
// quando un utente ottiene o rinnova un lock su una nota

public class LockToken implements Serializable {

     private int noteId;
    private String username; // chi possiede il lock
    private Date expiresAt;  // scadenza attuale del lock

    // Obbligatorio per GWT-RPC
    public LockToken() {}

    public LockToken(int noteId, String username, Date expiresAt) {
        this.noteId = noteId;
        this.username = username;
        this.expiresAt = expiresAt;
    }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    @Override
    public String toString() {
        return "LockToken{noteId=" + noteId +
               ", username='" + username + '\'' +
               ", expiresAt=" + expiresAt + '}';
    }

    
}
