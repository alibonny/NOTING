package com.google.gwt.sample.noting.server.Core;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.google.gwt.sample.noting.server.DBManager;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;

public class NoteCercaCoreImpl implements NoteCercaCore {

    @Override
    public List<Note> getNotesOf(String owner) throws NotingException {
        if (owner == null || owner.isBlank()) throw new NotingException("Utente non valido.");
        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        List<Note> userNotes = notesDB.get(owner);
        return (userNotes == null) ? new ArrayList<>() : new ArrayList<>(userNotes); // copia difensiva
    }

    @Override
    public Note getById(int noteId) throws NotingException {
        if (noteId <= 0) throw new NotingException("ID nota non valido.");
        Note n = DBManager.getNoteById().get(noteId);
        if (n == null) throw new NotingException("Nota inesistente.");

        // ricostruisci la lista condivisione “viva” dalla share map
        List<String> lst = DBManager.getListaCondivisione().get(noteId);
        List<String> condivisi = (lst == null) ? new ArrayList<>() : new ArrayList<>(lst);

        // crea un DTO per non mutare l’oggetto salvato
        Note dto = new Note(n.getTitle(), n.getContent(), n.getStato(), condivisi, n.getOwnerUsername());
        dto.setId(n.getId());
        dto.setOwnerUsername(n.getOwnerUsername());
        return dto;
    }

    @Override
    public List<Note> search(String owner, String query) throws NotingException {
        if (owner == null || owner.isBlank()) throw new NotingException("Utente non valido.");
        if (query == null || query.trim().isEmpty()) throw new NotingException("Query di ricerca mancante.");

        String q = query.toLowerCase();
        List<Note> noteUtente = DBManager.getNotesDatabase().get(owner);
        if (noteUtente == null) return new ArrayList<>();

        return noteUtente.stream()
                .filter(n -> {
                    String t = (n.getTitle() == null) ? "" : n.getTitle();
                    String c = (n.getContent() == null) ? "" : n.getContent();
                    return t.toLowerCase().contains(q) || c.toLowerCase().contains(q);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Note> sharedWith(String username) throws NotingException {
        if (username == null || username.isBlank()) throw new NotingException("Utente non valido.");

        ConcurrentMap<Integer, List<String>> share = DBManager.getListaCondivisione();
        Map<Integer, Note> noteById = DBManager.getNoteById();

        List<Note> result = new ArrayList<>();
        if (share != null) {
            for (Map.Entry<Integer, List<String>> e : share.entrySet()) {
                Integer noteId = e.getKey();
                List<String> sharedWith = e.getValue();
                if (sharedWith != null && sharedWith.contains(username)) {
                    Note note = noteById.get(noteId);
                    if (note != null && !username.equals(note.getOwnerUsername())) {
                        result.add(note);
                    }
                }
            }
        }
        return result;
    }
}
