package com.google.gwt.sample.noting.client;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface VisualizzaNotaViewListener {
    void onBack();
    void onSalvaNota(Note nota);
   // void onModificaNota(Note nota);
   // void onStatoNotaChanged(Note nota, Note.Stato nuovoStato);
    void onEliminaNota(Note nota);
    void onCreaUnaCopia(Note nota);
    void onAnnullaCondivisione(Note nota);
    void onRimuoviUtenteCondivisione(Note nota, String username, AsyncCallback<Note> callback);

    void onRichiediLock(int noteId);

    void trovaUtente2(Note nota, String username, AsyncCallback<Boolean> callback);

    void aggiungiCondivisione(int notaId, String username, AsyncCallback<Note> callback);

    void getNotaById(int noteId, AsyncCallback<Note> callback);


}
