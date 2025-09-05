package com.google.gwt.sample.noting.server;


import java.nio.file.Path;
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

public class NoteRetrievalAndSearchTest {
 
        @TempDir
    Path temp;

    @BeforeEach
    void setUp() {
        DBManager.openForTests(temp.resolve("retrieval_search.mapdb"));
        DBManager.resetForTests();

        // utenti di test
        DBManager.getUsersDatabase().put("alice", "x");
        DBManager.getUsersDatabase().put("bob", "x");
        DBManager.commit();

        // test mode senza sessione
        NoteServiceImpl._setTestUser(new User("alice"));
    }

    @AfterEach
    void tearDown() {
        NoteServiceImpl._clearTestUser();
        DBManager.closeForTests();
    }

    // --------- getNoteUtente ----------

    @Test
    void getNoteUtente_vuoto_se_nessuna_nota() throws Exception {
        var svc = new NoteServiceImpl();

        List<Note> mine = svc.getNoteUtente();
        assertNotNull(mine);
        assertTrue(mine.isEmpty(), "Mi aspetto lista vuota se l'utente non ha note");
    }

        @Test
    void getNoteUtente_restituisce_solo_note_di_alice() throws Exception {
        var svc = new NoteServiceImpl();

        // creo 2 note per alice
        svc.creazioneNota("A1", "C1", Note.Stato.Privata, null);
        svc.creazioneNota("A2", "C2", Note.Stato.Privata, null);

        // creo 1 nota per bob simulando utente diverso
        NoteServiceImpl._setTestUser(new User("bob"));
        svc.creazioneNota("B1", "C3", Note.Stato.Privata, null);

        // torno ad alice e verifico
        NoteServiceImpl._setTestUser(new User("alice"));
        List<Note> mine = svc.getNoteUtente();

        assertEquals(2, mine.size());
        assertEquals("A1", mine.get(0).getTitle());
        assertEquals("A2", mine.get(1).getTitle());
    }

        @Test
    void getNoteUtente_senza_test_user_lancia() {
        var svc = new NoteServiceImpl();
        NoteServiceImpl._clearTestUser(); // niente sessione e niente test-user

        NotingException ex = assertThrows(NotingException.class, svc::getNoteUtente);
        assertTrue(ex.getMessage().contains("Utente di test non impostato"),
        "Messaggio atteso quando non c'è sessione né TEST_USER");
    }


    /*
     * Cosa coprono questi test

Lista vuota quando l’utente non ha note.

Solo le note dell’utente corrente (alice), anche se altri utenti hanno note nel DB.

Errore chiaro se non c’è sessione e non è impostato TEST_USER.
     */





     // --------- searchNotes ----------

        @Test
    void searchNotes_match_su_titolo() throws Exception {
        var svc = new NoteServiceImpl();

        // alice crea 3 note
        svc.creazioneNota("Spesa settimanale", "Compra latte e pane", Note.Stato.Privata, null);
        svc.creazioneNota("Studio", "Ripasso algoritmi", Note.Stato.Privata, null);
        svc.creazioneNota("Viaggio", "Itinerario Roma", Note.Stato.Privata, null);
        DBManager.commit();

        var results = svc.searchNotes("spesa");
        assertEquals(1, results.size());
        assertEquals("Spesa settimanale", results.get(0).getTitle());
    }

    @Test
    void searchNotes_match_su_contenuto() throws Exception {
        var svc = new NoteServiceImpl();

        svc.creazioneNota("ToDo", "Compra LATTE e uova", Note.Stato.Privata, null);
        svc.creazioneNota("Altro", "Nulla di rilevante", Note.Stato.Privata, null);
        DBManager.commit();

        var results = svc.searchNotes("latte"); // case-insensitive
        assertEquals(1, results.size());
        assertEquals("ToDo", results.get(0).getTitle());
    }

     @Test
    void searchNotes_case_insensitive() throws Exception {
        var svc = new NoteServiceImpl();

        svc.creazioneNota("Appunti ALGORITMI", "Note varie", Note.Stato.Privata, null);
        DBManager.commit();

        assertEquals(1, svc.searchNotes("algoritmi").size());
        assertEquals(1, svc.searchNotes("ALGORITMI").size());
        assertEquals(1, svc.searchNotes("Algoritmi").size());
    }

        
    @Test
    void searchNotes_nessun_risultato_lista_vuota() throws Exception {
        var svc = new NoteServiceImpl();

        svc.creazioneNota("Grocery", "Milk and bread", Note.Stato.Privata, null);
        DBManager.commit();

        var results = svc.searchNotes("xyz_non_trovera_mai");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

        @Test
    void searchNotes_senza_test_user_lancia() {
        // disattivo la test-mode per simulare assenza utente/sessione
        NoteServiceImpl._clearTestUser();
        var svc = new NoteServiceImpl();

        NotingException ex = assertThrows(NotingException.class, () -> svc.searchNotes("qualcosa"));
        assertTrue(ex.getMessage().contains("Utente di test non impostato"),
            "Atteso errore chiaro quando non c'è sessione né TEST_USER");
    }


  

    
}
