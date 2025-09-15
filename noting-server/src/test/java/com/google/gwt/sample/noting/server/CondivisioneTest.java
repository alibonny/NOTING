package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

public class CondivisioneTest {
     @TempDir
    Path tempDir;

    private Path dbFile;
    private NoteServiceImpl service;

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("mapdb-test.db");
        DBManager.openForTests(dbFile);
        DBManager.resetForTests();
        service = new NoteServiceImpl();
        service._clearTestUser();
    }

    @AfterEach
    void tearDown() {
        NoteServiceImpl._clearTestUser();
        DBManager.closeForTests();
    }
    

    // =============== TEST per aggiungiCondivisione() ===============

    

    @Test
    void aggiungiCondivisione_username_invalido() {
        NoteServiceImpl._setTestUser(new User("alice"));
        assertThrows(NotingException.class, () -> service.aggiungiCondivisione(1, null));
        assertThrows(NotingException.class, () -> service.aggiungiCondivisione(1, "   "));
    }

    @Test
    void aggiungiCondivisione_note_non_esistente() {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd");
        DBManager.getUsersDatabase().put("bob", "pwd");
        DBManager.commit();

        assertThrows(NotingException.class, () -> service.aggiungiCondivisione(999, "bob"));
    }

    @Test
    void aggiungiCondivisione_caller_non_proprietario() {
        NoteServiceImpl._setTestUser(new User("intruso"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa, new ArrayList<>(), "owner");
        note.setId(1);
        DBManager.getNoteById().put(1, note);

        DBManager.getUsersDatabase().put("owner", "pwd");
        DBManager.getUsersDatabase().put("intruso", "pwd");
        DBManager.getUsersDatabase().put("bob", "pwd");
        DBManager.commit();

        assertThrows(NotingException.class, () -> service.aggiungiCondivisione(1, "bob"));
    }

    @Test
    void aggiungiCondivisione_utente_non_esistente() {
        NoteServiceImpl._setTestUser(new User("alice"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa, new ArrayList<>(), "alice");
        note.setId(1);
        DBManager.getNoteById().put(1, note);

        DBManager.getUsersDatabase().put("alice", "pwd");
        DBManager.commit();

        assertThrows(NotingException.class, () -> service.aggiungiCondivisione(1, "bob"));
    }

    @Test
    void aggiungiCondivisione_con_se_stesso() {
        NoteServiceImpl._setTestUser(new User("alice"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa, new ArrayList<>(), "alice");
        note.setId(1);
        DBManager.getNoteById().put(1, note);

        DBManager.getUsersDatabase().put("alice", "pwd");
        DBManager.commit();

        assertThrows(NotingException.class, () -> service.aggiungiCondivisione(1, "alice"));
    }

    @Test //caso di successo (normale)
    void aggiungiCondivisione_successo() throws Exception {
         // utente autenticato
        NoteServiceImpl._setTestUser(new User("alice"));

        // nota esistente
        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa, new ArrayList<>(), "alice");
        note.setId(1);
        DBManager.getNoteById().put(1, note);

        // utenti esistenti
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");

        // nota anche nella lista per "alice"
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));
        DBManager.commit();

        //chiamata del metodo aggiungiCondivisione()
        Note updated = service.aggiungiCondivisione(1, "bob");

        //controlli principali (la nota condivisa non deve essere null e tra gli utenti condivisi deve esserci "bob")
        assertNotNull(updated);
        assertTrue(updated.getUtentiCondivisi().contains("bob"));

        // deve aggiornare la nota in notesDatabase
        var notesOfAlice = DBManager.getNotesDatabase().get("alice");
        assertEquals(1, notesOfAlice.size());
        assertTrue(notesOfAlice.get(0).getUtentiCondivisi().contains("bob"));

        // deve aggiornare anche la mappa listaCondivisione
        var list = DBManager.getListaCondivisione().get(1);
        assertEquals(1, list.size());
        assertEquals("bob", list.get(0));

    }

    @Test
    void aggiungiCondivisione_utente_già_condiviso() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa, new ArrayList<>(Arrays.asList("bob")), "alice");
        note.setId(1);
        DBManager.getNoteById().put(1, note);

        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");

        DBManager.getListaCondivisione().put(1, new ArrayList<>(Arrays.asList("bob")));
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));
        DBManager.commit();

        Note updated = service.aggiungiCondivisione(1, "bob");

        // Non deve duplicare bob
        var list = DBManager.getListaCondivisione().get(1);
        assertEquals(1, list.size());
        assertEquals("bob", list.get(0));
    }

}