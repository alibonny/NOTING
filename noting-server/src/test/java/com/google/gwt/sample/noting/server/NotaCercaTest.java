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

        // 1) Query mancante
    @Test
    void searchNotes_throwsException_whenQueryIsNullOrEmpty() {
        NoteServiceImpl._setTestUser(new User("alice"));
        assertThrows(NotingException.class, () -> service.searchNotes(null));
        assertThrows(NotingException.class, () -> service.searchNotes(""));
        assertThrows(NotingException.class, () -> service.searchNotes("   "));
    }

    // 2) Utente mancante
    @Test
    void searchNotes_throwsException_whenUserIsNotAuthenticated() {
        assertThrows(NotingException.class, () -> service.searchNotes("ciao"));
    }

    // 3) Utente presente ma senza note
    @Test
    void searchNotes_returnsEmptyList_whenUserHasNoNotes() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        List<Note> result = service.searchNotes("qualcosa");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // 4) Utente e query validi con note che matchano
    @Test
    void searchNotes_returnsMatchingNotes_whenTitleOrContentContainsQuery() throws Exception {
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

       


    




    
}
