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
    @UiField Button backButton;
    @UiField ListBox statoBox;


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
        System.out.println("Salvataggio nota: " + titoloBox.getText());
       
        String statoSelezionato = statoBox.getSelectedItemText()
        .replace(" ", "");

         Note.Stato stato = Note.Stato.valueOf(statoSelezionato);
         Window.alert("Salvataggio nota: " + titoloBox.getText() + "\nStato selezionato:"
         + stato.name());


        if (listener != null) {

            listener.onSave(statoSelezionato, statoSelezionato, stato);
        }
    }

    @UiHandler("backButton")
    void onBackClick(ClickEvent e) {
        if (listener != null) {
            listener.onBack();
        }
    }
   
}