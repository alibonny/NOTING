package com.google.gwt.sample.noting.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

public class CreaCopiaNotaTest {

    @TempDir
    Path tempDir;

    private Path dbFile;
    private NoteServiceImpl service;

    private static final String OWNER = "thomas";
    private static final String OTHER = "alice";

    @BeforeEach
    void setUp() {
    dbFile = tempDir.resolve("mapdb-test.db");
    DBManager.openForTests(dbFile);
    DBManager.resetForTests();

    service = new NoteServiceImpl();

    NoteServiceImpl._clearTestUser();
    NoteServiceImpl._setTestUser(new User("thomas"));

    // Seed: due note di "thomas"
    Note n1 = new Note();
    int id1 = DBManager.nextNoteId();
    n1.setId(id1);
    n1.setTitle("Appunti");
    n1.setContent("contenuto");
    n1.setOwnerUsername("thomas");

    Note n2 = new Note();
    int id2 = DBManager.nextNoteId();
    n2.setId(id2);
    n2.setTitle("Checklist");
    n2.setContent("cose da fare");
    n2.setOwnerUsername("thomas");

    // Indici globali
    DBManager.getNoteById().put(id1, n1);
    DBManager.getNoteById().put(id2, n2);

    // Lista note per owner (costruita manualmente)
    List<Note> notesThomas = new ArrayList<>();
    notesThomas.add(n1);
    notesThomas.add(n2);
    DBManager.getNotesDatabase().put("thomas", notesThomas);

    // Condivisioni (vuote) per ciascuna nota
    DBManager.getListaCondivisione().put(id1, new ArrayList<>());
    DBManager.getListaCondivisione().put(id2, new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        NoteServiceImpl._clearTestUser();
        DBManager.closeForTests();
    }

    // 1) INPUT NON VALIDi: username non combacia con l'utente di sessione OPPURE notaId non valido
    @Test
    void creaCopia_input_non_validi() {
        // username diverso dal caller (caller = "thomas" da requireUser)
        assertThrows(NotingException.class, () -> service.creaCopiaNota(OTHER, 1));

        // notaId non valido
        assertThrows(NotingException.class, () -> service.creaCopiaNota(OWNER, 0));
        assertThrows(NotingException.class, () -> service.creaCopiaNota(OWNER, -1));
    }

    // 2) PERMESSI: puoi copiare solo se sei proprietario o se sei nella lista condivisa
    @Test
    void creaCopia_permessi() {
        // cambio utente di sessione in "alice" e passo username coerente
        NoteServiceImpl._setTestUser(new User(OTHER));

        assertThrows(NotingException.class, () -> service.creaCopiaNota(OTHER, 1));

        shareWith(1,OTHER);
        int before = DBManager.getNoteById().size();

        assertDoesNotThrow(() -> service.creaCopiaNota(OTHER, 1));
        assertEquals(before + 1, DBManager.getNoteById().size());
    }

    // 3) STATO PRIVATO: la copia deve avere stato = Privata
    @Test
    void creaCopia_stato_privato() {
        assertDoesNotThrow(() -> service.creaCopiaNota(OWNER, 1));

        // recupero la copia
        ConcurrentMap<String, List<Note>> notesByOwner = DBManager.getNotesDatabase();
        List<Note> listOwner = notesByOwner.get(OWNER);
        assertNotNull(listOwner);
        Note copia = listOwner.stream()
                .filter(n -> "Appunti (Copia)".equals(n.getTitle()))
                .findFirst()
                .orElse(null);

        assertNotNull(copia, "La copia deve esistere");
        assertEquals(Note.Stato.Privata, copia.getStato(), "La copia deve essere Privata");
    }

    private void shareWith(int notaId, String user) {
    var map = DBManager.getListaCondivisione();
    List<String> l = map.get(notaId);
    l = (l == null) ? new ArrayList<>() : new ArrayList<>(l); // evita alias/immutabilità
    if (!l.contains(user)) l.add(user);
    map.put(notaId, l);
    DBManager.commit(); // se MapDB richiede commit; altrimenti è innocuo
}

    
}



