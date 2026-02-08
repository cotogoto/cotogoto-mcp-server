package jp.livlog.cotogoto.mcpserver.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import jp.livlog.cotogoto.mcpserver.api.ConversationRelayService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ConversationToolService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationToolService.class);

    private final ConversationRelayService relayService;

    public ConversationToolService(ConversationRelayService relayService) {
        this.relayService = relayService;
    }

    @Tool(
            name = "cotogoto_conversation",
            description = "ユーザーがCotogoto（コトゴト）またはノビィ（NOBY）との会話による回答を求めている場合に使用します。"
    )
    public String conversation(String message) {
        logger.info("MCP tool call: cotogotoConversation message={}", message);
        ObjectMapper objectMapper = new ObjectMapper();
        String response = relayService.sendConversation(message);
        JsonNode payload = objectMapper.readTree(response);

        return payload.get("commandResponse").asString();
    }
}
