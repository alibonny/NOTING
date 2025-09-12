package com.google.gwt.sample.noting.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
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
import com.google.gwt.i18n.client.DateTimeFormat;

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
    @UiField ListBox statoBox; 
    @UiField HTMLPanel shareSection;
    @UiField FlowPanel sharedUsersPanel;
    @UiField Button aggiungiUtenteButton;

    @UiField Button historyButton;
    @UiField ListBox historyBox;
    
    private VisualizzaNotaViewListener listener;
    private Note nota; //salvataggio della nota corrente
    protected Note.Stato stato;
    private User user; // per tenere traccia dello stato della nota

    public VisualizzaNotaView(Note nota, User user) {
        initWidget(uiBinder.createAndBindUi(this));
        this.nota = nota;
        this.user = user;

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

    boolean enableModifica = owner.equals(utente) || (nota.getStato() == Note.Stato.CondivisaSCR);
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

    @UiHandler("historyButton")
    void onHistoryButtonClick(ClickEvent e) {
        historyButton.setEnabled(false); // Disabilita per evitare doppi click
        listener.onGetNoteHistory(nota.getId(), new AsyncCallback<List<NoteMemento>>() {
            @Override
            public void onSuccess(List<NoteMemento> historyList) {
                historyBox.clear();
                if (historyList == null || historyList.isEmpty()) {
                    Window.alert("Nessuna cronologia disponibile per questa nota.");
                    historyButton.setEnabled(true);
                    return;
                }
                
                // Popola il menu a tendina
                historyBox.addItem("Seleziona una versione...");
                DateTimeFormat dtf = DateTimeFormat.getFormat("dd/MM/yyyy HH:mm:ss");
                for (int i = 0; i < historyList.size(); i++) {
                    NoteMemento memento = historyList.get(i);
                    historyBox.addItem(dtf.format(memento.getTimestamp()), String.valueOf(i));
                }
                
                historyBox.setVisible(true); // Mostra il menu
                historyButton.setEnabled(true);
            }

            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Errore nel caricamento della cronologia: " + caught.getMessage());
                historyButton.setEnabled(true);
            }
        });
    }

    @UiHandler("historyBox")
    void onHistorySelection(ChangeEvent e) {
        if (historyBox.getSelectedIndex() <= 0) { // Ignora la prima opzione "Seleziona..."
            return;
        }

        int historyIndex = Integer.parseInt(historyBox.getValue(historyBox.getSelectedIndex()));
        boolean confirm = Window.confirm("Vuoi ripristinare la nota a questa versione? Le modifiche non salvate andranno perse.");
        if (confirm) {
            listener.onRestoreNote(nota.getId(), historyIndex, new AsyncCallback<Note>() {
                @Override
                public void onSuccess(Note restoredNote) {
                    Window.alert("Nota ripristinata con successo!");
                    // Aggiorna la vista con i dati della nota ripristinata
                    nota = restoredNote;
                    titoloBox.setText(restoredNote.getTitle());
                    contenutoArea.setText(restoredNote.getContent());
                    historyBox.setVisible(false); // Nascondi di nuovo il menu
                }

                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Errore durante il ripristino: " + caught.getMessage());
                }
            });
        }
        historyBox.setSelectedIndex(0); // Resetta la selezione
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
