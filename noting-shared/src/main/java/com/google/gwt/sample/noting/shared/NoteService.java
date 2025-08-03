package com.google.gwt.sample.noting.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("noteService")
public interface NoteService extends RemoteService {
    User login(String username, String password) throws NotingException;
    User register(String username, String password) throws NotingException;
    void logout();
    void creazioneNota(String titolo, String contenuto, Note.Stato stato) throws NotingException;
    void eliminaUltimaNota();
    List<Note> getNoteUtente() throws NotingException;
    void updateNota(Note notaModificata, Note.Stato nuovoStato) throws NotingException;
    void eliminaNota(String username, int notaId) throws NotingException;
    List<Note> searchNotes(String query) throws NotingException;
    List<String> getAllUsernames();

}