package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.Window;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;

public class NotingApp implements EntryPoint{

    private final NoteServiceAsync noteService = GWT.create(NoteService.class);

    public void onModuleLoad() {
      
        //LOGIN
        loginView.getLoginButton().addClickHandler(event -> {
            String username = loginView.getUsername();
            String password = loginView.getPassword();

            noteService.login(username, password, new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                    Window.alert("Errore di login: " + caught.getMessage());
                }

                public void onSuccess(User result) {
                    // SE IL LOGIN HA SUCCESSO, CAMBIA SCHERMATA
                    loadHomeScreen(result);
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

    private void loadHomeScreen(User loggedInUser) {
        // pulizia schermata
        RootPanel.get("root").clear();
        
        // creazione nuova istanza vista 
        HomeView homeView = new HomeView();
   
        RootPanel.get("root").add(homeView);
        
        //Window.alert("Benvenuto " + loggedInUser.getUsername() + "!"); 
    }
}