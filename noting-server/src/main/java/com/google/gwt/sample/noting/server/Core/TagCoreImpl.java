package com.google.gwt.sample.noting.server.Core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import com.google.gwt.sample.noting.server.DBManager;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.Tag;

public class TagCoreImpl implements TagCore{


    @Override
    public ConcurrentMap<String, Tag> getTagsDatabase() {
        return DBManager.getTagsDatabase();
    }

    @Override
    public List<String> getTagsForNote(int noteId) {
        return DBManager.getNoteTags().getOrDefault(noteId, new ArrayList<>());
    }

    @Override
    public void addTagToNote(int noteId, String tagName)throws NotingException {
        ConcurrentMap<Integer,List<String>> noteTags = DBManager.getNoteTags();
        List<String> tags = noteTags.computeIfAbsent(noteId, k -> new ArrayList<>());

        if (tags.contains(tagName)) {
        throw new NotingException("Errore: il tag '" + tagName + "' è già associato alla nota.");
        }

        tags.add(tagName);

        Note n = DBManager.getNoteById().get(noteId);
        if (n != null) {
            n.addTag(tagName);
            n.setTags(new ArrayList<>(tags)); // simile a come fai in removeTag
        }

        noteTags.put(noteId, tags);
        DBManager.commit();
    }

    @Override
    public void updateNoteTags(int noteId, List<String> tags) {
        Note nota = DBManager.getNoteById().get(noteId);
        if (nota == null) return;

        ConcurrentMap<Integer, List<String>> noteTags = DBManager.getNoteTags();
        List<String> current = noteTags.getOrDefault(noteId, new ArrayList<>());

        // aggiorna modello e indice
        nota.setTags(new ArrayList<>(tags));
        current.clear();

        for (String tagName : tags) {
            current.add(tagName);
        }
        
        noteTags.put(noteId, current);

            DBManager.commit();
    }

    @Override
    public void removeTagFromNote(int noteId, String tagName) throws NotingException {
        ConcurrentMap<Integer,List<String>> noteTags = DBManager.getNoteTags();
        List<String> tags = noteTags.get(noteId);

    if (tags == null || !tags.contains(tagName)) {
        throw new NotingException("Errore: il tag '" + tagName + "' non è associato alla nota.");
    }
    tags.remove(tagName);

    Note n = DBManager.getNoteById().get(noteId);
    if (n != null) {
        n.removeTag(tagName);
        n.setTags(new ArrayList<>(tags));
    }

    noteTags.put(noteId, tags);
    DBManager.commit();
    }

    @Override
    public void createNewTag(String tagName){
         
        ConcurrentMap<String, Tag> tagsDB = DBManager.getTagsDatabase();
        if (!tagsDB.containsKey(tagName)) {
            tagsDB.put(tagName, new Tag(tagName));
            DBManager.commit();
        }
    }

}

  





