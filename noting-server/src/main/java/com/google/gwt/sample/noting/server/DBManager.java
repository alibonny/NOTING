package com.google.gwt.sample.noting.server;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.mapdb.DB;
import org.mapdb.DBMaker;

// CLASSE CHE SI OCCUPA DELL'APERTURA DEL DB ALL'AVVIO E DELLA SUA CORRETTA CHIUSURA 

@WebListener
public class DBManager implements ServletContextListener {

    private static DB db;
    private static ConcurrentMap<String, String> usersDatabase;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Inizializzazione del database MapDB...");
        
        // percorso del file del database all'interno della webapp
        String dbPath = sce.getServletContext().getRealPath("/WEB-INF/noting.db");
        File dbFile = new File(dbPath);
        dbFile.getParentFile().mkdirs();

        db = DBMaker.fileDB(new File(dbPath))
                    .transactionEnable()
                    .closeOnJvmShutdown()
                    .make();

        // Ottiene la mappa degli utenti (o la crea se non esiste)
        usersDatabase = db.hashMap("users", org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING).createOrOpen();
        
        // Aggiungi un utente di default se la mappa è vuota
        if (usersDatabase.isEmpty()) {
            usersDatabase.put("test", "test");
            db.commit(); // Salva le modifiche
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
    
    public static void commit() {
        if (db != null && !db.isClosed()) {
            db.commit();
        }
    }
}