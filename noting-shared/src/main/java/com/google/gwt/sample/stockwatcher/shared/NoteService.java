package com.google.gwt.sample.stockwatcher.shared;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("noteService")
public interface NoteService extends RemoteService {
    User login(String username, String password) throws Exception;
    User register(String username, String password) throws Exception;
}
