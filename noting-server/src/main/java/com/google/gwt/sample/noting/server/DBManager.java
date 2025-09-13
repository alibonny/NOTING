package com.google.gwt.sample.noting.server;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteMemento;
import com.google.gwt.sample.noting.shared.Tag;

@WebListener
public class DBManager implements ServletContextListener {

    // --- Stato e componenti base ---
    private static DB db;

    // Dati applicativi
    private static ConcurrentMap<String, String> usersDatabase;              // username -> password
    private static ConcurrentMap<String, List<Note>> notesDatabase;          // ownerUsername -> note dell'owner
    private static ConcurrentMap<Integer, List<String>> listaCondivisione;   // noteId -> destinatari
    private static ConcurrentMap<Integer, Note> noteById;     
    private static ConcurrentMap<String, Tag> tagsDatabase;                  // tagName -> tag object
    private static ConcurrentMap<Integer, List<String>> noteTags;           // noteId -> lista di tag names  

    private static final ConcurrentMap<Integer, LinkedList<NoteMemento>> noteHistoryDatabase = new ConcurrentHashMap<>();

    // Sequenza ID atomica (sostituisce Var<Integer>)
    private static org.mapdb.Atomic.Long noteIdSeq;

    // --- Lifecycle Servlet ---

    public static void saveNoteMemento(int noteId, Note nota) {
        if (nota == null) return;
        LinkedList<NoteMemento> history = noteHistoryDatabase.computeIfAbsent(noteId, k -> new LinkedList<>());
        // Limita la cronologia a max 3 versioni
        if (history.size() >= 3) history.removeFirst();
        history.addLast(new NoteMemento(nota.getTitle(), nota.getContent(), new java.util.Date()));
    }

    public static List<NoteMemento> getNoteHistory(int noteId) {
        LinkedList<NoteMemento> history = noteHistoryDatabase.get(noteId);
        return history != null ? new LinkedList<>(history) : new LinkedList<>();
    }

    public static NoteMemento getNoteHistoryEntry(int noteId, int historyIndex) {
        LinkedList<NoteMemento> history = noteHistoryDatabase.get(noteId);
        if (history == null || historyIndex < 0 || historyIndex >= history.size()) return null;
        return history.get(historyIndex);
    }

    public static Note restoreNoteFromMemento(int noteId, NoteMemento memento) {
        // Cerca la nota e aggiorna titolo/contenuto
        for (List<Note> notes : getNotesDatabase().values()) {
            for (Note n : notes) {
                if (n.getId() == noteId) {
                    n.setTitle(memento.getTitle());
                    n.setContent(memento.getContent());
                    return n;
                }
            }
        }
        return null;
    }
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[DB] Inizializzazione MapDB (file)...");
        final String dbPath = sce.getServletContext().getRealPath("/WEB-INF/noting-v3.db");
        final File dbFile = new File(dbPath);
        dbFile.getParentFile().mkdirs();

        db = DBMaker
                .fileDB(dbFile)
                .transactionEnable()
                .closeOnJvmShutdown()
                .make();

        initMapsAndIndexes();
        ensureIdSequenceAligned();

        if (usersDatabase.isEmpty()) {
            usersDatabase.put("test", "test");
            db.commit();
            System.out.println("[DB] Utente di default 'test' creato.");
        }

        System.out.println("[DB] Database inizializzato con successo.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        safeClose();
        System.out.println("[DB] Database MapDB chiuso correttamente.");
    }

    // --- API per Test ---

    /** Apre un file DB dedicato ai test (diverso dal runtime). */
   public static void openForTests(Path ignored) {
    closeForTests();
    db = DBMaker
            .memoryDB()            // <<--- IN MEMORIA
            .transactionEnable()
            .make();
    initMapsAndIndexes();
    ensureIdSequenceAligned();
}


    /** Pulisce completamente i dati tra un test e l'altro. */
    public static void resetForTests() {
        ensureReady();
        usersDatabase.clear();
        notesDatabase.clear();
        listaCondivisione.clear();
        noteById.clear();
        noteIdSeq.set(1L); // riparte da 1
        db.commit();
    }

    /** Chiude il DB e azzera i riferimenti (fine test). */
    public static void closeForTests() {
        safeClose();
        clearRefs();
    }

    // --- Inizializzazione Strutture ---

    /** Crea/riapre tutte le strutture e l'atomic long per gli ID. */
    private static void initMapsAndIndexes() {
        ensureDbOpen();

        usersDatabase       = db.hashMap("users",             Serializer.STRING,  Serializer.STRING).createOrOpen();
        notesDatabase       = db.hashMap("notes",             Serializer.STRING,  Serializer.JAVA  ).createOrOpen();
        listaCondivisione   = db.hashMap("listaCondivisione", Serializer.INTEGER, Serializer.JAVA  ).createOrOpen();
        noteById            = db.hashMap("noteById",          Serializer.INTEGER, Serializer.JAVA  ).createOrOpen();
        tagsDatabase        = db.hashMap("tags",              Serializer.STRING,  Serializer.JAVA  ).createOrOpen();
        noteTags            = db.hashMap("noteTags",          Serializer.INTEGER, Serializer.JAVA  ).createOrOpen();
        noteIdSeq           = db.atomicLong("noteIdSeq").createOrOpen();

        // Inizializza i tag predefiniti se non esistono
        if (tagsDatabase.isEmpty()) {
            String[] defaultTags = {"Università", "Tempo libero", "Importante", "Promemoria", "Cose da fare"};
            for (String tagName : defaultTags) {
                tagsDatabase.put(tagName, new Tag(tagName));
            }
            db.commit();
        }

        // Ricostruisci indice id->nota se mancante ma sono presenti note in notesDatabase
        if (noteById.isEmpty() && !notesDatabase.isEmpty()) {
            for (List<Note> list : notesDatabase.values()) {
                if (list == null) continue;
                for (Note n : list) {
                    if (n != null) noteById.put(n.getId(), n);
                }
            }
            db.commit();
        }
    }

