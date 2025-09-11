package com.google.gwt.sample.noting.server.Core;

import java.util.List;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;

public interface SharingCore {

    // Directory
    List<String> getAllUsernames();

    // Lookup utente (no self)
    boolean userExistsDifferentFrom(String callerUsername, String candidateUsername) throws NotingException;
    boolean userExistsDifferentFrom(Note nota, String callerUsername, String candidateUsername) throws NotingException;

    // Mutazioni condivisione
    Note addShare(String callerUsername, int noteId, String targetUsername) throws NotingException;           // owner aggiunge
    void removeUserFromShare(String callerUsername, int noteId, String targetUsername) throws NotingException; // owner rimuove
    void clearShareList(String callerUsername, int noteId) throws NotingException;                             // owner svuota

    // Self-service: l’utente rimuove se stesso
    void cancelShareForUser(String username, int noteId) throws NotingException;
}
