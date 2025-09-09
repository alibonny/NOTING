package com.google.gwt.sample.noting.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.noting.shared.Note;
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
    @UiField SuggestBox cercaUtenteDaAggiungere;
    @UiField Button confermaUtenteDaAggiungere;

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
        renderCondivisioni();
     
        }else {
        shareSection.setVisible(false);
        sharedUsersPanel.clear();
        }

         cercaUtenteDaAggiungere.setVisible(false);
        confermaUtenteDaAggiungere.setVisible(false);

    }

    public void setVisualizzaNotaViewListener(VisualizzaNotaViewListener listener) {
        this.listener = listener;
    }

    @UiHandler("aggiungiUtenteButton")  
    void onAggiungiUtenteClick(ClickEvent e){

    cercaUtenteDaAggiungere.setVisible(true);
    confermaUtenteDaAggiungere.setVisible(true);
    cercaUtenteDaAggiungere.setText("");
    cercaUtenteDaAggiungere.setFocus(true);

    }

    @UiHandler("confermaUtenteDaAggiungere")
void onConfermaUtenteDaAggiungereClick(ClickEvent e) {
    String username = cercaUtenteDaAggiungere.getText().trim();
    if (username.isEmpty()) {
        Window.alert("Inserisci uno username.");
        return;
    }
    aggiuntiUtenteListaCondivisa(username);
}


    private void aggiuntiUtenteListaCondivisa(final String usernameDaAggiungere) {
    if (listener == null) return;

   confermaUtenteDaAggiungere.setEnabled(false);

    listener.trovaUtente2(nota, usernameDaAggiungere, new AsyncCallback<Boolean>() {
        @Override public void onSuccess(Boolean exist) {
            if (!Boolean.TRUE.equals(exist)) {
                Window.alert("Utente non trovato");
                confermaUtenteDaAggiungere.setEnabled(false);
                return;
            }
            // chiediamo al server di aggiungere davvero userNorm tra i condivisi della nota notaId
            listener.aggiungiCondivisione(nota.getId(), usernameDaAggiungere, new AsyncCallback<Note>() {
                @Override public void onSuccess(Note fresh) {
                    nota = fresh;
                    shareSection.setVisible(true);
                    renderCondivisioni();
                    Window.alert("Utente " + usernameDaAggiungere + " aggiunto con successo!");
                    cercaUtenteDaAggiungere.setText("");
                    cercaUtenteDaAggiungere.setVisible(false);
                    confermaUtenteDaAggiungere.setVisible(false);
                    confermaUtenteDaAggiungere.setEnabled(true);
                    salvaButton.setVisible(true);
                }
                @Override public void onFailure(Throwable caught) {
                    Window.alert("Errore nel salvataggio della condivisione: " + caught.getMessage());
                    confermaUtenteDaAggiungere.setEnabled(true);
                }
            });
        }
        @Override public void onFailure(Throwable caught) {
            Window.alert("Errore durante la ricerca: " + caught.getMessage());
            confermaUtenteDaAggiungere.setEnabled(true);
        }
    });
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
    if (listener != null && nota != null) {
        // opzionale: disabilita il bottone durante la richiesta
        modificaButton.setEnabled(false);
        listener.onRichiediLock(nota.getId());
    }
    }

        // Chiamato da NotingApp quando il lock è stato CONFERMATO dal server
    public void enableEditing(boolean isOwner) {
        titoloBox.setReadOnly(false);
        contenutoArea.setReadOnly(false);
        if (isOwner) {
            statoBox.setEnabled(true);
        }
        salvaButton.setVisible(true);
        modificaButton.setEnabled(true); // riabilita il bottone
    }

    // Chiamato da NotingApp quando il lock è NEGATO o perso
    public void disableEditingWithMessage(String message) {
        // ripristina stato "solo lettura"
        titoloBox.setReadOnly(true);
        contenutoArea.setReadOnly(true);
        statoBox.setEnabled(false);
        salvaButton.setVisible(false);
        modificaButton.setEnabled(true); // riabilita per eventuale retry
        if (message != null && !message.isEmpty()) {
            Window.alert(message);
        }
    }
        
    

