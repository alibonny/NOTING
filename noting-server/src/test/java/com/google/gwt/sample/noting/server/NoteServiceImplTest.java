package com.google.gwt.sample.noting.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gwt.sample.noting.shared.Note;

public class NoteServiceImplTest {

    private NoteServiceImpl service;
    private final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        service = new NoteServiceImpl();

        ConcurrentMap<String,List<Note>> db = DBManager.getNotesDatabase();
        //db.clear();

        Note n = new Note("Titolo iniziale", "Contenuto iniziale", Note.Stato.Privata,USERNAME);
        n.setId(1);
        db.put(USERNAME, new ArrayList<>(List.of(n)));
    }


    @Test
    void testCercaUtente() {

    }

    @Test
    void testCreaCopiaNota() {

    }

    @Test
    void testCreazioneNota() {

    }

    @Test
    void testEliminaNota() {

    }

    @Test
    void testEliminaUltimaNota() {

    }

    @Test
    void testGetAllUsernames() {

    }

    @Test
    void testGetCondiviseConMe() {

    }

    @Test
    void testGetNoteUtente() {

    }

    @Test
    void testLogin() {  
        

    }

    @Test
    void testLogout() {

    }

    @Test
    void testRegister() {

    }

    @Test
    void testSearchNotes() {

    }

    @Test
    void testUpdateNota()throws Exception {

        Note modificata = new Note("Titolo modificato", "Contenuto modificato", Note.Stato.Privata,USERNAME);
        modificata.setId(1);

        service.updateNota(modificata, Note.Stato.Privata);

        List<Note> notes = DBManager.getNotesDatabase().get(USERNAME);
        assertNotNull(notes);
        assertEquals(1, notes.size());
        Note aggiornata = notes.get(0);
        assertEquals(1, aggiornata.getId());
        assertEquals("Titolo nuovo", aggiornata.getTitle());
        assertEquals("Contenuto nuovo", aggiornata.getContent());

        

    }

    
}
