package com.google.gwt.sample.noting.server.Core;

import com.google.gwt.sample.noting.shared.NotingException;
import com.google.gwt.sample.noting.shared.User;

public interface  AuthServiceCore {
     User login(String username, String password) throws NotingException;
     User register(String username, String password) throws NotingException;

    
}
