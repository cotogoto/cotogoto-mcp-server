package jp.livlog.cotogoto.mcpserver.api;

import java.io.IOException;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/mcp/conversations")
@Validated
public class ConversationController {

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Object acceptConversation(@Valid @RequestBody ConversationRequest request) {
        if (request.apiToken() == null || request.apiToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("apiToken is required"));
        }

        if (request.entry() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("entry is required"));
        }

        SseEmitter emitter = new SseEmitter(0L);
        ConversationAcceptedResponse response = new ConversationAcceptedResponse(
                true,
                List.of(request.entry().turnId()),
                null);

        try {
            emitter.send(SseEmitter.event()
                    .name("conversation.accepted")
                    .data(response));
            emitter.complete();
        }
        catch (IOException exception) {
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public record ConversationRequest(
            @NotBlank String sessionId,
            String apiToken,
            @Valid Entry entry) {
    }

    public record Entry(
            @NotBlank String turnId,
            @NotBlank String role,
            @NotBlank String content) {
    }

    public record ConversationAcceptedResponse(
            boolean accepted,
            List<String> storedTurnIds,
            String commandResponse) {
    }

    public record ErrorResponse(String message) {
    }
}
