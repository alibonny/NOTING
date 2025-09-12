package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;


public class AuthServiceTest {

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
    void tearDown(){
        NoteServiceImpl._clearTestUser();
        DBManager.closeForTests();
    }


    // -------------------------
    // REGISTER
    // -------------------------

    @Test
    void register_reject_null_or_empty(){
        assertThrows(NotingException.class, () -> service.register(null, "x"));
        assertThrows(NotingException.class, () -> service.register("bob", null));
        assertThrows(NotingException.class, () -> service.register("   ", "x"));
        assertThrows(NotingException.class, () -> service.register("bob", ""));
    }

    @Test
    void register_success_persists_user() throws Exception{
        User u = service.register("newUser", "pwd123");
        assertNotNull(u);
        assertEquals("newUser", u.getUsername());

        ConcurrentMap<String,String> users = DBManager.getUsersDatabase();
        assertEquals("pwd123", users.get("newUser"));
    }

    @Test
    void register_duplicate_username_throws() throws Exception {
        service.register("alice", "a1");
        assertThrows(NotingException.class, () -> service.register("alice", "a2"));
    }

    // -------------------------
    // LOGIN
    // -------------------------
      @Test
    void login_rejects_null_or_empty() {
        assertThrows(NotingException.class, () -> service.login(null, "x"));
        assertThrows(NotingException.class, () -> service.login("x", null));
        assertThrows(NotingException.class, () -> service.login(" ", " "));
    }

    @Test
    void login_wrong_user_or_password_throws() {
        DBManager.getUsersDatabase().put("mario", "ok");
        DBManager.commit();

        assertThrows(NotingException.class, () -> service.login("mario", "bad"));
        assertThrows(NotingException.class, () -> service.login("unknown", "x"));
    }

    @Test
    void login_success_returns_user() throws Exception {
        DBManager.getUsersDatabase().put("eva", "pwd");
        DBManager.commit();

        User u = service.login(" eva ", "pwd");
        assertNotNull(u);
        assertEquals("eva", u.getUsername());
    }
    
}