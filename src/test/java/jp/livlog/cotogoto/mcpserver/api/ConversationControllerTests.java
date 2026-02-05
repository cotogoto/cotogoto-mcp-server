package jp.livlog.cotogoto.mcpserver.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAcceptConversationAsSse() throws Exception {
        String payload = """
                {
                  \"sessionId\": \"SESSION_ID\",
                  \"apiToken\": \"API_TOKEN\",
                  \"entry\": {
                    \"turnId\": \"turn-1\",
                    \"role\": \"user\",
                    \"content\": \"おはようございます\"
                  }
                }
                """;

        mockMvc.perform(post("/api/mcp/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:conversation.accepted")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"accepted\":true")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"storedTurnIds\":[\"turn-1\"]")));
    }

    @Test
    void shouldReturnBadRequestWhenApiTokenMissing() throws Exception {
        String payload = """
                {
                  \"sessionId\": \"SESSION_ID\",
                  \"entry\": {
                    \"turnId\": \"turn-1\",
                    \"role\": \"user\",
                    \"content\": \"おはようございます\"
                  }
                }
                """;

        mockMvc.perform(post("/api/mcp/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("apiToken is required")));
    }

    @Test
    void shouldReturnBadRequestWhenEntryMissing() throws Exception {
        String payload = """
                {
                  \"sessionId\": \"SESSION_ID\",
                  \"apiToken\": \"API_TOKEN\"
                }
                """;

        mockMvc.perform(post("/api/mcp/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("entry is required")));
    }

}
