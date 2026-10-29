package fr.tp.messagerie;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/** WebSocket OkHttp natif. JWT obligatoire dans Authorization (JwtHandshakeInterceptor). */
public final class MessageSocket {
    public interface Listener {
        void onConnected();
        void onIncoming(JSONObject event);
        void onDisconnected(String reason);
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .pingInterval(25, TimeUnit.SECONDS)
            .build();
    private volatile WebSocket socket;
    private volatile boolean connected;

    public void connect(String baseUrl, String websocketPath, String token, Listener listener) {
        close();
        String url = AppConfig.websocketUrl(baseUrl, websocketPath);
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT absent : connecte-toi d'abord en REST");
        }
        // Le JwtHandshakeInterceptor de ton serveur lit uniquement ce header.
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .build();
        socket = client.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                if (socket != webSocket) {
                    webSocket.close(1000, "Connexion remplacée");
                    return;
                }
                connected = true;
                listener.onConnected();
            }

            @Override public void onMessage(WebSocket webSocket, String text) {
                if (socket != webSocket) return;
                try {
                    listener.onIncoming(new JSONObject(text));
                } catch (Exception ignored) {
                    // Les événements non JSON ne font pas planter le client.
                }
            }

            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                if (socket != webSocket) return;
                connected = false;
                listener.onDisconnected("WebSocket fermé : " + code);
            }

            @Override public void onFailure(WebSocket webSocket, Throwable error, Response response) {
                if (socket != webSocket) return;
                connected = false;
                String reason = response == null ? error.getMessage() : "HTTP " + response.code();
                listener.onDisconnected("WebSocket : " + reason);
            }
        });
    }

    public boolean send(JSONObject message) {
        return connected && socket != null && socket.send(message.toString());
    }

    public boolean isConnected() { return connected; }

    public void close() {
        connected = false;
        WebSocket previous = socket;
        socket = null;
        if (previous != null) previous.close(1000, "Fermeture client");
    }
}
