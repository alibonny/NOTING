package com.google.gwt.sample.stockwatcher.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

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

    public LoginView() {
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
}