package jp.livlog.cotogoto.mcpserver.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ConversationRelayService {

    private final ObjectMapper objectMapper;
    private final URI upstreamUri;
    private final Executor executor;

    public ConversationRelayService(
            ObjectMapper objectMapper,
            @Value("${cotogoto.upstream.conversations-url:https://app.cotogoto.ai/webapi/api/mcp/conversations}") String upstreamUrl) {
        this.objectMapper = objectMapper;
        this.upstreamUri = URI.create(upstreamUrl);
        this.executor = Executors.newCachedThreadPool();
    }

    public void relay(ConversationController.ConversationRequest request, SseEmitter emitter) {
        executor.execute(() -> {
            try {
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

                int status = connection.getResponseCode();
                if (status >= 400) {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorBody = new StringBuilder();
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorBody.append(line);
                        }
                        emitter.send(SseEmitter.event().name("conversation.error").data(errorBody.toString()));
                    }
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    String eventName = null;
                    StringBuilder data = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) {
                            if (eventName != null || data.length() > 0) {
                                SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event();
                                if (eventName != null) {
                                    eventBuilder.name(eventName);
                                }
                                eventBuilder.data(data.toString());
                                emitter.send(eventBuilder);
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
                            if (data.length() > 0) {
                                data.append('\n');
                            }
                            data.append(line.substring("data:".length()).trim());
                        }
                    }
                }

                emitter.complete();
            }
            catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });
    }
}
