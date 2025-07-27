package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.noting.shared.Note;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window; 
import com.google.gwt.user.client.ui.*;

public class VisualizzaNotaView extends Composite {

    interface VisualizzaNotaViewUiBinder extends UiBinder<Widget, VisualizzaNotaView> {}
    private static VisualizzaNotaViewUiBinder uiBinder = GWT.create(VisualizzaNotaViewUiBinder.class);

    @UiField TextBox titoloBox;
    @UiField TextArea contenutoArea;
    @UiField Button salvaButton;
    @UiField Button modificaButton;
    @UiField Button eliminaButton;
    @UiField Button backButton;

    private VisualizzaNotaViewListener listener;
    private Note nota; //così salviamo la nota corrente

    public VisualizzaNotaView(Note nota) {
        initWidget(uiBinder.createAndBindUi(this));
        this.nota = nota;
        titoloBox.setText(nota.getTitle());
        contenutoArea.setText(nota.getContent());
        contenutoArea.setReadOnly(true);

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
    }

    @UiHandler("salvaButton")
    void onSalvaClick(ClickEvent e) {
        contenutoArea.setReadOnly(true);
        salvaButton.setVisible(false);

        // aggiorna il contenuto nella nota
        nota.setContent(contenutoArea.getText());

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

    @UiHandler("backButton")
    void onBackClick(ClickEvent e) {
        if (listener != null) {
            listener.onBack();
        }
    }
}
