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

public class VisualizzaNotaView extends Composite {

    interface VisualizzaNotaViewUiBinder extends UiBinder<Widget, VisualizzaNotaView> {}
    private static VisualizzaNotaViewUiBinder uiBinder = GWT.create(VisualizzaNotaViewUiBinder.class);

    @UiField TextBox titoloBox;
    @UiField TextArea contenutoArea;
    @UiField Button salvaButton;
    @UiField Button modificaButton;
    @UiField Button eliminaButton;
    @UiField Button creaUnaCopia;
    @UiField HTML backLink;
    @UiField ListBox statoBox; // per mostrare lo stato della nota

    private VisualizzaNotaViewListener listener;
    private Note nota; //così salviamo la nota corrente
    protected Note.Stato stato; // per tenere traccia dello stato della nota

    public VisualizzaNotaView(Note nota) {
        initWidget(uiBinder.createAndBindUi(this));
        this.nota = nota;
        titoloBox.setText(nota.getTitle());
        contenutoArea.setText(nota.getContent());
        contenutoArea.setReadOnly(true);

        for (Note.Stato stato : Note.Stato.values()) {
            statoBox.addItem(stato.name());
        }

        statoBox.setSelectedIndex(nota.getStato().ordinal()); // imposta lo stato corrente della nota

        statoBox.setEnabled(false); // disabilita la modifica dello stato

        // il bottone salva è inizialmente disattivato, viene attivato quando si effettuano modifiche
        salvaButton.setVisible(false);
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
}
