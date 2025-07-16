package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.Window;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;

public class NotingApp implements EntryPoint {

    private final NoteServiceAsync noteService = GWT.create(NoteService.class);

    public void onModuleLoad() {
        LoginView loginView = new LoginView();

        loginView.getLoginButton().addClickHandler(event -> {
            String username = loginView.getUsername();
            String password = loginView.getPassword();

            noteService.login(username, password, new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                    Window.alert("Errore di login: " + caught.getMessage());
                }
                public void onSuccess(User result) {
                    Window.alert("Login effettuato con successo! Benvenuto " + result.getUsername());
                    // Qui caricheremo l'interfaccia principale dell'app
                }
            });
        });

        // Per ora rimuoviamo la logica di registrazione per semplificare
        loginView.getRegisterButton().setVisible(false);

        // Assicurati che nel tuo HTML ci sia un div con id="root"
        RootPanel.get("root").add(loginView);
    }
}