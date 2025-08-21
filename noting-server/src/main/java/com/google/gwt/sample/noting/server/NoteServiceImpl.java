package com.google.gwt.sample.noting.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mapdb.Atomic;

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
    public void creazioneNota(String titolo, String contenuto, Note.Stato stato, List<String> utentiCondivisi) throws NotingException { //aggiungere lista degli utenti a cui condividere la nota
        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            throw new NotingException("Utente non autenticato. Impossibile creare la nota.");
        }

        User user = (User) session.getAttribute("user");
        String username = user.getUsername();

       // ===========================
    //  SVUOTA SUBITO IL "DB"
    // ===========================
    ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
    ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();
/*
    try {
        // Svuota entrambe le mappe e azzera il contatore ID
        notesDB.clear();
        listaCondivisione.clear();
        DBManager.getNoteIdCounter().set(0);
        DBManager.commit();
        System.out.println("[DEV] DB svuotato: notesDB, listaCondivisione e contatore ID azzerati.");
    } catch (Exception ex) {
        // In caso di dati vecchi/incompatibili, prova almeno a “resettare” le mappe
        System.err.println("[DEV] Errore durante lo svuotamento MapDB: " + ex);
        // Se hai un metodo di utilità in DBManager per re-inizializzare, chiamalo qui:
        // DBManager.reinitNotesAndShares(); // <-- opzionale, se lo implementi
        // In alternativa, prova comunque a proseguire con mappe vuote logiche:
        // (non c'è molto altro da fare senza rigenerare il file fisico)
    } */

        

         List<String> destinatari = (utentiCondivisi == null ? Collections.<String>emptyList() : utentiCondivisi)
            .stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(s -> !s.equals(username)) // opzionale: non condividere con se stessi
            .distinct()
            .collect(Collectors.toList());



        Note nuovaNota = new Note(titolo, contenuto, stato,destinatari);

            // Assegno ID univoco PRIMA di usarlo come chiave nelle mappe
        Atomic.Var<Integer> noteIdCounter = DBManager.getNoteIdCounter();
        int newId;
        synchronized (noteIdCounter) {
        newId = noteIdCounter.get();
        noteIdCounter.set(newId + 1);
         }
        nuovaNota.setId(newId);
     
        // Salvo la nota nella lista dell'utente
        synchronized (username.intern()) {
        List<Note> userNotes = notesDB.get(username);
        if (userNotes == null) {
            userNotes = new ArrayList<>();
        }
        userNotes.add(nuovaNota);
        notesDB.put(username, userNotes);
        }

       // Determino se lo stato è "condiviso" o "privato"
         boolean isCondivisa =
            stato == Note.Stato.Condivisa
         || stato == Note.Stato.CondivisaSCR     // <-- rinomina se il tuo enum usa un altro identificatore
         || "Condivisa".equalsIgnoreCase(stato.name())
         || "CondivisainSCR".equalsIgnoreCase(stato.name());

         // Inizializzo/aggiorno la lista di condivisione:
           // - vuota se privata
           // - lista degli username se condivisa
         List<String> valoriCondivisione = isCondivisa ? destinatari : new ArrayList<>();
         listaCondivisione.put(newId, valoriCondivisione);

         DBManager.commit();

         System.out.println("Nota creata da: " + username
            + " con titolo: " + titolo
            + " e stato: " + stato.name()
            + " (ID=" + newId + "), condivisa con: " + valoriCondivisione);
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
    public void updateNota(Note notaModificata, Note.Stato nuovoStato) throws NotingException {
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
            // recupero note dell'utente
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

        @Override
    public List<String> getAllUsernames() {
        Map<String, String> userDb = DBManager.getUsersDatabase();
        return new ArrayList<>(userDb.keySet()); // Solo username (le chiavi)
    }

    @Override
    public boolean cercaUtente(String username) throws NotingException {
         HttpSession session = getThreadLocalRequest().getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            throw new NotingException("Utente non autenticato.");
        }

        User username_proprietario = (User) session.getAttribute("user");

        boolean risultato = false;
        String usernameDb = DBManager.getUsersDatabase().get(username);
        for (String user : DBManager.getUsersDatabase().keySet()) {

    //Window.alert("user = " + user);
    //Window.alert("username_proprietario = " + username_proprietario.getUsername());
        // Utente trovato, ma non deve essere lo stesso che è loggato
        if (user.equals(username) ){   
            
            risultato = true;
        }
    }

        return risultato; // Utente non trovato 

       
    }

    @Override
public List<Note> getCondiviseConMe() throws NotingException {
    HttpSession session = getThreadLocalRequest().getSession(false);
    if (session == null || session.getAttribute("user") == null) {
        throw new NotingException("Utente non autenticato.");
    }

    String username = ((User) session.getAttribute("user")).getUsername();

    ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();
    ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();

    List<Note> result = new ArrayList<>();

    // 1) Prendo gli ID delle note condivise con questo utente (gestendo null e duplicati)
    Set<Integer> sharedIds = new HashSet<>();
    for (Map.Entry<Integer, List<String>> e : listaCondivisione.entrySet()) {
        List<String> destinatari = e.getValue();
        if (destinatari != null && destinatari.contains(username)) {
            sharedIds.add(e.getKey());
        }
    }
    if (sharedIds.isEmpty()) {
        return result; // niente da restituire
    }

    // 2) Creo un indice id -> Note per ricerca O(1)
    Map<Integer, Note> byId = new HashMap<>();
    for (List<Note> noteList : notesDB.values()) {
        if (noteList == null) continue;
        for (Note n : noteList) {
            // opzionale: se vuoi escludere le note create da me, scommenta:
             if (username.equals(n.getOwnerUsername())) continue;
            byId.put(n.getId(), n);
        }
    }

    // 3) Ricompongo la lista nell'ordine degli ID (mantieni ordine di inserimento se vuoi)
    for (Integer id : sharedIds) {
        Note n = byId.get(id);
        if (n != null) {
            result.add(n);
        }
        // se n == null, l'ID è "orfano": la nota è stata cancellata/mai salvata → ignora
    }

    return result;
}


    
}