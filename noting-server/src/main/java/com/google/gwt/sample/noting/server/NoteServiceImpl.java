package com.google.gwt.sample.noting.server;

import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class NoteServiceImpl extends RemoteServiceServlet implements NoteService {

    @Override
    public void logout() {
        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = request.getSession(false); // Non creare una nuova sessione se non esiste
        if (session != null) {
            session.invalidate();
        }
    }
    
    @Override
    public User login(String username, String password) throws Exception {
        if (username == null || password == null) {
            System.out.println("USERNAME E PASSWORD NON POSSONO ESSERE VUOTI.");
            throw new Exception("Username e password non possono essere vuoti.");
        }

        ConcurrentMap<String, String> users = DBManager.getUsersDatabase();
        String storedPassword = users.get(username);

        if (storedPassword != null && storedPassword.equals(password)) {
            System.out.println("LOGIN RIUSCITO PER:  " + username);
            return new User(username); // Login corretto
            
        } else {
            System.out.println("CREDENZIALI NON VALIDE");
            throw new Exception("Credenziali non valide.");
        }
    }

    @Override
    public User register(String username, String password) throws Exception {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            System.out.println("USERNAME E PASSWORD NON POSSONO ESSERE VUOTI.");
            throw new Exception("Username e password non possono essere vuoti.");
        }

        ConcurrentMap<String, String> users = DBManager.getUsersDatabase();
        
        // putIfAbsent è atomico e thread-safe
        if (users.putIfAbsent(username.trim(), password) == null) {
            DBManager.commit(); // Salva permanentemente la modifica sul file
            System.out.println("Nuovo utente registrato con MapDB: " + username);
            return new User(username.trim()); // Registrazione completata
        } else {
            System.out.println(username + " USERNAME GIà ESISTENTE.");
            throw new Exception("Username già esistente.");
        }
    }


    @Override
    public void creazioneNota(String titolo, String contenuto) {
        // Implementazione della logica per la creazione di una nota
        // Questa parte del codice dovrebbe essere completata in base alle specifiche del progetto.
        System.out.println("Creazione nota non ancora implementata.");
    }

    @Override
    public void eliminaUltimaNota() {
        // Implementazione della logica per eliminare l'ultima nota
        // Questa parte del codice dovrebbe essere completata in base alle specifiche del progetto.
        System.out.println("Eliminazione ultima nota non ancora implementata.");
    }

}