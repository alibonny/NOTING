/*package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class HomeView extends Composite {

    interface HomeViewUiBinder extends UiBinder<Widget, HomeView> {}
    private final NoteServiceAsync noteService = GWT.create(NoteService.class);

    private static HomeViewUiBinder uiBinder = GWT.create(HomeViewUiBinder.class);

    @UiField
    Label usernameLabel;

    @UiField
    Button logoutButton;

    @UiField
    Button createNoteButton;

    private LoginView loginView;

    public HomeView(User user) {
        loginView = new LoginView();
        // Inizializza la HomeView con l'utente passato come parametro
        HomeView homeView = this;
        initWidget(uiBinder.createAndBindUi(this));
        usernameLabel.setText("Benvenuto, " + user.getUsername());
    }

    public Button getLogoutButton() {
        return logoutButton;
    }

    public Button getCreateNoteButton() {
        return createNoteButton;
    }


    public  void loadHomeView(User loggedInUser) {
        //homeView = new HomeView(loggedInUser);
        // Imposta i gestori degli eventi per la HomeView
        // Questo metodo viene chiamato per associare i gestori degli eventi ai pulsanti della HomeView.
        setupHomeViewHandlers();

        RootPanel.get("root").clear();
        RootPanel.get("root").add(this);
    }

    public  void setupHomeViewHandlers() {
        this.getLogoutButton().addClickHandler(event -> {
            noteService.logout(new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                  //  Window.alert("Errore durante il logout: " + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    // Se il logout ha successo, torna alla schermata di login.
                    loginView.loadLoginView();
                }
            });
        });

        this.getCreateNoteButton().addClickHandler(event -> {
            noteService.creazioneNota(new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                   // Window.alert("Errore durante la creazione della nota: " + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    //Window.alert("Nota creata con successo!");
                    // Qui potresti aggiungere logica per aggiornare la vista o mostrare le note create.
                  // loadCreateNoteView();

                }
            });


        });
    }
}
    */
    package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

public class HomeView extends Composite {

    interface HomeViewUiBinder extends UiBinder<Widget, HomeView> {}
    private static HomeViewUiBinder uiBinder = GWT.create(HomeViewUiBinder.class);

    @UiField
    Label usernameLabel;

    @UiField
    Button logoutButton;

    @UiField
    Button createNoteButton;

    private final NoteServiceAsync noteService = GWT.create(NoteService.class);
    private LogoutListener logoutListener;

    public HomeView(User user) {
        initWidget(uiBinder.createAndBindUi(this));
        usernameLabel.setText("Benvenuto, " + user.getUsername());
    }

    public void setLogoutListener(LogoutListener listener) {
        this.logoutListener = listener;
    }

    public void loadHomeView() {
        setupHomeViewHandlers();
        RootPanel.get("root").clear();
        RootPanel.get("root").add(this);
    }

    private void setupHomeViewHandlers() {
        logoutButton.addClickHandler(event -> {
            noteService.logout(new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    // gestisci errore
                }

                @Override
                public void onSuccess(Void result) {
                    if (logoutListener != null) {
                        logoutListener.onLogout();
                    }
                }
            });
        });

        createNoteButton.addClickHandler(event -> {
            // gestisci creazione nota (opzionale da implementare in seguito)
        });
    }
}
