package com.google.gwt.sample.noting.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NotingException; 
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

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
}