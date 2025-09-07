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

import com.google.gwt.sample.noting.shared.LockStatus;
import com.google.gwt.sample.noting.shared.LockToken;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NotingException; 
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;



public class NoteServiceImpl extends RemoteServiceServlet implements NoteService {

    static User TEST_USER = null;
    static void _setTestUser(User u) { TEST_USER = u; }
    static void _clearTestUser() { TEST_USER = null; }


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
        HttpSession session = (request != null) ? request.getSession(false) : null;

      //  if (session == null || session.getAttribute("user") == null) {
       //     throw new NotingException("Utente non autenticato. Impossibile creare la nota.");
       // }

        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if(user == null){
            if(TEST_USER != null){
                user = TEST_USER;
            } else {
                throw new NotingException("Utente di test non impostato. Impossibile creare la nota.");
            }
        }

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
        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = (request != null) ? request.getSession(false) : null;

        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            if (TEST_USER != null) {
                user = TEST_USER; // modalità test
            } else {
                throw new NotingException("Utente di test non impostato. Impossibile recuperare le note.");
            }
        }

        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        List<Note> noteUtente = notesDB.get(user.getUsername());
        return (noteUtente != null) ? noteUtente : new ArrayList<>();
    }

    @Override
    public void updateNota(Note notaModificata) throws NotingException {
        HttpServletRequest request = getThreadLocalRequest();
        HttpSession session = (request != null) ? request.getSession(false) : null;
        User user = (session != null) ? (User) session.getAttribute("user") : null;

         if (user == null) {
            if (TEST_USER != null) {
                 user = TEST_USER;
            } else {
                 throw new NotingException("Utente di test non impostato. Impossibile aggiornare la nota.");
            }
        }
        
        String username = user.getUsername();
        int noteId = notaModificata.getId();
        String owner = notaModificata.getOwnerUsername();

        // 2) Verifica lock: deve essere attivo e posseduto da 'username'
        var st = NoteLockManager.getInstance().status(noteId);
        if (!(st.locked && username.equals(st.owner))) {
            throw new NotingException("Non possiedi il lock di modifica su questa nota.");
        }
        

         // 3) Recupero archivio note
    ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
    List<Note> userNotes = notesDB.get(owner);
    if (userNotes == null) {
        throw new NotingException("Nota non trovata (owner sconosciuto).");
    }

    // 4) Sezione critica per-NOTA (non per-utente!)
    synchronized (("note-" + noteId).intern()) {
        boolean updated = false;
        for (int i = 0; i < userNotes.size(); i++) {
            if (userNotes.get(i).getId() == noteId) {
                userNotes.set(i, notaModificata);
                updated = true;
                break;
            }
        }
        if (!updated) {
            throw new NotingException("Nota non trovata.");
        }
        // Rimetti la lista (se necessario nella tua impl)
        notesDB.put(owner, userNotes);
    }

    // 5) Commit persistente
    DBManager.commit();
    System.out.println("Nota aggiornata da " + username + " (ID " + noteId + ") titolo='" + notaModificata.getTitle() + "'");
    }

    @Override
    public List<Note> searchNotes(String query) throws NotingException {
        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = (request != null) ? request.getSession(false) : null;

        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            if (TEST_USER != null) {
                user = TEST_USER; // modalità test, niente sessione
            } else {
                throw new NotingException("Utente di test non impostato. Impossibile cercare note.");
            }
        }

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


    private User requireUser() throws NotingException {
    HttpServletRequest request = this.getThreadLocalRequest();
    HttpSession session = (request != null) ? request.getSession(false) : null;

    User user = (session != null) ? (User) session.getAttribute("user") : null;

    if (user == null) {
        if (TEST_USER != null) {
            // modalità test, utile in sviluppo/unit test
            return TEST_USER;
        } else {
            throw new NotingException("Utente non autenticato.");
        }
    }
    return user;
    }


      // =======================
    // RPC LOCKING
    // =======================
    @Override
    public LockStatus getLockStatus(int noteId) throws NotingException {
        // opzionale: puoi verificare permessi di lettura su notaId
        var st = NoteLockManager.getInstance().status(noteId);
        return new LockStatus(st.noteId, st.locked, st.owner, st.expiresAt);
    }

    @Override
    public LockToken tryAcquireLock(int noteId) throws NotingException {
        User u = requireUser();
        // (consigliato) consenti acquire solo se l'utente ha permessi di modifica
        ensureCanEdit(u, noteId);

        var tok = NoteLockManager.getInstance().tryAcquire(noteId, u.getUsername());
        if (tok == null) {
            var st = NoteLockManager.getInstance().status(noteId);
            String owner = (st.owner != null) ? st.owner : "sconosciuto";
            throw new NotingException("Nota in modifica da: " + owner);
        }
        System.out.println("[LOCK] ACQUIRE note=" + noteId + " by " + u.getUsername() + " exp=" + tok.expiresAt);
        return new LockToken(tok.noteId, tok.username, tok.expiresAt);
    }

    @Override
    public LockToken renewLock(int noteId) throws NotingException {
        User u = requireUser();
        var tok = NoteLockManager.getInstance().renew(noteId, u.getUsername());
        if (tok == null) throw new NotingException("Lock scaduto o non posseduto.");
        // Log sintetico
        // System.out.println("[LOCK] RENEW note=" + noteId + " by " + u.getUsername());
        return new LockToken(tok.noteId, tok.username, tok.expiresAt);
    }

    @Override
    public void releaseLock(int noteId) throws NotingException {
        User u = requireUser();
        NoteLockManager.getInstance().release(noteId, u.getUsername());
        System.out.println("[LOCK] RELEASE note=" + noteId + " by " + u.getUsername());
    }

    /**
     * Verifica rapida: l'utente è owner o ha SCR su questa nota?
     * Usa le tue strutture DBManager (notesDB, listaCondivisione, ecc.).
     * Adatta se la tua logica permessi è diversa.
     */
    private void ensureCanEdit(User u, int noteId) throws NotingException {
        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();

        // Cerca la nota per ID (tra tutte le note). Se hai un indice, usalo.
        Note found = null;
        String ownerUser = null;
        for (Map.Entry<String, List<Note>> e : notesDB.entrySet()) {
            for (Note n : e.getValue()) {
                if (n.getId() == noteId) {
                    found = n; ownerUser = e.getKey();
                    break;
                }
            }
            if (found != null) break;
        }
        if (found == null) throw new NotingException("Nota non trovata.");

        String username = u.getUsername();
        boolean isOwner = username.equals(ownerUser);
        boolean canSCR = false;

        if (found.getStato() == Note.Stato.CondivisaSCR) {
            List<String> condivisi = listaCondivisione.get(noteId);
            if (condivisi != null && condivisi.contains(username)) {
                canSCR = true;
            }
        }

        if (!(isOwner || canSCR)) {
            throw new NotingException("Non hai i permessi per modificare questa nota.");
        }
    }

    // =======================
    // TUA LOGICA ESISTENTE...
    // (altri metodi RPC già presenti)
    // =======================











}


    
