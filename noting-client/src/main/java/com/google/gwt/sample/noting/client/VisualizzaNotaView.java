package com.google.gwt.sample.noting.client;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;


import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.sample.noting.shared.LockToken;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.sample.noting.shared.NoteMemento;
import com.google.gwt.sample.noting.shared.User;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class VisualizzaNotaView extends Composite {

    interface VisualizzaNotaViewUiBinder extends UiBinder<Widget, VisualizzaNotaView> {}
    private static VisualizzaNotaViewUiBinder uiBinder = GWT.create(VisualizzaNotaViewUiBinder.class);

    @UiField TextBox titoloBox;
    @UiField TextArea contenutoArea;
    @UiField Button salvaButton, modificaButton, eliminaButton, creaUnaCopia, annullaCondivisione;
    @UiField HTML backLink;
    @UiField ListBox statoBox;
    @UiField HTMLPanel shareSection;
    @UiField FlowPanel sharedUsersPanel;
    @UiField Button aggiungiUtenteButton, confermaUtenteDaAggiungere, addTagButton, restoreButton, eliminaTagButton;
    @UiField SuggestBox cercaUtenteDaAggiungere;
    @UiField TextBox newTagBox;
    @UiField ListBox tagBox;
    @UiField ListBox historyBox;
    @UiField Label tagLabel;
    @UiField HorizontalPanel tagControlsPanel;
    @UiField Label topRightTagLabel;


    private VisualizzaNotaViewListener listener;
    private Note nota;
    private User user;
    private boolean isCreatingNewTag = false;
    private List<String> selectedTags;
    private String selectedTag;

    public VisualizzaNotaView(Note nota, User user) {
        initWidget(uiBinder.createAndBindUi(this));
        this.nota = nota;
        this.user = user;

        this.selectedTags = (nota.getTags() != null) ? new ArrayList<>(nota.getTags()) : new ArrayList<>();
        this.selectedTag = !this.selectedTags.isEmpty() ? this.selectedTags.get(0) : null;

        GWT.log("Tag selezionati (" + this.selectedTags.size() + "): " + this.selectedTags.toString());
       

        setUpInitialUi();

        restoreButton.setVisible(false);
        historyBox.setVisible(false);

        restoreButton.addClickHandler(event -> {
            historyBox.setVisible(true);
            historyBox.removeStyleName("hidden");

            if (listener != null && nota != null) {
                listener.onGetNoteHistory(nota.getId(), new AsyncCallback<List<NoteMemento>>() {
                    @Override
                    public void onSuccess(List<NoteMemento> history) {
                        historyBox.clear();

                        historyBox.addItem("Seleziona versione", "-1");
                         if (history == null || history.isEmpty()) return;

                        historyBox.addItem("Originale", "0");

                         for (int i = 1; i < history.size(); i++) {
                    NoteMemento m = history.get(i);
                    String label = "Modifica " + i; // opzionale: aggiungi timestamp se vuoi
                    historyBox.addItem(label, String.valueOf(i));



                      //  DateTimeFormat fmt = DateTimeFormat.getFormat("dd/MM/yyyy HH:mm");
                       // historyBox.addItem(" ", "Seleziona una versione");
                       // for (int i = 1; i < history.size(); i++) {
                      //      NoteMemento m = history.get(i);
                       //     historyBox.addItem(fmt.format(m.getTimestamp()) + " - " + m.getTitle(), String.valueOf(i));
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore nel caricamento della cronologia: " + caught.getMessage());
                    }
                });
            }
        });

        historyBox.addChangeHandler(event -> {
            int selectedIndex = historyBox.getSelectedIndex();
            if (selectedIndex <= 0) return; // 0 = "Seleziona versione"

            String val = historyBox.getValue(selectedIndex);
            int historyIndex;
            try {
                historyIndex = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return;
            }
            if (selectedIndex >= 0 && listener != null) {
                listener.onRestoreNote(nota.getId(), selectedIndex, new AsyncCallback<Note>() {
                    @Override
                    public void onSuccess(Note restored) {
                        setNota(restored);

                        Window.alert("Nota ripristinata!");
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore nel ripristino: " + caught.getMessage());
                    }
                });
            }
        });
    }

    public void setNota(Note nota) {
        this.nota = nota;
        titoloBox.setText(nota.getTitle());
        contenutoArea.setText(nota.getContent());

        if (nota.getStato() != null) {
            for (int i = 0; i < statoBox.getItemCount(); i++) {
                if (nota.getStato().name().equals(statoBox.getValue(i))) {
                    statoBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        selectedTags = (nota.getTags() != null) ? new ArrayList<>(nota.getTags()) : new ArrayList<>();
        if (!selectedTags.isEmpty()) {
            topRightTagLabel.setText("Tag: " + String.join(", ", selectedTags));
        } else {
            topRightTagLabel.setText("Tag:");
        }

        updateShareSectionVisibility();
        //applyReadOnly();
        //updateTagDisplay();
    }
        

    private boolean isOwner() {
        return nota != null && user != null &&
               nota.getOwnerUsername() != null &&
               nota.getOwnerUsername().equals(user.getUsername());
    }

    private boolean isSharedState() {
        return nota != null && (nota.getStato() == Note.Stato.Condivisa ||
                               nota.getStato() == Note.Stato.CondivisaSCR);
    }

    private boolean canEdit() {
        return isOwner() || (nota != null && nota.getStato() == Note.Stato.CondivisaSCR);
    }

    private void updateShareSectionVisibility() {
        boolean visible = isOwner() && isSharedState();

        // Rimuove o aggiunge manualmente lo stile "hidden" per garantire la visibilità
        if (visible) {
            shareSection.removeStyleName("hidden");
            renderCondivisioni();
        } else {
            shareSection.addStyleName("hidden");
            sharedUsersPanel.clear();
        }

        shareSection.setVisible(visible);
    }

    private void applyReadOnly() {
        titoloBox.setReadOnly(true);
        contenutoArea.setReadOnly(true);
        statoBox.setEnabled(false);
        salvaButton.setVisible(true); // Lasciamo visibile Salva
        salvaButton.setEnabled(false); // Ma disabilitato in read-only

        tagControlsPanel.setVisible(false);
        tagLabel.setVisible(false);

        modificaButton.setEnabled(true);
    }

    private void applyEdit(boolean owner) {
        titoloBox.setReadOnly(false);
        contenutoArea.setReadOnly(false);
        statoBox.setEnabled(owner);
        salvaButton.setVisible(true);
        modificaButton.setEnabled(true);
    }

    private void setUpInitialUi() {
        String titolo = (nota != null && nota.getTitle() != null) ? nota.getTitle() : "";
        String contenuto = (nota != null && nota.getContent() != null) ? nota.getContent() : "";
        titoloBox.setText(titolo);
        contenutoArea.setText(contenuto);
        topRightTagLabel.setText("Tag:"); 
        if (nota.getTags() != null && !nota.getTags().isEmpty()) {
            String allTags = String.join(", ", nota.getTags());
            topRightTagLabel.setText("Tag: " + allTags);
        } else {
            topRightTagLabel.setText("Tag:");
        }

        applyReadOnly();

        statoBox.clear();
        for (Note.Stato s : Note.Stato.values()) {
            statoBox.addItem(s.name(), s.name());
        }

        Note.Stato stato = (nota != null) ? nota.getStato() : null;
        if (stato != null) {
            for (int i = 0; i < statoBox.getItemCount(); i++) {
                if (stato.name().equals(statoBox.getValue(i))) {
                    statoBox.setSelectedIndex(i);
                    break;
                }
            }
        } else if (statoBox.getItemCount() > 0) {
            statoBox.setSelectedIndex(0);
        }

        modificaButton.setEnabled(canEdit());

        if (!isOwner() && isSharedState()) {
            annullaCondivisione.setVisible(true);
        } else {
            annullaCondivisione.setVisible(false);
        }

        updateShareSectionVisibility();

        cercaUtenteDaAggiungere.setVisible(false);
        confermaUtenteDaAggiungere.setVisible(false);
        confermaUtenteDaAggiungere.setEnabled(true);

        tagLabel.setVisible(false); 
        tagBox.setVisible(false);
        newTagBox.setVisible(false);
        addTagButton.setVisible(false);
        eliminaTagButton.setVisible(false);
    }

    public void setVisualizzaNotaViewListener(VisualizzaNotaViewListener listener) {
        this.listener = listener;
    }

    @UiHandler("aggiungiUtenteButton")
    void onAggiungiUtenteClick(ClickEvent e) {
        cercaUtenteDaAggiungere.setVisible(true);
        confermaUtenteDaAggiungere.setVisible(true);
        cercaUtenteDaAggiungere.setText("");
        cercaUtenteDaAggiungere.setFocus(true);
    }

    @UiHandler("confermaUtenteDaAggiungere")
    void onConfermaUtenteDaAggiungereClick(ClickEvent e) {
        String username = (cercaUtenteDaAggiungere.getText() != null) ?
                          cercaUtenteDaAggiungere.getText().trim() : "";
        if (username.isEmpty()) {
            Window.alert("Inserisci uno username.");
            return;
        }
        aggiungiUtenteListaCondivisa(username);
    }

    private void aggiungiUtenteListaCondivisa(final String usernameDaAggiungere) {
        if (listener == null || nota == null) return;

        confermaUtenteDaAggiungere.setEnabled(false);

        listener.trovaUtente2(nota, usernameDaAggiungere, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean exist) {
                if (!Boolean.TRUE.equals(exist)) {
                    Window.alert("Utente non trovato");
                    confermaUtenteDaAggiungere.setEnabled(true);
                    return;
                }

                listener.aggiungiCondivisione(nota.getId(), usernameDaAggiungere, new AsyncCallback<Note>() {
                    @Override
                    public void onSuccess(Note fresh) {
                        nota = fresh;
                        updateShareSectionVisibility();
                        Window.alert("Utente " + usernameDaAggiungere + " aggiunto con successo!\nRICORDA DI SALVARE PRIMA DI TORNARE ALLA HOME");
                        cercaUtenteDaAggiungere.setText("");
                        cercaUtenteDaAggiungere.setVisible(false);
                        confermaUtenteDaAggiungere.setVisible(false);
                        confermaUtenteDaAggiungere.setEnabled(true);
                        salvaButton.setVisible(true);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Errore nel salvataggio della condivisione: " + caught.getMessage());
                        confermaUtenteDaAggiungere.setEnabled(true);
                    }
                });
            }

            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Errore durante la ricerca: " + caught.getMessage());
                confermaUtenteDaAggiungere.setEnabled(true);
            }
        });
    }


    @UiHandler("modificaButton")
    void onModificaClick(ClickEvent e) {

        if (!canEdit()) {
            Window.alert("Non hai i permessi per modificare questa nota.");
            return;
        }

        if (listener != null && nota != null) {
            listener.onRichiediLock(nota.getId());
            modificaButton.setEnabled(false);
        }
        restoreButton.setVisible(true);
        enableEditing(isOwner());
    }

   public void enableEditing(boolean isOwner) {
        applyEdit(isOwner);
        salvaButton.setEnabled(true);

        tagControlsPanel.setVisible(true);
        tagLabel.setVisible(true);
        tagBox.setVisible(true);
        updateTagDisplay();
        updateShareSectionVisibility();
        addTagButton.setVisible(true);      
        eliminaTagButton.setVisible(true);  
        addTagButton.setEnabled(true);
        eliminaTagButton.setEnabled(true);
        

        if (nota.getTags() != null && !nota.getTags().isEmpty()) {
            tagLabel.setText("Tag: ");
        } else {
            tagLabel.setText("Tag:");
        }
    }

    public void disableEditingWithMessage(String message) {
        applyReadOnly();
        if (message != null && !message.isEmpty()) {
            Window.alert(message);
        }
    }

    @UiHandler("salvaButton")
    void onSalvaClick(ClickEvent e) {
        String selectedValue = statoBox.getSelectedValue();
        if (selectedValue == null && statoBox.getSelectedIndex() >= 0) {
            selectedValue = statoBox.getItemText(statoBox.getSelectedIndex());
        }

        nota.setTitle(titoloBox.getText());
        nota.setContent(contenutoArea.getText());
        if (selectedValue != null) {
            nota.setStato(Note.Stato.valueOf(selectedValue));
        }

        // aggiornamento dei tag prima del salvataggio
        if (selectedTags != null && !selectedTags.isEmpty()) {
            nota.setTags(new ArrayList<>(selectedTags));
        }

        applyReadOnly();
        salvaButton.setEnabled(false);

        if (listener != null) {
            listener.onSalvaNota(nota);
            listener.getNotaById(nota.getId(), new AsyncCallback<Note>() {
                @Override
                public void onSuccess(Note fresh) {
                    nota = fresh;

                    String state = nota.getStato().name();
                    for (int i = 0; i < statoBox.getItemCount(); i++) {
                        if (state.equals(statoBox.getValue(i))) {
                            statoBox.setSelectedIndex(i);
                            break;
                        }
                    }

                    updateShareSectionVisibility();
                    Window.alert("Nota salvata con successo!\nTitolo: " + nota.getTitle()
                            + "\nStato: " + nota.getStato().name());
                    salvaButton.setEnabled(true);

                    if (historyBox.isVisible() && historyBox.getSelectedIndex() >= 0) {
                        int selectedIndex = historyBox.getSelectedIndex();
                        listener.onRestoreNote(nota.getId(), selectedIndex, new AsyncCallback<Note>() {
                            @Override
                            public void onSuccess(Note restored) {
                                setNota(restored);
                                historyBox.setVisible(false);
                                restoreButton.setVisible(false);
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert("Errore nel ripristino: " + caught.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    applyEdit(isOwner());
                    Window.alert("Errore nel recupero della nota aggiornata: " + caught.getMessage());
                    salvaButton.setEnabled(true);
                }
            });
        }
    }

    @UiHandler("eliminaButton")
    void onEliminaClick(ClickEvent e) {
        boolean confirm = Window.confirm("Elimina definitivamente?");
        if (confirm && listener != null) {
            listener.onEliminaNota(nota);
        }
    }

    @UiHandler("backLink")
    void onBackLinkClick(ClickEvent e) {
        if (listener != null) {
            listener.onBack();
        }
    }

    @UiHandler("creaUnaCopia")
    void onCreaUnaCopiaClick(ClickEvent e) {
        if (listener != null) {
            listener.onCreaUnaCopia(nota);
        }
    }

    @UiHandler("annullaCondivisione")
    void onAnnullaCondivisioneClick(ClickEvent e) {
        boolean confirm = Window.confirm("Annulla la condivisione di questa nota?");
        if (confirm && listener != null) {
            listener.onAnnullaCondivisione(nota);
        }
    }

    private void onRemoveUserClick(Note n, String username) {
        boolean confirm = Window.confirm("Rimuovere l'utente " + username + " dalla condivisione?");
        if (!confirm || listener == null) return;

        listener.onRimuoviUtenteCondivisione(n, username, new AsyncCallback<Note>() {
            @Override public void onSuccess(Note fresh) {
                nota = fresh;
                updateShareSectionVisibility();
            }

            @Override public void onFailure(Throwable caught) {
                Window.alert("Errore nella rimozione: " + caught.getMessage());
            }
        });
    }

    private void renderCondivisioni() {
        sharedUsersPanel.clear();
        if (nota.getUtentiCondivisi() != null && !nota.getUtentiCondivisi().isEmpty()) {
            for (String u : nota.getUtentiCondivisi()) {
                HorizontalPanel row = new HorizontalPanel();
                row.setSpacing(5);
                Label userLabel = new Label(u);
                Button removeButton = new Button("X");
                removeButton.addClickHandler(e -> onRemoveUserClick(nota, u));
                row.add(userLabel);
                row.add(removeButton);
                sharedUsersPanel.add(row);
            }
        } else {
            sharedUsersPanel.add(new HTML("Nessun utente con cui è condivisa questa nota."));
        }
    }

    private void updateTagDisplay() {
    tagBox.clear();
    tagBox.addItem("Seleziona tag", "");
    tagBox.addItem("Crea nuovo", "new");

    String[] defaultTags = {"Università", "Tempo libero", "Importante", "Promemoria", "Cose da fare"};
    for (String tag : defaultTags) {
        tagBox.addItem(tag, tag);
    }

    if (nota != null && nota.getTags() != null && !nota.getTags().isEmpty()) {
        for (String tag : nota.getTags()) {
            boolean exists = false;
            for (int i = 0; i < tagBox.getItemCount(); i++) {
                if (tagBox.getValue(i).equals(tag)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                tagBox.addItem(tag, tag);
            }
        }

        String allTags = String.join(", ", nota.getTags());
        topRightTagLabel.setText("Tag: " + allTags);
    } else {
        topRightTagLabel.setText("Tag:");
    }
}

   

    @UiHandler("tagBox")
    void onTagBoxChange(ChangeEvent event) {
    String selectedValue = tagBox.getSelectedValue();

    if ("new".equals(selectedValue)) {
        newTagBox.setVisible(true);
        newTagBox.setFocus(true);
        isCreatingNewTag = true;
    } else if (!selectedValue.isEmpty()) {
        newTagBox.setVisible(false);
        isCreatingNewTag = false;
        selectedTag = selectedValue;
    }
}

    @UiHandler("addTagButton")
    void onAddTagClick(ClickEvent e) {
        if (isCreatingNewTag) {
        String newTag = newTagBox.getText().trim();
        if (!newTag.isEmpty()) {
            selectedTag = newTag;
            if (!selectedTags.contains(selectedTag)) {
                selectedTags.add(selectedTag);
            }
            nota.setTags(new ArrayList<>(selectedTags));
            addTagToNote(selectedTag);

            // aggiungo il nuovo tag 
            boolean exists = false;
            for (int i = 0; i < tagBox.getItemCount(); i++) {
                if (tagBox.getValue(i).equals(newTag)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                int lastIndex = tagBox.getItemCount() - 1;
                tagBox.insertItem(newTag, newTag, lastIndex);
            }

            newTagBox.setVisible(false);
            newTagBox.setText("");
            isCreatingNewTag = false;
            tagBox.setSelectedIndex(0);

            topRightTagLabel.setText("Tag: " + String.join(", ", selectedTags));
        }
            } else {
                String selectedValue = tagBox.getSelectedValue();
                if (!selectedValue.isEmpty() && !"new".equals(selectedValue)) {
                    selectedTag = selectedValue;
                    if (!selectedTags.contains(selectedTag)) {
                        selectedTags.add(selectedTag);
                    }
                nota.setTags(new ArrayList<>(selectedTags));
                addTagToNote(selectedTag);
                tagBox.setSelectedIndex(0);

            topRightTagLabel.setText("Tag: " + String.join(", ", selectedTags));
        }
    }
}

    @UiHandler("eliminaTagButton")
    void onEliminaTagClick(ClickEvent e) {
    String selectedValue = tagBox.getSelectedValue();
    if (selectedValue != null && !selectedValue.isEmpty() && !"new".equals(selectedValue)) {
        String tagToRemove = selectedValue;

        
        selectedTags.remove(tagToRemove);
        nota.setTags(new ArrayList<>(selectedTags));

        
        listener.onRemoveTagFromNote(nota.getId(), tagToRemove, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                topRightTagLabel.setText("Tag: " + String.join(", ", selectedTags));

                Window.alert("Tag rimosso con successo!");
            }

            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Errore nella rimozione del tag: " + caught.getMessage());
            }
        });
    } else {
        Window.alert("Seleziona un tag valido da rimuovere.");
    }
}



    private void addTagToNote(String tag) {
        listener.onAddTagToNote(nota.getId(), tag, new AsyncCallback<Void>() {
        @Override
        public void onSuccess(Void result) {
            if (nota.getTags() == null) {
                nota.setTags(new ArrayList<>());
            }
            if (!nota.getTags().contains(tag)) {
                nota.addTag(tag);
            }
            updateTagDisplay();
            Window.alert("Tag '" + tag + "' aggiunto con successo!");
        }

        @Override
        public void onFailure(Throwable caught) {
            Window.alert("Errore nell'aggiunta del tag: " + caught.getMessage());
        }
    });
}

    private void addTagToNoteFinal(String tag) {
        listener.onAddTagToNote(nota.getId(), tag, new AsyncCallback<Void>() {
        @Override
        public void onSuccess(Void result) {
            if (nota.getTags() == null) {
                nota.setTags(new ArrayList<>());
            }
            nota.addTag(tag);
            updateTagDisplay();
            Window.alert("Tag '" + tag + "' aggiunto con successo!");
        }

        @Override
        public void onFailure(Throwable caught) {
            Window.alert("Errore nell'aggiunta del tag: " + caught.getMessage());
            }
        });
    }
}
