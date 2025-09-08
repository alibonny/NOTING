package com.google.gwt.sample.noting.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

        // if (session == null || session.getAttribute("user") == null) {
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
            .filter(s -> !s.equals(username)) 
            .distinct()
            .collect(Collectors.toList());

        Note nuovaNota = new Note(titolo, contenuto, stato, destinatari, username);
        nuovaNota.setOwnerUsername(username); 

    // ====== NUOVO BLOCCO: genera ID atomico e "prenota" sull'indice ======
        int newId;
        Note prev;
        do {
            newId = DBManager.nextNoteId();
            nuovaNota.setId(newId);
            prev = DBManager.getNoteById().putIfAbsent(newId, nuovaNota);
           
        } while (prev != null);

        // Salvo la nota nella lista dell'utente
        synchronized (username.intern()) {
            List<Note> userNotes = notesDB.get(username);
            if (userNotes == null) {
                userNotes = new ArrayList<>();
            }
            userNotes.add(nuovaNota);
            notesDB.put(username, userNotes);
        }

        // Determino se la nota è condivisa
        boolean isCondivisa =
                stato == Note.Stato.Condivisa
            || stato == Note.Stato.CondivisaSCR
            || "Condivisa".equalsIgnoreCase(stato.name())
            || "CondivisainSCR".equalsIgnoreCase(stato.name());

        // Inizializzo/aggiorno lista condivisione
        //List<String> valoriCondivisione = isCondivisa ? destinatari : new ArrayList<>();
        //listaCondivisione.put(newId, valoriCondivisione);
        // Inizializzo/aggiorno lista condivisione SOLO se condivisa
        List<String> valoriCondivisione;
        if (isCondivisa && !destinatari.isEmpty()) {
            valoriCondivisione = destinatari;
            listaCondivisione.put(newId, destinatari);
            System.out.println("ID " + newId + " condivisa con: " + destinatari);
        } else {
            valoriCondivisione = Collections.emptyList();
            listaCondivisione.remove(newId); // non lasciare tracce di note private
            System.out.println("ID " + newId + " NON condivisa (nota privata)");
        }

        DBManager.commit();

        System.out.println("Nota creata da: " + username
            + " con titolo: " + titolo
            + " e stato: " + stato.name()
            + " (ID=" + nuovaNota.getId() + "), condivisa con: " + valoriCondivisione);
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

        String username = user.getUsername();
        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        List<Note> userNotes = notesDB.get(username);

        if (userNotes == null) {
            return new ArrayList<>();
        }

        // Ritorna una copia della lista per evitare problemi di modifica concorrente
        return new ArrayList<>(userNotes);
    }


  @Override
