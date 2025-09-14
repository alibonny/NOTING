package com.google.gwt.sample.noting.client;
import java.util.List;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteMemento;
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

    void onGetNoteHistory(int noteId, AsyncCallback<List<NoteMemento>> callback);
    void onRestoreNote(int noteId, int historyIndex, AsyncCallback<Note> callback);

    void onAddTagToNote(int noteId, String tagName, AsyncCallback<Void> callback);
    void onRemoveTagFromNote(int noteId, String tagName, AsyncCallback<Void> callback);
    void onCreateNewTag(String tagName, AsyncCallback<Void> callback);
}
