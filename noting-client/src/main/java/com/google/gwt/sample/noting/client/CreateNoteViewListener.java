package com.google.gwt.sample.noting.client;

import com.google.gwt.sample.noting.shared.Note;

public interface CreateNoteViewListener {
    void onSave(String titolo, String contenuto, Note.Stato stato);
    void mostraUtenti();
    void onBack();
}
