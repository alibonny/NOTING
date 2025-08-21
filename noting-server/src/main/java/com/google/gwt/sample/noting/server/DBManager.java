package com.google.gwt.sample.noting.server;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.Atomic;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.User;


@WebListener
public class DBManager implements ServletContextListener {

    private static DB db;
    private static ConcurrentMap<String, String> usersDatabase;
    private static ConcurrentMap<String, List<Note>> notesDatabase; // Mappa per le note
    private static ConcurrentMap<Integer, List<String>> listaCondivisione; // Mappa per le note condivise
    private static Atomic.Var<Integer> noteIdCounter;


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
}