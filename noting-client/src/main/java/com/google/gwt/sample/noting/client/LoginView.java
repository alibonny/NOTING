//package com.google.gwt.sample.noting.client;
/* 
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.NoteService;
import com.google.gwt.sample.noting.shared.NoteServiceAsync;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class LoginView extends Composite {

    // Collegamento della classe al file .ui.xml
    interface LoginViewUiBinder extends UiBinder<Widget, LoginView> {}
    private static LoginViewUiBinder uiBinder = GWT.create(LoginViewUiBinder.class);

    // Collegamento dei campi del layout a variabili Java
    @UiField
    TextBox usernameBox;

    @UiField
    PasswordTextBox passwordBox;

    @UiField
    Button loginButton;

    @UiField
    Button registerButton;
    
    
    private HomeView homeView;
    private final NoteServiceAsync noteService = GWT.create(NoteService.class);



    public LoginView() {
        LoginView loginView = this;
        HomeView homeView = new HomeView(new User("")); // Inizializza HomeView con un utente vuoto
        initWidget(uiBinder.createAndBindUi(this));
    }

    public String getUsername() {
        return usernameBox.getText();
    }

    public String getPassword() {
        return passwordBox.getText();
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Button getRegisterButton() {
        return registerButton;
    }

     public void loadLoginView() {

        this.setupLoginViewHandlers();

        RootPanel.get("root").clear();
        RootPanel.get("root").add(this);
    }


    


    //Login
     public void setupLoginViewHandlers() {
        this.getLoginButton().addClickHandler(event -> {
            // con questa chiamata GWT manda la richiesta al server dove NoteServiceImpl.login viene eseguito 
            noteService.login(this.getUsername(), this.getPassword(), new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                    Window.alert("Errore di login: " + caught.getMessage());
                }

                public void onSuccess(User result) {
                    // Se il login ha successo, carica la schermata home.
                    homeView.loadHomeView(result);
                }
            });
        });

        

        //REGISTRAZIONE 
        this.getRegisterButton().addClickHandler(event -> {
            noteService.register(this.getUsername(), this.getPassword(), new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                     Window.alert("Errore di registrazione: " + caught.getMessage());
                }
                public void onSuccess(User result) {
                    Window.alert("Registrazione completata! Ora puoi effettuare il login.");
                }
            });
        });

        // Mostra la vista di login all'avvio
        RootPanel.get("root").add(this);
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class LoginView extends Composite {

    interface LoginViewUiBinder extends UiBinder<Widget, LoginView> {}
    private static LoginViewUiBinder uiBinder = GWT.create(LoginViewUiBinder.class);

    @UiField
    TextBox usernameBox;

    @UiField
    PasswordTextBox passwordBox;

    @UiField
    Button loginButton;

    @UiField
    Button registerButton;

    private final NoteServiceAsync noteService = GWT.create(NoteService.class);
    private LogListener logListener;

    
//"Chiamami quando succede qualcosa!"
    public void setLogListener(LogListener listener) {
        this.logListener = listener;
    }

    public LogListener getLogListener() {
        return logListener;
    }

    public LoginView() {
        initWidget(uiBinder.createAndBindUi(this));
     
        setupHandlers();
       
    }
  

    private void setupHandlers() {    
        
        
    loginButton.addClickHandler(event -> {
    String username = usernameBox.getText();
    String password = passwordBox.getText();

    if (logListener != null) {
        logListener.onLogin(username, password);
      }
    });


        registerButton.addClickHandler(event -> {
            noteService.register(getUsername(), getPassword(), new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                    Window.alert("Errore di registrazione: " + caught.getMessage());
                }

                public void onSuccess(User result) {
                    Window.alert("Registrazione completata! Ora puoi effettuare il login.");
                }
            });
        });
    }

    private String getUsername() {
        return usernameBox.getText();
    }

    private String getPassword() {
        return passwordBox.getText();
    }
}
