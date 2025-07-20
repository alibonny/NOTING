package com.google.gwt.sample.noting.shared;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

// CLASS E CHE ELENCA I METODI CHE POSSONO ESSERE RICHIAMATI DALL'UTENTE
// Usata dal Server
// Interfaccia sincrona
// La implementa NoteServiceImpl(lato server)


@RemoteServiceRelativePath("noteService")

public interface NoteService extends RemoteService {
    User login(String username, String password) throws Exception;
    User register(String username, String password) throws Exception;
    void logout();
    void creazioneNota(String titolo, String contenuto);
    void eliminaUltimaNota();

}