@UiHandler("salvaButton")
void onSalvaClick(ClickEvent e) {
    // UI → model
    String selectedValue = statoBox.getSelectedValue();
    if (selectedValue == null) selectedValue = statoBox.getItemText(statoBox.getSelectedIndex());

    nota.setTitle(titoloBox.getText());
    nota.setContent(contenutoArea.getText());
    if (selectedValue != null) {
        nota.setStato(Note.Stato.valueOf(selectedValue));
    }

    // Chiudi edit lato UI
    titoloBox.setReadOnly(true);
    contenutoArea.setReadOnly(true);
    statoBox.setEnabled(false);
    salvaButton.setVisible(false);
    salvaButton.setEnabled(false); // evita doppi click

    if (listener != null) {
        // invia salvataggio
        listener.onSalvaNota(nota);

        // ricarica la nota fresca dal server e allinea la UI
        listener.getNotaById(nota.getId(), new AsyncCallback<Note>() {
            @Override
            public void onSuccess(Note fresh) {
                nota = fresh;

                // riallinea combo stato
                String state = nota.getStato().name();
                for (int i = 0; i < statoBox.getItemCount(); i++) {
                    if (state.equals(statoBox.getValue(i))) {
                        statoBox.setSelectedIndex(i);
                        break;
                    }
                }

                // mostra/nascondi sezione condivisione e ridisegna
                boolean isOwner  = user != null && nota.getOwnerUsername().equals(user.getUsername());
                boolean isShared = (nota.getStato() == Note.Stato.Condivisa || nota.getStato() == Note.Stato.CondivisaSCR);
                shareSection.setVisible(isOwner && isShared);
                if (shareSection.isVisible()) {
                    renderCondivisioni();
                } else {
                    sharedUsersPanel.clear();
                }

                // messaggio finale
                Window.alert("Nota salvata con successo!\nTitolo: " + nota.getTitle()
                             + "\nStato: " + nota.getStato().name());
                salvaButton.setEnabled(true);
            }

            @Override
            public void onFailure(Throwable caught) {
                // in caso di errore, riapri l’editing per non perdere il lavoro
                titoloBox.setReadOnly(false);
                contenutoArea.setReadOnly(false);
                statoBox.setEnabled(true);
                salvaButton.setVisible(true);
                salvaButton.setEnabled(true);
                Window.alert("Errore nel recupero della nota aggiornata: " + caught.getMessage());
            }
        });
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


   private void onRemoveUserClick(Note n, String username) {
    boolean confirm = Window.confirm("Rimuovere l'utente " + username + " dalla condivisione?");
    if (!confirm || listener == null) return;

    // (opzionale) disabilita la UI del pannello per evitare doppi click
    // es.: removeButton.setEnabled(false); se tieni un riferimento

    listener.onRimuoviUtenteCondivisione(n, username, new AsyncCallback<Note>() {
        @Override public void onSuccess(Note fresh) {
            nota = fresh;
            // Se non è più condivisa o non sei owner, nascondi la sezione
            boolean isOwner  = nota.getOwnerUsername().equals(user.getUsername());
            boolean isShared = (nota.getStato() == Note.Stato.Condivisa || nota.getStato() == Note.Stato.CondivisaSCR);
            shareSection.setVisible(isOwner && isShared);

            if (shareSection.isVisible()) {
                renderCondivisioni();              // <<-- ridisegna la lista
            } else {
                sharedUsersPanel.clear();
            }
        }
        @Override public void onFailure(Throwable caught) {
            Window.alert("Errore nella rimozione: " + caught.getMessage());
            // (opzionale) riabilita pulsante X se l’avevi disabilitato
        }
    });
}


    // rigenera elenco visuale degli utenti condivisi della nota corrente
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


   


}
