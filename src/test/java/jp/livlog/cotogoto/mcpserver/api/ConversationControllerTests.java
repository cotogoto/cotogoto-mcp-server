package jp.livlog.cotogoto.mcpserver.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTests {

    private MockMvc mockMvc;

    @Mock
    private ConversationRelayService relayService;

    @BeforeEach
    void setUp() {
        ConversationController controller = new ConversationController(relayService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

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
                .andExpect(status().isOk());

        verify(relayService, times(1)).relay(any(), any());
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
