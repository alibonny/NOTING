package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.server.Core.NoteComandiCoreImpl;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

public class EliminaServiceTest {
     @TempDir
    Path tempDir;

    private Path dbFile;
    private NoteServiceImpl service;
    private NoteComandiCoreImpl comandi;
    private  final User TEST_USER = new User("thomas");

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("mapdb-test.db");
        DBManager.openForTests(dbFile);
        DBManager.resetForTests();
        service = new NoteServiceImpl();
        comandi = new NoteComandiCoreImpl();
        NoteServiceImpl._clearTestUser();
        NoteServiceImpl._setTestUser(new User("thomas")); 


        Note nota = new Note();
        nota.setId(1);
        nota.setOwnerUsername("thomas");

        
        Note n2 = new Note();
        n2.setId(2);
        n2.setOwnerUsername("thomas");

        
        DBManager.getNoteById().put(1, nota);
        DBManager.getNoteById().put(2,n2);

        List<Note>list = new ArrayList<>();
        list.add(nota);
        list.add(n2);
        DBManager.getNotesDatabase().put("thomas", list);
   
    }

    @AfterEach
    void tearDown(){
        NoteServiceImpl._clearTestUser();
        DBManager.closeForTests();
    }

    // -------------------------
    // DELETE NOTE
    // -------------------------

    // 1) input non validi -
    @Test
    void delete_input_non_validi() {
        assertThrows(NotingException.class, () -> comandi.deleteNote(null, 1));
      
    }

        // 2) nota non trovata -> eccezione
    @Test
    void delete_nota_non_trovata() {
       
        
        assertThrows(NotingException.class, () -> comandi.deleteNote("thomas", 99));

        assertTrue(DBManager.getNoteById().containsKey(1));
        assertNotNull(DBManager.getNotesDatabase().get("thomas"));
        assertTrue(DBManager.getNotesDatabase().get("thomas").stream().anyMatch(n -> n.getId() == 1));
        assertFalse(DBManager.getListaCondivisione().containsKey(99));

        
    }

    //3) nota trovata 
    @Test
    void delete_nota_trovata(){
    assertDoesNotThrow(() -> comandi.deleteNote("thomas", 1));

    // id=1 eliminato, id=2 resta
    assertFalse(DBManager.getNoteById().containsKey(1));
    assertTrue(DBManager.getNoteById().containsKey(2));

    var listThomas = DBManager.getNotesDatabase().get("thomas");
    assertNotNull(listThomas);
    assertTrue(listThomas.stream().noneMatch(n -> n.getId() == 1));
    assertTrue(listThomas.stream().anyMatch(n -> n.getId() == 2));

    assertFalse(DBManager.getListaCondivisione().containsKey(1));

    }

    
}
