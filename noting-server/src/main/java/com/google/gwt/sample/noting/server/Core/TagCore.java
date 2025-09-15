// src/main/java/com/google/gwt/sample/noting/server/repo/TagRepository.java
package com.google.gwt.sample.noting.server.Core;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.Tag;

public interface TagCore {

    /** Ritorna la mappa "nomeTag -> Tag" (per leggere/creare tag globali). */
    ConcurrentMap<String, Tag> getTagsDatabase();

    /** Ritorna i nomi tag associati alla nota. */
    List<String> getTagsForNote(int noteId);

    /** Aggiunge un tag (per nome) alla nota (idempotente). */
    void addTagToNote(int noteId, String tagName) throws NotingException;

    /** Sostituisce completamente l’elenco di tag di una nota. */
    void updateNoteTags(int noteId, List<String> tags);

    /** Rimuove un tag dalla nota; errore se non presente. */
    void removeTagFromNote(int noteId, String tagName) throws NotingException;

    void createNewTag(String tagName);

}
