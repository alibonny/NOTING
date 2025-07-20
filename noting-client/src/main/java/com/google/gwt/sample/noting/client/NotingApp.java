/*package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;

public class NotingApp implements EntryPoint{

    //La classe NoteServiceAsync viene utilizzata per effettuare chiamate asincrone al servizio di backend.
    // con questa instanziazzione GWT generea il codice che comunica con il server
    private final NoteServiceAsync noteService = GWT.create(NoteService.class);
    private LoginView loginView = new LoginView();
    private HomeView homeView;
    private CreateNoteView createNote;
    private User loggedInUser; //     

    public void onModuleLoad() {
        GWT.log("Modulo GWT caricato");

        loginView.loadLoginView();
    }
    /* 
     public void loadLoginView() {
        loginView = new LoginView();

        loginView.setupLoginViewHandlers();

        RootPanel.get("root").clear();
        RootPanel.get("root").add(loginView);
    }
    */

  /*   //LOGIN
    private void setupLoginViewHandlers() {
        loginView.getLoginButton().addClickHandler(event -> {
            // con questa chiamata GWT manda la richiesta al server dove NoteServiceImpl.login viene eseguito 
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
     /* 
    public  void loadHomeView(User loggedInUser) {
        homeView = new HomeView(loggedInUser);
        // Imposta i gestori degli eventi per la HomeView
        // Questo metodo viene chiamato per associare i gestori degli eventi ai pulsanti della HomeView.
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

        homeView.getCreateNoteButton().addClickHandler(event -> {
            noteService.creazioneNota(new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Errore durante la creazione della nota: " + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    Window.alert("Nota creata con successo!");
                    // Qui potresti aggiungere logica per aggiornare la vista o mostrare le note create.
                   loadCreateNoteView();

                }
            });


        });

       

   } 
       private void loadCreateNoteView() {
        createNote = new CreateNoteView();
        // Imposta i gestori degli eventi per la HomeView
        // Questo metodo viene chiamato per associare i gestori degli eventi ai pulsanti della HomeView.
        setupCreateNoteViewHandlers();

        RootPanel.get("root").clear();
        RootPanel.get("root").add(createNote);
        }


        private void setupCreateNoteViewHandlers() {
            createNote.getBackButton().addClickHandler(event -> {
                    Window.alert("Torno alla Home!");

                // Torna alla HomeView
                    loadHomeView(loggedInUser); // Assicurati di passare l'utente loggato corrente
            });

        }

}*/

package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;


public class NotingApp implements EntryPoint {

    private final NoteServiceAsync service = GWT.create(NoteService.class);
    private LoginView loginView;
    private HomeView homeView;
    private User loggedInUser;


    public void onModuleLoad() {
        LoginView loginView = new LoginView();

        loginView.setLogListener(new LogListener() {
            @Override
            public void onLogin(String username, String password) {
              //  NoteServiceAsync service = GWT.create(NoteService.class);

                service.login(username, password, new AsyncCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        HomeView home = new HomeView(user);
                        RootPanel.get().clear();
                        RootPanel.get().add(home);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Login fallito: " + caught.getMessage());
                    }
                });
            }
        });

        RootPanel.get().add(loginView);
    }
/*
    private void loadLoginView() {
        loginView = new LoginView();
        loginView.setLogListener(new LogListener() {
            public void onLogin(String username, String password) {
                GWT.log("Tentativo di login con: " + username);
            }

            public void onLoginSuccess(User user) {
                loggedInUser = user;
                loadHomeView(user);
            }

            public void onLoginError(Throwable caught) {
                Window.alert("Login fallito: " + caught.getMessage());
            }

            public void onLogout() {
                GWT.log("Logout richiesto...");
            }

            public void onLogoutSuccess() {
                Window.alert("Logout eseguito correttamente.");
                loadLoginView();
            }

            public void onLogoutError(Throwable caught) {
                Window.alert("Errore durante il logout: " + caught.getMessage());
            }
        });

        loginView.loadLoginView();
    }

      









    private void loadHomeView(User user) {
    homeView = new HomeView(user);
    
    homeView.setHomeViewListener(new HomeViewListener() {
       // public void onLogoutRequested() {
        //    loadLoginView(); // logout
        //}

        public void onCreateNote() {
            loadCreateNoteView(); // crea nota
        }
    });

    homeView.loadHomeView();
}






















    private void loadCreateNoteView() {
    CreateNoteView createNoteView = new CreateNoteView();

    RootPanel.get("root").clear();
    RootPanel.get("root").add(createNoteView);
}




// Istanzia la view di login
        LoginView loginView = new LoginView();

        // Imposta il listener per gestire il successo del login
        loginView.setLoginSuccessListener(new LoginSuccessListener() {
            @Override
            public void onLoginSuccess(User user) {
                // Quando il login ha successo, mostra la HomeView
                HomeView homeView = new HomeView(user);

                // Svuota la pagina e inserisce la nuova schermata
                RootPanel.get().clear();
                RootPanel.get().add(homeView);
            }
        });

        // Mostra la schermata di login all'avvio
        RootPanel.get().add(loginView);
    }

/* */
}
