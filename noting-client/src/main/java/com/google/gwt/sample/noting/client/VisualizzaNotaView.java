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
    private User user; // per tenere traccia dello stato della nota

    public VisualizzaNotaView(Note nota, User user) {
    initWidget(uiBinder.createAndBindUi(this));
    this.nota = nota;
    this.user = user;

    // Imposta i valori iniziali dei campi
    setUpInitialUi();

   

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
        shareSection.setVisible(visible);
        if (visible) renderCondivisioni(); else sharedUsersPanel.clear();
    }

    private void applyReadOnly(){
        titoloBox.setReadOnly(true);
        contenutoArea.setReadOnly(true);
        statoBox.setEnabled(false);
        salvaButton.setVisible(false);
        salvaButton.setEnabled(true);
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
    
    // modalità iniziale : solo lettura
    applyReadOnly();


    statoBox.clear();
    for (Note.Stato s : Note.Stato.values()) {
        statoBox.addItem(s.name(),s.name());
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

        // Permessi
    modificaButton.setEnabled(canEdit());

    // Annulla condivisione: visibile solo se NON sei owner
    annullaCondivisione.setVisible(!isOwner());

    // Sezione condivisione: visibile solo se owner + stato condiviso
    updateShareSectionVisibility();

    // UI “aggiungi utente” nascosta di default
    cercaUtenteDaAggiungere.setVisible(false);
    confermaUtenteDaAggiungere.setVisible(false);
    confermaUtenteDaAggiungere.setEnabled(true);
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
     String username = (cercaUtenteDaAggiungere.getText() != null)
            ? cercaUtenteDaAggiungere.getText().trim() : "";   
    if(username.isEmpty()){
         Window.alert("Inserisci uno username.");
        return;
    }
    aggiungiUtenteListaCondivisa(username);
    }



    private void aggiungiUtenteListaCondivisa(final String usernameDaAggiungere) {
    if (listener == null || nota == null) return;

   confermaUtenteDaAggiungere.setEnabled(false);

    listener.trovaUtente2(nota, usernameDaAggiungere, new AsyncCallback<Boolean>() {
        @Override public void onSuccess(Boolean exist) {
            if (!Boolean.TRUE.equals(exist)) {
                Window.alert("Utente non trovato");
                confermaUtenteDaAggiungere.setEnabled(true);
                return;
            }

            listener.aggiungiCondivisione(nota.getId(), usernameDaAggiungere, new AsyncCallback<Note>() {
                @Override public void onSuccess(Note fresh) {
                    nota = fresh;
                    updateShareSectionVisibility();
                    Window.alert("Utente " + usernameDaAggiungere + " aggiunto con successo!\nRICORDA DI SALVARE PRIMA DI TORNARE ALLA HOME");
                    cercaUtenteDaAggiungere.setText("");
                    cercaUtenteDaAggiungere.setVisible(false);
                    confermaUtenteDaAggiungere.setVisible(false);
                    confermaUtenteDaAggiungere.setEnabled(true);
                    salvaButton.setVisible(true); // serve salvare
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
    if (!canEdit()) {
        Window.alert("Non hai i permessi per modificare questa nota.");
        return;
    }
    if (listener != null && nota != null) {
        modificaButton.setEnabled(false);
        listener.onRichiediLock(nota.getId());
     }
    }

    public void enableEditing(boolean isOwner) {
        applyEdit(isOwner);
    }

    // Chiamato da NotingApp quando il lock è NEGATO o perso
    public void disableEditingWithMessage(String message) {
        applyReadOnly();
        if (message != null && !message.isEmpty()) {
            Window.alert(message);
        }
    }
        

@UiHandler("salvaButton")
void onSalvaClick(ClickEvent e) {
    // UI → model
    String selectedValue = statoBox.getSelectedValue();
    if (selectedValue == null && statoBox.getSelectedIndex() >= 0) {
        selectedValue = statoBox.getItemText(statoBox.getSelectedIndex());
    }

    nota.setTitle(titoloBox.getText());
    nota.setContent(contenutoArea.getText());
    if (selectedValue != null) {
        nota.setStato(Note.Stato.valueOf(selectedValue));
    }

    applyReadOnly();
    salvaButton.setEnabled(false);

    if (listener != null) {
        listener.onSalvaNota(nota);
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

                updateShareSectionVisibility();
                Window.alert("Nota salvata con successo!\nTitolo: " + nota.getTitle()
                             + "\nStato: " + nota.getStato().name());
                salvaButton.setEnabled(true);
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
