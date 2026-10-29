package fr.tp.messagerie;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Client PC Java Swing utilisant exactement les routes de l'application Android. */
public final class DesktopApp extends JFrame {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MessageSocket socket = new MessageSocket();
    private final DefaultListModel<Peer> peerModel = new DefaultListModel<>();

    private final JTextField serverInput = new JTextField(AppConfig.DEFAULT_SERVER_URL);
    private final JTextField wsPathInput = new JTextField(AppConfig.WEBSOCKET_PATH);
    private final JTextField emailInput = new JTextField();
    private final JTextField usernameInput = new JTextField();
    private final JPasswordField passwordInput = new JPasswordField();
    private final JList<Peer> usersList = new JList<>(peerModel);
    private final JTextArea messagesText = new JTextArea();
    private final JTextField messageInput = new JTextField();
    private final JLabel conversationLabel = new JLabel("Sélectionne un utilisateur.");
    private final JLabel statusLabel = new JLabel("Non connecté : démarre Spring Boot puis connecte-toi.");
    private final JButton sendButton = new JButton("Envoyer");
    private final JButton refreshButton = new JButton("Actualiser les utilisateurs");
    private final JButton historyButton = new JButton("Charger l'historique");

    // Ces champs sont modifiés uniquement sur le thread Swing (EDT).
    private String token = "";
    private String ownId = "";
    private String ownEmail = "";
    private String activeBaseUrl = "";
    private int sessionEpoch = 0;

