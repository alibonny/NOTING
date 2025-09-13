package com.google.gwt.sample.noting.client;

import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.LockToken;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteMemento;
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
        
        loginView.setLogListener(new LogListener() {
            @Override
            public void onLogin(String username,String password){
        
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
        }

        @Override
        public void onRegister(String username, String password){
            service.register(username, password, new AsyncCallback<User>(){
                @Override
                public void onSuccess(User user){
                    Window.alert("Registrazione completata");
                }
                @Override
                public void onFailure(Throwable caught){
                    Window.alert("Register fallito" + caught.getMessage());
                }
            });
        }

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
                stopLockHeartbeat(); // ferma il rinnovo del lock se attivo
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
            public void onSave(String titolo, String contenuto, Note.Stato stato, List<String> utentiList, List<String> tags) {
                service.creazioneNota(titolo, contenuto, stato, utentiList, tags, new AsyncCallback<Void>() {
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
                stopLockHeartbeat(); // ferma il rinnovo del lock se attivo
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
            public void onGetNoteHistory(int noteId, AsyncCallback<List<NoteMemento>> callback) {
                service.getNoteHistory(noteId, callback);
            }

            @Override
            public void onRestoreNote(int noteId, int historyIndex, AsyncCallback<Note> callback) {
                service.restoreNoteFromHistory(noteId, historyIndex, callback);
            }

            @Override
            public void onBack() {
                stopLockHeartbeat();
                loadHomeView(); // torna alla home
            }

            @Override
            public void onSalvaNota(Note notaModificata) {
                service.updateNota(notaModificata, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        stopLockHeartbeat(); // ferma il rinnovo del lock
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
                        stopLockHeartbeat(); // ferma il rinnovo del lock
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
            public void onAddTagToNote(int noteId, String tagName, AsyncCallback<Void> callback) {
                service.addTagToNote(noteId, tagName, callback);
            }

            @Override
            public void onRemoveTagFromNote(int noteId, String tagName, AsyncCallback<Void> callback) {
                service.removeTagFromNote(noteId, tagName, callback);
            }

            @Override
            public void onCreateNewTag(String tagName, AsyncCallback<Void> callback) {
                service.createNewTag(tagName, callback);
            }

            @Override
            public void onCreaUnaCopia(Note notaDaCopiare) {
                Window.alert("utente: " + loggedInUser.getUsername());
                service.creaCopiaNota(loggedInUser.getUsername(), notaDaCopiare.getId(), new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        stopLockHeartbeat(); // ferma il rinnovo del lock
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
            @Override
            public void onAnnullaCondivisione(Note notaDaAnnullare) {
                service.annullaCondivisione(loggedInUser.getUsername(), notaDaAnnullare.getId(), new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        stopLockHeartbeat();
                        Window.alert("Condivisione della nota annullata con successo!");
                        loadHomeView(); // Torna alla home dopo aver annullato la condivisione
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore nell'annullamento della condivisione: " + caught.getMessage());
                    }
                });
            }

            @Override
            public void onRimuoviUtenteCondivisione(Note nota, String username, AsyncCallback<Note> callback){
                service.rimuoviUtenteCondivisione(nota.getId(),username, new AsyncCallback<Note>() {
                    @Override
                    public void onSuccess(Note fresh) {
                        Window.alert("Utente rimosso dalla condivisione con successo!");

                    

                        // ricarica la vista con l'oggetto nota aggiornato
                        loadVisualizzaNota(fresh, user);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore nella rimozione dell'utente dalla condivisione: " + caught.getMessage());
                    }
                });

            }


            @Override
            public void onRichiediLock(int noteId) {
                service.tryAcquireLock(noteId, new AsyncCallback<LockToken>(){
                    @Override
                    public void onSuccess(LockToken token) {
                        boolean isOwner = loggedInUser.getUsername().equals(nota.getOwnerUsername());
                        view.enableEditing(isOwner);
                        startLockHeartbeat(noteId); // il timer che fa renew ogni 30s
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        view.disableEditingWithMessage("Nota già in modifica da un altro utente: " + caught.getMessage());
                        loadHomeView();
                    } 
                });
            }

        
            @Override
            public void trovaUtente2(Note nota,String username, AsyncCallback<Boolean> callback) {
                    System.out.println("cerca utente" + username);

                    service.cercaUtente2(nota,username,new AsyncCallback<Boolean>() {
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


            @Override
            public void aggiungiCondivisione(int notaId, String username, AsyncCallback<Note> callback){
                service.aggiungiCondivisione(notaId, username, new AsyncCallback<Note>() {
            @Override
            public void onSuccess(Note result) {
                // inoltra al callback del caller
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                // inoltra al callback del caller
                callback.onFailure(caught);
            }
                
            });
            
        }

        @Override
        public void getNotaById(int noteId, AsyncCallback<Note> callback) {
            service.getNotaById(noteId, callback);
        }

    });
                
    RootPanel.get().clear();
    RootPanel.get().add(view);
}

// --- Lock heartbeat (semplice) ---
private com.google.gwt.user.client.Timer lockTimer;
private Integer editingNoteId;

private void startLockHeartbeat(int noteId) {
    stopLockHeartbeat();            // safety
    editingNoteId = noteId;
    lockTimer = new com.google.gwt.user.client.Timer() {
        @Override public void run() {
            service.renewLock(editingNoteId, new AsyncCallback<LockToken>() {
                @Override public void onSuccess(LockToken t) { /* ok */ }
                @Override public void onFailure(Throwable caught) {
                    Window.alert("Lock perso: " + caught.getMessage());
                    stopLockHeartbeat();
                }
            });
        }
    };
    lockTimer.scheduleRepeating(90_000); // ogni 30s
}

private void stopLockHeartbeat() {
    if (lockTimer != null) { lockTimer.cancel(); lockTimer = null; }
    if (editingNoteId != null) {
        final int toRelease = editingNoteId; // evita race se si azzera dopo
        service.releaseLock(toRelease, new AsyncCallback<Void>() {
            @Override public void onSuccess(Void r) { /* ok */ }
            @Override public void onFailure(Throwable c) { /* ignora */ }
        });
        editingNoteId = null;
    }
}


 }


