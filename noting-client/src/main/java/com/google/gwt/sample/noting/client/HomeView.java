package com.google.gwt.sample.noting.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextBox;

public class HomeView extends Composite {

    interface HomeViewUiBinder extends UiBinder<Widget, HomeView> {}
    private static HomeViewUiBinder uiBinder = GWT.create(HomeViewUiBinder.class);

    @UiField Label usernameLabel;
    @UiField Button logoutButton;
    @UiField Button createNoteButton;
    @UiField VerticalPanel noteListPanel;
    @UiField TextBox searchBox;
    @UiField Button searchButton;

    private HomeViewListener listener;

    public HomeView(User user) {
        initWidget(uiBinder.createAndBindUi(this));
        usernameLabel.setText(user.getUsername());
    }
    
    public void setHomeViewListener(HomeViewListener listener) {
        this.listener = listener;
    }

    public void setNotes(List<Note> notes) {
        noteListPanel.clear();
        for (Note note : notes) {
            Button noteButton = new Button(note.getTitle());
            noteButton.addClickHandler(e -> {
                if (listener != null) {
                    listener.onNoteSelected(note);
                }
            });
            noteListPanel.add(noteButton);
        }
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

    @UiHandler("searchButton")
    void onSearchClick(ClickEvent e) {
        if (listener != null) {
            listener.onSearch(searchBox.getText());
        }
    }
}