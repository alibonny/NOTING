package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.server.Core.NoteComandiCoreImpl;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
;

public class UpdateNotaTest {

    @TempDir
    Path tempDir;

    private Path dbFile;

    // SUT di livello "core"
    private NoteComandiCoreImpl comandi;

    private static final String OWNER = "thomas";
    private static final String ALICE = "alice";

    private int existingId;

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("mapdb-test.db");
        DBManager.openForTests(dbFile);
        DBManager.resetForTests();

        // utenti (servono per eventuale normalizzazione destinari/tag, anche se qui non è strettamente necessario)
      //  DBManager.getUsersDatabase().put(OWNER, new User(OWNER));
        //DBManager.getUsersDatabase().put(ALICE, new User(ALICE));

        comandi = new NoteComandiCoreImpl();

        // Seed: una nota esistente dell'owner
        Note n = new Note();
        existingId = DBManager.nextNoteId();
        n.setId(existingId);
        n.setTitle("Titolo iniziale");
        n.setContent("Contenuto iniziale");
        n.setStato(Note.Stato.Privata); // pubblica con condivisioni
        n.setOwnerUsername(OWNER);

        // indici globali
        DBManager.getNoteById().put(existingId, n);

        // lista note per owner
        List<Note> notes = new ArrayList<>();
        notes.add(n);
        DBManager.getNotesDatabase().put(OWNER, notes);

        // condivisioni e tag iniziali
        DBManager.getListaCondivisione().put(existingId, new ArrayList<>(List.of(ALICE)));
        DBManager.getNoteTags().put(existingId, new ArrayList<>(List.of("old")));
    }

    @AfterEach
    void tearDown() {
        DBManager.closeForTests();
    }

    // 1) INPUT NON VALIDI: caller nullo/vuoto, nota nulla, id <= 0
    @Test
    void updateNota_input_non_validi() {
        // caller null
        assertThrows(NotingException.class,
            () -> comandi.updateNote(null, new Note()));

        // caller vuoto/spazi
        assertThrows(NotingException.class,
            () -> comandi.updateNote("   ", new Note()));

        // nota nulla
        assertThrows(NotingException.class,
            () -> comandi.updateNote(OWNER, null));

        // id <= 0
        Note bad = new Note();
        bad.setId(0);
        assertThrows(NotingException.class,
            () -> comandi.updateNote(OWNER, bad));

        bad.setId(-5);
        assertThrows(NotingException.class,
            () -> comandi.updateNote(OWNER, bad));
    }

    // 2) ID NON TROVATO: id valido ma non presente in noteById
    @Test
    void updateNota_id_non_trovato() {
        Note ghost = new Note();
        ghost.setId(existingId + 999); // non esiste
        ghost.setTitle("X");
        ghost.setContent("Y");
        ghost.setStato(Note.Stato.Privata);

        assertThrows(NotingException.class,
            () -> comandi.updateNote(OWNER, ghost));
    }

    // 3) SUCCESSO MINIMO: aggiorna titolo/contenuto; stato Privata -> condivisioni rimosse; tag aggiornati
    @Test
void updateNota_successo() {
    int byIdBefore = DBManager.getNoteById().size();
    int ownerListBefore = DBManager.getNotesDatabase().get(OWNER).size();

    // preparo la nota modificata
    Note mod = new Note();
    mod.setId(existingId);
    mod.setTitle("Titolo aggiornato");
    mod.setContent("Contenuto aggiornato");
    mod.setStato(Note.Stato.Privata);      // passando a Privata, condivisioni devono sparire
    mod.setUtentiCondivisi(List.of(ALICE)); // verrà ignorato perché Privata

    // qui usiamo ArrayList per accettare null e stringhe vuote
    List<String> rawTags = new ArrayList<>();
    rawTags.add("new");
    rawTags.add(" todo ");
    rawTags.add(null);
    rawTags.add("");
    mod.setTags(rawTags);

    assertDoesNotThrow(() -> comandi.updateNote(OWNER, mod));

    // indici: dimensioni stabili (aggiornamento in-place)
    assertEquals(byIdBefore, DBManager.getNoteById().size());
    assertEquals(ownerListBefore, DBManager.getNotesDatabase().get(OWNER).size());

    // verifica nota aggiornata
    Note updated = DBManager.getNoteById().get(existingId);
    assertNotNull(updated);
    assertEquals("Titolo aggiornato", updated.getTitle());
    assertEquals("Contenuto aggiornato", updated.getContent());
    assertEquals(OWNER, updated.getOwnerUsername(), "L'owner deve rimanere invariato");
    assertEquals(Note.Stato.Privata, updated.getStato());

    // condivisioni rimosse perché Privata
    assertNull(DBManager.getListaCondivisione().get(existingId),
               "Le condivisioni devono essere rimosse per note Private");

    // tag normalizzati
    List<String> savedTags = DBManager.getNoteTags().get(existingId);
    assertNotNull(savedTags);
    assertEquals(List.of("new", "todo"), savedTags);
}

}
