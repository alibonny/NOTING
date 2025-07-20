package com.google.gwt.sample.noting.client;

public interface CreateNoteViewListener {
    void onPublish(String titolo, String contenuto);
    void onDelete();
    void onBack();
}
