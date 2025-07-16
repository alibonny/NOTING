package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

public class NotingApp implements EntryPoint{

    private final NoteServiceAsync noteService = GWT.create(NoteService.class);
    private LoginView loginView;
    private HomeView homeView;

    public void onModuleLoad() {
        loadLoginView();
    }
     
     private void loadLoginView() {
        loginView = new LoginView();

        setupLoginViewHandlers();

        RootPanel.get("root").clear();
        RootPanel.get("root").add(loginView);
    }

    //LOGIN
    private void setupLoginViewHandlers() {
        loginView.getLoginButton().addClickHandler(event -> {
            noteService.login(loginView.getUsername(), loginView.getPassword(), new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                    Window.alert("Errore di login: " + caught.getMessage());
                }

                public void onSuccess(User result) {
                    // Se il login ha successo, carica la schermata home.
                    loadHomeView(result);
                }
            });
        });

        //REGISTRAZIONE 
        loginView.getRegisterButton().addClickHandler(event -> {
            noteService.register(loginView.getUsername(), loginView.getPassword(), new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                     Window.alert("Errore di registrazione: " + caught.getMessage());
                }
                public void onSuccess(User result) {
                    Window.alert("Registrazione completata! Ora puoi effettuare il login.");
                }
            });
        });

        // Mostra la vista di login all'avvio
        RootPanel.get("root").add(loginView);
    }
    
    /**
     * Metodo per caricare la schermata principale dopo il login.
     * @param loggedInUser L'utente che ha effettuato l'accesso.
    */

    private void loadHomeView(User loggedInUser) {
        homeView = new HomeView(loggedInUser);
  
        setupHomeViewHandlers();

        RootPanel.get("root").clear();
        RootPanel.get("root").add(homeView);
    }

    private void setupHomeViewHandlers() {
        homeView.getLogoutButton().addClickHandler(event -> {
            noteService.logout(new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Errore durante il logout: " + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    // Se il logout ha successo, torna alla schermata di login.
                    loadLoginView();
                }
            });
        });
    }
}