package com.google.gwt.sample.noting.client;

import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

public class NotingApp implements EntryPoint {
    
    private final NoteServiceAsync service = GWT.create(NoteService.class);
    private User loggedInUser; // Mantiene lo stato dell'utente loggato

    @Override
    public void onModuleLoad() {
        loadLoginView();
    }

    private void loadLoginView() {
        this.loggedInUser = null; 
        LoginView loginView = new LoginView();
        
        loginView.setLogListener((username, password) -> {
            service.login(username, password, new AsyncCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    loggedInUser = user;
                    loadHomeView();
                }

                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Login fallito: " + caught.getMessage());
                }
            });
        });

        RootPanel.get().clear();
        RootPanel.get().add(loginView);
    }

    private void loadHomeView() {
        if (loggedInUser == null) return; // Sicurezza
        
        HomeView homeView = new HomeView(loggedInUser);

        homeView.setHomeViewListener(new HomeViewListener() {
            @Override
            public void onLogout() {
                service.logout(new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Window.alert("Logout effettuato con successo!");
                        loadLoginView();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore durante il logout: " + caught.getMessage());
                    }
                });
            }

            @Override
            public void onCreateNote() {
                loadCreateNoteView();
            }

             @Override
            public void onNoteSelected(Note note) {
                loadVisualizzaNota(note);
            }

            @Override
            public void onSearch(String query) {
                service.searchNotes(query, new AsyncCallback<List<Note>>() {
                    @Override
                    public void onSuccess(List<Note> results) {
                        homeView.setNotes(results);
                       
                    }
                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore ricerca: " + caught.getMessage());
                    }
                });
            }

        });

        service.getNoteUtente(new AsyncCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> result) {
                homeView.setNotes(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Errore nel caricamento delle note: " + caught.getMessage());
            }
        });


        RootPanel.get().clear();
        RootPanel.get().add(homeView);
    }

    private void loadCreateNoteView() {
        CreateNoteView createNoteView = new CreateNoteView();

        createNoteView.setCreateNoteViewListener(new CreateNoteViewListener() {
            @Override
            public void onSave(String titolo, String contenuto, Note.Stato stato) {
                service.creazioneNota(titolo, contenuto, stato, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Window.alert("Nota salvata con successo!");
                        loadHomeView(); // Torna alla home
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore nel salvataggio della nota: " + caught.getMessage());
                    }
                });
            }

            @Override
            public void onBack() {
                loadHomeView(); 
            }
        });

        RootPanel.get().clear();
        RootPanel.get().add(createNoteView);
    }

   private void loadVisualizzaNota(Note nota) {
    VisualizzaNotaView view = new VisualizzaNotaView(nota);

    view.setVisualizzaNotaViewListener(new VisualizzaNotaViewListener() {
        @Override
        public void onBack() {
            loadHomeView(); // torna alla home
        }

        @Override
        public void onSalvaNota(Note notaModificata) {
            service.updateNota(notaModificata, new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Window.alert("Nota salvata con successo!");
                    loadHomeView(); // torna alla home dopo il salvataggio
                }

                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Errore nel salvataggio: " + caught.getMessage());
                }
            });
        }

        @Override
        public void onEliminaNota(Note notaDaEliminare) {
            service.eliminaNota(loggedInUser.getUsername(), notaDaEliminare.getId(), new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Window.alert("Nota eliminata con successo!");
                    loadHomeView();
                }

                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Errore durante l'eliminazione: " + caught.getMessage());
                }
            });
        }
    });

    RootPanel.get().clear();
    RootPanel.get().add(view);
}



}