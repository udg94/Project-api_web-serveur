package fr.tp.messagerie;

/** Routes de ton APPController et de ton WebSocketConfig Spring Boot. */
public final class AppConfig {
    private AppConfig() { }

    public static final String DEFAULT_SERVER_URL = "http://localhost:8080";
    public static final String REGISTER_PATH = "/api/comptes";
    public static final String LOGIN_PATH = "/api/auth/login";
    public static final String USERS_PATH = "/api/utilisateurs";
    // Historique : GET /api/messages/{utilisateurId} (déjà implémenté côté serveur).
    public static final String HISTORY_PATH_PREFIX = "/api/messages/";
    // Confirmé dans WebSocketConfig.registerWebSocketHandlers().
    public static final String WEBSOCKET_PATH = "/ws/messages";

    public static String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String result = url.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    public static String websocketUrl(String baseUrl, String websocketPath) {
        String normalized = normalizeBaseUrl(baseUrl);
        String path = websocketPath == null ? "" : websocketPath.trim();
        if (path.isEmpty()) path = WEBSOCKET_PATH;
        if (!path.startsWith("/")) path = "/" + path;
        if (path.contains("?") || path.contains("#") || path.contains(" ")) {
            throw new IllegalArgumentException("Chemin WebSocket invalide : " + path);
        }
        if (normalized.startsWith("https://")) return "wss://" + normalized.substring(8) + path;
        if (normalized.startsWith("http://")) return "ws://" + normalized.substring(7) + path;
        throw new IllegalArgumentException("L'adresse doit commencer par http:// ou https://");
    }
}
