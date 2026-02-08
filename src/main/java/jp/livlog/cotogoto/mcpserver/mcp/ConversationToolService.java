package jp.livlog.cotogoto.mcpserver.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import jp.livlog.cotogoto.mcpserver.api.ConversationRelayService;

@Service
public class ConversationToolService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationToolService.class);

    private final ConversationRelayService relayService;

    public ConversationToolService(ConversationRelayService relayService) {
        this.relayService = relayService;
    }

    @Tool(
            name = "cotogotoConversation",
            description = "Send every provided user message to cotogoto AI as-is and return its response."
    )
    public String conversation(String message) {
        logger.info("MCP tool call: cotogotoConversation message={}", message);
        return relayService.sendConversation(message);
    }
}
