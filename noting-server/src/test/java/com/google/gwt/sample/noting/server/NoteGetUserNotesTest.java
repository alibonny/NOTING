package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class NoteGetUserNotesTest {

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
 // 1) Nessun utente (né sessione, né TEST_USER) -> eccezione
    @Test
    void getNoteUtente_utente_non_loggato() {
        assertThrows(NotingException.class, () -> service.getNoteUtente());
    }

    // 2) Utente presente ma senza note -> lista vuota
    @Test
    void getNoteUtente_utente_senza_note() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        List<Note> notes = service.getNoteUtente();
        assertNotNull(notes);
        assertTrue(notes.isEmpty());
    }

    // 3) Utente con note -> restituisce copia difensiva (modifiche non impattano il DB)
    @Test
    void getNoteUtente_defensive_db() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));

        List<Note> stored = Arrays.asList(
            new Note("T1", "C1", Note.Stato.Privata),
            new Note("T2", "C2", Note.Stato.Privata)
        );
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(stored));
        DBManager.commit();

        List<Note> returned = service.getNoteUtente();
        assertEquals(2, returned.size());

        // Provo a modificare la lista restituita
        returned.add(new Note("INTRUSO", "X", Note.Stato.Privata));

        // La lista nel DB non deve cambiare
        List<Note> again = service.getNoteUtente();
        assertEquals(2, again.size());
        assertTrue(again.stream().noneMatch(n -> "INTRUSO".equals(n.getTitle())));
    }

    // 4) Non restituisce note di altri utenti
    @Test
    void getNoteUtente_note_utente() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));

        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(
            new Note("Mie", "solo mie", Note.Stato.Privata)
        )));
        DBManager.getNotesDatabase().put("bob", new ArrayList<>(Arrays.asList(
            new Note("DiBob", "non mie", Note.Stato.Privata)
        )));
        DBManager.commit();

        List<Note> notes = service.getNoteUtente();
        assertEquals(1, notes.size());
        assertEquals("Mie", notes.get(0).getTitle());
    }


    // Note condivise con utente 
    @Test
    void getCondiviseConMe_utente_non_autenticato() {
        assertThrows(NotingException.class, () -> service.getCondiviseConMe());
    }

    @Test
    void getCondiviseConMe_utente_senza_condivisioni() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        // Nessuna voce nelle mappe
        assertTrue(service.getCondiviseConMe().isEmpty());
    }

    @Test
    void getCondiviseConMe_successo() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));

        // Note globali (per ID)
        Note n1 = new Note("Spec di progetto", "testo", Note.Stato.Condivisa, new ArrayList<>(), "bob");
        Note n2 = new Note("Mia nota", "contenuto", Note.Stato.Condivisa, new ArrayList<>(), "alice"); // deve essere esclusa
        Note n3 = new Note("Appunti ML", "gradiente", Note.Stato.Condivisa, new ArrayList<>(), "charlie");

        DBManager.getNoteById().put(1, n1);
        DBManager.getNoteById().put(2, n2);
        DBManager.getNoteById().put(3, n3);

        // Lista condivisione (notaId -> utenti)
        DBManager.getListaCondivisione().put(1, new ArrayList<>(Arrays.asList("alice")));        // include alice → OK (bob → alice)
        DBManager.getListaCondivisione().put(2, new ArrayList<>(Arrays.asList("alice")));        // include alice ma owner=alice → ESCLUDI
        DBManager.getListaCondivisione().put(3, new ArrayList<>(Arrays.asList("charlie")));      // non include alice → IGNORA
        DBManager.commit();

        var result = service.getCondiviseConMe();
        assertEquals(1, result.size());
        assertEquals("Spec di progetto", result.get(0).getTitle());
    }

    @Test
    void getCondiviseConMe_input_non_valido() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));

        // Condivisione che punta a una nota inesistente
        DBManager.getListaCondivisione().put(99, new ArrayList<>(Arrays.asList("alice")));
        // Nessuna put in noteById per id=99
        DBManager.commit();

        var result = service.getCondiviseConMe();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}