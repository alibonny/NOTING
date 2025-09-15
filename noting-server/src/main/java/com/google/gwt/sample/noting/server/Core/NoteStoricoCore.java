package com.google.gwt.sample.noting.server.Core;


import java.util.List;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteMemento;

public interface NoteStoricoCore {
        /** Salva un memento della nota (titolo+contenuto+data). Può applicare una policy di size interna. */
    void saveMemento(int noteId, Note nota);

    /** Ritorna la lista dei memento (copia difensiva). */
    List<NoteMemento> getHistory(int noteId);

    /** Ritorna una singola entry per indice, oppure null se out-of-range. */
    NoteMemento getEntry(int noteId, int historyIndex);

    Note restoreNoteFromMemento(int noteId,NoteMemento noteMemento); // applica memento alla nota e persiste

}
