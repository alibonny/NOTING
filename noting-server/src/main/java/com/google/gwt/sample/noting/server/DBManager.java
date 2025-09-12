package com.google.gwt.sample.noting.server;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteHistory;


@WebListener
public class DBManager implements ServletContextListener {

    // --- Stato e componenti base ---
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

    private static ConcurrentMap<Integer, NoteHistory> noteHistoryDatabase;

    private static void initMaps() {
        // Crea/riapre tutte le strutture
        usersDatabase = db.hashMap("users", Serializer.STRING, Serializer.STRING).createOrOpen();
        notesDatabase = db.hashMap("notes", Serializer.STRING, Serializer.JAVA).createOrOpen();
        listaCondivisione = db.hashMap("listaCondivisione", Serializer.INTEGER, Serializer.JAVA).createOrOpen();
        noteById = db.hashMap("noteById", Serializer.INTEGER, Serializer.JAVA).createOrOpen();
        noteHistoryDatabase = db.hashMap("noteHistory", Serializer.INTEGER, Serializer.JAVA).createOrOpen();

        noteIdCounter = db.atomicVar("noteIdCounter", Serializer.INTEGER).createOrOpen();
        if (noteIdCounter.get() == null) {
            noteIdCounter.set(1);
        }

        // riallineo indice se vuoto ma ci sono note
        if (noteById.isEmpty() && !notesDatabase.isEmpty()) {
            for (List<Note> list : notesDatabase.values()) {
                if (list == null) continue;
                for (Note n : list) {
                    if (n != null) noteById.put(n.getId(), n);
                }
            }
            db.commit();
        }
    }
    private static void ensureOpenInMemory() {
        if (db == null || db.isClosed()) {
            db = DBMaker.memoryDB()
                    .transactionEnable()
                    .make();
        }
    
        if (usersDatabase == null || notesDatabase == null || listaCondivisione == null || noteById == null || noteIdCounter == null || noteHistoryDatabase == null) {
            initMaps();
        }
    }

    // Apri un DB MapDB su un file (diverso da quello “vero”) per i test
    public static void openForTests(Path filePath) {
        closeForTests(); // chiude eventuale DB precedente
        db = DBMaker.fileDB(filePath.toFile())
                .transactionEnable()
                .closeOnJvmShutdown()
                .make();
        initMaps();
    }

    // Pulisci i dati tra un test e l'altro
    public static void resetForTests() {
        ensureOpenInMemory();
        usersDatabase.clear();
        notesDatabase.clear();
        listaCondivisione.clear();
        noteById.clear(); 
        noteHistoryDatabase.clear(); 
        noteIdCounter.set(1);
        db.commit();
    }

    // Chiudi e azzera i riferimenti (fine test)
    public static void closeForTests() {
        try {
            if (db != null && !db.isClosed()) db.close();
        } catch (Exception ignore) {}
        db = null;
        usersDatabase = null;
        notesDatabase = null;
        listaCondivisione = null;
        noteById = null;
        noteIdCounter = null;
        noteHistoryDatabase = null;
    }

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

        initMaps(); // <--- usa questo, niente duplicati sotto

        if (usersDatabase.isEmpty()) {
            usersDatabase.put("test", "test");
            db.commit();
            System.out.println("Utente di default 'test' creato.");
        }
        System.out.println("Database inizializzato con successo.");
    }

                /* 
    // Mappa degli utenti
    usersDatabase = db.hashMap("users", Serializer.STRING, Serializer.STRING).createOrOpen();
    
    // Mappa delle note
    notesDatabase = db.hashMap("notes", Serializer.STRING, Serializer.JAVA).createOrOpen();

    listaCondivisione = db.hashMap("listaCondivisione", Serializer.INTEGER, Serializer.JAVA).createOrOpen();

    noteById = db.hashMap("noteById", Serializer.INTEGER, Serializer.JAVA).createOrOpen();

    noteIdCounter = db.atomicVar("noteIdCounter", Serializer.INTEGER).createOrOpen();   
    // --- riallinea indice se vuoto ma ci sono già note salvate ---
    if (noteById.isEmpty() && !notesDatabase.isEmpty()) {
        for (List<Note> list : notesDatabase.values()) {
            if (list == null) continue;
            for (Note n : list) {
                if (n != null && n.getId() > maxId) maxId = n.getId();
            }
        }

        */
    //    db.commit();
    // }
    /* 
        noteIdCounter = db.atomicVar("noteIdCounter", Serializer.INTEGER).createOrOpen();
        if (noteIdCounter.get() == null) {
        noteIdCounter.set(1);
        }

        if (usersDatabase.isEmpty()) {
            usersDatabase.put("test", "test");
            db.commit();
        }
    }

    // --- Helpers di basso livello ---

    private static void ensureDbOpen() {
        if (db == null || db.isClosed()) {
            throw new IllegalStateException("DB non inizializzato. Avvia il container servlet o usa openForTests().");
        }
    }

    private static void ensureReady() {
        ensureDbOpen();
        if (usersDatabase == null || notesDatabase == null || listaCondivisione == null || noteById == null || noteIdSeq == null) {
            throw new IllegalStateException("Strutture MapDB non pronte. Chiama initMapsAndIndexes().");
        }
    }

    private static void safeClose() {
        try {
            if (db != null && !db.isClosed()) db.close();
        } catch (Exception ignore) {
            // best effort
        }
    }

    private static void clearRefs() {
        db = null;
        usersDatabase = null;
        notesDatabase = null;
        listaCondivisione = null;
        noteById = null;
        noteIdSeq = null;
    }

    // --- API Pubblica ---

    /** Restituisce la mappa utenti (username->password). */
    public static ConcurrentMap<String, String> getUsersDatabase() {
        ensureReady();
        return usersDatabase;
    }

    /** Restituisce la mappa delle note possedute per owner. */
    public static ConcurrentMap<String, List<Note>> getNotesDatabase() {
        ensureReady();
        return notesDatabase;
    }

    /** Restituisce la mappa condivisioni (noteId->lista destinatari). */
    public static ConcurrentMap<Integer, List<String>> getListaCondivisione() {
        ensureReady();
        return listaCondivisione;
    }

    /** Indice globale note per ID. */
    public static ConcurrentMap<Integer, Note> getNoteById() {
        ensureReady();
        return noteById;
    }

    public static ConcurrentMap<Integer, NoteHistory> getNoteHistoryDatabase() {
        if (noteHistoryDatabase == null) ensureOpenInMemory();
        return noteHistoryDatabase;
    }

    public static void commit() {
        ensureDbOpen();
        db.commit();
    }

    /** Utility: note possedute da utente. */
    public static List<Note> getUserOwnedNotes(String username) {
        ensureReady();
        return notesDatabase.getOrDefault(username, List.of());
    }

    /** Utility: note condivise con utente (non owner). */
    public static List<Note> getUserSharedNotes(String username) {
        ensureReady();
        List<Note> shared = new ArrayList<>();
        if (username == null) return shared;

        for (Map.Entry<Integer, List<String>> entry : listaCondivisione.entrySet()) {
            Integer id = entry.getKey();
            List<String> destinatari = entry.getValue();

            if (destinatari != null && destinatari.contains(username)) {
                Note n = noteById.get(id);
                if (n != null && !username.equals(n.getOwnerUsername())) {
                    shared.add(n);
                }
            }
        }
        return shared;
    }





}






