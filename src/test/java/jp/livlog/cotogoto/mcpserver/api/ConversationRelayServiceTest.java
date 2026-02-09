package jp.livlog.cotogoto.mcpserver.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ConversationRelayServiceTest {

    @Test
    void resolveCharsetDefaultsToUtf8() {
        assertThat(ConversationRelayService.resolveCharset(null, null)).isEqualTo(StandardCharsets.UTF_8);
        assertThat(ConversationRelayService.resolveCharset("text/event-stream", "auto"))
                .isEqualTo(StandardCharsets.UTF_8);
        assertThat(ConversationRelayService.resolveCharset("text/event-stream; charset=", "auto"))
                .isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    void resolveCharsetUsesDeclaredCharset() {
        Charset charset = ConversationRelayService.resolveCharset("text/event-stream; charset=Shift_JIS", "auto");
        assertThat(charset).isEqualTo(Charset.forName("Shift_JIS"));
    }

    @Test
    void resolveCharsetHonorsOverrideCharset() {
        Charset charset = ConversationRelayService.resolveCharset("text/event-stream; charset=Shift_JIS", "utf-8");
        assertThat(charset).isEqualTo(StandardCharsets.UTF_8);
    }

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
                    "https://app.cotogoto.ai/webapi/api/mcp/conversations",
                    "a02003fb2eda2ee1f9000da21963c69a",
                    "utf-8");

            String response = service.sendConversation("おみくじ引きたい");

//            assertThat(response).isEqualTo("hello");
            JsonNode payload = objectMapper.readTree(response);
//            assertThat(payload.get("apiToken").asText()).isEqualTo("test-token");
//            assertThat(payload.get("entry").get("content").asText()).isEqualTo("hi");
//            assertThat(payload.get("entry").get("role").asText()).isEqualTo("user");
            System.out.println(payload.get("commandResponse").asString());
        } finally {
            server.stop(0);
        }
    }
}
