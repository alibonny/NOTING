package com.google.gwt.sample.noting.client;

import java.util.List;

import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface CreateNoteViewListener {
    void onSave(String titolo, String contenuto, Note.Stato stato, List<String> utentiList, List<String> tags);
    void onBack();
    void trovaUtente(String username, AsyncCallback<Boolean> callback);

}


