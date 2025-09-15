package com.google.gwt.sample.noting.client;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.User;

public interface HomeViewListener {
    void onCreateNote();
    void onLogout();
    void onNoteSelected(Note note, User user);
    void onSearch(String query);
    
    void myNotesSelected();
    void condiviseConMeSelected();

    void onFilterSearch(String currentView, String filterType, String filterValue);
}