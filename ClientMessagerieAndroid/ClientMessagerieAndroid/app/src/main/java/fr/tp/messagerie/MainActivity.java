package fr.tp.messagerie;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client Android entièrement en Java, sans fichier de layout XML.
 * REST pour inscription/login/utilisateurs/historique, WebSocket pour le temps réel.
 */
public final class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MessageSocket socket = new MessageSocket();
    private final List<Peer> peers = new ArrayList<>();

    private EditText serverInput;
    private EditText emailInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText messageInput;
    private EditText websocketPathInput;
    private Spinner usersSpinner;
    private TextView statusText;
    private TextView selectedUserText;
    private TextView messagesText;
    private ScrollView messageScroll;
    private ScrollView settingsScroll;
    private LinearLayout settingsPanel;
    private Button settingsToggle;

    private String token = "";
    private String activeBaseUrl = "";
    private String ownUsername = "";
    private String ownEmail = "";
    private String ownId = "";
    private int sessionEpoch = 0;
    private boolean fillingSpinner = false;
    private String selectedPeerId = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildInterface();
        loadSettings();
        setStatus("Non connecté. Configure l'URL du serveur, puis connecte-toi.");
    }

    private void buildInterface() {
        LinearLayout root = column();
        root.setPadding(dp(12), dp(10), dp(12), dp(10));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("Messagerie TP — Android");
        title.setTextSize(21);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        settingsToggle = button("⚙ Afficher / masquer la connexion", root, () -> {
            boolean show = settingsPanel.getVisibility() != View.VISIBLE;
            settingsPanel.setVisibility(show ? View.VISIBLE : View.GONE);
            settingsScroll.getLayoutParams().height = show ? dp(340) : 0;
            settingsScroll.requestLayout();
        });

        // Cette zone est masquée après le login pour réserver l'écran au chat.
        settingsScroll = new ScrollView(this);
        settingsPanel = column();
        settingsPanel.setPadding(dp(2), dp(4), dp(2), dp(6));
        settingsScroll.addView(settingsPanel);
        root.addView(settingsScroll, new LinearLayout.LayoutParams(-1, 0, 0.0f));
        // wrap_content mais limité en hauteur sur un téléphone : la zone défile.
        LinearLayout.LayoutParams settingsParams = (LinearLayout.LayoutParams) settingsScroll.getLayoutParams();
        settingsParams.height = dp(340);
        settingsScroll.setLayoutParams(settingsParams);

        serverInput = input("URL serveur : http://192.168.1.10:8080", settingsPanel, false);
        websocketPathInput = input("Chemin WebSocket (confirmé : /ws/messages)", settingsPanel, false);
        emailInput = input("Email (login et inscription)", settingsPanel, false);
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        usernameInput = input("Username (inscription uniquement)", settingsPanel, false);
        passwordInput = input("Mot de passe (champ JSON mdp)", settingsPanel, true);

        LinearLayout authRow = row(settingsPanel);
        button("S'inscrire", authRow, this::register);
        button("Se connecter", authRow, this::login);
        button("Déconnexion", settingsPanel, this::logout);

        statusText = new TextView(this);
        statusText.setTextSize(13);
        statusText.setPadding(0, dp(8), 0, dp(7));
        root.addView(statusText);

        LinearLayout peerRow = row(root);
        usersSpinner = new Spinner(this);
        peerRow.addView(usersSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));
        button("↻ Utilisateurs", peerRow, this::refreshUsers);

        usersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!fillingSpinner && position >= 0 && position < peers.size()) {
                    choosePeer(peers.get(position));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        selectedUserText = new TextView(this);
        selectedUserText.setText("Sélectionne un utilisateur dans la liste.");
        selectedUserText.setTextSize(13);
        selectedUserText.setPadding(0, dp(2), 0, dp(6));
        root.addView(selectedUserText);

        // L'ID du destinataire est récupéré automatiquement depuis GET /api/utilisateurs.
        button("Charger l'historique", root, this::loadHistory);

        messageScroll = new ScrollView(this);
        messagesText = new TextView(this);
        messagesText.setTextSize(16);
        messagesText.setPadding(dp(10), dp(10), dp(10), dp(10));
        messagesText.setText("Les messages apparaîtront ici.");
        messageScroll.addView(messagesText);
        root.addView(messageScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout compose = row(root);
        messageInput = input("Écris un message…", compose, false);
        messageInput.setSingleLine(false);
        messageInput.setMinLines(1);
        messageInput.setMaxLines(3);
        ((LinearLayout.LayoutParams) messageInput.getLayoutParams()).width = 0;
        ((LinearLayout.LayoutParams) messageInput.getLayoutParams()).weight = 1;
        button("Envoyer", compose, this::sendMessage);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row(LinearLayout parent) {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.HORIZONTAL);
        result.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(result, new LinearLayout.LayoutParams(-1, -2));
        return result;
    }

    private EditText input(String hint, LinearLayout parent, boolean secret) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setTextSize(14);
        edit.setSingleLine(true);
        if (secret) edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        parent.addView(edit, new LinearLayout.LayoutParams(-1, -2));
        return edit;
    }

    private Button button(String text, LinearLayout parent, Runnable listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        parent.addView(button);
        button.setOnClickListener(v -> listener.run());
        return button;
    }

    private int dp(int size) { return (int) (size * getResources().getDisplayMetrics().density); }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        serverInput.setText(settings.getString("server", AppConfig.DEFAULT_SERVER_URL));
        websocketPathInput.setText(settings.getString("wsPath", AppConfig.WEBSOCKET_PATH));
    }

    private void saveSettings() {
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putString("server", AppConfig.normalizeBaseUrl(serverInput.getText().toString()))
                .putString("wsPath", websocketPathInput.getText().toString().trim())
                .apply();
    }

    private String serverUrl() {
        String url = AppConfig.normalizeBaseUrl(serverInput.getText().toString());
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL attendue : http://IP_DU_PC:PORT");
        }
        return url;
    }

    private void register() {
        try {
            final String base = serverUrl();
            final String email = emailInput.getText().toString().trim();
            final String username = usernameInput.getText().toString().trim();
            final String password = passwordInput.getText().toString();
            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                notice("Inscription : email, username et mot de passe obligatoires.");
                return;
            }
            saveSettings();
            setStatus("Création du compte…");
            io.execute(() -> {
                try {
                    Object response = ApiClient.register(base, email, username, password);
                    String id = DataParser.ownIdFromLogin(response);
                    runOnUiThread(() -> {
                        setStatus("Compte créé" + (id.isEmpty() ? "" : ", ID = " + id)
                                + ". Clique sur Se connecter.");
                        notice("Compte créé : " + email);
                    });
                } catch (Exception e) { showError("Inscription", e); }
            });
        } catch (Exception e) { notice(e.getMessage()); }
    }

    private void login() {
        try {
            final String base = serverUrl();
            final String email = emailInput.getText().toString().trim();
            final String password = passwordInput.getText().toString();
            final String wsPath = websocketPathInput.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                notice("Connexion : email et mot de passe obligatoires.");
                return;
            }
            saveSettings();
            final int epoch = ++sessionEpoch;
            socket.close();
            token = "";
            setStatus("Connexion REST…");
            io.execute(() -> {
                try {
                    Object response = ApiClient.login(base, email, password);
                    String jwt = DataParser.token(response);
                    if (jwt.isEmpty()) throw new IllegalStateException(
                            "Token manquant dans AuthResponse : vérifie le JSON du login.");
                    String id = DataParser.ownIdFromLogin(response);
                    if (id.isEmpty()) id = DataParser.ownIdFromJwt(jwt);
                    final String discoveredId = id;
                    JSONObject auth = DataParser.asObject(response);
                    final String loggedUsername = DataParser.userName(auth);
                    final String loggedEmail = DataParser.userEmail(auth);
                    runOnUiThread(() -> {
                        if (epoch != sessionEpoch) return;
                        token = jwt;
                        ownUsername = loggedUsername;
                        ownEmail = loggedEmail.isEmpty() ? email : loggedEmail;
                        ownId = discoveredId;
                        activeBaseUrl = base;
                        passwordInput.setText("");
                        settingsPanel.setVisibility(View.GONE);
                        settingsScroll.getLayoutParams().height = 0;
                        settingsScroll.requestLayout();
                        setStatus("REST connecté : " + ownEmail + " (ID " + ownId + "). WebSocket…");
                        connectSocket(epoch, wsPath);
                        refreshUsers();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (epoch == sessionEpoch) setStatus("Erreur login : " + e.getMessage());
                    });
                }
            });
        } catch (Exception e) { notice(e.getMessage()); }
    }

    private void connectSocket(int epoch, String wsPath) {
        try {
            socket.connect(activeBaseUrl, wsPath, token, new MessageSocket.Listener() {
                @Override public void onConnected() {
                    runOnUiThread(() -> {
                        if (epoch == sessionEpoch) setStatus("REST + WebSocket connectés · " + ownEmail
                                + " · mon ID : " + ownId);
                    });
                }
                @Override public void onIncoming(JSONObject event) {
                    runOnUiThread(() -> { if (epoch == sessionEpoch) receiveMessage(event); });
                }
                @Override public void onDisconnected(String reason) {
                    runOnUiThread(() -> {
                        if (epoch == sessionEpoch) setStatus(reason + " (REST reste accessible)");
                    });
                }
            });
        } catch (Exception e) { setStatus("Erreur de WebSocket : " + e.getMessage()); }
    }

    private void logout() {
        ++sessionEpoch;
        socket.close();
        token = "";
        activeBaseUrl = "";
        ownUsername = "";
        ownEmail = "";
        ownId = "";
        selectedPeerId = "";
        selectedUserText.setText("Sélectionne un utilisateur dans la liste.");
        peers.clear();
        usersSpinner.setAdapter(null);
        messagesText.setText("");
        settingsScroll.getLayoutParams().height = dp(340);
        settingsScroll.requestLayout();
        settingsPanel.setVisibility(View.VISIBLE);
        setStatus("Déconnecté.");
    }

    private void refreshUsers() {
        if (!authenticated()) return;
        final String currentToken = token;
        final String base = activeBaseUrl;
        final int epoch = sessionEpoch;
        io.execute(() -> {
            try {
                // UserActivity(email, id, statut) : on lit l'ID dans la clé JSON "id".
                JSONArray users = DataParser.asArray(ApiClient.users(base, currentToken),
                        "utilisateurs", "users", "data", "results", "content");
                if (users == null) throw new IllegalStateException(
                        "Réponse /api/utilisateurs inattendue : tableau JSON attendu.");
                List<Peer> found = new ArrayList<>();
                for (int i = 0; i < users.length(); i++) {
                    JSONObject item = users.optJSONObject(i);
                    if (item == null) continue;
                    String id = DataParser.userId(item);
                    String email = DataParser.userEmail(item);
                    String username = DataParser.userName(item);
                    // On ne propose pas son propre compte comme destinataire.
                    if (!ownId.isEmpty() && ownId.equals(id)) continue;
                    if (!ownEmail.isEmpty() && ownEmail.equalsIgnoreCase(email)) continue;
                    String name = !email.isEmpty() ? email : !username.isEmpty()
                            ? username : "Utilisateur " + (i + 1);
                    if (id.isEmpty()) continue; // Un message nécessite un destination_id.
                    found.add(new Peer(id, name, DataParser.presence(item)));
                }
                runOnUiThread(() -> {
                    if (epoch != sessionEpoch) return;
                    String previousId = selectedPeerId;
                    peers.clear();
                    peers.addAll(found);
                    fillingSpinner = true;
                    ArrayAdapter<Peer> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, peers);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    usersSpinner.setAdapter(adapter);
                    int position = 0;
                    for (int i = 0; i < peers.size(); i++) {
                        if (peers.get(i).id.equals(previousId)) { position = i; break; }
                    }
                    if (!peers.isEmpty()) usersSpinner.setSelection(position);
                    fillingSpinner = false;
                    if (peers.isEmpty()) {
                        selectedPeerId = "";
                        selectedUserText.setText("Aucun autre utilisateur avec un ID valide.");
                        messagesText.setText("Aucun utilisateur disponible.");
                    } else {
                        choosePeer(peers.get(position));
                    }
                });
            } catch (Exception e) { showError("Liste des utilisateurs", e, epoch); }
        });
    }

    private void choosePeer(Peer peer) {
        if (peer == null) return;
        selectedUserText.setText("Discussion avec " + peer.name + " (#" + peer.id
                + ") — " + peer.presence);
        if (peer.id.equals(selectedPeerId)) return;
        selectedPeerId = peer.id;
        messagesText.setText("Aucun message dans cette conversation.");
        // L'historique se charge si la route existe ; le chat reste utilisable sinon.
        loadHistory();
    }

    private String recipientId() {
        Peer peer = selectedPeer();
        return peer == null ? "" : peer.id;
    }

    private void loadHistory() {

        if (!authenticated()) return;

        final String otherId = recipientId();

        if (otherId.isEmpty()) {
            notice("Sélectionne un utilisateur dans la liste.");
            return;
        }

        Peer peer = selectedPeer();

        final String peerName = peer == null
                ? "Utilisateur #" + otherId
                : peer.name;

        final int epoch = sessionEpoch;
        final String currentToken = token;
        final String base = activeBaseUrl;
        final String currentOwnId = ownId;

        io.execute(() -> {

            try {

                // 1. Appel API GET /api/messages/{utilisateurId}
                Object response = ApiClient.history(
                        base,
                        currentToken,
                        otherId
                );

                // 2. Conversion de la réponse en JSONArray
                JSONArray messages;

                if (response instanceof JSONArray) {
                    messages = (JSONArray) response;

                } else if (response instanceof String) {
                    messages = new JSONArray((String) response);

                } else {
                    throw new IllegalStateException(
                            "Le serveur doit retourner un tableau JSON."
                    );
                }

                StringBuilder history = new StringBuilder();

                // 3. Parcourir les messages
                for (int i = 0; i < messages.length(); i++) {

                    JSONObject item = messages.optJSONObject(i);

                    if (item == null) continue;

                    // 4. Récupérer les données envoyées par Spring Boot
                    String body = item.optString("contenu", "");

                    String sender = item.optString(
                            "expediteur_id", ""
                    );

                    String senderName = item.optString(
                            "expediteur_username", ""
                    );

                    String date = item.optString("date", "");

                    if (body.isEmpty()) continue;

                    // 5. Vérifier si le message vient de moi
                    boolean isMine = currentOwnId.equals(sender)
                            && !currentOwnId.isEmpty();

                    String displayName = isMine
                            ? "Moi"
                            : !senderName.isEmpty()
                            ? senderName
                            : peerName;

                    // 6. Construire l'historique
                    history.append(displayName);

                    if (!date.isEmpty()) {
                        history.append(" · ").append(date);
                    }

                    history.append("\n")
                            .append(body)
                            .append("\n\n");
                }

                // 7. Afficher les messages sur Android
                runOnUiThread(() -> {

                    if (epoch != sessionEpoch
                            || !recipientId().equals(otherId)) {
                        return;
                    }

                    messagesText.setText(
                            history.length() == 0
                                    ? "Aucun message dans cette conversation."
                                    : history.toString()
                    );

                    scrollToBottom();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (epoch != sessionEpoch
                            || !recipientId().equals(otherId)) {
                        return;
                    }

                    setStatus(
                            "Erreur chargement historique : " + e.getMessage()
                    );
                });
            }
        });
    }

    private void sendMessage() {
        if (!authenticated()) return;
        final String targetId = recipientId();
        if (targetId.isEmpty()) { notice("Sélectionne un destinataire dans la liste."); return; }
        if (!socket.isConnected()) {
            notice("WebSocket non connecté. Vérifie son chemin et les logs Spring Boot.");
            return;
        }
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;
        try {
            JSONObject outgoing = new JSONObject();
            // MessageHandler déduit l'expéditeur et le username de la session JWT.
            // MessageForm doit recevoir destination_id et contenu.
            DataParser.putId(outgoing, "destination_id", targetId);
            outgoing.put("contenu", content);
            if (socket.send(outgoing)) {
                messageInput.setText("");
                appendMessage("Moi (envoyé sur le canal, livraison non confirmée)", content);
            } else { notice("Échec de l'envoi WebSocket."); }
        } catch (Exception e) { notice("Erreur : " + e.getMessage()); }
    }

    private void receiveMessage(JSONObject rawEvent) {
        JSONObject event = DataParser.messagePayload(rawEvent);
        if (event == null) return;
        String content = DataParser.string(event, "contenu", "content", "text");
        if (content.isEmpty()) return;
        // Ton handler transmet MessageForm, pas l'entité Message.
        // Ces alias couvrent l'entité JPA fournie et plusieurs variantes du record.
        String sender = DataParser.messageSenderId(event);
        String recipient = DataParser.messageRecipientId(event);
        String senderName = DataParser.messageSenderName(event);
        String targetId = recipientId();
        Peer peer = selectedPeer();
        String name = peer == null ? "Utilisateur #" + targetId : peer.name;
        if (!targetId.isEmpty() && sender.equals(targetId)
                && (recipient.isEmpty() || ownId.isEmpty() || recipient.equals(ownId))) {
            appendMessage(senderName.isEmpty() ? name : senderName, content);
        } else if (sender.isEmpty() || targetId.isEmpty()) {
            // MessageForm.java non fourni : afficher le message au lieu de le perdre.
            String visibleSender = !senderName.isEmpty() ? senderName
                    : !sender.isEmpty() ? "Utilisateur #" + sender : "Expéditeur inconnu";
            appendMessage(visibleSender, content);
        } else if (!sender.equals(targetId)) {
            // Message d'une autre conversation : prévenir sans mélanger les discussions.
            notice("Nouveau message de " + (!senderName.isEmpty() ? senderName : "#" + sender));
        }
    }

    private void appendMessage(String name, String content) {
        String previous = messagesText.getText().toString();
        if (previous.equals("Aucun message dans cette conversation.") ||
                previous.equals("Les messages apparaîtront ici.")) previous = "";
        messagesText.setText(previous + (previous.isEmpty() ? "" : "\n") + name + "\n" + content + "\n");
        scrollToBottom();
    }

    private void scrollToBottom() { messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_DOWN)); }

    private Peer selectedPeer() {
        int index = usersSpinner.getSelectedItemPosition();
        return index >= 0 && index < peers.size() ? peers.get(index) : null;
    }

    private boolean authenticated() {
        if (token.isEmpty()) { notice("Connecte-toi d'abord."); return false; }
        return true;
    }

    private void setStatus(String text) { statusText.setText(text); }
    private void notice(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    private void showError(String action, Exception error) { showError(action, error, sessionEpoch); }
    private void showError(String action, Exception error, int epoch) {
        runOnUiThread(() -> {
            if (epoch == sessionEpoch) setStatus(action + " : " + error.getMessage());
        });
    }

    @Override protected void onDestroy() {
        ++sessionEpoch;
        socket.close();
        io.shutdownNow();
        super.onDestroy();
    }

    private static final class Peer {
        final String id;
        final String name;
        final String presence;

        Peer(String id, String name, String presence) {
            this.id = id;
            this.name = name;
            this.presence = presence;
        }
        @Override public String toString() { return name + (id.isEmpty() ? "" : " (#" + id + ")") + " — " + presence; }
    }
}
