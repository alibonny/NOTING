package com.google.gwt.sample.noting.client;

import com.google.gwt.sample.noting.shared.Note;

public interface HomeViewListener {
    void onCreateNote();
    void onLogout();
    void onNoteSelected(Note note);
    void onSearch(String query);


    // DUE METODI PER IL COMPORTAMENTO DEI DUE NUOVI BOTTONI
    void myNotesSelected();
    void condiviseConMeSelected();
}