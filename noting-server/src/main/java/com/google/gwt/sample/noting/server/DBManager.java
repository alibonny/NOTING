package com.google.gwt.sample.noting.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.google.gwt.sample.noting.shared.Note;


@WebListener
public class DBManager implements ServletContextListener {

    private static DB db;
    //utente registrati con username e password
    private static ConcurrentMap<String, String> usersDatabase;
    // tutte le note create da ciascuno utente, dove memorizza le note per owner
    private static ConcurrentMap<String, List<Note>> notesDatabase; // Mappa per le note
    // lista di username a cui è condivisa una nota, dice a chi + condivisa ogni nota per id
    private static ConcurrentMap<Integer, List<String>> listaCondivisione; // Mappa per le note condivise
    // contatore per gli ID delle note
    private static Atomic.Var<Integer> noteIdCounter;
    // undice idNota -> Nota, è un indice veloce per recuperare una nota dall'ID
    private static ConcurrentMap<Integer,Note> noteById;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Inizializzazione del database MapDB...");
        
        String dbPath = sce.getServletContext().getRealPath("/WEB-INF/noting.db");
        File dbFile = new File(dbPath);
        dbFile.getParentFile().mkdirs();

        db = DBMaker.fileDB(new File(dbPath))
                    .transactionEnable()
                    .closeOnJvmShutdown()
                    .make();

        // Mappa degli utenti
        usersDatabase = db.hashMap("users", Serializer.STRING, Serializer.STRING).createOrOpen();
        
        // Mappa delle note
        notesDatabase = db.hashMap("notes", Serializer.STRING, Serializer.JAVA).createOrOpen();

        listaCondivisione = db.hashMap("listaCondivisione", Serializer.INTEGER, Serializer.JAVA).createOrOpen();

        noteById = db.hashMap("noteById", Serializer.INTEGER, Serializer.JAVA).createOrOpen();


        // --- riallinea indice se vuoto ma ci sono già note salvate ---
    if (noteById.isEmpty() && !notesDatabase.isEmpty()) {
        for (List<Note> list : notesDatabase.values()) {
            if (list == null) continue;
            for (Note n : list) {
                if (n != null) noteById.put(n.getId(), n);
            }
        }
        db.commit();
    }

        noteIdCounter = db.atomicVar("noteIdCounter", Serializer.INTEGER).createOrOpen();
        if (noteIdCounter.get() == null) {
        noteIdCounter.set(1);
        }

        if (usersDatabase.isEmpty()) {
            usersDatabase.put("test", "test");
            db.commit();
            System.out.println("Utente di default 'test' creato.");
        }
        
        System.out.println("Database inizializzato con successo.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (db != null && !db.isClosed()) {
            db.close();
            System.out.println("Database MapDB chiuso correttamente.");
        }
    }

    public static ConcurrentMap<String, String> getUsersDatabase() {
        return usersDatabase;
    }
    
    public static ConcurrentMap<String, List<Note>> getNotesDatabase() {
        return notesDatabase;
    }

    public static ConcurrentMap<Integer, List<String>> getListaCondivisione() {
        return listaCondivisione;
    }
    
    public static void commit() {
        if (db != null && !db.isClosed()) {
            db.commit();
        }
    }

    public static Atomic.Var<Integer> getNoteIdCounter() {
    return noteIdCounter;
    }

    //Lista delle note di un utente
    public static List<Note> getUserOwnedNotes(String username) {
        return notesDatabase.getOrDefault(username,List.of());
    }

    public static List<Note> getUserSharedNotes(String username) {
    List<Note> shared = new ArrayList<>();
    if (username == null) return shared;

    for (Map.Entry<Integer, List<String>> entry : listaCondivisione.entrySet()) {
        Integer id = entry.getKey();
        List<String> destinatari = entry.getValue();

        if (destinatari != null && destinatari.contains(username)) {
            Note n = noteById.get(id);
            if (n != null && !username.equals(n.getOwnerUsername())) { // opzionale: escludi le tue
                shared.add(n);
            }
        }
    }

    // opzionale: se vuoi evitare che chi modifica la lista tocchi gli oggetti originali
    // return Collections.unmodifiableList(shared);

    return shared;
    }

    

    public static ConcurrentMap<Integer, Note> getNoteById() {
        return noteById;
    }
}






