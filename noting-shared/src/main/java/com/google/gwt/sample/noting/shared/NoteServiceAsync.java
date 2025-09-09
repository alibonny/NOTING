/*package com.google.gwt.sample.noting.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

//Viene usata da Client
// è un interfaccia asincrona, che GWT GENERA AUTOMATICAMENTE

public interface NoteServiceAsync {
    void login(String username, String password, AsyncCallback<User> callback);
    void register(String username, String password, AsyncCallback<User> callback);
    void logout(AsyncCallback<Void> async);
    void creazioneNota(String titolo, String contenuto,Note.Stato stato, List<String> utentiCondivisi ,AsyncCallback<Void> callback);
    void eliminaUltimaNota(AsyncCallback<Void> callback);
    void getNoteUtente(AsyncCallback<List<Note>> callback);
    void updateNota(Note notaModificata, AsyncCallback<Void> callback);
    void eliminaNota(String username, int notaId, AsyncCallback<Void> callback);
    void searchNotes(String query, AsyncCallback<List<Note>> callback);
    void getAllUsernames(AsyncCallback<List<String>> callback);
    void cercaUtente(String username, AsyncCallback<Boolean> callback);

    
    void getCondiviseConMe(AsyncCallback<List<Note>> callback);  
    
    void creaCopiaNota(String username, int notaId, AsyncCallback<Void> callback);
    void annullaCondivisione(String username, int notaId, AsyncCallback<Void> callback);
    void rimuoviUtenteCondivisione(int notaId, String username, AsyncCallback<Void> callback);


    // metodi per la gestione del lock
    void getLockStatus(int noteId, AsyncCallback<LockStatus> callback);
    void tryAcquireLock(int noteId, AsyncCallback<LockToken> callback);
    void renewLock(int noteId, AsyncCallback<LockToken> callback);
    void releaseLock(int noteId, AsyncCallback<Void> callback);
}*/
package com.google.gwt.sample.noting.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface NoteServiceAsync {
    void annullaCondivisione(String username, int notaId, AsyncCallback<Void> arg3);
    void cercaUtente(String username, AsyncCallback<Boolean> arg2);
    void creaCopiaNota(String username, int notaId, AsyncCallback<Void> arg3);
    void creazioneNota(String titolo, String contenuto, Note.Stato stato, List<String> utentiCondivisi, AsyncCallback<Void> arg5);
    void eliminaNota(String username, int notaId, AsyncCallback<Void> arg3);
    void eliminaUltimaNota(AsyncCallback<Void> arg1);
    void getAllUsernames(AsyncCallback<List<String>> arg1);
    void getCondiviseConMe(AsyncCallback<List<Note>> arg1);
    void getLockStatus(int noteId, AsyncCallback<LockStatus> arg2);
    void getNoteUtente(AsyncCallback<List<Note>> arg1);
    void login(String username, String password, AsyncCallback<User> arg3);
    void logout(AsyncCallback<Void> arg1);
    void register(String username, String password, AsyncCallback<User> arg3);
    void releaseLock(int noteId, AsyncCallback<Void> arg2);
    void renewLock(int noteId, AsyncCallback<LockToken> arg2);
    void rimuoviUtenteCondivisione(int notaId, String username, AsyncCallback<Note> arg3);
    void searchNotes(String query, AsyncCallback<List<Note>> arg2);
    void svuotaCondivisioneNota(int notaId, AsyncCallback<Void> arg2);
    void tryAcquireLock(int noteId, AsyncCallback<LockToken> arg2);
    void updateNota(Note notaModificata, AsyncCallback<Void> arg2);
    
    
    
    
    void cercaUtente2(Note nota, String username, AsyncCallback<Boolean> arg) ;
    void aggiungiCondivisione(int noteId, String username, AsyncCallback<Note> callback);
    void getNotaById(int noteId, AsyncCallback<Note> cb);



}