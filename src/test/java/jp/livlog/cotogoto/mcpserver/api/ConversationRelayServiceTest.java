package jp.livlog.cotogoto.mcpserver.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ConversationRelayServiceTest {

    @Test
    void sendConversationPostsToUpstream() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/mcp/conversations", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String response = "event: message\n" +
                    "data: hello\n\n";
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            byte[] payload = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            ConversationRelayService service = new ConversationRelayService(
                    objectMapper,
                    "http://localhost:" + port + "/mcp/conversations",
                    "test-token");

            String response = service.sendConversation("hi");

            assertThat(response).isEqualTo("hello");
            JsonNode payload = objectMapper.readTree(requestBody.get());
            assertThat(payload.get("apiToken").asText()).isEqualTo("test-token");
            assertThat(payload.get("entry").get("content").asText()).isEqualTo("hi");
            assertThat(payload.get("entry").get("role").asText()).isEqualTo("user");
        } finally {
            server.stop(0);
        }
    }
}
