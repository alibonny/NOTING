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
import com.google.gwt.user.client.ui.HorizontalPanel;
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
    
    @UiField ListBox tagBox;
    @UiField TextBox newTagBox;
    @UiField Button confirmNewTagButton;
    @UiField Button addTagButton;
    @UiField Label topRightTagLabel;
    
    private List<String> utentiList = new ArrayList<>(); // Lista per gli utenti condivisi
    private List<String> selectedTags = new ArrayList<>();


    private CreateNoteViewListener listener;
    private boolean isCreatingNewTag = false;

    public CreateNoteView() {
        initWidget(uiBinder.createAndBindUi(this));
        // Inizializza lo stato della nota
        for (Note.Stato stato : Note.Stato.values()) {
            statoBox.addItem(stato.name());
        }
        utenteBox.setVisible(false); // Nascondi il campo utente inizialmente
        newTagBox.setVisible(false);
        confirmNewTagButton.setVisible(false);

        topRightTagLabel.setText("Tag:"); 

        loadAvailableTags();
    }


    public void setCreateNoteViewListener(CreateNoteViewListener listener) {
        this.listener = listener;
    }

    private void loadAvailableTags() {
        tagBox.clear();
        tagBox.addItem("Seleziona tag", "");
        
        String[] defaultTags = {"Università", "Tempo libero", "Importante", "Promemoria", "Cose da fare"};
        for (String tag : defaultTags) {
            tagBox.addItem(tag, tag);
        }
        
        tagBox.addItem("Crea nuovo", "new");
    }

    @UiHandler("tagBox")
    void onTagBoxChange(ChangeEvent event) {
        String selectedValue = tagBox.getSelectedValue();
        
        if ("new".equals(selectedValue)) {
            newTagBox.setVisible(true);
            confirmNewTagButton.setVisible(true);
            addTagButton.setVisible(false);
            newTagBox.setFocus(true);
            isCreatingNewTag = true;
         } else if (!selectedValue.isEmpty() && !"".equals(selectedValue)) {
            addTagButton.setVisible(true);
            newTagBox.setVisible(false);
            confirmNewTagButton.setVisible(false);
            isCreatingNewTag = false;
        }
    }

     @UiHandler("addTagButton")
    void onAddTagClick(ClickEvent e) {
        String selectedValue = tagBox.getSelectedValue();
        
        if ("new".equals(selectedValue)) {
            newTagBox.setVisible(true);
            confirmNewTagButton.setVisible(true);
            addTagButton.setVisible(false);
            newTagBox.setFocus(true);
            isCreatingNewTag = true;
        } else if (!selectedValue.isEmpty() && !"".equals(selectedValue)) {
            String selectedTag = tagBox.getSelectedItemText();
            addTagToSelection(selectedTag);
        }
    }

    @UiHandler("confirmNewTagButton")
    void onConfirmNewTagClick(ClickEvent e) {
        String newTag = newTagBox.getText().trim();
        if (!newTag.isEmpty()) {
            addTagToSelection(newTag);
            int lastIndex = tagBox.getItemCount() - 1;
            tagBox.insertItem(newTag, newTag, lastIndex);

            tagBox.setSelectedIndex(0);

            newTagBox.setVisible(false);
            confirmNewTagButton.setVisible(false);
            addTagButton.setVisible(true);
            newTagBox.setText("");
            isCreatingNewTag = false;
        }
    }

    private void addTagToSelection(String tag) {
        if (!selectedTags.contains(tag)) {
            selectedTags.add(tag);
            updateSelectedTagsLabel();
            Window.alert("Tag aggiunto: " + tag);
            topRightTagLabel.setText("Tag: " + String.join(", ", selectedTags));
            tagBox.setSelectedIndex(0);
        }
    }

    private void updateSelectedTagsLabel() {
        String tagsText = "Tag selezionati: " + String.join(", ", selectedTags);
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

            List<String> tagsToSave = new ArrayList<>();
            for (int i = 0; i < tagBox.getItemCount(); i++) {
                tagsToSave.add(tagBox.getItemText(i));
            }

            String newTag = newTagBox.getText().trim();
            if (!newTag.isEmpty() && !tagsToSave.contains(newTag)) {
                tagsToSave.add(newTag);
            }
            
            if (listener != null) {
                listener.onSave(titolo, contenuto, stato, utentiList, new ArrayList<>(selectedTags));
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