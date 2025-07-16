package com.google.gwt.sample.noting.server;

import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class NoteServiceImpl extends RemoteServiceServlet implements NoteService {

    @Override
    public User login(String username, String password) throws Exception {
        // Logica di login super semplificata per il test
        if ("test".equals(username) && "test".equals(password)) {
            return new User(username);
        } else {
            throw new Exception("Username o password non validi");
        }
    }
}