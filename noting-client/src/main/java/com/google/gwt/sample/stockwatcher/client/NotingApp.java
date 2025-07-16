
// Import necessari per il funzionamento di base di GWT
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

// Import per la comunicazione asincrona con il server (RPC)
import com.google.gwt.user.client.rpc.AsyncCallback;

// Import per i componenti dell'interfaccia utente (UI)
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.Window;

// Import delle tue classi personalizzate dal modulo "shared"
import com.google.gwt.sample.stockwatcher.shared.NoteService;
import com.google.gwt.sample.stockwatcher.shared.NoteServiceAsync;
import com.google.gwt.sample.stockwatcher.shared.User;

// Import della tua classe per la vista di login
import com.google.gwt.sample.stockwatcher.client.LoginView;

// CLASSE DI IMPLEMENTAZIONE INTERFACCIA UTENTE

public class StockWatcher implements EntryPoint {

    private final NoteServiceAsync noteService = GWT.create(NoteService.class);

    public void onModuleLoad() {
        // 1. Crea un'istanza della tua nuova vista
        LoginView loginView = new LoginView();

        // 2. Aggiungi i gestori degli eventi (la logica rimane qui)
        loginView.getLoginButton().addClickHandler(event -> {
            noteService.login(loginView.getUsername(), loginView.getPassword(), new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                    Window.alert("Errore di login: " + caught.getMessage());
                }
                public void onSuccess(User result) {
                    Window.alert("Login effettuato con successo! Benvenuto " + result.getUsername());
                    // Qui caricherai l'interfaccia principale dell'app
                }
            });
        });

        loginView.getRegisterButton().addClickHandler(event -> {
            noteService.register(loginView.getUsername(), loginView.getPassword(), new AsyncCallback<User>() {
                public void onFailure(Throwable caught) {
                    Window.alert("Errore di registrazione: " + caught.getMessage());
                }
                public void onSuccess(User result) {
                    Window.alert("Registrazione completata! Ora puoi effettuare il login.");
                }
            });
        });

        // 3. Aggiungi la vista alla pagina
        RootPanel.get("stockList").add(loginView);
    }
}