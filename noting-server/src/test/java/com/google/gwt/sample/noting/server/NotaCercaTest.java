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
public class NotaCercaTest {

    @TempDir
    Path tempDir;

    private Path dbFile;
    private NoteServiceImpl service;
    private final User TEST_USER = new User("thomas");

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



    // =============== TEST per searchNotes() ===============


        // 1) Query mancante
    @Test
    void searchNotes_query_non_valida() {
        NoteServiceImpl._setTestUser(new User("alice"));
        assertThrows(NotingException.class, () -> service.searchNotes(null));
        assertThrows(NotingException.class, () -> service.searchNotes(""));
        assertThrows(NotingException.class, () -> service.searchNotes("   "));
    }

    // 2) Utente mancante
    @Test
    void searchNotes_utente_non_loggato() {
        assertThrows(NotingException.class, () -> service.searchNotes("ciao"));
    }

    // 3) Utente presente ma senza note
    @Test
    void searchNotes_utente_senza_note() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        List<Note> result = service.searchNotes("qualcosa");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // 4) Utente e query validi con note che matchano
    @Test
    void searchNotes_successo() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));

        List<Note> aliceNotes = Arrays.asList(
            new Note("Lista Spesa", "latte uova pane", Note.Stato.Privata),
            new Note("Appunti Reti", "TCP/IP e routing", Note.Stato.Privata),
            new Note("Idee progetto", "Prova con GWT", Note.Stato.Privata)
        );
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(aliceNotes));
        DBManager.commit();

        // match su titolo
        List<Note> r1 = service.searchNotes("lista");
        assertEquals(1, r1.size());
        assertEquals("Lista Spesa", r1.get(0).getTitle());

        // match su contenuto
        List<Note> r2 = service.searchNotes("ROUTING");
        assertEquals(1, r2.size());
        assertEquals("Appunti Reti", r2.get(0).getTitle());

    }



    // =============== TEST per getNotaById() ===============


    @Test
    void getNotaById_successo() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));

        var note = new Note("titolo", "contenuto", Note.Stato.Condivisa,
                new ArrayList<>(List.of("bob", "charlie")), "alice");
        note.setId(1);

        DBManager.getNoteById().put(1, note);
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");
        DBManager.getUsersDatabase().put("charlie", "pwd3");
        DBManager.getListaCondivisione().put(1, new ArrayList<>(List.of("bob", "charlie")));
        DBManager.getNotesDatabase().put("alice", new ArrayList<>(Arrays.asList(note)));
        DBManager.commit();

        Note dto = service.getNotaById(1);

        // DTO corretto
        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("titolo", dto.getTitle());
        assertEquals("contenuto", dto.getContent());
        assertEquals(Note.Stato.Condivisa, dto.getStato());
        assertEquals("alice", dto.getOwnerUsername());
        assertTrue(dto.getUtentiCondivisi().contains("bob"));
        assertTrue(dto.getUtentiCondivisi().contains("charlie"));

        // Modifica DTO non deve impattare l'oggetto originale
        dto.getUtentiCondivisi().add("INTRUSO");
        Note original = DBManager.getNoteById().get(1);
        assertFalse(original.getUtentiCondivisi().contains("INTRUSO"));
        assertEquals(2, original.getUtentiCondivisi().size());
    }

    @Test
    void getNotaById_id_non_valido() {
        NoteServiceImpl._setTestUser(new User("alice"));
        assertThrows(NotingException.class, () -> service.getNotaById(0));
        assertThrows(NotingException.class, () -> service.getNotaById(-5));
    }

    @Test
    void getNotaById_nota_inesistente() {
        NoteServiceImpl._setTestUser(new User("alice"));
        assertThrows(NotingException.class, () -> service.getNotaById(99));
    }
    
}