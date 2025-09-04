package com.google.gwt.sample.noting.server;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

class AuthServiceTest {

    @TempDir
    Path tempDir; // cartella temporanea diversa ad ogni run

    @BeforeEach
    void setUp() {
        DBManager.openForTests(tempDir.resolve("testdb.mapdb"));
        DBManager.resetForTests();

        // preparo l’utente nel DB di test (file)
        DBManager.getUsersDatabase().put("mario", "password");
        DBManager.commit();
    }

    @AfterEach
    void tearDown() {
        DBManager.closeForTests();
    }

    @Test
    void login_ok_leggeDalFile() throws Exception {
        NoteServiceImpl service = new NoteServiceImpl();
        User u = service.login("mario", "password");
        assertNotNull(u);
        assertEquals("mario", u.getUsername());
    }

    @Test
    void login_passwordSbagliata_lancia() {
        NoteServiceImpl service = new NoteServiceImpl();
        assertThrows(NotingException.class, () -> service.login("mario", "wrong"));
    }

    @Test
    void login_inputNull_lancia() {
        NoteServiceImpl service = new NoteServiceImpl();
        assertAll(
            () -> assertThrows(NotingException.class, () -> service.login(null, "x")),
            () -> assertThrows(NotingException.class, () -> service.login("mario", null))
        );
    }
 
    @Test
    void register_scriveNelDbDiTest() throws Exception {
    NoteServiceImpl service = new NoteServiceImpl();
    User u = service.register("alice", "P@ssw0rd!");
    assertEquals("alice", u.getUsername());
    assertEquals("P@ssw0rd!", DBManager.getUsersDatabase().get("alice"));
    }


}
