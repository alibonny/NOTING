package com.google.gwt.sample.noting.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
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
             // Guardia per evitare NPE quando esegui i test senza servlet/sessione
            HttpServletRequest request = this.getThreadLocalRequest();
            if (request != null) {
            HttpSession session = request.getSession(true);
            if (session != null) {
                session.setAttribute("user", user);
            }
        }

           // HttpServletRequest request = this.getThreadLocalRequest();
           // HttpSession session = request.getSession(true);
           // session.setAttribute("user", user);
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
        System.out.println("Creazione nota per utente: " + username);

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
    } 
    */

        

         List<String> destinatari = (utentiCondivisi == null ? Collections.<String>emptyList() : utentiCondivisi)
            .stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(s -> !s.equals(username)) // opzionale: non condividere con se stessi
            .distinct()
            .collect(Collectors.toList());



        Note nuovaNota = new Note(titolo, contenuto, stato,destinatari,username);

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

         System.out.println("Notal creata da: " + username
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
/* 
    @Override
public List<Note> getCondiviseConMe() throws NotingException {
    HttpSession session = getThreadLocalRequest().getSession(false);
    if (session == null || session.getAttribute("user") == null) {
        throw new NotingException("Utente non autenticato.");
    }

    String username = ((User) session.getAttribute("user")).getUsername();

    ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();
    ConcurrentMap<Integer, Note> noteById = DBManager.getNoteById();

    List<Note> result = new ArrayList<>();

    // 1) Prendo gli ID delle note condivise con questo utente (gestendo null e duplicati)
    for (Map.Entry<Integer, List<String>> entry : listaCondivisione.entrySet()) {
    Integer idNota = entry.getKey();
    List<String> destinatari = entry.getValue();

        if (destinatari != null && destinatari.contains(username)) {
            Note n = noteById.get(idNota);
            if (n != null && !username.equals(n.getOwnerUsername())) { // opzionale: escludi le tue
                result.add(n);
            }
        }
    }
    return result;
    }
   */
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
        //    LinkedHashSet per mantenere l’ordine di inserimento, come nel metodo precedente.
        Set<Integer> sharedIds = new LinkedHashSet<>();
        if (listaCondivisione != null) {
            for (Map.Entry<Integer, List<String>> e : listaCondivisione.entrySet()) {
                List<String> destinatari = e.getValue();
                if (destinatari != null && destinatari.contains(username)) {
                    sharedIds.add(e.getKey());
                }
            }
        }
        if (sharedIds.isEmpty()) {
            return result; // niente da restituire
        }

        // 2) Creo un indice id -> Note per ricerca O(1), ESCLUDENDO le note dell’owner corrente
        Map<Integer, Note> byId = new java.util.HashMap<>();
        if (notesDB != null) {
            for (List<Note> noteList : notesDB.values()) {
                if (noteList == null) continue;
                for (Note n : noteList) {
                    if (n == null) continue;
                    // stessa logica del tuo metodo precedente: escludi le note create da me
                    if (username.equals(n.getOwnerUsername())) continue;
                    byId.put(n.getId(), n);
                }
            }
        }

        // 3) Ricompongo la lista seguendo l’ordine degli ID trovati
        for (Integer id : sharedIds) {
            Note n = byId.get(id);
            if (n != null) {
                result.add(n);
            }
            // se n == null → ID orfano (nota cancellata o incoerenza) → ignorata
        }

        return result;
    }


    @Override
    public void creaCopiaNota(String username, int notaId) throws NotingException {

        // Controlli input
        if (username == null || username.isBlank()) {
            throw new NotingException("Utente non autenticato.");
        }
        if (notaId <= 0) {
            throw new NotingException("ID nota non valido.");
        }

        // Recupero strutture
        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        List<Note> userNotes   = DBManager.getUserOwnedNotes(username);
        List<Note> sharedNotes = DBManager.getUserSharedNotes(username);

        // Cerca la nota da copiare
        Note sorgente = null;
        for (Note n : userNotes) {
            if (n.getId() == notaId) { 
                sorgente = n; 
                break; 
            }
        }
        if (sorgente == null) {
            for (Note n : sharedNotes) {
                if (n.getId() == notaId) { 
                    sorgente = n; 
                    break; 
                }
            }
        }

        if (sorgente == null) {
            throw new NotingException("Nota non trovata o non accessibile.");
        }

        // Crea la copia (eventualmente copia anche tag/metadata/owner)
        Note notaDaCopiare = new Note(sorgente.getTitle(), sorgente.getContent(), Note.Stato.Privata);
        // Se esiste un campo owner/autore: notaDaCopiare.setOwner(username);
        // Se servono tag: notaDaCopiare.setTags(new ArrayList<>(sorgente.getTags()));
        // Timestamps: notaDaCopiare.setCreatedAt(Instant.now()); ecc.

        // Assegna ID univoco
        Atomic.Var<Integer> noteIdCounter = DBManager.getNoteIdCounter();
        int newId;
        synchronized (noteIdCounter) {
            int current = noteIdCounter.get();
            newId = current + 1;      
            noteIdCounter.set(newId);
        }
        notaDaCopiare.setId(newId);

        // Salva la nota nella lista dell'utente
        notesDB.compute(username, (u, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(notaDaCopiare);
            return list; 
        });

        DBManager.commit();
        System.out.println("Copia della nota ID: " + notaId + " creata per l'utente: " + username + " con nuovo ID: " + newId);
    }

    @Override
    public void annullaCondivisione(String username, int notaId) throws NotingException{
        if (username == null || username.isBlank()) {
            throw new NotingException("Utente non autenticato.");
        }
        if (notaId <= 0) {
            throw new NotingException("ID nota non valido.");
        }

        ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();
        List<String> destinatari = listaCondivisione.get(notaId);
        if (destinatari != null && destinatari.remove(username)) {
            // Aggiorna la mappa solo se la lista è cambiata
            for (int i = 0; i < destinatari.size(); i++) {
                System.out.println("Destinatario " + i + ": " + destinatari.get(i));
            }
            listaCondivisione.put(notaId, destinatari);
            DBManager.commit();
            System.out.println("Utente " + username + " rimosso dalla condivisione della nota ID: " + notaId);
        } else {
            throw new NotingException("Utente non trovato nella lista di condivisione per la nota ID: " + notaId);
        }
    }


    public void rimuoviUtenteCondivisione(int notaId, String username) throws NotingException {
        if (username == null || username.isBlank()) {
            throw new NotingException("Utente non autenticato.");
        }
        if (notaId <= 0) {
            throw new NotingException("ID nota non valido.");
        }

        ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();
        List<String> destinatari = listaCondivisione.get(notaId);
        if (destinatari != null && destinatari.remove(username)) {
            // Aggiorna la mappa solo se la lista è cambiata
            for (int i = 0; i < destinatari.size(); i++) {
                System.out.println("Destinatario " + i + ": " + destinatari.get(i));
            }
            listaCondivisione.put(notaId, destinatari);
            DBManager.commit();
            System.out.println("Utente " + username + " rimosso dalla condivisione della nota ID: " + notaId);
        } else {
            throw new NotingException("Utente non trovato nella lista di condivisione per la nota ID: " + notaId);
        }
    }

}


    
