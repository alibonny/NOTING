package com.google.gwt.sample.noting.server.Core;

import java.util.List;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;

public interface NoteCercaCore {

    List<Note> getNotesOf(String owner) throws NotingException;
    Note getById(int noteId) throws NotingException;
    List<Note> search(String owner, String query) throws NotingException;
    List<Note> sharedWith(String username) throws NotingException;
    List<Note> searchByFilter(String owner, String currentView, String filterType, String filterValue) throws NotingException;
}
    

