package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

public class NoteCreationAndUpdateTest {

    @TempDir
    Path temp; // cartella temporanea diversa ad ogni run

    
    @BeforeEach
    void setUp() {
        DBManager.openForTests(temp.resolve("testdb.mapdb"));
        DBManager.resetForTests();

        // preparo l’utente nel DB di test (file)
        DBManager.getUsersDatabase().put("mario", "password");
        DBManager.getUsersDatabase().put("alice", "x");
        DBManager.getUsersDatabase().put("bob", "x");
        DBManager.getUsersDatabase().put("carol", "x");
        DBManager.commit();

        NoteServiceImpl._setTestUser(new User("alice"));

    }

     @AfterEach
    void tearDown() {
    DBManager.closeForTests();
    }


   @Test
    void creazioneNota_privata_ok() throws Exception {
        var svc = new NoteServiceImpl();

        svc.creazioneNota("Titolo", "Contenuto", Note.Stato.Privata, null);
        DBManager.commit();

        var notes = DBManager.getNotesDatabase().get("alice");
        assertNotNull(notes);
        assertEquals(1, notes.size());

        Note n = notes.get(0);
        assertEquals("Titolo", n.getTitle());
        assertEquals("Contenuto", n.getContent());
        assertEquals(Note.Stato.Privata, n.getStato());

        var share = DBManager.getListaCondivisione().get(n.getId());
        assertTrue(share == null || share.isEmpty(), "Privata → lista condivisione vuota");
    }

    @Test
    void creazioneNota_condivisa_pulisceEDeduplica() throws Exception {
        var svc = new NoteServiceImpl();

        List<String> dest = new ArrayList<>(Arrays.asList("  bob  ", null, "", "carol", "bob", "alice"));
        svc.creazioneNota("N1", "C1", Note.Stato.Condivisa, dest);
        DBManager.commit();

        var created = DBManager.getNotesDatabase().get("alice").get(0);
        var share = DBManager.getListaCondivisione().get(created.getId());
        assertEquals(List.of("bob", "carol"), share);
    }

    @Test
    void creazioneNota_senzaUtente_lancia() {
        NoteServiceImpl._clearTestUser(); // nessuna servlet, nessun test-user

        var svc = new NoteServiceImpl();
        assertThrows(NotingException.class,
        () -> svc.creazioneNota("T", "C", Note.Stato.Privata, null));
    }

    @Test
    void creazioneNota_dueVolte_incrementaId() throws Exception {
        var svc = new NoteServiceImpl();

        svc.creazioneNota("T1", "C1", Note.Stato.Privata, null);
        svc.creazioneNota("T2", "C2", Note.Stato.Privata, null);
        DBManager.commit();

        var notes = DBManager.getNotesDatabase().get("alice");
        assertEquals(2, notes.size());
        assertNotEquals(notes.get(0).getId(), notes.get(1).getId());
    }

    /* Test update nota */

    @Test
    void update_nota_esistente_ok() throws Exception {
        var svc = new NoteServiceImpl();

        svc.creazioneNota("Titolo", "Contenuto", Note.Stato.Privata, null);
        DBManager.commit();

        List<Note> n = DBManager.getNotesDatabase().get("alice");
        assertNotNull(n);
        assertEquals(1, n.size());
        Note originale = n.get(0);

        // la aggiorno
        Note modificata = new Note("Nuovo titolo", "Nuovo contenuto", Note.Stato.Condivisa, List.of("bob", "carol"), "alice");
        modificata.setId(originale.getId());
        
        svc.updateNota(modificata, Note.Stato.Condivisa);
        DBManager.commit();

         var after = DBManager.getNotesDatabase().get("alice");
        assertEquals(1, after.size());
        var saved = after.get(0);
        assertEquals(originale.getId(), saved.getId());
        assertEquals("Nuovo titolo", saved.getTitle());
        assertEquals("Nuovo contenuto", saved.getContent());    
    }
    
/*
    @Test
    void updateIdNonValidoNonModifica() throws Exception {
        var svc = new NoteServiceImpl();

        svc.creazioneNota("Titolo", "Contenuto", Note.Stato.Privata, Collections.emptyList());
        DBManager.commit();

        var before = DBManager.getNotesDatabase().get("alice");
        assertEquals(1, before.size());
        var idEsistente = before.get(0).getId();

        // Provo ad aggiornare con ID <= 0
        Note modificata = new Note("Nuovo", "Nuovo", Note.Stato.Privata);
        modificata.setId(0);
        modificata.setOwnerUsername("alice");
        svc.updateNota(modificata, Note.Stato.Privata);
        DBManager.commit();

        var after = DBManager.getNotesDatabase().get("alice");
        assertEquals(1, after.size()); // invariato
        assertEquals(idEsistente, after.get(0).getId());
        assertEquals("Titolo", after.get(0).getTitle());
        assertEquals("C", after.get(0).getContent());

    }  */


    @Test
    void update_nota_non_trovata_lancia() throws Exception {
    var svc = new NoteServiceImpl();

    // Creo una nota
    svc.creazioneNota("T1", "C1", Note.Stato.Privata, Collections.emptyList());
    DBManager.commit();

    // Provo ad aggiornare un ID inesistente
    Note modificata = new Note("X", "Y", Note.Stato.Privata);
    modificata.setId(999);
    modificata.setOwnerUsername("alice");

    var ex = assertThrows(NotingException.class,
            () -> svc.updateNota(modificata, Note.Stato.Privata));
    assertEquals("Nota non trovata.", ex.getMessage());

    // DB invariato
    var notes = DBManager.getNotesDatabase().get("alice");
    assertEquals(1, notes.size());
    assertEquals("T1", notes.get(0).getTitle());
}

    @Test
    void update_senza_testUser_lancia() throws Exception {
        // disattivo la modalità test per simulare assenza utente/sessione
        NoteServiceImpl._clearTestUser();

        var svc = new NoteServiceImpl();
        Note modificata = new Note("T", "C", Note.Stato.Privata);
        modificata.setId(1);
        modificata.setOwnerUsername("alice");

        var ex = assertThrows(NotingException.class,
        () -> svc.updateNota(modificata, Note.Stato.Privata));
        assertTrue(ex.getMessage().contains("Utente di test non impostato"));
    }




}