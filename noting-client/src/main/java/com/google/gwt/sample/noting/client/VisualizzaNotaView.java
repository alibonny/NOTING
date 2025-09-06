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

    // Imposta i valori iniziali dei campi
    setUpIinitialUi();

   

    }

    private void setUpIinitialUi() {
    String titolo = (nota != null && nota.getTitle() != null) ? nota.getTitle() : "";
    String contenuto = (nota != null && nota.getContent() != null) ? nota.getContent() : "";
    titoloBox.setText(titolo);
    contenutoArea.setText(contenuto);
    titoloBox.setReadOnly(true);
    contenutoArea.setReadOnly(true);

    salvaButton.setVisible(false); // nasconde il pulsante "Salva" inizialmente

    statoBox.clear();
    for (Note.Stato s : Note.Stato.values()) {
        statoBox.addItem(s.name());
    }
    Note.Stato stato = (nota != null) ? nota.getStato() : null;
    if (stato != null) {
        // selezione robusta per value
        for (int i = 0; i < statoBox.getItemCount(); i++) {
            if (stato.name().equals(statoBox.getValue(i))) {
                statoBox.setSelectedIndex(i);
                break;
            }
        }
    } else if (statoBox.getItemCount() > 0) {
        statoBox.setSelectedIndex(0);
    }
    statoBox.setEnabled(false); // disabilita la modifica inizialmente


    // Gestione della sezione di condivisione e modifica
    String owner = nota.getOwnerUsername();
    String  utente = (user != null) ? user.getUsername() : null;

    boolean enableModifica =
        owner.equals(utente) || (nota.getStato() == Note.Stato.CondivisaSCR);
    modificaButton.setEnabled(enableModifica);

    
    if (owner != null && owner.equals(utente)) {
    // stesso utente → nascondi il pulsante
    annullaCondivisione.setVisible(false);
    GWT.log("Pulsante nascosto (owner == user)");
    } else {
    // utenti diversi → mostra il pulsante
    annullaCondivisione.setVisible(true);
    GWT.log("Pulsante visibile (owner != user)");
    }

    
    if((nota.getStato() == Note.Stato.Condivisa || nota.getStato() == Note.Stato.CondivisaSCR)
     && nota.getOwnerUsername().equals(utente)) {
        shareSection.setVisible(true);

        // Popola la lista degli utenti con cui la nota è condivisa
        sharedUsersPanel.clear();
        if (nota.getUtentiCondivisi() != null && !nota.getUtentiCondivisi().isEmpty()) {
            for (String utente1 : nota.getUtentiCondivisi()) {
                HorizontalPanel row = new HorizontalPanel();
                row.setSpacing(5);

                Label userLabel = new Label(utente1);
                Button removeButton = new Button("X");
                removeButton.addClickHandler(e -> onRemoveUserClick(nota,utente1));

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
       Note.Stato stato = (nota != null) ? nota.getStato() : null;
    String owner = (nota != null) ? nota.getOwnerUsername() : null;
    String utente = (user != null) ? user.getUsername() : null;
    boolean isOwner = owner != null && owner.equals(utente);

    // Non owner e non CondivisaSCR: il bottone dovrebbe essere disabilitato, ma mettiamo guardia
    if (!isOwner && stato != Note.Stato.CondivisaSCR) {
        Window.alert("Non hai i permessi per modificare questa nota.");
        return;
    }

    if (stato == Note.Stato.CondivisaSCR || isOwner) {
        // Pulsante Modifica è abilitato ma i campi restano NON editabili
        titoloBox.setReadOnly(false);
        contenutoArea.setReadOnly(false);
    }
        if(isOwner){
        statoBox.setEnabled(true);
        }

        salvaButton.setVisible(true);
       
    }

    @UiHandler("salvaButton")
    void onSalvaClick(ClickEvent e) {
    // Commit valori UI → model
    String selectedValue = statoBox.getSelectedValue(); // ora è valorizzato (addItem(label,value))
    if (selectedValue == null) selectedValue = statoBox.getItemText(statoBox.getSelectedIndex());

    nota.setTitle(titoloBox.getText());
    nota.setContent(contenutoArea.getText());
    if (selectedValue != null) {
        nota.setStato(Note.Stato.valueOf(selectedValue));
    }

    // Chiudi edit
    titoloBox.setReadOnly(true);
    contenutoArea.setReadOnly(true);
    statoBox.setEnabled(false);
    salvaButton.setVisible(false);

    Window.alert("Nota salvata con successo!\nTitolo: " + nota.getTitle() + "\nStato: " + nota.getStato().name());

    if (listener != null) {
        listener.onSalvaNota(nota);
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
