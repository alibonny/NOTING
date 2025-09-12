package com.google.gwt.sample.noting.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

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
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;


public class NoteServiceImpl extends RemoteServiceServlet implements NoteService {

    static User TEST_USER = null;
    static void _setTestUser(User u) { TEST_USER = u; }
    static void _clearTestUser() { TEST_USER = null; }

    private final AuthServiceCore auth = new AuthServiceCoreImpl();
    private final NoteComandiCore comandi = new NoteComandiCoreImpl();
    private final NoteCercaCore cerca = new NoteCercaCoreImpl();
    private final SharingCore sharing = new SharingCoreImpl();



    @Override //TEST FATTO
    public User login(String username, String password) throws NotingException {
        User u = auth.login(username,password);
        saveUserInSession(u);
        return u;
    }

    @Override //TEST FATTO
    public User register(String username, String password) throws NotingException {
        User u = auth.register(username, password);
        saveUserInSession(u);
        return u;
    }

    @Override
    public void logout() {
        HttpServletRequest request = this.getThreadLocalRequest();
        if (request == null) return;
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
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

        @Override //TEST FATTO
    public List<Note> getNoteUtente() throws NotingException {
        String owner = requireUser().getUsername();
        return cerca.getNotesOf(owner);
    }

    @Override //TEST FATTO
    public Note getNotaById(int noteId) throws NotingException {
        // opzionale: qui potresti verificare che l’utente corrente sia owner
        // o che la nota sia condivisa con lui, se vuoi stringere i permessi.
        requireUser();
        return cerca.getById(noteId);
    }

    @Override //TEST FATTO
    public List<Note> searchNotes(String query) throws NotingException {
        String owner = requireUser().getUsername();
        return cerca.search(owner, query);
    }

    @Override //TEST FATTO
    public List<Note> getCondiviseConMe() throws NotingException {
        String me = requireUser().getUsername();
        return cerca.sharedWith(me);
    }

    @Override //TEST FATTO
    public void svuotaCondivisioneNota(int notaId) throws NotingException {
        String me = requireUser().getUsername();
        sharing.clearShareList(me, notaId);
    }

    @Override //TEST FATTO
    public List<String> getAllUsernames() {
        return sharing.getAllUsernames();
    }

    @Override //TEST FATTO
    public boolean cercaUtente(String username) throws NotingException {
        String me = requireUser().getUsername();
        return sharing.userExistsDifferentFrom(me, username);
    }

    @Override //TEST FATTO
    public boolean cercaUtente2(Note nota, String username) throws NotingException {
        String me = requireUser().getUsername();
        return sharing.userExistsDifferentFrom(nota, me, username);
    }

    @Override //TEST FATTO
    public Note aggiungiCondivisione(int noteId, String username) throws NotingException {
        String me = requireUser().getUsername();
        // opzionale: verifica lock se vuoi impedire cambi sharing durante editing altrui
        return sharing.addShare(me, noteId, username);
    }

    @Override //TEST FATTO
    public void annullaCondivisione(String username, int notaId) throws NotingException {
        // sicurezza: il parametro deve coincidere con l’utente loggato
        String me = requireUser().getUsername();
        if (!me.equals(username)) throw new NotingException("Username non corrispondente.");
        sharing.cancelShareForUser(me, notaId);
    }

    @Override //TEST FATTO
    public void rimuoviUtenteCondivisione(int notaId, String username) throws NotingException {
        String me = requireUser().getUsername();
        sharing.removeUserFromShare(me, notaId, username);
    }











    





    
    


    
    



     


 




  

   
    
   


    



    private User requireUser() throws NotingException {
    HttpServletRequest request = this.getThreadLocalRequest();
    HttpSession session = (request != null) ? request.getSession(false) : null;

    User user = (session != null) ? (User) session.getAttribute("user") : null;

    if (user != null) return user;

    // Modalità test: comoda per unit/integration test senza servlet
    if (TEST_USER != null) return TEST_USER;

    throw new NotingException("Utente non autenticato.");
    
    }

   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
    private void saveUserInSession(User user) {
        HttpServletRequest req = this.getThreadLocalRequest();
        if  (req == null) return;
        HttpSession session = req.getSession(true);
        if (session != null) session.setAttribute("user", user);
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


    
