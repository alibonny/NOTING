package com.google.gwt.sample.noting.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class HomeView extends Composite {

    interface HomeViewUiBinder extends UiBinder<Widget, HomeView> {}
    private static HomeViewUiBinder uiBinder = GWT.create(HomeViewUiBinder.class);

    @UiField Label usernameLabel;
    @UiField Button logoutButton;
    @UiField Button createNoteButton;
    @UiField TextBox searchBox;
    @UiField Button searchButton;
    
    @UiField FlowPanel noteListPanel;

    @UiField ListBox filterTypeBox;
    @UiField TextBox filterValueBox;
    @UiField Button applyFilterButton;

    private User user; // per tenere traccia dell'utente loggato
        
    private HomeViewListener listener;
    private String currentView = "myNotes";

    public HomeView(User user) {
        initWidget(uiBinder.createAndBindUi(this));
        usernameLabel.setText(user.getUsername());
        this.user = user;                    
        usernameLabel.setText(
        (this.user!=null && this.user.getUsername()!=null) ? this.user.getUsername() : "(utente)");

        filterTypeBox.addItem("Autore");
        filterTypeBox.addItem("Tag");
    }
    
    
    public void setHomeViewListener(HomeViewListener listener) {
        this.listener = listener;
    }

    public void setNotes(List<Note> notes) {
        noteListPanel.clear();

        for (Note note : notes) {
            // 1. Creiamo un contenitore per la nostra nota (il post-it)
            FlowPanel noteWidget = new FlowPanel();
            noteWidget.addStyleName("noteItem"); // Stile generico del post-it

            // 2. Aggiungiamo lo stile specifico per l'immagine di sfondo
            switch (note.getStato()) {
                case Privata:
                    noteWidget.addStyleName("nota-privata-img");
                    break;
                case Condivisa:
                case CondivisaSCR:
                    noteWidget.addStyleName("nota-condivisa-img");
                    break;
            }

            // 3. Aggiungiamo il titolo della nota
            Label titleLabel = new Label(note.getTitle());
            noteWidget.add(titleLabel);

            // 4. Aggiungiamo il gestore del click all'intero widget
            noteWidget.addDomHandler(e -> {
            String u = (this.user != null ? this.user.getUsername() : "NULL_USER");
            //GWT.log("[CLICK] user=" + u + ", note=" + (note!=null?note.getTitle():"NULL_NOTE"));
            if (listener != null && this.user != null) {
                listener.onNoteSelected(note, this.user);
            } else {
                Window.alert("USER NULL: rifai login");
            }
        }, ClickEvent.getType());


            // 5. Aggiungiamo il nostro nuovo post-it alla griglia
            noteListPanel.add(noteWidget);
        }
    }
  
    @UiHandler("applyFilterButton")
    void onApplyFilterClick(ClickEvent e) {
        if (listener != null) {
            String filterType = filterTypeBox.getSelectedItemText();
            String filterValue = filterValueBox.getText();
            // vista corrente
            listener.onFilterSearch(currentView, filterType, filterValue);
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
    
    @UiHandler("myNotes")
    void onMyNotesClick(ClickEvent e) {
        currentView = "myNotes"; // Imposta lo stato
        if (listener != null) {
            listener.myNotesSelected();
        }
    }

    @UiHandler("condiviseConMe")
    void onCondiviseConMeClick(ClickEvent e) {
        currentView = "sharedWithMe"; // Imposta lo stato
        if (listener != null) {
            listener.condiviseConMeSelected();
        }
    }

}