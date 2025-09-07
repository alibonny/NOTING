package com.google.gwt.sample.noting.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SuggestBox;
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
    @UiField SuggestBox utenteBox; // Aggiunto per la selezione dell'utente

    @UiField HTML backLink;

    @UiField Label utentiCondivisi; // Aggiunto per mostrare gli utenti condivisi
    
    
    private List<String> utentiList = new ArrayList<>(); // Lista per gli utenti condivisi



    private CreateNoteViewListener listener;

    public CreateNoteView() {
        initWidget(uiBinder.createAndBindUi(this));
        // Inizializza lo stato della nota
        for (Note.Stato stato : Note.Stato.values()) {
            statoBox.addItem(stato.name());
        }
        utenteBox.setVisible(false); // Nascondi il campo utente inizialmente
    }

    public void setCreateNoteViewListener(CreateNoteViewListener listener) {
        this.listener = listener;
    }

@UiHandler("statoBox")
void onStatoBoxChange(ChangeEvent event) {
    String selectedText = statoBox.getItemText(statoBox.getSelectedIndex());

    if ("Condivisa".equals(selectedText) || "CondivisaSCR".equals(selectedText)) {
        utenteBox.setVisible(true);
    } else if ("Privata".equals(selectedText)) {
        utenteBox.setVisible(false);
    } else {
        utenteBox.setVisible(true);
    }
}
/* 
public void setUserList(List<String> usernames) {
        userListBox.clear();
        for (String username : usernames) {
            userListBox.addItem(username);
        }
    }
*/
/* 
    public String getSelectedUser() {
        return utenteBox
    }
*/
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
            listener.onSave(titolo, contenuto, stato, utentiList);
     }
    }

   @UiHandler("backLink")
    void onBackLinkClick(ClickEvent e) {
        if (listener != null) {
            listener.onBack();
        }
    }

    @UiHandler("shareBtn")
    void onShareClick(ClickEvent e) {
    String username = utenteBox.getText().trim();


         if (listener != null) {
            listener.trovaUtente(username, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                if (Boolean.TRUE.equals(exists)) {
                    // SOLO se esiste aggiorno UI e lista
                    String esistenti = utentiCondivisi.getText();
                    utentiCondivisi.setText(esistenti + (esistenti.isEmpty() ? "" : ", ") + username);
                    utentiList.add(username);
                    utenteBox.setText("");
                    Window.alert("Utente trovato e aggiunto!");
                } else {
                    Window.alert("Username non trovato.");
                }
            }
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Errore durante la ricerca: " + caught.getMessage());
            }
        });
    }

    
}


   
}