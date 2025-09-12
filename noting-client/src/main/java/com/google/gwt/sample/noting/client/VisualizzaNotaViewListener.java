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
    void onRimuoviUtenteCondivisione(Note nota, String username);
    void onGetNoteHistory(int noteId, AsyncCallback<List<NoteMemento>> callback);
    void onRestoreNote(int noteId, int historyIndex, AsyncCallback<Note> callback);
}
