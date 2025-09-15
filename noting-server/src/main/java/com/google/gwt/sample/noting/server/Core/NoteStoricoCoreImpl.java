package com.google.gwt.sample.noting.server.Core;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gwt.sample.noting.server.DBManager;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteMemento;

public class NoteStoricoCoreImpl implements NoteStoricoCore {
    
    private final ConcurrentMap<Integer, LinkedList<NoteMemento>> historyMap = new ConcurrentHashMap<>();


    @Override
    public void saveMemento(int noteId, Note nota) {
        if (nota == null) return;

        LinkedList<NoteMemento> history =
                historyMap.computeIfAbsent(noteId, k -> new LinkedList<>());

        // Mantieni al massimo 4 elementi (come tua logica attuale: rimuove il più vecchio)
        if (history.size() >= 4) history.removeFirst();

        history.addLast(new NoteMemento(
                nota.getTitle(),
                nota.getContent(),
                new Date()
        ));
    }

    @Override
    public List<NoteMemento> getHistory(int noteId) {
        LinkedList<NoteMemento> h = historyMap.get(noteId);
        return (h != null) ? new LinkedList<>(h) : new LinkedList<>();
    }

    @Override
    public NoteMemento getEntry(int noteId, int historyIndex) {
        LinkedList<NoteMemento> h = historyMap.get(noteId);
        if (h == null || historyIndex < 0 || historyIndex >= h.size()) return null;
        return h.get(historyIndex);
    }

     @Override
    public Note restoreNoteFromMemento(int noteId, NoteMemento memento) {
          for (List<Note> notes : DBManager.getNotesDatabase().values()) {
            for (Note n : notes) {
                if (n.getId() == noteId) {
                    n.setTitle(memento.getTitle());
                    n.setContent(memento.getContent());
                    return n;
                }
            }
        }
        return null;
    }



}
