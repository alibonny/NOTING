package com.google.gwt.sample.noting.client;

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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class VisualizzaNotaView extends Composite {

    interface VisualizzaNotaViewUiBinder extends UiBinder<Widget, VisualizzaNotaView> {}
    private static VisualizzaNotaViewUiBinder uiBinder = GWT.create(VisualizzaNotaViewUiBinder.class);

    @UiField TextBox titoloBox;
    @UiField TextArea contenutoArea;
    @UiField Button salvaButton;
    @UiField Button modificaButton;
    @UiField Button eliminaButton;
    @UiField Button creaUnaCopia;
    @UiField Button annullaCondivisione;
    @UiField HTML backLink;
    @UiField ListBox statoBox; // per mostrare lo stato della nota
    @UiField HTMLPanel shareSection;
    @UiField FlowPanel sharedUsersPanel;
    @UiField Button aggiungiUtenteButton;

    private VisualizzaNotaViewListener listener;
    private Note nota; //così salviamo la nota corrente
    protected Note.Stato stato;
    private User user; // per tenere traccia dello stato della nota

    public VisualizzaNotaView(Note nota, User user) {
    initWidget(uiBinder.createAndBindUi(this));
    this.nota = nota;
    this.user = user;

    // titoli/contenuto con default sicuri
    titoloBox.setText(nota != null && nota.getTitle() != null ? nota.getTitle() : "");
    contenutoArea.setText(nota != null && nota.getContent() != null ? nota.getContent() : "");
    contenutoArea.setReadOnly(true);

    // popola la ListBox stati
    statoBox.clear();
    for (Note.Stato s : Note.Stato.values()) {
        // usa anche il "value", così dopo potrai fare getSelectedValue()
        statoBox.addItem(s.name(), s.name());
    }

    // selezione stato SENZA NPE
    if (nota != null && nota.getStato() != null) {
        statoBox.setSelectedIndex(nota.getStato().ordinal());
    } else if (statoBox.getItemCount() > 0) {
        statoBox.setSelectedIndex(0); // fallback
        // opzionale: nota.setStato(Note.Stato.valueOf(statoBox.getSelectedValue()));
    }
    statoBox.setEnabled(false);

   String owner = nota.getOwnerUsername();
   GWT.log("Owner della nota: " + owner);
    String uname = (user != null ? user.getUsername() : null);

    // Log in console GWT
    GWT.log("Owner: " + owner + " | Logged user: " + uname);

    if (owner != null && owner.equals(uname)) {
    // stesso utente → nascondi il pulsante
    annullaCondivisione.setVisible(false);
    GWT.log("Pulsante nascosto (owner == user)");
    } else {
    // utenti diversi → mostra il pulsante
    annullaCondivisione.setVisible(true);
    GWT.log("Pulsante visibile (owner != user)");
    }


    salvaButton.setVisible(false);

    if((nota.getStato() == Note.Stato.Condivisa || nota.getStato() == Note.Stato.CondivisaSCR)
     && nota.getOwnerUsername().equals(uname)) {
        shareSection.setVisible(true);

        // Popola la lista degli utenti con cui la nota è condivisa
        sharedUsersPanel.clear();
        if (nota.getUtentiCondivisi() != null && !nota.getUtentiCondivisi().isEmpty()) {
            for (String utente : nota.getUtentiCondivisi()) {
                HorizontalPanel row = new HorizontalPanel();
                row.setSpacing(5);

                Label userLabel = new Label(utente);
                Button removeButton = new Button("X");
                removeButton.addClickHandler(e -> onRemoveUserClick(nota,utente));

                row.add(userLabel);
                row.add(removeButton);

                sharedUsersPanel.add(row);
            }
        } else {
            sharedUsersPanel.add(new HTML("Nessun utente con cui è condivisa questa nota."));
            }
        }else {
        shareSection.setVisible(false);
        }

    }


    public void setVisualizzaNotaViewListener(VisualizzaNotaViewListener listener) {
        this.listener = listener;
    }

    @UiHandler("modificaButton")
    void onModificaClick(ClickEvent e) {
        contenutoArea.setReadOnly(false);
        salvaButton.setVisible(true); // mostra il pulsante "Salva"
        statoBox.setEnabled(true); // abilita la modifica dello stato

        if (listener != null) {
            listener.onStatoNotaChanged(nota, stato);; // notifica il listener che si sta modificando la nota
        }
    }

    @UiHandler("salvaButton")
    void onSalvaClick(ClickEvent e) {
        contenutoArea.setReadOnly(true);
        salvaButton.setVisible(false);

        // aggiorna il contenuto nella nota
        nota.setContent(contenutoArea.getText());

        String statoSelezionato = statoBox.getSelectedValue(); // o getSelectedItemText().replace(" ", "")
        nota.setStato(Note.Stato.valueOf(statoSelezionato));
        Window.alert("Nota salvata con successo!\nTitolo: " + titoloBox.getText() + "\nStato: " + statoSelezionato);


        if (listener != null) {
            listener.onSalvaNota(nota, nota.getStato()); // notifica il listener che la nota è stata salvata
        }
    }

    @UiHandler("eliminaButton")
    void onEliminaClick(ClickEvent e) {
        // mostra un dialog di conferma
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


    private void onRemoveUserClick(Note nota,String username) {
        boolean confirm = Window.confirm("Rimuovere l'utente " + username + " dalla condivisione?");
        if (confirm && listener != null) {
            listener.onRimuoviUtenteCondivisione(nota, username);
        }
    }

   


}
