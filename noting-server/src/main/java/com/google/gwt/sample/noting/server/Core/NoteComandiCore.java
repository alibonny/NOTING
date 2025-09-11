package com.google.gwt.sample.noting.server.Core;

import java.util.List;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NotingException;

public interface  NoteComandiCore {
    void creazioneNota(String owner, String titolo, String contenuto, Note.Stato stato, List<String> utenti) throws NotingException;
    void updateNote(String callerUsername, Note notaModificata) throws NotingException;
    void deleteNote(String owner, int notaId) throws NotingException;
    void copyNote(String caller, int notaId) throws NotingException;
    
}
