package com.google.gwt.sample.noting.server;

//questa classe rapprsenta il gestore del lock lato server

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// garantisce che una sola persona alla volta modificichi una nota
// ha un meccanismo di scadenza automatica del lock


final class NoteLockManager {

    static final class Entry {
        final String username;
        volatile long expiresAtMillis;

        Entry(String username, long expiresAtMillis) {
            this.username = username;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private static final long LEASE_MS = 90_000L; // 90 secondi
    private static final NoteLockManager INSTANCE = new NoteLockManager();


    // Mappa che associa l'ID della nota al suo stato di lock
    private final Map<Integer, Entry> lockMap = new ConcurrentHashMap<>();


    private NoteLockManager() {}

    public static NoteLockManager getInstance() {
        return INSTANCE;
    }

    // Controlla lo stato attuale del lock
    public synchronized Status status(int noteId) {
        long now = System.currentTimeMillis();
        Entry entry = lockMap.get(noteId);

        if (entry == null || entry.expiresAtMillis < now) {
            // Nessun lock attivo o il lock è scaduto
            lockMap.remove(noteId);
            entry = null;
        } 
        if(entry == null) return new Status(noteId, false, null, null);
            // Lock attivo
             return new Status(noteId, true, entry.username, new Date(entry.expiresAtMillis));
    }

    // Tenta di acquisire il lock per l'utente specificato
    public synchronized Token tryAcquire(int noteId, String username) {
        long now = System.currentTimeMillis();
        Entry entry = lockMap.get(noteId);

        if (entry == null || entry.expiresAtMillis >= now &&
            !entry.username.equals(username)) {
                return null;            
        }
        long exp = now + LEASE_MS;
        lockMap.put(noteId, new Entry(username, exp));
        return new Token(noteId, username, new Date(exp));
    }

    public synchronized Token renew(int noteId, String username) {
        long now = System.currentTimeMillis();
        Entry entry = lockMap.get(noteId);

        if (entry == null || entry.expiresAtMillis < now || !entry.username.equals(username)) return null;
        entry.expiresAtMillis = now + LEASE_MS;
        return new Token(noteId, username, new Date(entry.expiresAtMillis));
    }

    synchronized void release(int noteId, String username) {
        Entry entry = lockMap.get(noteId);
        if (entry != null && entry.username.equals(username)) {
            lockMap.remove(noteId);
        }
    }

    static final class Status {
        final int noteId; final boolean locked; final String owner; final Date expiresAt;
        Status(int noteId, boolean locked, String owner, Date expiresAt) {
            this.noteId = noteId; this.locked = locked; this.owner = owner; this.expiresAt = expiresAt;
        }
    }
    static final class Token {
        final int noteId; final String username; final Date expiresAt;
        Token(int noteId, String username, Date expiresAt) {
            this.noteId = noteId; this.username = username; this.expiresAt = expiresAt;
        }
    }






    


}