public void updateNota(Note notaModificata) throws NotingException {
    HttpServletRequest request = getThreadLocalRequest();
    HttpSession session = (request != null) ? request.getSession(false) : null;
    User user = (session != null) ? (User) session.getAttribute("user") : null;
    if (user == null) { 
        if (TEST_USER != null) user = TEST_USER; 
        else throw new NotingException("Utente di test non impostato. Impossibile aggiornare la nota."); 
    }

    final String caller = user.getUsername();
    final int noteId = notaModificata.getId();
    if (noteId <= 0) throw new NotingException("ID nota non valido.");

    final ConcurrentMap<Integer, Note> noteById = DBManager.getNoteById();
    final ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
    final ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();

    // Lock fine-grained sulla singola nota
    final Object noteLock = ("NOTE_LOCK_" + noteId).intern();
    synchronized (noteLock) {

        // 1) Recupero nota esistente e owner effettivo (owner non si cambia qui)
        Note esistente = noteById.get(noteId);
        if (esistente == null) throw new NotingException("Nota non trovata (ID=" + noteId + ").");
        final String owner = esistente.getOwnerUsername();

        // 2) Permessi: deve essere owner o utente condiviso attuale
        boolean canEdit = caller.equals(owner);
        if (!canEdit) {
            List<String> currentShare = listaCondivisione.getOrDefault(noteId, List.of());
            canEdit = currentShare.contains(caller);
        }
        if (!canEdit) throw new NotingException("Non hai i permessi per modificare questa nota.");

        // 3) Prepara i nuovi campi (id e owner restano quelli dell'originale)
        final String newTitle   = notaModificata.getTitle();
        final String newContent = notaModificata.getContent();
        final Note.Stato newStato = notaModificata.getStato();

        // Normalizza destinatari: senza null, trim, distinct, no owner
List<String> newDest = notaModificata.getUtentiCondivisi().stream()
    .filter(Objects::nonNull)
    .map(String::trim)
    .filter(s -> !s.isEmpty())
    .filter(s -> !s.equals(owner))   // non condividere con l’owner
    .distinct()
    .collect(Collectors.toList());

        // Se diventa privata, nessuna condivisione
        if (newStato == Note.Stato.Privata) {
            newDest = List.of();
        }

        // 4) Crea l'oggetto aggiornato (mantieni id e owner originali)
        Note aggiornata = new Note(newTitle, newContent, newStato, newDest, owner);
        aggiornata.setId(noteId);
        aggiornata.setOwnerUsername(owner);

        // 5) Aggiorna indice globale in modo atomico (evita lost update)
        boolean replaced = noteById.replace(noteId, esistente, aggiornata);
        if (!replaced) {
            throw new NotingException("La nota è stata modificata in parallelo. Riprova.");
        }

        // 6) Aggiorna la lista dell'owner
        synchronized (owner.intern()) {
            List<Note> userNotes = notesDB.get(owner);
            if (userNotes == null) userNotes = new ArrayList<>();
            boolean updated = false;
            for (int i = 0; i < userNotes.size(); i++) {
                if (userNotes.get(i).getId() == noteId) {
                    userNotes.set(i, aggiornata);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                // Riallinea se per qualche motivo mancava
                userNotes.add(aggiornata);
            }
            notesDB.put(owner, userNotes);
        }

        // 7) Riallinea SEMPRE la lista condivisione
        if (aggiornata.getStato() == Note.Stato.Privata || aggiornata.getUtentiCondivisi().isEmpty()) {
            listaCondivisione.remove(noteId);
        } else {
            listaCondivisione.put(noteId, aggiornata.getUtentiCondivisi());
        }
    }

    DBManager.commit();
    System.out.println("Nota aggiornata da " + caller + " (ID=" + noteId + ", titolo: " + notaModificata.getTitle() + ")");
}



    @Override
    public void svuotaCondivisioneNota(int notaId) throws NotingException {
        if (notaId <= 0) {
            throw new NotingException("ID nota non valido.");
        }

        ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();
        List<String> destinatari = listaCondivisione.get(notaId);

        if (destinatari != null) {
            destinatari.clear(); // svuota completamente la lista
            listaCondivisione.put(notaId, destinatari); // aggiorna la mappa
            DBManager.commit();
            System.out.println("Lista di condivisione svuotata per la nota ID: " + notaId);
        } else {
            throw new NotingException("Lista di condivisione non trovata per la nota ID: " + notaId);
        }
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

        DBManager.getNoteById().remove(notaId);
        DBManager.getListaCondivisione().remove(notaId);

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
        Map<Integer, Note> noteById = DBManager.getNoteById(); // Usa la mappa globale per ID

        List<Note> result = new ArrayList<>();

        if (listaCondivisione != null) {
            // Itera sulla mappa di condivisione
            for (Map.Entry<Integer, List<String>> entry : listaCondivisione.entrySet()) {
                Integer noteId = entry.getKey();
                List<String> sharedWith = entry.getValue();

                // Controlla se la nota è condivisa con l'utente corrente
                if (sharedWith != null && sharedWith.contains(username)) {
                    Note note = noteById.get(noteId); // Recupera la nota specifica tramite il suo ID

                    // Aggiungi la nota alla lista dei risultati se esiste e non è dell'utente stesso
                    if (note != null && !note.getOwnerUsername().equals(username)) {
                        result.add(note);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void creaCopiaNota(String username, int notaId) throws NotingException {
        System.out.println("=== INIZIO creaCopiaNota - username: " + username + ", notaId: " + notaId + " ===");
        
       User user =  requireUser();

        // Verifica che l'username corrisponda all'utente autenticato
        if (!user.getUsername().equals(username)) {
            throw new NotingException("Username non corrispondente.");
        }

        if (notaId <= 0) {
            throw new NotingException("ID nota non valido.");
        }

        // Cerca la nota originale dall'indice globale
        Note sorgente = DBManager.getNoteById().get(notaId);
        if (sorgente == null)  throw new NotingException("Nota con ID " + notaId + " non trovata.");  
        
        System.out.println("Nota trovata: " + sorgente.getTitle() + " di " + sorgente.getOwnerUsername() + "ID:" + sorgente.getId());

        // Verifica accesso (proprietario o condivisa con l'utente)
        boolean hasAccess = false;
        
        if (username.equals(sorgente.getOwnerUsername())) {
            hasAccess = true;
            System.out.println("Utente è proprietario della nota");
        } else {
            // Verifica se è condivisa
            ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();
            List<String> sharedWith = listaCondivisione.get(notaId);
            if (sharedWith != null && sharedWith.contains(username)) {
                hasAccess = true;
                System.out.println("Nota è condivisa con l'utente");
            }
        }
        
        if (!hasAccess) {
            throw new NotingException("Non hai i permessi per copiare questa nota.");
        }

        // Crea la copia usando lo stesso costruttore di creazioneNota
        Note notaCopia = new Note(
            sorgente.getTitle() + " (Copia)",
            sorgente.getContent(),
            Note.Stato.Privata,
            Collections.emptyList(), // Nessuna condivisione per la copia
            username // Il nuovo proprietario
        );
        notaCopia.setOwnerUsername(username);


    // ====== Genera ID atomico e "prenota" sull'indice globale ======
        int newId;
        Note prev;
        do {
            newId = DBManager.nextNoteId();     // <-- sostituisce getNoteIdCounter()
            notaCopia.setId(newId);
            prev = DBManager.getNoteById().putIfAbsent(newId, notaCopia);
        } while (prev != null); // in collisione (rarissima), riprova

        System.out.println("Creando copia con ID: " + notaCopia.getId() + " per utente: " + username);

        // Salva nella lista dell'utente (stesso pattern di creazioneNota)
        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        synchronized (username.intern()) {
            List<Note> userNotes = notesDB.get(username);
            if (userNotes == null)  userNotes = new ArrayList<>();
            userNotes.add(notaCopia);
            notesDB.put(username, userNotes);
        }

        DBManager.commit();

        System.out.println("Copia creata con successo! ID originale: " + notaId + 
                        ", nuovo ID: " + newId + ", titolo: " + notaCopia.getTitle());
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
        return new LockStatus(st.getNoteId(), st.isLocked(), st.getProprietarioLock(), st.getExpiresDate());
    }

    @Override
    public LockToken tryAcquireLock(int noteId) throws NotingException {
        User u = requireUser();
        System.out.println("[LOCK] tryAcquire START note=" + noteId + " user=" + u.getUsername());

        if (noteId <= 0) throw new NotingException("noteId non valido: " + noteId);

        
    var stBefore = NoteLockManager.getInstance().status(noteId);
    System.out.println("[LOCK] status BEFORE: locked=" + stBefore.isLocked() +
        " owner=" + stBefore.getProprietarioLock() + " exp=" + stBefore.getExpiresDate());
        // (consigliato) consenti acquire solo se l'utente ha permessi di modifica
        ensureCanEdit(u, noteId);

        var tok = NoteLockManager.getInstance().tryAcquire(noteId, u.getUsername());
      
        if (tok == null) {
            var st = NoteLockManager.getInstance().status(noteId);
        System.out.println("[LOCK] tryAcquire DENY note=" + noteId + " reqUser=" + u.getUsername() +
            " statusNow locked=" + st.isLocked() + " owner=" + st.getProprietarioLock() + " exp=" + st.getExpiresDate());
        String owner = (st.getProprietarioLock() != null) ? st.getProprietarioLock() : "sconosciuto";
        throw new NotingException("Nota in modifica da: " + owner);
        }
        System.out.println("[LOCK] ACQUIRE note=" + noteId + " by " + u.getUsername() + " exp=" + tok.getExpiresAt());
        return new LockToken(tok.getNoteId(), tok.getUsername(), tok.getExpiresAt());
    }

    @Override
    public LockToken renewLock(int noteId) throws NotingException {
        User u = requireUser();
        var tok = NoteLockManager.getInstance().renew(noteId, u.getUsername());
        if (tok == null) throw new NotingException("Lock scaduto o non posseduto.");
        // Log sintetico
         System.out.println("[LOCK] RENEW note=" + noteId + " by " + u.getUsername());
        return new LockToken(tok.getNoteId(), tok.getUsername(), tok.getExpiresAt());
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

}


    
