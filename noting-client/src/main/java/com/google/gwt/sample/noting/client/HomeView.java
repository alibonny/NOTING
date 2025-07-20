package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class HomeView extends Composite {

    interface HomeViewUiBinder extends UiBinder<Widget, HomeView> {}
    private static HomeViewUiBinder uiBinder = GWT.create(HomeViewUiBinder.class);

    @UiField Label usernameLabel;
    @UiField Button logoutButton;
    @UiField Button createNoteButton;

    private HomeViewListener listener;

    public HomeView(User user) {
        initWidget(uiBinder.createAndBindUi(this));
        usernameLabel.setText(user.getUsername());
    }
    
    public void setHomeViewListener(HomeViewListener listener) {
        this.listener = listener;
    }

    @UiHandler("logoutButton")
    void onLogoutClick(ClickEvent e) {
        if (listener != null) {
            listener.onLogout();
        }
    }
    
    @UiHandler("createNoteButton")
    void onCreateNoteClick(ClickEvent e) {
        if (listener != null) {
            listener.onCreateNote();
        }
    }
}