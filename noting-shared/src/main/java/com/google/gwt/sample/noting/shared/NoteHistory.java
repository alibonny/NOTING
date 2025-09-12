package com.google.gwt.sample.noting.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// classe di cronologia dello stato di una nota (massimo 3)
public class NoteHistory implements Serializable {
    private List<NoteMemento> history = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 3;

    public void saveState(Note note) {
        if (history.size() >= MAX_HISTORY_SIZE) {
            history.remove(0);
        }
        history.add(note.createMemento());
    }

    public NoteMemento getMemento(int index) {
        return history.get(index);
    }

    public List<NoteMemento> getHistory() {
        return new ArrayList<>(history);
    }
}