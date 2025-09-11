package com.google.gwt.sample.noting.server.Core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.gwt.sample.noting.server.DBManager;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;

public class SharingCoreImpl implements SharingCore {

    // =======================
    // Directory
    // =======================
    @Override
    public List<String> getAllUsernames() {
        Map<String, String> userDb = DBManager.getUsersDatabase();
        return new ArrayList<>(userDb.keySet());
    }

    // =======================
    // Lookup utente
    // =======================
    @Override
    public boolean userExistsDifferentFrom(String caller, String candidate) throws NotingException {
        if (caller == null || caller.isBlank()) throw new NotingException("Utente non autenticato.");
        if (candidate == null) return false;
        candidate = candidate.trim();
        if (candidate.isEmpty()) return false;
        if (caller.equals(candidate)) return false;
        return DBManager.getUsersDatabase().containsKey(candidate);
    }

    @Override
    public boolean userExistsDifferentFrom(Note nota, String caller, String candidate) throws NotingException {
        // Nota non serve per la verifica, la firma replica la tua API
        return userExistsDifferentFrom(caller, candidate);
    }

    // =======================
    // Mutazioni condivisione (owner-only)
    // =======================
    @Override
    public Note addShare(String caller, int noteId, String targetUsername) throws NotingException {
        if (caller == null || caller.isBlank()) throw new NotingException("Utente non autenticato.");
        if (targetUsername == null) throw new NotingException("Username mancante.");
        targetUsername = targetUsername.trim();
        if (targetUsername.isEmpty()) throw new NotingException("Username vuoto.");

        ConcurrentMap<Integer, Note> noteById = DBManager.getNoteById();
        Note nota = noteById.get(noteId);
        if (nota == null) throw new NotingException("Nota inesistente.");

        // solo owner può modificare la share list
        String owner = nota.getOwnerUsername();
        if (!caller.equals(owner)) {
            throw new NotingException("Solo il proprietario può modificare la condivisione.");
        }

        if (!DBManager.getUsersDatabase().containsKey(targetUsername)) {
            throw new NotingException("Utente non esistente.");
        }
        if (caller.equals(targetUsername)) {
            throw new NotingException("Non puoi condividere con te stesso.");
        }

        ConcurrentMap<Integer, List<String>> shareMap = DBManager.getListaCondivisione();
        List<String> lista = shareMap.get(noteId);
        if (lista == null) {
            lista = new ArrayList<>();
            shareMap.put(noteId, lista);
        }
        if (!lista.contains(targetUsername)) {
            lista.add(targetUsername);
            // riflette sulla nota e sugli indici
            nota.setUtentiCondivisi(new ArrayList<>(lista));
            noteById.put(noteId, nota);

            synchronized (owner.intern()) {
                List<Note> userNotes = DBManager.getNotesDatabase().get(owner);
                if (userNotes != null) {
                    for (int i = 0; i < userNotes.size(); i++) {
                        if (userNotes.get(i).getId() == noteId) {
                            userNotes.set(i, nota);
                            break;
                        }
                    }
                    DBManager.getNotesDatabase().put(owner, userNotes);
                }
            }

            DBManager.commit();
        }
        return nota;
    }