    /** Allinea la sequenza noteIdSeq al valore max(noteId)+1 per evitare collisioni dopo riavvio. */
    private static void ensureIdSequenceAligned() {
        int maxId = 0;

        // 1) guarda l'indice primario
        for (Integer id : noteById.keySet()) {
            if (id != null && id > maxId) maxId = id;
        }
        // 2) doppia safety: esplora anche notesDatabase
        for (List<Note> list : notesDatabase.values()) {
            if (list == null) continue;
            for (Note n : list) {
                if (n != null && n.getId() > maxId) maxId = n.getId();
            }
        }

        long current = noteIdSeq.get();
        long target  = (long) maxId + 1L;
        if (current < target) {
            noteIdSeq.set(target);
            db.commit();
        }
    }

    // --- Helpers di basso livello ---

    private static void ensureDbOpen() {
        if (db == null || db.isClosed()) {
            throw new IllegalStateException("DB non inizializzato. Avvia il container servlet o usa openForTests().");
        }
    }

    private static void ensureReady() {
        ensureDbOpen();
        if (usersDatabase == null || notesDatabase == null || listaCondivisione == null || noteById == null || noteIdSeq == null) {
            throw new IllegalStateException("Strutture MapDB non pronte. Chiama initMapsAndIndexes().");
        }
    }

    private static void safeClose() {
        try {
            if (db != null && !db.isClosed()) db.close();
        } catch (Exception ignore) {
            // best effort
        }
    }

    private static void clearRefs() {
        db = null;
        usersDatabase = null;
        notesDatabase = null;
        listaCondivisione = null;
        noteById = null;
        noteIdSeq = null;
    }

    // --- API Pubblica ---

    /** Restituisce la mappa utenti (username->password). */
    public static ConcurrentMap<String, String> getUsersDatabase() {
        ensureReady();
        return usersDatabase;
    }

    /** Restituisce la mappa delle note possedute per owner. */
    public static ConcurrentMap<String, List<Note>> getNotesDatabase() {
        ensureReady();
        return notesDatabase;
    }

    /** Restituisce la mappa condivisioni (noteId->lista destinatari). */
    public static ConcurrentMap<Integer, List<String>> getListaCondivisione() {
        ensureReady();
        return listaCondivisione;
    }

    /** Indice globale note per ID. */
    public static ConcurrentMap<Integer, Note> getNoteById() {
        ensureReady();
        return noteById;
    }

    /** Prossimo ID nota (atomico). */
    public static int nextNoteId() {
        ensureReady();
        long v = noteIdSeq.getAndIncrement();
        if (v > Integer.MAX_VALUE) {
            throw new IllegalStateException("Overflow ID note (long -> int).");
        }
        return (int) v;
    }

    /** Commit esplicito della transazione MapDB. */
    public static void commit() {
        ensureDbOpen();
        db.commit();
    }

    /** Utility: note possedute da utente. */
    public static List<Note> getUserOwnedNotes(String username) {
        ensureReady();
        return notesDatabase.getOrDefault(username, List.of());
    }

    /** Utility: note condivise con utente (non owner). */
    public static List<Note> getUserSharedNotes(String username) {
        ensureReady();
        List<Note> shared = new ArrayList<>();
        if (username == null) return shared;

        for (Map.Entry<Integer, List<String>> entry : listaCondivisione.entrySet()) {
            Integer id = entry.getKey();
            List<String> destinatari = entry.getValue();

            if (destinatari != null && destinatari.contains(username)) {
                Note n = noteById.get(id);
                if (n != null && !username.equals(n.getOwnerUsername())) {
                    shared.add(n);
                }
            }
        }
        return shared;
    }

    // Metodi per la gestione dei tag
    public static ConcurrentMap<String, Tag> getTagsDatabase() {
        ensureReady();
        return tagsDatabase;
    }

    public static ConcurrentMap<Integer, List<String>> getNoteTags() {
        ensureReady();
        return noteTags;
    }

    public static List<String> getTagsForNote(int noteId) {
        ensureReady();
        return noteTags.getOrDefault(noteId, new ArrayList<>());
    }

    public static void addTagToNote(int noteId, String tagName) {
        ensureReady();
        List<String> tags = noteTags.computeIfAbsent(noteId, k -> new ArrayList<>());
        if (!tags.contains(tagName)) {
            tags.add(tagName);
            db.commit();
        }
    }

    public static void removeTagFromNote(int noteId, String tagName) {
        ensureReady();
        List<String> tags = noteTags.get(noteId);
        if (tags != null) {
            tags.remove(tagName);
            db.commit();
        }
    }

    public static void removeAllTagsFromNote(int noteId) {
        ensureReady();
        noteTags.remove(noteId);
        db.commit();
    }

}
