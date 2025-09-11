package com.google.gwt.sample.noting.server.Core;

import java.util.concurrent.ConcurrentMap;

import com.google.gwt.sample.noting.server.DBManager;
import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

public class AuthServiceCoreImpl implements AuthServiceCore {

    @Override
    public User login (String username, String password) throws NotingException {
        if (username == null || password == null) {
            throw new NotingException("Username e password non possono essere vuoti.");
        }

        username = username.trim();
        if (username.isEmpty() || password.isEmpty())
          {  throw new NotingException("Username e password non possono essere vuoti."); 
        }

        ConcurrentMap<String, String> users = DBManager.getUsersDatabase();
        String storedPassword = users.get(username);

        if (storedPassword != null && storedPassword.equals(password)) {
            User user = new User(username);
            return user;
        }
        throw new NotingException("Credenziali non valide");
    }

        @Override
    public User register(String username, String password) throws NotingException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new NotingException("Username e password non possono essere vuoti.");
        }

        ConcurrentMap<String, String> users = DBManager.getUsersDatabase();
            
        if (users.putIfAbsent(username.trim(), password) == null) {
            DBManager.commit();
             User user = new User(username);

            return user;
            } 
        throw new NotingException("Username già esistente.");
    
    }

    
 
}
