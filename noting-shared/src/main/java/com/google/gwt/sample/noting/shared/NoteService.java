package com.google.gwt.sample.noting.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("noteService")
public interface NoteService extends RemoteService {
    User login(String username, String password) throws NotingException;
    User register(String username, String password) throws NotingException;
    void logout();
    void creazioneNota(String titolo, String contenuto, Note.Stato stato, List<String>utentiCondivisi) throws NotingException;
    void eliminaUltimaNota();
    List<Note> getNoteUtente() throws NotingException;
    void updateNota(Note notaModificata) throws NotingException;

    void svuotaCondivisioneNota(int notaId) throws NotingException;
    void eliminaNota(String username, int notaId) throws NotingException;
    List<Note> searchNotes(String query) throws NotingException;
    List<String> getAllUsernames();
    boolean cercaUtente(String username) throws NotingException;

    List<Note> getCondiviseConMe() throws NotingException;

    void creaCopiaNota(String username, int notaId) throws NotingException;
    void annullaCondivisione(String username, int notaId) throws NotingException;
    void rimuoviUtenteCondivisione(int notaId, String username) throws NotingException;

    // metodi per la gestione del lock


    // Restuisce lo stato corrent del lock per la nota indicata
    LockStatus getLockStatus(int noteId) throws NotingException;

    // Tenta di acquisire il lock per utente corrente
    LockToken tryAcquireLock(int noteId) throws NotingException;

    //Rinnova il lock posseduto dall'utente corrente
    LockToken renewLock(int noteId) throws NotingException;

    //Rilascia il lock posseduto dall'utente corrente
    void releaseLock(int noteId) throws NotingException;


}



