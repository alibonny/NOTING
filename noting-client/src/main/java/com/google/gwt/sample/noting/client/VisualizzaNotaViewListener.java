package com.google.gwt.sample.noting.client;
import com.google.gwt.sample.noting.shared.Note;

public interface VisualizzaNotaViewListener {
    void onBack();
    void onSalvaNota(Note nota);
   // void onModificaNota(Note nota);
   // void onStatoNotaChanged(Note nota, Note.Stato nuovoStato);
    void onEliminaNota(Note nota);
    void onCreaUnaCopia(Note nota);
    void onAnnullaCondivisione(Note nota);
    void onRimuoviUtenteCondivisione(Note nota, String username);

    void onRichiediLock(int noteId);


}