    @Override
    public void removeUserFromShare(String caller, int noteId, String targetUsername) throws NotingException {
        if (caller == null || caller.isBlank()) throw new NotingException("Utente non autenticato.");
        if (noteId <= 0) throw new NotingException("ID nota non valido.");
        if (targetUsername == null || targetUsername.isBlank())
            throw new NotingException("Username da rimuovere mancante.");

        Note nota = DBManager.getNoteById().get(noteId);
        if (nota == null) throw new NotingException("Nota inesistente.");

        String owner = nota.getOwnerUsername();
        if (!caller.equals(owner)) {
            throw new NotingException("Solo il proprietario può rimuovere utenti dalla condivisione.");
        }

        ConcurrentMap<Integer, List<String>> shareMap = DBManager.getListaCondivisione();
        List<String> destinatari = shareMap.get(noteId);
        if (destinatari != null && destinatari.remove(targetUsername)) {
            shareMap.put(noteId, destinatari);
            nota.setUtentiCondivisi(new ArrayList<>(destinatari));
            DBManager.getNoteById().put(noteId, nota);

            synchronized (owner.intern()) {
                List<Note> userNotes = DBManager.getNotesDatabase().get(owner);
                if (userNotes != null) {
                    for (int i = 0; i < userNotes.size(); i++) {
                        if (userNotes.get(i).getId() == noteId) {
                            userNotes.set(i, nota);
                            break;
                        }
                    }
                    DBManager.getNotesDatabase().put(owner, userNotes);
                }
            }

            DBManager.commit();
        } else {
            throw new NotingException("Utente non trovato nella lista di condivisione.");
        }
    }

    @Override
    public void clearShareList(String caller, int noteId) throws NotingException {
        if (caller == null || caller.isBlank()) throw new NotingException("Utente non autenticato.");
        if (noteId <= 0) throw new NotingException("ID nota non valido.");

        Note nota = DBManager.getNoteById().get(noteId);
        if (nota == null) throw new NotingException("Nota inesistente.");

        String owner = nota.getOwnerUsername();
        if (!caller.equals(owner)) {
            throw new NotingException("Solo il proprietario può svuotare la condivisione.");
        }

        ConcurrentMap<Integer, List<String>> shareMap = DBManager.getListaCondivisione();
        List<String> destinatari = shareMap.get(noteId);
        if (destinatari == null) {
            throw new NotingException("Lista di condivisione non trovata per la nota.");
        }

        destinatari.clear();
        shareMap.put(noteId, destinatari);

        nota.setUtentiCondivisi(new ArrayList<>());
        DBManager.getNoteById().put(noteId, nota);

        synchronized (owner.intern()) {
            List<Note> userNotes = DBManager.getNotesDatabase().get(owner);
            if (userNotes != null) {
                for (int i = 0; i < userNotes.size(); i++) {
                    if (userNotes.get(i).getId() == noteId) {
                        userNotes.set(i, nota);
                        break;
                    }
                }
                DBManager.getNotesDatabase().put(owner, userNotes);
            }
        }

        DBManager.commit();
        System.out.println("Lista di condivisione svuotata per la nota ID: " + noteId);
    }

    // =======================
    // Self-service
    // =======================
    @Override
    public void cancelShareForUser(String username, int noteId) throws NotingException {
        if (username == null || username.isBlank()) throw new NotingException("Utente non autenticato.");
        if (noteId <= 0) throw new NotingException("ID nota non valido.");

        ConcurrentMap<Integer, List<String>> shareMap = DBManager.getListaCondivisione();
        List<String> destinatari = shareMap.get(noteId);
        if (destinatari != null && destinatari.remove(username)) {
            shareMap.put(noteId, destinatari);

            Note nota = DBManager.getNoteById().get(noteId);
            if (nota != null) {
                nota.setUtentiCondivisi(new ArrayList<>(destinatari));
                DBManager.getNoteById().put(noteId, nota);

                String owner = nota.getOwnerUsername();
                synchronized (owner.intern()) {
                    List<Note> userNotes = DBManager.getNotesDatabase().get(owner);
                    if (userNotes != null) {
                        for (int i = 0; i < userNotes.size(); i++) {
                            if (userNotes.get(i).getId() == noteId) {
                                userNotes.set(i, nota);
                                break;
                            }
                        }
                        DBManager.getNotesDatabase().put(owner, userNotes);
                    }
                }
            }

            DBManager.commit();
            System.out.println("Utente " + username + " ha annullato la condivisione della nota ID: " + noteId);
        } else {
            throw new NotingException("Utente non trovato nella lista di condivisione.");
        }
    }
}
