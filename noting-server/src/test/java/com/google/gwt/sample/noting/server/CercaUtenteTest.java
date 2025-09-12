package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.server.DBManager;
import com.google.gwt.sample.noting.server.NoteServiceImpl;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;
public class CercaUtenteTest {

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

    // =============== TEST per cercaUtente() ===============

    @Test
    void cercaUtente_returnsTrue_whenUserExistsAndIsDifferent() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");

        assertTrue(service.cercaUtente("bob"));
    }

    @Test
    void cercaUtente_returnsFalse_whenUserDoesNotExist() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd1");

        assertFalse(service.cercaUtente("bob"));
    }

    @Test
    void cercaUtente_returnsFalse_whenCandidateIsNullOrBlank() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd1");

        assertFalse(service.cercaUtente(null));
        assertFalse(service.cercaUtente("   "));
    }

    @Test
    void cercaUtente_returnsFalse_whenCandidateIsSameAsCaller() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd1");

        assertFalse(service.cercaUtente("alice"));
    }

    @Test
    void cercaUtente_throwsException_whenCallerNotAuthenticated() {
        assertThrows(NotingException.class, () -> service.cercaUtente("bob"));
    }

    // =============== TEST per cercaUtente2() ===============

    @Test
    void cercaUtente2_returnsTrue_whenUserExistsAndIsDifferent() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd1");
        DBManager.getUsersDatabase().put("bob", "pwd2");

        Note nota = new Note("titolo", "c", Note.Stato.Privata, new ArrayList<>(), "alice");

        assertTrue(service.cercaUtente2(nota, "bob"));
    }

    @Test
    void cercaUtente2_returnsFalse_whenUserDoesNotExist() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd1");

        Note nota = new Note("titolo", "c", Note.Stato.Privata, new ArrayList<>(), "alice");

        assertFalse(service.cercaUtente2(nota, "bob"));
    }

    @Test
    void cercaUtente2_returnsFalse_whenCandidateIsNullOrBlankOrSameAsCaller() throws Exception {
        NoteServiceImpl._setTestUser(new User("alice"));
        DBManager.getUsersDatabase().put("alice", "pwd1");

        Note nota = new Note("titolo", "c", Note.Stato.Privata, new ArrayList<>(), "alice");

        assertFalse(service.cercaUtente2(nota, null));
        assertFalse(service.cercaUtente2(nota, "   "));
        assertFalse(service.cercaUtente2(nota, "alice"));
    }

    @Test
    void cercaUtente2_throwsException_whenCallerNotAuthenticated() {
        Note nota = new Note("titolo", "c", Note.Stato.Privata, new ArrayList<>(), "alice");

        assertThrows(NotingException.class, () -> service.cercaUtente2(nota, "bob"));
    }



    // =============== TEST per getAllUsernames() ===============

    @Test //Database vuoto --> ritorna lista vuota
    void getAllUsernames_returnsEmptyList_whenNoUsersPresent() {
        assertTrue(service.getAllUsernames().isEmpty());
    }

    @Test //Database con più utenti --> ritorna tutte le chiavi (caso normale)
    void getAllUsernames_returnsAllKeysFromUserDatabase() {
        Map<String, String> userDb = DBManager.getUsersDatabase();
        userDb.put("alice", "pwd1");
        userDb.put("bob", "pwd2");
        userDb.put("charlie", "pwd3");
        DBManager.commit();

        var result = service.getAllUsernames();

        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList("alice", "bob", "charlie")));
    }

    @Test //Difensive copy --> la lista restituita non deve modificare il DB
    void getAllUsernames_returnsNewList_notBackedByDatabase() {
        Map<String, String> userDb = DBManager.getUsersDatabase();
        userDb.put("alice", "pwd1");
        DBManager.commit();

        var result = service.getAllUsernames();
        result.add("intruso");

        // DB non deve essere alterato
        var again = service.getAllUsernames();
        assertEquals(1, again.size());
        assertEquals("alice", again.get(0));
    }
}

