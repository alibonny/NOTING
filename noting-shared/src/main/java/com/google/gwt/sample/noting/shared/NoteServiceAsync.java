package com.google.gwt.sample.noting.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

//Viene usata da Client
// è un interfaccia asincrona, che GWT GENERA AUTOMATICAMENTE

public interface NoteServiceAsync {
    void login(String username, String password, AsyncCallback<User> callback);
    void register(String username, String password, AsyncCallback<User> callback);
    void logout(AsyncCallback<Void> async);
    void creazioneNota(String titolo, String contenuto,Note.Stato stato ,AsyncCallback<Void> callback);
    void eliminaUltimaNota(AsyncCallback<Void> callback);
    void getNoteUtente(AsyncCallback<List<Note>> callback);
    void updateNota(Note notaModificata, AsyncCallback<Void> callback);
    void eliminaNota(String username, int notaId, AsyncCallback<Void> callback);
    void searchNotes(String query, AsyncCallback<List<Note>> callback);
}