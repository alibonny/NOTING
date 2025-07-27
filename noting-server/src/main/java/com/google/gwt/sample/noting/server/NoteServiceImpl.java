package com.google.gwt.sample.noting.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NotingException; 
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.mapdb.Atomic;

public class NoteServiceImpl extends RemoteServiceServlet implements NoteService {

    @Override
    public User login(String username, String password) throws NotingException {
        if (username == null || password == null) {
            throw new NotingException("Username e password non possono essere vuoti.");
        }

        ConcurrentMap<String, String> users = DBManager.getUsersDatabase();
        String storedPassword = users.get(username);

        if (storedPassword != null && storedPassword.equals(password)) {
            User user = new User(username);
            HttpServletRequest request = this.getThreadLocalRequest();
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            return user;
        } else {
            throw new NotingException("Credenziali non valide.");
        }
    }

    @Override
    public void logout() {
        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
    
    @Override
    public User register(String username, String password) throws NotingException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new NotingException("Username e password non possono essere vuoti.");
        }

        ConcurrentMap<String, String> users = DBManager.getUsersDatabase();
        
        if (users.putIfAbsent(username.trim(), password) == null) {
            DBManager.commit();
            return new User(username.trim());
        } else {
            throw new NotingException("Username già esistente.");
        }
    }

    @Override
    public void creazioneNota(String titolo, String contenuto) throws NotingException {
        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            throw new NotingException("Utente non autenticato. Impossibile creare la nota.");
        }

        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        
        Note nuovaNota = new Note(titolo, contenuto, username);
        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        
        synchronized(username.intern()) {
            List<Note> userNotes = notesDB.get(username);
            if (userNotes == null) {
                userNotes = new ArrayList<>();
            }

             // Assegna ID univoco alla nuova nota
            Atomic.Var<Integer> noteIdCounter = DBManager.getNoteIdCounter();
            int newId = noteIdCounter.get();
            noteIdCounter.set(newId + 1);
            nuovaNota.setId(newId);

            userNotes.add(nuovaNota);
            notesDB.put(username, userNotes);
        }

        DBManager.commit();
        System.out.println("Nota creata da " + username + " con titolo: " + titolo);
    }

    @Override
    public void eliminaUltimaNota() {
        // Implementazione non richiesta
    }

    @Override
    public List<Note> getNoteUtente() throws NotingException {
    HttpSession session = getThreadLocalRequest().getSession(false);
    if (session == null || session.getAttribute("user") == null) {
        throw new NotingException("Utente non autenticato.");
    }

    User user = (User) session.getAttribute("user");
    ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();

    List<Note> noteUtente = notesDB.get(user.getUsername());
    return (noteUtente != null) ? noteUtente : new ArrayList<>();
    }

    @Override
    public void updateNota(Note notaModificata) throws NotingException {
        HttpServletRequest request = getThreadLocalRequest();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            throw new NotingException("Utente non autenticato.");
        }

        User user = (User) session.getAttribute("user");
        String username = user.getUsername();

        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        Atomic.Var<Integer> noteIdCounter = DBManager.getNoteIdCounter();

        synchronized (username.intern()) {
            List<Note> userNotes = notesDB.get(username);
            if (userNotes == null) {
                userNotes = new ArrayList<>();
            }

            if (notaModificata.getId() <= 0) {
                // nuova nota
                //int newId = noteIdCounter.get();
                //noteIdCounter.set(newId + 1);
                //notaModificata.setId(newId);
                //userNotes.add(notaModificata);
                //System.out.println("Creata nuova nota ID: " + newId);
                System.out.println("LA NOTA CHE VUOI AGGIORNARE NON è STATA TROVATA");
            } else {
                // nota esistente
                boolean updated = false;
                for (int i = 0; i < userNotes.size(); i++) {
                    if (userNotes.get(i).getId() == notaModificata.getId()) {
                        userNotes.set(i, notaModificata);
                        updated = true;
                        System.out.println("Aggiornata nota ID: " + notaModificata.getId());
                        break;
                    }
                }
                if (!updated) throw new NotingException("Nota non trovata.");
            }

            notesDB.put(username, userNotes);
        }

        DBManager.commit();
        System.out.println("Nota aggiornata da " + username + " con titolo: " + notaModificata.getTitle());
    }

    @Override
    public List<Note> searchNotes(String query) throws NotingException {
        HttpSession session = getThreadLocalRequest().getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            throw new NotingException("Utente non autenticato.");
        }

        User user = (User) session.getAttribute("user");
        List<Note> noteUtente = DBManager.getNotesDatabase().get(user.getUsername());
        if (noteUtente == null) return new ArrayList<>();

        return noteUtente.stream()
            .filter(n -> 
                n.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                n.getContent().toLowerCase().contains(query.toLowerCase())
            )
            .collect(Collectors.toList());
    }

    @Override
    public void eliminaNota(String username, int notaId) throws NotingException {
        if (username == null) {
            throw new NotingException("Utente non autenticato.");
        }

        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        synchronized (username.intern()) {
            List<Note> userNotes = notesDB.get(username);
            if (userNotes != null) {
                userNotes.removeIf(n -> n.getId() == notaId);
                notesDB.put(username, userNotes);
            }
        }

        DBManager.commit();
    }
    
}