    private DesktopApp() {
        super("Messagerie TP — PC");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildInterface();
        setEnabledSessionControls(false);
        setSize(1050, 720);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                ++sessionEpoch;
                socket.shutdown();
                io.shutdownNow();
            }
        });
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        JPanel left = new JPanel(new BorderLayout(6, 8));
        left.setPreferredSize(new Dimension(335, 600));
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Connexion au serveur"));
        loginPanel.add(fieldWithLabel("Adresse HTTP", serverInput));
        loginPanel.add(fieldWithLabel("Chemin WebSocket", wsPathInput));
        loginPanel.add(fieldWithLabel("Email", emailInput));
        loginPanel.add(fieldWithLabel("Username (inscription)", usernameInput));
        loginPanel.add(fieldWithLabel("Mot de passe", passwordInput));
        JPanel authButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        JButton registerButton = new JButton("S'inscrire");
        JButton loginButton = new JButton("Se connecter");
        JButton logoutButton = new JButton("Déconnexion");
        authButtons.add(registerButton);
        authButtons.add(loginButton);
        authButtons.add(logoutButton);
        loginPanel.add(authButtons);
        left.add(loginPanel, BorderLayout.NORTH);

        JPanel usersPanel = new JPanel(new BorderLayout(5, 5));
        usersPanel.setBorder(BorderFactory.createTitledBorder("Utilisateurs"));
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersPanel.add(new JScrollPane(usersList), BorderLayout.CENTER);
        usersPanel.add(refreshButton, BorderLayout.SOUTH);
        left.add(usersPanel, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        JPanel header = new JPanel(new BorderLayout(5, 5));
        conversationLabel.setFont(conversationLabel.getFont().deriveFont(Font.BOLD, 17f));
        header.add(conversationLabel, BorderLayout.CENTER);
        header.add(historyButton, BorderLayout.EAST);
        right.add(header, BorderLayout.NORTH);
        messagesText.setEditable(false);
        messagesText.setLineWrap(true);
        messagesText.setWrapStyleWord(true);
        messagesText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        messagesText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        messagesText.setText("L'historique apparaîtra ici.");
        right.add(new JScrollPane(messagesText), BorderLayout.CENTER);
        JPanel compose = new JPanel(new BorderLayout(8, 0));
        compose.add(messageInput, BorderLayout.CENTER);
        compose.add(sendButton, BorderLayout.EAST);
        right.add(compose, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.33);
        split.setDividerLocation(340);
        root.add(split, BorderLayout.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 2));
        root.add(statusLabel, BorderLayout.SOUTH);

        registerButton.addActionListener(e -> register());
        loginButton.addActionListener(e -> login());
        logoutButton.addActionListener(e -> logout());
        refreshButton.addActionListener(e -> refreshUsers());
        historyButton.addActionListener(e -> loadHistory());
        sendButton.addActionListener(e -> sendMessage());
        messageInput.addActionListener(e -> sendMessage());
        usersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Peer peer = usersList.getSelectedValue();
                if (peer != null) {
                    conversationLabel.setText("Discussion avec " + peer.name + " (#" + peer.id + ")");
                    messagesText.setText("Chargement de l'historique…");
                    loadHistory();
                }
            }
        });
    }

    private JPanel fieldWithLabel(String label, JTextField field) {
        JPanel line = new JPanel(new BorderLayout(4, 2));
        line.setBorder(BorderFactory.createEmptyBorder(2, 5, 3, 5));
        JLabel title = new JLabel(label);
        line.add(title, BorderLayout.NORTH);
        line.add(field, BorderLayout.CENTER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 53));
        return line;
    }

    private void setEnabledSessionControls(boolean loggedIn) {
        sendButton.setEnabled(loggedIn);
        messageInput.setEnabled(loggedIn);
        refreshButton.setEnabled(loggedIn);
        historyButton.setEnabled(loggedIn);
        usersList.setEnabled(loggedIn);
    }

    private String serverUrl() {
        String url = AppConfig.normalizeBaseUrl(serverInput.getText());
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL attendue : http://localhost:8080");
        }
        return url;
    }

    private void register() {
        final String base;
        try { base = serverUrl(); }
        catch (Exception e) { errorDialog(e.getMessage()); return; }
        final String email = emailInput.getText().trim();
        final String username = usernameInput.getText().trim();
        final String password = new String(passwordInput.getPassword());
        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            errorDialog("Pour l'inscription, renseigne email, username et mot de passe.");
            return;
        }
        statusLabel.setText("Inscription en cours…");
        io.execute(() -> {
            try {
                ApiClient.register(base, email, username, password);
                SwingUtilities.invokeLater(() -> statusLabel.setText("Compte créé. Connecte-toi avec cet email."));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Inscription : " + e.getMessage()));
            }
        });
    }

    private void login() {
        final String base;
        try { base = serverUrl(); }
        catch (Exception e) { errorDialog(e.getMessage()); return; }
        final String email = emailInput.getText().trim();
        final String password = new String(passwordInput.getPassword());
        final String wsPath = wsPathInput.getText().trim();
        if (email.isEmpty() || password.isEmpty()) {
            errorDialog("Renseigne ton email et ton mot de passe.");
            return;
        }
        final int epoch = ++sessionEpoch;
        socket.close();
        token = "";
        ownId = "";
        peerModel.clear();
        setEnabledSessionControls(false);
        statusLabel.setText("Connexion REST en cours…");
        io.execute(() -> {
            try {
                Object response = ApiClient.login(base, email, password);
                String jwt = DataParser.token(response);
                if (jwt.isEmpty()) throw new IllegalStateException("Token JWT manquant dans la réponse.");
                String id = DataParser.ownIdFromLogin(response);
                if (id.isEmpty()) id = DataParser.ownIdFromJwt(jwt);
                final String foundId = id;
                SwingUtilities.invokeLater(() -> {
                    if (epoch != sessionEpoch) return;
                    token = jwt;
                    ownId = foundId;
                    ownEmail = email;
                    activeBaseUrl = base;
                    passwordInput.setText("");
                    setEnabledSessionControls(true);
                    statusLabel.setText("Connecté en REST : " + email + " (ID " + ownId + "). WebSocket…");
                    connectSocket(epoch, wsPath);
                    refreshUsers();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (epoch == sessionEpoch) statusLabel.setText("Erreur de connexion : " + e.getMessage());
                });
            }
        });
    }

    private void connectSocket(int epoch, String wsPath) {
        try {
            socket.connect(activeBaseUrl, wsPath, token, new MessageSocket.Listener() {
                @Override public void onConnected() {
                    SwingUtilities.invokeLater(() -> {
                        if (epoch == sessionEpoch)
                            statusLabel.setText("REST + WebSocket connectés · " + ownEmail + " · ID " + ownId);
                    });
                }
                @Override public void onIncoming(JSONObject event) {
                    SwingUtilities.invokeLater(() -> {
                        if (epoch == sessionEpoch) receiveMessage(event);
                    });
                }
                @Override public void onDisconnected(String reason) {
                    SwingUtilities.invokeLater(() -> {
                        if (epoch == sessionEpoch) statusLabel.setText(reason + " (REST disponible)");
                    });
                }
            });
        } catch (Exception e) {
            statusLabel.setText("Erreur WebSocket : " + e.getMessage());
        }
    }

    private void logout() {
        ++sessionEpoch;
        socket.close();
        token = "";
        ownId = "";
        ownEmail = "";
        activeBaseUrl = "";
        peerModel.clear();
        conversationLabel.setText("Sélectionne un utilisateur.");
        messagesText.setText("L'historique apparaîtra ici.");
        setEnabledSessionControls(false);
        statusLabel.setText("Déconnecté.");
    }

    private void refreshUsers() {
        if (token.isEmpty()) { errorDialog("Connecte-toi d'abord."); return; }
        final String jwt = token;
        final String base = activeBaseUrl;
        final String myId = ownId;
        final String myEmail = ownEmail;
        final int epoch = sessionEpoch;
        Peer previous = usersList.getSelectedValue();
        final String previousId = previous == null ? "" : previous.id;
        statusLabel.setText("Chargement de la liste des utilisateurs…");
        io.execute(() -> {
            try {
                JSONArray users = DataParser.asArray(ApiClient.users(base, jwt),
                        "utilisateurs", "users", "data", "results", "content");
                if (users == null) throw new IllegalStateException("Liste d'utilisateurs JSON attendue.");
                List<Peer> found = new ArrayList<>();
                for (int i = 0; i < users.length(); i++) {
                    JSONObject item = users.optJSONObject(i);
                    if (item == null) continue;
                    String id = DataParser.userId(item);
                    String email = DataParser.userEmail(item);
                    String username = DataParser.userName(item);
                    if (id.isEmpty() || id.equals(myId)) continue;
                    if (!myEmail.isEmpty() && email.equalsIgnoreCase(myEmail)) continue;
                    String displayName = !username.isEmpty() ? username
                            : !email.isEmpty() ? email : "Utilisateur #" + id;
                    found.add(new Peer(id, displayName, DataParser.presence(item)));
                }
                SwingUtilities.invokeLater(() -> {
                    if (epoch != sessionEpoch) return;
                    peerModel.clear();
                    Peer selection = null;
                    for (Peer peer : found) {
                        peerModel.addElement(peer);
                        if (peer.id.equals(previousId)) selection = peer;
                    }
                    if (selection == null && !found.isEmpty()) selection = found.get(0);
                    if (selection != null) usersList.setSelectedValue(selection, true);
                    else {
                        conversationLabel.setText("Aucun autre utilisateur.");
                        messagesText.setText("Aucun utilisateur disponible.");
                    }
                    statusLabel.setText("Liste actualisée : " + found.size() + " utilisateur(s).");
                });
            } catch (Exception e) {
                showError("Liste des utilisateurs", e, epoch);
            }
        });
    }

    private void loadHistory() {
        if (token.isEmpty()) return;
        Peer peer = usersList.getSelectedValue();
        if (peer == null) { statusLabel.setText("Choisis un utilisateur à gauche."); return; }
        final String peerId = peer.id;
        final String peerName = peer.name;
        final String myId = ownId;
        final String jwt = token;
        final String base = activeBaseUrl;
        final int epoch = sessionEpoch;
        io.execute(() -> {
            try {
                // Le GET renvoie directement un tableau JSON : [{...}, {...}].
                JSONArray messages = DataParser.asArray(ApiClient.history(base, jwt, peerId),
                        "messages", "historique", "data", "results", "content");
                if (messages == null) throw new IllegalStateException("Tableau JSON attendu pour l'historique.");
                StringBuilder history = new StringBuilder();
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject item = DataParser.messagePayload(messages.optJSONObject(i));
                    if (item == null) continue;
                    String body = DataParser.string(item, "contenu", "content", "text");
                    if (body.isEmpty()) continue;
                    String sender = DataParser.messageSenderId(item);
                    String senderName = DataParser.messageSenderName(item);
                    String date = DataParser.messageDate(item);
                    boolean isMine = !myId.isEmpty() && myId.equals(sender);
                    String displayName = isMine ? "Moi" : !senderName.isEmpty() ? senderName : peerName;
                    history.append(displayName);
                    if (!date.isEmpty()) history.append(" · ").append(date.replace('T', ' '));
                    history.append("\n").append(body).append("\n\n");
                }
                SwingUtilities.invokeLater(() -> {
                    if (epoch != sessionEpoch) return;
                    Peer current = usersList.getSelectedValue();
                    if (current == null || !current.id.equals(peerId)) return;
                    messagesText.setText(history.length() == 0
                            ? "Aucun message dans cette conversation." : history.toString());
                    messagesText.setCaretPosition(messagesText.getDocument().getLength());
                });
            } catch (Exception e) {
                showError("Historique", e, epoch);
            }
        });
    }

    private void sendMessage() {
        Peer peer = usersList.getSelectedValue();
        if (token.isEmpty() || peer == null) {
            errorDialog("Connecte-toi et sélectionne un destinataire."); return;
        }
        if (!socket.isConnected()) {
            errorDialog("WebSocket déconnecté. Vérifie /ws/messages et le JWT."); return;
        }
        String content = messageInput.getText().trim();
        if (content.isEmpty()) return;
        try {
            JSONObject outgoing = new JSONObject();
            DataParser.putId(outgoing, "destination_id", peer.id);
            outgoing.put("contenu", content);
            if (socket.send(outgoing)) {
                messageInput.setText("");
                appendMessage("Moi (envoyé, livraison non confirmée)", content);
            } else {
                statusLabel.setText("Échec de l'envoi WebSocket.");
            }
        } catch (Exception e) {
            errorDialog("Envoi : " + e.getMessage());
        }
    }

    private void receiveMessage(JSONObject rawEvent) {
        JSONObject event = DataParser.messagePayload(rawEvent);
        if (event == null) return;
        String body = DataParser.string(event, "contenu", "content", "text");
        if (body.isEmpty()) return;
        String sender = DataParser.messageSenderId(event);
        String recipient = DataParser.messageRecipientId(event);
        String senderName = DataParser.messageSenderName(event);
        Peer activePeer = usersList.getSelectedValue();
        if (activePeer != null && activePeer.id.equals(sender)
                && (recipient.isEmpty() || recipient.equals(ownId))) {
            appendMessage(senderName.isEmpty() ? activePeer.name : senderName, body);
        } else {
            statusLabel.setText("Nouveau message de " +
                    (!senderName.isEmpty() ? senderName : sender.isEmpty() ? "un utilisateur" : "#" + sender) +
                    " : sélectionne cette conversation pour voir l'historique.");
        }
    }

    private void appendMessage(String name, String content) {
        String previous = messagesText.getText();
        if (previous.equals("Aucun message dans cette conversation.") ||
                previous.equals("L'historique apparaîtra ici.") ||
                previous.equals("Chargement de l'historique…")) previous = "";
        messagesText.setText(previous + (previous.isEmpty() ? "" : "\n")
                + name + "\n" + content + "\n");
        messagesText.setCaretPosition(messagesText.getDocument().getLength());
    }

    private void showError(String action, Exception e, int epoch) {
        SwingUtilities.invokeLater(() -> {
            if (epoch == sessionEpoch) statusLabel.setText(action + " : " + e.getMessage());
        });
    }

    private void errorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Messagerie TP", JOptionPane.WARNING_MESSAGE);
    }

    private record Peer(String id, String name, String presence) {
        @Override public String toString() { return name + " (#" + id + ") — " + presence; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new DesktopApp().setVisible(true);
        });
    }
}
