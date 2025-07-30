package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
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