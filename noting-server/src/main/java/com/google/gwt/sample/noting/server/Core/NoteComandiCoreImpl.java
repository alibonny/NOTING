package com.google.gwt.sample.noting.server.Core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.google.gwt.sample.noting.server.DBManager;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;

public class NoteComandiCoreImpl implements NoteComandiCore {

    @Override
    public void creazioneNota(String owner, String titolo, String contenuto, Note.Stato stato, List<String> utentiCondivisi, List<String> tags)
        throws NotingException {
          if (owner == null || owner.isBlank()) {
            throw new NotingException("Utente non valido (owner mancante).");
        }
        if (titolo == null || titolo.trim().isEmpty()) {
            throw new NotingException("Il titolo non può essere vuoto.");
        }
         if (contenuto == null || contenuto.trim().isEmpty()) {
            throw new NotingException("Il contenuto non può essere vuoto.");
        }
        titolo = titolo.trim();
        contenuto = contenuto.trim();
        if (stato == null) stato = Note.Stato.Privata;

        // normalizza destinatari (no null, no vuoti, no owner, solo utenti esistenti)
        List<String> destinatari = (utentiCondivisi == null) ? List.of()
                : utentiCondivisi.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.equals(owner))
                    .distinct()
                    .filter(u -> DBManager.getUsersDatabase().containsKey(u))
                    .collect(Collectors.toList());

        // ID atomico
        int id = DBManager.nextNoteId();

        Note n = new Note(titolo, contenuto, stato);
        n.setId(id);
        n.setOwnerUsername(owner);
        n.setUtentiCondivisi(destinatari);

        // salva su notesByOwner (copia difensiva)
        ConcurrentMap<String, List<Note>> notesByOwner = DBManager.getNotesDatabase();
        List<Note> list = notesByOwner.get(owner);
        list = (list == null) ? new ArrayList<>() : new ArrayList<>(list);
        list.add(n);
        notesByOwner.put(owner, list);

        // salva sugli indici globali
        DBManager.getNoteById().put(id, n);
        DBManager.getListaCondivisione().put(id, new ArrayList<>(destinatari));

        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                DBManager.addTagToNote(n.getId(), tag);
            }
        }

        DBManager.commit();  
    }

    @Override
     public void updateNote(String caller, Note notaModificata) throws NotingException {
        if (caller == null || caller.isBlank()) throw new NotingException("Utente non valido.");
        if (notaModificata == null) throw new NotingException("Nota mancante.");
        final int noteId = notaModificata.getId();
        if (noteId <= 0) throw new NotingException("ID nota non valido.");

        final ConcurrentMap<Integer, Note> noteById = DBManager.getNoteById();
        final ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        final ConcurrentMap<Integer, List<String>> shareMap = DBManager.getListaCondivisione();

        // recupera e tieni owner originale
        Note esistente = noteById.get(noteId);
        if (esistente == null) throw new NotingException("Nota non trovata (ID=" + noteId + ").");
        final String owner = esistente.getOwnerUsername();

        // prepara nuovi valori
        String newTitle   = notaModificata.getTitle();
        String newContent = notaModificata.getContent();
        Note.Stato newStato = notaModificata.getStato();

        List<String> newDest = (notaModificata.getUtentiCondivisi() == null ? List.<String>of()
                : notaModificata.getUtentiCondivisi()).stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(s -> !s.equals(owner))
            .distinct()
            .collect(Collectors.toList());

        if (newStato == Note.Stato.Privata || newDest.isEmpty()) {
            newDest = new ArrayList<>();
        }

        // crea nota aggiornata mantenendo id/owner
        Note aggiornata = new Note(newTitle, newContent, newStato, newDest, owner);
        aggiornata.setId(noteId);
        aggiornata.setOwnerUsername(owner);

        // indice globale
        noteById.put(noteId, aggiornata);

        // lista dell’owner
        synchronized (owner.intern()) {
            List<Note> userNotes = notesDB.get(owner);
            if (userNotes == null) userNotes = new ArrayList<>();
            boolean replaced = false;
            for (int i = 0; i < userNotes.size(); i++) {
                if (userNotes.get(i).getId() == noteId) {
                    userNotes.set(i, aggiornata);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) userNotes.add(aggiornata);
            notesDB.put(owner, userNotes);
        }



        // condivisione
        if (aggiornata.getStato() == Note.Stato.Privata || aggiornata.getUtentiCondivisi().isEmpty()) {
            shareMap.remove(noteId);
        } else {
            shareMap.put(noteId, aggiornata.getUtentiCondivisi());
        }

        DBManager.commit();
    }

    @Override
    public void deleteNote(String owner, int notaId) throws NotingException{
        if (owner == null || owner.isBlank()) throw new NotingException("Utente non autenticato.");
        if (notaId <= 0) throw new NotingException("ID nota non valido.");

        // verifica esistenza e proprietà
        Note n = DBManager.getNoteById().get(notaId);
        if (n == null) throw new NotingException("Nota non trovata.");
        if (!owner.equals(n.getOwnerUsername())) {
            throw new NotingException("Solo il proprietario può eliminare la nota.");
        }

        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        synchronized (owner.intern()) {
            List<Note> userNotes = notesDB.get(owner);
            if (userNotes != null) {
                userNotes.removeIf(nn -> nn.getId() == notaId);
                notesDB.put(owner, userNotes);
            }
        }

        DBManager.getNoteById().remove(notaId);
        DBManager.getListaCondivisione().remove(notaId);

        DBManager.commit();
    }

    @Override
    public void copyNote(String caller, int notaId) throws NotingException{
        if (caller == null || caller.isBlank()) throw new NotingException("Utente non valido.");
        if (notaId <= 0) throw new NotingException("ID nota non valido.");

        Note sorgente = DBManager.getNoteById().get(notaId);
        if (sorgente == null) throw new NotingException("Nota con ID " + notaId + " non trovata.");

        boolean hasAccess = caller.equals(sorgente.getOwnerUsername());
        if (!hasAccess) {
            List<String> sharedWith = DBManager.getListaCondivisione().get(notaId);
            hasAccess = (sharedWith != null && sharedWith.contains(caller));
        }
        if (!hasAccess) throw new NotingException("Non hai i permessi per copiare questa nota.");

        String titoloCopia = sorgente.getTitle() + " (Copia)";
        Note copia = new Note(titoloCopia, sorgente.getContent(), Note.Stato.Privata, List.of(), caller);
        copia.setOwnerUsername(caller);

        int id = DBManager.nextNoteId();
        copia.setId(id);

        ConcurrentMap<String, List<Note>> notesByOwner = DBManager.getNotesDatabase();
        List<Note> list = notesByOwner.get(caller);
        list = (list == null) ? new ArrayList<>() : new ArrayList<>(list);
        list.add(copia);
        notesByOwner.put(caller, list);

        // salva sugli indici globali
        DBManager.getNoteById().put(id, copia);
        DBManager.getListaCondivisione().put(id, new ArrayList<>());

        DBManager.commit();


    }

}


    

