package com.google.gwt.sample.noting.client;

import com.google.gwt.sample.noting.shared.Note;

public interface HomeViewListener {
    void onCreateNote();
    void onLogout();
    void onNoteSelected(Note note);
}
