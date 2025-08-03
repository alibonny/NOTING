package com.google.gwt.sample.noting.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class CreateNoteView extends Composite {

    interface CreateNoteViewUiBinder extends UiBinder<Widget, CreateNoteView> {}
    private static CreateNoteViewUiBinder uiBinder = GWT.create(CreateNoteViewUiBinder.class);

    @UiField TextBox titoloBox;
    @UiField TextArea contenutoBox;
    @UiField Button saveButton;

    @UiField ListBox statoBox;
    @UiField ListBox userListBox; // Aggiunto per la selezione dell'utente

    @UiField HTML backLink;


    private CreateNoteViewListener listener;

    public CreateNoteView() {
        initWidget(uiBinder.createAndBindUi(this));
        // Inizializza lo stato della nota
        for (Note.Stato stato : Note.Stato.values()) {
            statoBox.addItem(stato.name());
        }
    }

    public void setCreateNoteViewListener(CreateNoteViewListener listener) {
        this.listener = listener;
    }

    @UiHandler("statoBox")
void onStatoBoxChange(ChangeEvent event) {
    String selectedValue = statoBox.getSelectedValue();
    // oppure:
    // String selectedText = statoBox.getItemText(statoBox.getSelectedIndex());
    Window.alert("Hai selezionato: " + selectedValue);
    if(listener != null && (selectedValue == "Condivisa" || selectedValue=="CondivisaSCR")) {
        listener.mostraUtenti(); // Notifica il listener della selezione dello stato
    }

    
}

public void setUserList(List<String> usernames) {
        userListBox.clear();
        for (String username : usernames) {
            userListBox.addItem(username);
        }
    }

    public String getSelectedUser() {
        return userListBox.getSelectedItemText();
    }

   @UiHandler("saveButton")
    void onSaveClick(ClickEvent e) {
        String titolo = titoloBox.getText();
        String contenuto = contenutoBox.getText();
        String statoSelezionato = statoBox.getSelectedItemText().replace(" ", "");
        Note.Stato stato = Note.Stato.valueOf(statoSelezionato);

        if (titolo.trim().isEmpty() || contenuto.trim().isEmpty()) {
            Window.alert("Titolo e contenuto non possono essere vuoti!");
            return;
        }

        if (listener != null) {
            listener.onSave(titolo, contenuto, stato);
        }
    }

   @UiHandler("backLink")
    void onBackLinkClick(ClickEvent e) {
        if (listener != null) {
            listener.onBack();
        }
    }
   
}