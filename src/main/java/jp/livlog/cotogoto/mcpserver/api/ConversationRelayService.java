package jp.livlog.cotogoto.mcpserver.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConversationRelayService {

    private final ObjectMapper objectMapper;
    private final URI upstreamUri;
    private final String apiToken;

    public ConversationRelayService(
            ObjectMapper objectMapper,
            @Value("${cotogoto.upstream.conversations-url:https://app.cotogoto.ai/webapi/api/mcp/conversations}") String upstreamUrl,
            @Value("${cotogoto.upstream.api-token:}") String apiToken) {
        this.objectMapper = objectMapper;
        this.upstreamUri = URI.create(upstreamUrl);
        this.apiToken = apiToken;
    }

    public String sendConversation(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("cotogoto.upstream.api-token is required for MCP tool calls");
        }

        Entry entry = new Entry(
                UUID.randomUUID().toString(),
                "user",
                message);
        ConversationRequest request = new ConversationRequest(
                UUID.randomUUID().toString(),
                apiToken,
                entry);

        try {
            HttpURLConnection connection = openConnection(request);
            int status = connection.getResponseCode();
            if (status >= 400) {
                String errorBody = readErrorBody(connection);
                throw new IllegalStateException("Upstream returned status " + status + ": " + errorBody);
            }

            StringBuilder aggregated = new StringBuilder();
            streamEvents(connection, (eventName, data) -> {
                if (data != null && !data.isBlank()) {
                    if (!aggregated.isEmpty()) {
                        aggregated.append('\n');
                    }
                    aggregated.append(data);
                }
            });
            return aggregated.toString();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to call upstream conversation API", exception);
        }
    }

    private HttpURLConnection openConnection(ConversationRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) upstreamUri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(0);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "text/event-stream");

        byte[] payload = objectMapper.writeValueAsBytes(request);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }
        return connection;
    }

    private String readErrorBody(HttpURLConnection connection) throws IOException {
        if (connection.getErrorStream() == null) {
            return "";
        }
        try (BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder errorBody = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorBody.append(line);
            }
            return errorBody.toString();
        }
    }

    private void streamEvents(HttpURLConnection connection, BiConsumer<String, String> handler) throws IOException {
        Objects.requireNonNull(handler, "handler");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            String eventName = null;
            StringBuilder data = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (eventName != null || !data.isEmpty()) {
                        handler.accept(eventName, data.toString());
                    }
                    eventName = null;
                    data = new StringBuilder();
                    continue;
                }

                if (line.startsWith("event:")) {
                    eventName = line.substring("event:".length()).trim();
                    continue;
                }

                if (line.startsWith("data:")) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(line.substring("data:".length()).trim());
                }
            }
        }
    }

    public record ConversationRequest(
            String sessionId,
            String apiToken,
            Entry entry) {
    }

    public record Entry(
            String turnId,
            String role,
            String content) {
    }
}
