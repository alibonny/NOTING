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


    

/********* LoginView   ************* */

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

    /****************  HomeView      ************************** */ 

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
            public void onNoteSelected(Note note, User user) {
                loadVisualizzaNota(note,user);
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

            @Override
            public void myNotesSelected() {
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
            }


            @Override
            public void condiviseConMeSelected() {
                service.getCondiviseConMe(new AsyncCallback<List<Note>>() {
                    @Override
                    public void onSuccess(List<Note> result) {
                        homeView.setNotes(result);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore nel caricamento delle note condivise: " + caught.getMessage());
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
            public void onSave(String titolo, String contenuto, Note.Stato stato, List<String> utentiList) {
                service.creazioneNota(titolo, contenuto, stato, utentiList, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Window.alert("Nota creata con successo!");
                        loadHomeView(); // Torna alla home dopo la creazione della nota
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

            @Override
            public void trovaUtente(String username, AsyncCallback<Boolean> callback) {
                service.cercaUtente( username,new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                if (Boolean.TRUE.equals(exists)) {
                    callback.onSuccess(true);
                    // eventuale logica di condivisione, es: service.condividiNota(...)
                } else {
                    callback.onSuccess(false);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Errore nella ricerca utente: " + caught.getMessage());
            }
            });
            }


        });

        RootPanel.get().clear();
        RootPanel.get().add(createNoteView);
    }

 private void loadVisualizzaNota(Note nota, User user) {
    GWT.log("[DBG] entro in loadVisualizzaNota con user=" 
    + (user != null ? user.getUsername() : "NULL")
    + ", note=" + (nota != null ? nota.getTitle() : "NULL"));
    VisualizzaNotaView view = new VisualizzaNotaView(nota,user);

    view.setVisualizzaNotaViewListener(new VisualizzaNotaViewListener() {
        @Override
        public void onBack() {
            loadHomeView(); // torna alla home
        }

        @Override
        public void onSalvaNota(Note notaModificata, Note.Stato nuovoStato) {
            service.updateNota(notaModificata, nuovoStato, new AsyncCallback<Void>() {
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
        public void onStatoNotaChanged(Note notaModificata, Note.Stato nuovoStato) {
            // Aggiorna lo stato della nota
            notaModificata.setStato(nuovoStato);
            service.updateNota(notaModificata, nuovoStato, new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Window.alert("Stato della nota aggiornato con successo!");
                }

                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Errore nell'aggiornamento dello stato: " + caught.getMessage());
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

        @Override
        public void onCreaUnaCopia(Note notaDaCopiare) {
            Window.alert("utente: " + loggedInUser.getUsername());
            service.creaCopiaNota(loggedInUser.getUsername(), notaDaCopiare.getId(), new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Window.alert("Copia della nota creata con successo!");
                    Window.alert("utente: " + loggedInUser.getUsername());

                    loadHomeView(); // Torna alla home dopo la creazione della copia
                }

                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Errore nella creazione della copia: " + caught.getMessage());
                    Window.alert("utente: " + loggedInUser.getUsername());

                }
            });
        }
    });

    RootPanel.get().clear();
    RootPanel.get().add(view);
}
}
