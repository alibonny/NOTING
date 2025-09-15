package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;
public class EliminaCondivisioneTest {

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
        NoteServiceImpl._clearTestUser();
    }

    @AfterEach
    void tearDown() {
        NoteServiceImpl._clearTestUser();
        DBManager.closeForTests();
    }



    // =============== TEST per rimuoviUtenteCondivisione() ===============

    @Test //caso di successo (caso normale)
    void rimuoviUtenteCondivisione_input_non_validi() throws Exception {
    // Simula login come alice
    NoteServiceImpl._setTestUser(new User("alice"));

    // Crea una nota di alice condivisa con bob
    var note = new Note("titolo", "contenuto", Note.Stato.Condivisa, new ArrayList<>(List.of("bob")), "alice");
    note.setId(1);

    // Popola DB
    DBManager.getNoteById().put(1, note);
    DBManager.getUsersDatabase().put("alice", "pwd1");
    DBManager.getUsersDatabase().put("bob", "pwd2");
    DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));
    DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("bob")));
    DBManager.commit();

    // Esegui rimozione
    service.rimuoviUtenteCondivisione(1, "bob");

    // --- Verifiche ---

    // Nota aggiornata: bob non deve più essere presente
    Note updated = DBManager.getNoteById().get(1);
    assertNotNull(updated);
    assertFalse(updated.getUtentiCondivisi().contains("bob"));

    // Mappa listaCondivisione aggiornata
    var list = DBManager.getListaCondivisione().get(1);
    assertNotNull(list);
    assertFalse(list.contains("bob"));
    assertEquals(0, list.size());

    // NotesDatabase dell'owner aggiornata
    var notesOfAlice = DBManager.getNotesDatabase().get("alice");
    assertEquals(1, notesOfAlice.size());
    assertFalse(notesOfAlice.get(0).getUtentiCondivisi().contains("bob"));
}

    @Test
    void rimuoviUtenteCondivisione_caller_non_proprietario() {
        NoteServiceImpl._setTestUser(new User("charlie"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa,
                new ArrayList<>(List.of("bob")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("charlie", "pwd3");
        DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("bob")));
        DBManager.commit();

        assertThrows(NotingException.class, () ->
                service.rimuoviUtenteCondivisione(1, "bob"));
    }

    @Test
    void rimuoviUtenteCondivisione_nota_inesistente() {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd1");

        assertThrows(NotingException.class, () ->
                service.rimuoviUtenteCondivisione(999, "bob"));
    }

    @Test
    void rimuoviUtenteCondivisione_username_mancante() {
        NoteServiceImpl._setTestUser(new User("alice"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa,
                new ArrayList<>(List.of("bob")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("bob")));
        DBManager.commit();

        assertThrows(NotingException.class, () ->
                service.rimuoviUtenteCondivisione(1, null));

        assertThrows(NotingException.class, () ->
                service.rimuoviUtenteCondivisione(1, "   "));
    }

    @Test
    void rimuoviUtenteCondivisione_username_non_in_condivisione() {
        NoteServiceImpl._setTestUser(new User("alice"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa,
                new ArrayList<>(List.of("bob")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");
        DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("bob")));
        DBManager.commit();

        assertThrows(NotingException.class, () ->
                service.rimuoviUtenteCondivisione(1, "charlie"));
    }



    // =============== TEST per annullaCondivisione() ===============

    @Test //caso di successo (caso nromale)
    void annullaCondivisione_input_invalidi() throws Exception {
        // utente loggato = bob
        NoteServiceImpl._setTestUser(new User("bob"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa,
                new ArrayList<>(List.of("bob", "charlie")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");
        DBManager.getUsersDatabase().put("charlie", "pwd3");
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));
        DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("bob", "charlie")));
        DBManager.commit();

        service.annullaCondivisione("bob", 1);

        // Nota aggiornata
        Note updated = DBManager.getNoteById().get(1);
        assertNotNull(updated);
        assertFalse(updated.getUtentiCondivisi().contains("bob"));
        assertTrue(updated.getUtentiCondivisi().contains("charlie"));

        // lista condivisione aggiornata
        var list = DBManager.getListaCondivisione().get(1);
        assertEquals(1, list.size());
        assertEquals("charlie", list.get(0));

        // notesDatabase aggiornato
        var notesOfAlice = DBManager.getNotesDatabase().get("alice");
        assertEquals(1, notesOfAlice.size());
        assertFalse(notesOfAlice.get(0).getUtentiCondivisi().contains("bob"));
    }

    @Test
    void annullaCondivisione_caller_diverso_utente_loggato() {
        NoteServiceImpl._setTestUser(new User("bob"));
        DBManager.getUsersDatabase().put("bob", "pwd2");

        assertThrows(NotingException.class, () ->
        service.annullaCondivisione("alice", 1));
    }

    @Test
    void annullaCondivisione_username_mancante() {
        NoteServiceImpl._setTestUser(new User("bob"));
        DBManager.getUsersDatabase().put("bob", "pwd2");

        assertThrows(NotingException.class, () ->
            service.annullaCondivisione(null, 1));

        assertThrows(NotingException.class, () ->
            service.annullaCondivisione("   ", 1));
    }

    @Test
    void annullaCondivisione_utente_non_in_lista_condivisa() {
        NoteServiceImpl._setTestUser(new User("bob"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa,
                new ArrayList<>(List.of("charlie")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");
        DBManager.getUsersDatabase().put("charlie", "pwd3");
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));
        DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("charlie")));
        DBManager.commit();

        assertThrows(NotingException.class, () ->
            service.annullaCondivisione("bob", 1));
    }

    @Test
    void annullaCondivisione_nota_inesistente() {
        NoteServiceImpl._setTestUser(new User("bob"));
        DBManager.getUsersDatabase().put("bob", "pwd2");

        assertThrows(NotingException.class, () ->
            service.annullaCondivisione("bob", 0));

        assertThrows(NotingException.class, () ->
            service.annullaCondivisione("bob", 999));
    }



    // =============== TEST per svuotaCondivisioneNota() ===============

    @Test //caso normale (la lista di condivisione viene svuotata dopo che la nota passa da CONDIVISA a PRIVATA)
    void svuotaCondivisioneNota_successo() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa,
                new ArrayList<>(List.of("bob", "charlie")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");
        DBManager.getUsersDatabase().put("charlie", "pwd3");
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));
        DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("bob", "charlie")));
        DBManager.commit();

        service.svuotaCondivisioneNota(1);

        // Nota aggiornata senza utenti condivisi
        Note updated = DBManager.getNoteById().get(1);
        assertNotNull(updated);
        assertTrue(updated.getUtentiCondivisi().isEmpty());

        // Lista condivisione vuota
        var list = DBManager.getListaCondivisione().get(1);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        // notesDatabase aggiornato
        var notesOfAlice = DBManager.getNotesDatabase().get("alice");
        assertEquals(1, notesOfAlice.size());
        assertTrue(notesOfAlice.get(0).getUtentiCondivisi().isEmpty());
    }


    @Test
    void svuotaCondivisioneNota_nota_inesistente() {
        NoteServiceImpl._setTestUser(new User("alice"));
        assertThrows(NotingException.class, () -> service.svuotaCondivisioneNota(0)); //id non valido
        assertThrows(NotingException.class, () -> service.svuotaCondivisioneNota(99)); //nota non esistente
    }

    @Test
    void svuotaCondivisioneNota_caller_non_proprietario() {
        NoteServiceImpl._setTestUser(new User("bob"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa,
                new ArrayList<>(List.of("bob")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));
        DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("bob")));
        DBManager.commit();

        assertThrows(NotingException.class, () -> service.svuotaCondivisioneNota(1));
    }

    @Test
    void svuotaCondivisioneNota_lista_non_trovata() {
        NoteServiceImpl._setTestUser(new User("alice"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa, new ArrayList<>(List.of("bob")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));

        DBManager.commit();

        assertThrows(NotingException.class, () -> service.svuotaCondivisioneNota(1));
    }
}