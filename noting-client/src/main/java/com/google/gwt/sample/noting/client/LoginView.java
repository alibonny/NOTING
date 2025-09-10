package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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

    private LogListener logListener;

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
        String username = usernameBox.getText();
        String password = passwordBox.getText();
        
        if(logListener != null){
        logListener.onRegister(username, password);
        }      
        });
     
    }

    private String getUsername() {
        return usernameBox.getText();
    }

    private String getPassword() {
        return passwordBox.getText();
    }
}
