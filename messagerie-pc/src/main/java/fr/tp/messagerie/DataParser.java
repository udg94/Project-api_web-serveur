package fr.tp.messagerie;

import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/** Analyse JSON REST et WebSocket, avec champs de l'entité Message et variantes possibles de MessageForm. */
public final class DataParser {
    private DataParser() { }

    public static String string(JSONObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            Object value = object.opt(key);
            if (value != null && value != JSONObject.NULL && !(value instanceof JSONObject)
                    && !(value instanceof JSONArray)) {
                String result = String.valueOf(value);
                if (!result.trim().isEmpty()) return result;
            }
        }
        return "";
    }

    public static JSONObject asObject(Object value) {
        return value instanceof JSONObject ? (JSONObject) value : null;
    }

    public static JSONArray asArray(Object value, String... keys) {
        if (value instanceof JSONArray) return (JSONArray) value;
        JSONObject object = asObject(value);
        if (object != null) {
            for (String key : keys) {
                Object candidate = object.opt(key);
                if (candidate instanceof JSONArray) return (JSONArray) candidate;
                if (candidate instanceof JSONObject) {
                    JSONArray nested = asArray(candidate, keys);
                    if (nested != null) return nested;
                }
            }
        }
        return null;
    }

    public static String token(Object response) {
        if (response instanceof String) return (String) response;
        JSONObject data = asObject(response);
        if (data == null) return "";
        String result = string(data, "token", "accessToken", "access_token", "jwt", "jwtToken");
        if (!result.isEmpty()) return result;
        JSONObject nested = asObject(data.opt("data"));
        if (nested == null) nested = asObject(data.opt("result"));
        return nested == null ? "" : string(nested, "token", "accessToken", "access_token", "jwt", "jwtToken");
    }

    public static String userId(JSONObject object) {
        return string(object, "id", "userId", "utilisateurId", "idUtilisateur", "_id");
    }

    public static String userEmail(JSONObject object) {
        return string(object, "email", "mail");
    }

    public static String userName(JSONObject object) {
        return string(object, "username", "nomUtilisateur", "login", "name");
    }

    public static String ownIdFromLogin(Object response) {
        JSONObject data = asObject(response);
        if (data == null) return "";
        String id = string(data, "userId", "utilisateurId", "id");
        if (!id.isEmpty()) return id;
        JSONObject user = asObject(data.opt("user"));
        if (user == null) user = asObject(data.opt("utilisateur"));
        if (user != null) return userId(user);
        JSONObject nested = asObject(data.opt("data"));
        if (nested != null) {
            id = string(nested, "userId", "utilisateurId", "id");
            if (!id.isEmpty()) return id;
        }
        return "";
    }

    /** JWT décodé uniquement pour aider l'interface, jamais pour valider l'authentification. */
    public static String ownIdFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "";
            String decoded = new String(Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8);
            JSONObject claims = new JSONObject(decoded);
            return string(claims, "userId", "utilisateurId", "id", "uid");
        } catch (Exception ignored) {
            return "";
        }
    }

    public static JSONObject messagePayload(JSONObject json) {
        if (json == null) return null;
        JSONObject inner = asObject(json.opt("message"));
        if (inner == null) inner = asObject(json.opt("payload"));
        if (inner == null) inner = asObject(json.opt("data"));
        return inner == null ? json : inner;
    }

    public static void putId(JSONObject json, String key, String id) throws JSONException {
        try {
            json.put(key, Long.parseLong(id));
        } catch (NumberFormatException ignored) {
            json.put(key, id);
        }
    }

    /**
     * L'entité JPA Message utilise expediteur_username, expediteur_id, destination_id,
     * contenu et date. Le WebSocket émet un MessageForm dont les noms restent à confirmer.
     */
    public static String messageSenderId(JSONObject message) {
        return string(message, "expediteur_id", "source_id", "expediteurId", "sourceId",
                "senderId", "sender_id", "fromId", "from_id", "id_source");
    }

    public static String messageRecipientId(JSONObject message) {
        return string(message, "destination_id", "destinataire_id", "destinationId",
                "destinataireId", "recipientId", "toId");
    }

    public static String messageSenderName(JSONObject message) {
        return string(message, "expediteur_username", "username", "expediteurUsername",
                "senderUsername", "sender_name");
    }

    public static String messageDate(JSONObject message) {
        return string(message, "date", "dateEnvoi", "date_envoi", "sentAt", "timestamp");
    }

    public static String presence(JSONObject user) {
        Object raw = user.opt("connecte");
        if (raw == null) raw = user.opt("online");
        if (raw == null) raw = user.opt("isOnline");
        if (raw == null) raw = user.opt("isConnected");
        if (raw == null) raw = user.opt("connected");
        if (raw == null) raw = user.opt("active");
        if (raw == null) raw = user.opt("isActive");
        if (raw == null) raw = user.opt("enLigne");
        if (raw == null) raw = user.opt("loggedIn");
        if (raw == null) raw = user.opt("isLoggedIn");
        if (raw == null) raw = user.opt("statut");
        if (raw == null) raw = user.opt("status");
        if (raw == null || raw == JSONObject.NULL) return "statut inconnu (nom du champ à confirmer)";
        String v = String.valueOf(raw).toLowerCase(java.util.Locale.ROOT);
        if (v.equals("true") || v.equals("online") || v.equals("connected") || v.equals("connecté")) return "en ligne";
        if (v.equals("false") || v.equals("offline") || v.equals("disconnected") || v.equals("hors ligne")) return "hors ligne";
        return String.valueOf(raw);
    }
}
