package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

public class CreazioneNotaTest {

    @TempDir
    Path tempDir;

    private Path dbFile;
    private NoteServiceImpl service;

    private static final String OWNER = "thomas";
    private static final String ALICE = "alice";
    private static final String UNKNOWN = "sconosciuto";

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("mapdb-test.db");
        DBManager.openForTests(dbFile);
        DBManager.resetForTests();

   
        service = new NoteServiceImpl();

        NoteServiceImpl._clearTestUser();
        NoteServiceImpl._setTestUser(new User(OWNER));

        // opzionale: inizializza struttura note per owner (vuota)
        DBManager.getNotesDatabase().put(OWNER, new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        NoteServiceImpl._clearTestUser();
        DBManager.closeForTests();
    }

    // 1) INPUT NON VALIDI: titolo/contenuto null o vuoti => NotingException
    @Test
    void creazioneNota_input_non_validi_titolo_contenuto() {
        // titolo null  
        assertThrows(NotingException.class, () ->
            service.creazioneNota(null, "contenuto", Note.Stato.Privata, null, null));

        // titolo vuoto/spazi
        assertThrows(NotingException.class, () ->
            service.creazioneNota(null, "contenuto", Note.Stato.Privata, null, null));

        // contenuto null
        assertThrows(NotingException.class, () ->
            service.creazioneNota(null, "contenuto", Note.Stato.Privata, null, null));

        // contenuto vuoto/spazi
        assertThrows(NotingException.class, () ->
            service.creazioneNota(null, "contenuto", Note.Stato.Privata, null, null));
    }

    // 2) OWNER NON VALIDO: owner null o vuoto => NotingException
    // (Nota: l'ID nota è generato internamente con DBManager.nextNoteId(), quindi non è un input
    // e non può essere "non valido" lato chiamante.)
    @Test
    void creazioneNota_owner_non_valido() {
        // owner null
        assertThrows(NotingException.class, () ->
            service.creazioneNota(null, "contenuto", Note.Stato.Privata, null, null));

        // owner vuoto/spazi
        assertThrows(NotingException.class, () ->
            service.creazioneNota(null, "contenuto", Note.Stato.Privata, null, null));
    }

    // 3) SUCCESSO: la nota viene creata correttamente
    // - stato di default Privata se passato null
    // - destinatari normalizzati: niente owner, niente null/vuoti, solo utenti esistenti (ALICE resta, UNKNOWN scartato)
        // - presente in noteById e nella lista dell’owner
        @Test
        void creazioneNota_successo() {
        // nessun destinatario/tag: test più “tranquillo”
        List<String> destinatari = List.of();
        List<String> tags = List.of();

        int byIdBefore = DBManager.getNoteById().size();
        int ownerListBefore = DBManager.getNotesDatabase().get(OWNER).size();

        assertDoesNotThrow(() ->
                service.creazioneNota("Titolo", "Contenuto", null, destinatari, tags)
        );

        assertEquals(byIdBefore + 1, DBManager.getNoteById().size());
        assertEquals(ownerListBefore + 1, DBManager.getNotesDatabase().get(OWNER).size());
        }

}
