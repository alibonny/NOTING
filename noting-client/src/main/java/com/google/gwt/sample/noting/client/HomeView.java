package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class HomeView extends Composite {

    interface HomeViewUiBinder extends UiBinder<Widget, HomeView> {}
    private static HomeViewUiBinder uiBinder = GWT.create(HomeViewUiBinder.class);

    @UiField
    Label usernameLabel;

    @UiField
    Button logoutButton;

    @UiField
    Button createNoteButton;

    public HomeView(User user) {
        initWidget(uiBinder.createAndBindUi(this));
        usernameLabel.setText("Benvenuto, " + user.getUsername());
    }

    public Button getLogoutButton() {
        return logoutButton;
    }

    public Button getCreateNoteButton() {
        return createNoteButton;
    }
}