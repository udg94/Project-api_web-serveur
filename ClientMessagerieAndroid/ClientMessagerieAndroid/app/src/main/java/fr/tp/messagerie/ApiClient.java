package fr.tp.messagerie;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** API REST minimale. A appeler exclusivement depuis un thread de fond. */
public final class ApiClient {
    private ApiClient() { }

    // APPController : @PostMapping("/comptes") attend email, username et mdp.
    public static Object register(String baseUrl, String email, String username,
                                  String password) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("username", username);
        payload.put("mdp", password);
        return post(baseUrl + AppConfig.REGISTER_PATH, payload, null);
    }

    // APPController : @PostMapping("/auth/login") attend email et mdp.
    public static Object login(String baseUrl, String email, String password) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("mdp", password);
        return post(baseUrl + AppConfig.LOGIN_PATH, payload, null);
    }

    public static Object users(String baseUrl, String token) throws Exception {
        return call("GET", baseUrl + AppConfig.USERS_PATH, null, token);
    }

    public static Object history(String baseUrl, String token, String otherUserId) throws Exception {
        String encodedId = java.net.URLEncoder.encode(otherUserId, "UTF-8");
        return call("GET", baseUrl + AppConfig.HISTORY_PATH_PREFIX + encodedId, null, token);
    }

    private static Object post(String url, JSONObject body, String token) throws Exception {
        return call("POST", url, body, token);
    }

    private static Object call(String method, String path, JSONObject body, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = read(stream);
            if (code < 200 || code >= 300) {
                String details = response.length() > 250 ? response.substring(0, 250) + "…" : response;
                throw new IOException("HTTP " + code + (details.isEmpty() ? "" : " : " + details));
            }
            if (response.trim().isEmpty()) return new JSONObject();
            return new JSONTokener(response).nextValue();
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line).append('\n');
        }
        return builder.toString().trim();
    }
}
