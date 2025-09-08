package com.google.gwt.sample.noting.server;

//questa classe rapprsenta il gestore del lock lato server

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gwt.sample.noting.shared.LockStatus;
import com.google.gwt.sample.noting.shared.LockToken;

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
    public synchronized LockStatus status(int noteId) {
        long now = System.currentTimeMillis();
        Entry entry = lockMap.get(noteId);

        if (entry == null || entry.expiresAtMillis < now) {
            // Nessun lock attivo o il lock è scaduto
            lockMap.remove(noteId);
            entry = null;
        } 
        if(entry == null) return new LockStatus(noteId, false, null, null);
            // Lock attivo
             return new LockStatus(noteId, true, entry.username, new Date(entry.expiresAtMillis));
    }

    // Tenta di acquisire il lock per l'utente specificato
    public synchronized LockToken tryAcquire(int noteId, String username) {
        long now = System.currentTimeMillis();
        Entry entry = lockMap.get(noteId);
           System.out.println("[LM] tryAcquire note=" + noteId + " reqUser=" + username +
        " existing=" + (entry != null ? (entry.username + " exp=" + entry.expiresAtMillis) : "null") +
        " now=" + now);

        if (entry != null && entry.expiresAtMillis >= now &&
            !entry.username.equals(username)) {
                        System.out.println("[LM] deny: another user holds valid lock");

                return null;            
        }
        
        // Se l'entry esiste ma è scaduta, pulisci
        if (entry != null && entry.expiresAtMillis < now) {
            lockMap.remove(noteId);
        }
        long exp = now + LEASE_MS;
        lockMap.put(noteId, new Entry(username, exp));
            System.out.println("[LM] grant lock to " + username + " until " + exp);

        return new LockToken(noteId, username, new Date(exp));
    }

    //rinnova la scadenza di un lock già esistente

    public synchronized LockToken renew(int noteId, String username) {
        long now = System.currentTimeMillis();
        Entry entry = lockMap.get(noteId);

        if (entry == null || entry.expiresAtMillis < now || !entry.username.equals(username)) return null;
        entry.expiresAtMillis = now + LEASE_MS;
        return new LockToken(noteId, username, new Date(entry.expiresAtMillis));
    }


    //rilascia il lock 
    synchronized void release(int noteId, String username) {
        //cerca il lock attuale della nota
        Entry entry = lockMap.get(noteId);
        // se il lock esiste ed è dello stesso utente che ha chiesto il rilascio
        if (entry != null && entry.username.equals(username)) {
            lockMap.remove(noteId);
        }
    }



}