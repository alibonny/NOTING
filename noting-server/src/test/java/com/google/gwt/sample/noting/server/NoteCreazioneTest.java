package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

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
import com.google.gwt.sample.noting.shared.Note.Stato;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

public class NoteCreazioneTest {
    
    @TempDir
    Path tempDir;

    private NoteServiceImpl service;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("mapdb-test.db");
        DBManager.openForTests(dbFile);
        DBManager.resetForTests();

        service = new NoteServiceImpl();
        // abilita TEST_USER per evitare le sessioni
        NoteServiceImpl._setTestUser(new User("tester"));

        // seed utenti per le condivisioni
        DBManager.getUsersDatabase().put("alice", "pwd");
        DBManager.getUsersDatabase().put("bob", "pwd");
        DBManager.commit();
    }

    @AfterEach
    void tearDown() {
        NoteServiceImpl._clearTestUser();
        DBManager.closeForTests();
    }

        @Test
    void creaNota_conTitoloValido_creaNota() throws Exception{
        service.creazioneNota("Titolo", "c", Stato.Privata, null);
        List<Note> notes = DBManager.getNotesDatabase().get("tester");
        assertEquals(1, notes.size());
    }


        @Test
    void creaNota_success_con_tutti_i_campi() throws Exception {
        service.creazioneNota(" Titolo ", "contenuto", Stato.Condivisa, List.of("alice", "bob"));

        // notesByOwner contiene la nota
        ConcurrentMap<String, List<Note>> notesByOwner = DBManager.getNotesDatabase();
        List<Note> notes = notesByOwner.get("tester");
        assertNotNull(notes);
        assertEquals(1, notes.size());

        Note n = notes.get(0);
        assertEquals("Titolo", n.getTitle());
        assertEquals("contenuto", n.getContent());
        assertEquals(Stato.Condivisa, n.getStato());
        assertEquals("tester", n.getOwnerUsername());
        assertTrue(n.getId() > 0);

        // noteById contiene la stessa nota
        Note byId = DBManager.getNoteById().get(n.getId());
        assertNotNull(byId);
        assertEquals("Titolo", byId.getTitle());

        // listaCondivisione contiene alice e bob
        List<String> share = DBManager.getListaCondivisione().get(n.getId());
        assertNotNull(share);
        assertTrue(share.containsAll(List.of("alice", "bob")));
    }

    @Test
void creaNota_contenutoNull_lanciaEccezione() {
    assertThrows(NotingException.class,
        () -> service.creazioneNota("Note1", null, null, null),
        "Se il contenuto è null deve lanciare eccezione");
}

@Test
void creaNota_condivisione_filtraDuplicati_e_SelfShare() throws Exception {
    service.creazioneNota("Note2", "c", Stato.Condivisa,
            List.of("alice", " alice ", "tester", "nonEsiste"));

    List<String> share = DBManager.getListaCondivisione()
                                  .get(DBManager.getNoteById().size()); // ultima nota
    assertEquals(List.of("alice"), share, "Dev’essere solo alice una volta");
}


    @Test
    void creaNota_titolo_vuoto_throws() {
        assertThrows(NotingException.class,
            () -> service.creazioneNota("   ", "c", Stato.Privata, null));
        assertThrows(NotingException.class,
            () -> service.creazioneNota(null, "c", Stato.Privata, null));
    }

    @Test
    void creaNota_incrementa_id_e_non_perde_note_precedenti() throws Exception {
        service.creazioneNota("N1", "c1", Stato.Privata, null);
        service.creazioneNota("N2", "c2", Stato.Privata, null);

        List<Note> notes = DBManager.getNotesDatabase().get("tester");
        assertEquals(2, notes.size());

        assertNotEquals(notes.get(0).getId(), notes.get(1).getId());
    }


    
}
