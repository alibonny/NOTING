package com.google.gwt.sample.noting.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.gwt.sample.noting.server.Core.AuthServiceCore;
import com.google.gwt.sample.noting.server.Core.AuthServiceCoreImpl;
import com.google.gwt.sample.noting.server.Core.NoteCercaCore;
import com.google.gwt.sample.noting.server.Core.NoteCercaCoreImpl;
import com.google.gwt.sample.noting.server.Core.NoteComandiCore;
import com.google.gwt.sample.noting.server.Core.NoteComandiCoreImpl;
import com.google.gwt.sample.noting.server.Core.SharingCore;
import com.google.gwt.sample.noting.server.Core.SharingCoreImpl;
import com.google.gwt.sample.noting.shared.LockStatus;
import com.google.gwt.sample.noting.shared.LockToken;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteHistory;
import com.google.gwt.sample.noting.shared.NoteMemento;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;


public class NoteServiceImpl extends RemoteServiceServlet implements NoteService {

    static User TEST_USER = null;
    static void _setTestUser(User u) { TEST_USER = u; }
    static void _clearTestUser() { TEST_USER = null; }

    // Metodo per ottenere l'utente autenticato
    private User getAuthenticatedUser() throws NotingException {
        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = (request != null) ? request.getSession(false) : null;
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user != null) {
            return user;
        }
        if (TEST_USER != null) {
            return TEST_USER; 
        }
        throw new NotingException("Utente non autenticato.");
    }

    @Override 
    public User login(String username, String password) throws NotingException {
        User u = auth.login(username,password);
        saveUserInSession(u);
        return u;
    }

    @Override
    public User register(String username, String password) throws NotingException {
        User u = auth.register(username, password);
        saveUserInSession(u);
        return u;
    }


    @Override
    public void creazioneNota(String titolo, String contenuto, Note.Stato stato, List<String> utenti)
        throws NotingException {
        String owner = requireUser().getUsername();
        comandi.creazioneNota(owner, titolo, contenuto, stato, utenti);
    }

    @Override
    public void updateNota(Note notaModificata)
        throws NotingException{

        User user = requireUser();    
        String caller = user.getUsername();
        int noteId = notaModificata.getId();

        if(noteId <= 0)throw new NotingException("ID nota non valido");

       
        // 3) lock applicativo (resta qui o spostalo in LockingCore)
        var st = NoteLockManager.getInstance().status(noteId);
        if (st == null || !st.isLocked() || !caller.equals(st.getProprietarioLock())) {
            throw new NotingException("Devi avere il lock esclusivo per modificare questa nota.");
        }
        var renewed = NoteLockManager.getInstance().renew(noteId, caller);
        if (renewed == null) throw new NotingException("Lock scaduto o non posseduto.");

        // 4) delega al Core che fa l’update su DBManager
        comandi.updateNote(caller, notaModificata);

        System.out.println("Nota aggiornata da " + caller +
                " (ID=" + noteId + ", titolo: " + notaModificata.getTitle() + ")");
    }

    @Override
    public void eliminaNota(String username, int notaId) throws NotingException {
    // 1) prendi sempre l’utente dalla sessione (ignora il parametro per sicurezza)
    String caller = requireUser().getUsername();

    // (opzionale ma consigliato) Policy sul lock:
    // - Se la nota è lockata da un altro, blocca la cancellazione
    var st = NoteLockManager.getInstance().status(notaId);
    if (st != null && st.isLocked() && !caller.equals(st.getProprietarioLock())) {
        throw new NotingException("Nota in modifica da: " +
                (st.getProprietarioLock() == null ? "sconosciuto" : st.getProprietarioLock()));
    }

    // 2) delega al Core (fa check ownership + rimozioni DB)
    comandi.deleteNote(caller, notaId);

    // 3) (opzionale) se il caller aveva il lock, rilascialo
    try { NoteLockManager.getInstance().release(notaId, caller); } catch (Exception ignore) {}

    System.out.println("Nota eliminata da " + caller + " (ID=" + notaId + ")");
    }


    @Override
    public void creaCopiaNota(String username, int notaId)throws NotingException{
        String caller = requireUser().getUsername();

        if(!caller.equals(username)){
            throw new NotingException("Username non corrisponde.");
        }

        comandi.copyNote(caller, notaId);
        System.out.println("Copia creata da " + caller + " (sorgente ID=" + notaId + ")");

    }

        @Override
    public List<Note> getNoteUtente() throws NotingException {
        String owner = requireUser().getUsername();
        return cerca.getNotesOf(owner);
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
        String owner = notaModificata.getOwnerUsername();
        int noteId = notaModificata.getId();

        // --- LOGICA MEMENTO ---
        // 1. Prima di salvare le modifiche, prendiamo lo stato attuale della nota dal DB.
        Note notaCorrente = DBManager.getNoteById().get(noteId);
        if (notaCorrente == null) {
            throw new NotingException("Impossibile salvare la cronologia: nota originale non trovata.");
        }

        // 2. Recuperiamo (o creiamo) l'oggetto che contiene la cronologia.
        NoteHistory history = DBManager.getNoteHistoryDatabase().get(noteId);
        if (history == null) {
            history = new NoteHistory();
        }

        // 3. Salviamo lo stato attuale nella cronologia usando il metodo che hai aggiunto in Note.java
        history.saveState(notaCorrente);
        DBManager.getNoteHistoryDatabase().put(noteId, history);
        // ---

        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        ConcurrentMap<Integer, List<String>> listaCondivisione = DBManager.getListaCondivisione();
        Atomic.Var<Integer> noteIdCounter = DBManager.getNoteIdCounter();

        
        synchronized (username.intern()) {
            // recupero note dell'utente
            List<Note> userNotes = notesDB.get(owner);
            if (userNotes == null) {
                userNotes = new ArrayList<>();
            }

            if (notaModificata.getId() <= 0) {
                // nuova nota con ID non valido
                System.out.println("LA NOTA CHE VUOI AGGIORNARE NON È STATA TROVATA");
            } else {
                // nota esistente
                boolean updated = false;
                for (int i = 0; i < userNotes.size(); i++) {
                    if (userNotes.get(i).getId() == notaModificata.getId()) {
                        // aggiorno lista utente
                        userNotes.set(i, notaModificata);

                        // aggiornamento indice globale
                        DBManager.getNoteById().put(notaModificata.getId(), notaModificata);

                        updated = true;
                        System.out.println("Aggiornata nota ID: " + notaModificata.getId());
                        break;
                    }
                }
                if (!updated) throw new NotingException("Nota non trovata.");
            }

            // reinserisco lista aggiornata per forzare la persistenza
            notesDB.put(owner, userNotes);
        }

        DBManager.commit();
        System.out.println("Nota aggiornata da " + username + " con titolo: " + notaModificata.getTitle());

    }

    @Override
    public List<Note> searchNotes(String query) throws NotingException {
        String owner = requireUser().getUsername();
        return cerca.search(owner, query);
    }

    @Override
    public List<Note> getCondiviseConMe() throws NotingException {
        String me = requireUser().getUsername();
        return cerca.sharedWith(me);
    }

    @Override
    public void svuotaCondivisioneNota(int notaId) throws NotingException {
        String me = requireUser().getUsername();
        sharing.clearShareList(me, notaId);
    }

    @Override
    public List<String> getAllUsernames() {
        return sharing.getAllUsernames();
    }

    @Override
    public boolean cercaUtente(String username) throws NotingException {
        String me = requireUser().getUsername();
        return sharing.userExistsDifferentFrom(me, username);
    }

    @Override
    public boolean cercaUtente2(Note nota, String username) throws NotingException {
        String me = requireUser().getUsername();
        return sharing.userExistsDifferentFrom(nota, me, username);
    }

    @Override
    public Note aggiungiCondivisione(int noteId, String username) throws NotingException {
        String me = requireUser().getUsername();
        // opzionale: verifica lock se vuoi impedire cambi sharing durante editing altrui
        return sharing.addShare(me, noteId, username);
    }

    @Override
    public void annullaCondivisione(String username, int notaId) throws NotingException {
        // sicurezza: il parametro deve coincidere con l’utente loggato
        String me = requireUser().getUsername();
        if (!me.equals(username)) throw new NotingException("Username non corrispondente.");
        sharing.cancelShareForUser(me, notaId);
    }

    @Override
    public void rimuoviUtenteCondivisione(int notaId, String username) throws NotingException {
        String me = requireUser().getUsername();
        sharing.removeUserFromShare(me, notaId, username);
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
        
        // Stesso pattern di autenticazione degli altri metodi
        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = (request != null) ? request.getSession(false) : null;
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            if (TEST_USER != null) {
                user = TEST_USER;
            } else {
                throw new NotingException("Utente non autenticato.");
            }
        }

        // Verifica che l'username corrisponda all'utente autenticato
        if (!user.getUsername().equals(username)) {
            throw new NotingException("Username non corrispondente.");
        }

        if (notaId <= 0) {
            throw new NotingException("ID nota non valido.");
        }

        // Cerca la nota originale dall'indice globale
        Note sorgente = DBManager.getNoteById().get(notaId);
        if (sorgente == null) {
            throw new NotingException("Nota con ID " + notaId + " non trovata.");
        }

        System.out.println("Nota trovata: " + sorgente.getTitle() + " di " + sorgente.getOwnerUsername());

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

        // Assegna nuovo ID
        Atomic.Var<Integer> noteIdCounter = DBManager.getNoteIdCounter();
        int newId;
        synchronized (noteIdCounter) {
            newId = noteIdCounter.get() + 1;
            noteIdCounter.set(newId);
        }
        notaCopia.setId(newId);
        notaCopia.setOwnerUsername(username);

        System.out.println("Creando copia con ID: " + newId + " per utente: " + username);

        // Salva nella lista dell'utente (stesso pattern di creazioneNota)
        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        synchronized (username.intern()) {
            List<Note> userNotes = notesDB.get(username);
            if (userNotes == null) {
                userNotes = new ArrayList<>();
            }
            userNotes.add(notaCopia);
            notesDB.put(username, userNotes);
        }

        // Aggiorna indice globale
        DBManager.getNoteById().put(newId, notaCopia);

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

    @Override
    public List<NoteMemento> getNoteHistory(int noteId) throws NotingException {
        getAuthenticatedUser(); // Assicura che l'utente sia loggato
        NoteHistory history = DBManager.getNoteHistoryDatabase().get(noteId);
        if (history != null) {
            return history.getHistory();
        }
        return new ArrayList<>(); // Ritorna sempre una lista vuota, mai null
    }

    @Override
    public Note restoreNoteFromHistory(int noteId, int historyIndex) throws NotingException {
        User user = getAuthenticatedUser();
        
        NoteHistory history = DBManager.getNoteHistoryDatabase().get(noteId);
        if (history == null) {
            throw new NotingException("Nessuna cronologia trovata per questa nota.");
        }

        List<NoteMemento> mementos = history.getHistory();
        if (historyIndex < 0 || historyIndex >= mementos.size()) {
            throw new NotingException("Versione della cronologia non valida.");
        }

        // Prendi lo stato salvato (memento)
        NoteMemento mementoToRestore = mementos.get(historyIndex);

        // Prendi la nota attuale per poterla modificare
        Note notaAttuale = DBManager.getNoteById().get(noteId);
        if (notaAttuale == null) {
             throw new NotingException("Nota da ripristinare non trovata.");
        }
        
        // Prima di ripristinare, salviamo lo stato attuale nella cronologia.
        // Così l'azione di "ripristino" può essere essa stessa annullata.
        history.saveState(notaAttuale);
        DBManager.getNoteHistoryDatabase().put(noteId, history);

        // Applica il vecchio stato alla nota attuale
        notaAttuale.restore(mementoToRestore);

        // Ora salva la nota modificata usando la stessa logica di `updateNota`
        // (senza la parte del memento per evitare un loop infinito)
        ConcurrentMap<String, List<Note>> notesDB = DBManager.getNotesDatabase();
        String owner = notaAttuale.getOwnerUsername();
        synchronized (owner.intern()) {
            List<Note> userNotes = notesDB.get(owner);
            boolean updated = false;
            for (int i = 0; i < userNotes.size(); i++) {
                if (userNotes.get(i).getId() == noteId) {
                    userNotes.set(i, notaAttuale);
                    updated = true;
                    break;
                }
            }
             if (!updated) {
                throw new NotingException("Errore durante il salvataggio della nota ripristinata.");
            }
            notesDB.put(owner, userNotes);
        }
        DBManager.getNoteById().put(noteId, notaAttuale);
        DBManager.commit();

        return notaAttuale;
    }
}


    
