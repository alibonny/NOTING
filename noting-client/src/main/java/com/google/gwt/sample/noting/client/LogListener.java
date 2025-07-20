package com.google.gwt.sample.noting.client;

//logListener è una variabile di tipo interfaccia, che permette alla View (LoginView) 
//di notificare il Controller (NotingApp) quando l’utente clicca
public interface LogListener {
    void onLogin(String username, String password);
}

