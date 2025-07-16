package com.google.gwt.sample.noting.shared;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

// CLASS E CHE ELENCA I METODI CHE POSSONO ESSERE RICHIAMATI DALL'UTENTE

@RemoteServiceRelativePath("noteService")
public interface NoteService extends RemoteService {
    User login(String username, String password) throws Exception;
    User register(String username, String password) throws Exception;

}