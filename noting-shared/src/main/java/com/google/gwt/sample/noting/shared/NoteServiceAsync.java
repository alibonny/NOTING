package com.google.gwt.sample.noting.shared;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface NoteServiceAsync {
    void login(String username, String password, AsyncCallback<User> callback);
    void register(String username, String password, AsyncCallback<User> callback);
}