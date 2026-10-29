package fr.tp.messagerie;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/** WebSocket avec authentification JWT dans l'en-tête HTTP de handshake. */
public final class MessageSocket {
    public interface Listener {
        void onConnected();
        void onIncoming(JSONObject event);
        void onDisconnected(String reason);
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .pingInterval(25, TimeUnit.SECONDS)
            .build();
    private final AtomicLong connectionVersion = new AtomicLong();
    private volatile WebSocket socket;
    private volatile boolean connected;

    public void connect(String baseUrl, String websocketPath, String token, Listener listener) {
        close();
        String url = AppConfig.websocketUrl(baseUrl, websocketPath);
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT absent : connecte-toi d'abord en REST");
        }
        final long version = connectionVersion.incrementAndGet();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .build();
        socket = client.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                if (version != connectionVersion.get()) {
                    webSocket.close(1000, "Connexion remplacée");
                    return;
                }
                connected = true;
                listener.onConnected();
            }

            @Override public void onMessage(WebSocket webSocket, String text) {
                if (version != connectionVersion.get()) return;
                try {
                    listener.onIncoming(new JSONObject(text));
                } catch (Exception ignored) {
                    // On ignore un message qui n'est pas un objet JSON.
                }
            }

            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                if (version != connectionVersion.get()) return;
                connected = false;
                listener.onDisconnected("WebSocket fermé : " + code);
            }

            @Override public void onFailure(WebSocket webSocket, Throwable error, Response response) {
                if (version != connectionVersion.get()) return;
                connected = false;
                String reason = response == null
                        ? (error == null ? "erreur réseau" : error.getMessage())
                        : "HTTP " + response.code();
                listener.onDisconnected("WebSocket : " + reason);
            }
        });
    }

    public boolean send(JSONObject message) {
        WebSocket active = socket;
        return connected && active != null && active.send(message.toString());
    }

    public boolean isConnected() { return connected; }

    public void close() {
        connectionVersion.incrementAndGet();
        connected = false;
        WebSocket previous = socket;
        socket = null;
        if (previous != null) previous.close(1000, "Fermeture client");
    }

    /** Arrêt définitif lors de la fermeture de l'application. */
    public void shutdown() {
        close();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
