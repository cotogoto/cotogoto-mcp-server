package com.cotogoto.mcpclient.transport;

import com.cotogoto.mcpclient.protocol.JsonRpcCodec;
import com.cotogoto.mcpclient.protocol.JsonRpcRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class StreamableHttpTransport {
    private final HttpClient httpClient;
    private final URI endpoint;
    private final JsonRpcCodec codec = new JsonRpcCodec();
    private final ObjectMapper mapper = new ObjectMapper();

    public StreamableHttpTransport(URI endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String startSession() {
        String requestId = UUID.randomUUID().toString();
        JsonRpcRequest request = JsonRpcRequest.request(requestId, "initialize", new ClientInfo("cotogoto-mcp-client"));
        HttpRequest httpRequest = HttpRequest.newBuilder(resolve("./initialize"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(codec.write(request)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Failed to initialize session: HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode sessionId = root.path("result").path("sessionId");
            if (sessionId.isMissingNode() || sessionId.asText().isBlank()) {
                throw new IllegalStateException("Session ID missing in initialize response");
            }
            return sessionId.asText();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to initialize session", e);
        }
    }

    public void streamRequests(String sessionId, Consumer<String> onMessage) {
        HttpRequest httpRequest = HttpRequest.newBuilder(resolve("./stream?sessionId=" + sessionId))
                .timeout(Duration.ofMinutes(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Failed to open stream: HTTP " + response.statusCode());
            }
            try (java.util.stream.Stream<String> lines = response.body()) {
                lines.filter(line -> !line.isBlank()).forEach(onMessage);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed while streaming", e);
        }
    }

    public void sendResponse(String sessionId, String jsonPayload) {
        HttpRequest httpRequest = HttpRequest.newBuilder(resolve("./response?sessionId=" + sessionId))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Failed to send response: HTTP " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to send response", e);
        }
    }

    private URI resolve(String path) {
        return endpoint.resolve(path);
    }

    private record ClientInfo(String name) {
    }
}
