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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversationToolService(ConversationRelayService relayService) {
        this.relayService = relayService;
    }

    @Tool(
            name = "cotogoto_conversation",
            description = "ユーザーがCotogoto（コトゴト）またはノビィ（NOBY）との会話による回答を求めている場合に使用します。"
    )
    public String conversation(String message) {
        logger.info("MCP tool call: cotogotoConversation message={}", message);
        return sendCommand(message);
    }

    @Tool(
            name = "cotogoto_work_start",
            description = "作業開始をCotogoto（コトゴト）またはノビィ（NOBY）に通知します。"
    )
    public String workStart() {
        logger.info("MCP tool call: cotogotoWorkStart");
        return sendCommand("作業開始");
    }

    @Tool(
            name = "cotogoto_work_complete",
            description = "作業完了をCotogoto（コトゴト）またはノビィ（NOBY）に通知します。"
    )
    public String workComplete() {
        logger.info("MCP tool call: cotogotoWorkComplete");
        return sendCommand("作業完了");
    }

    @Tool(
            name = "cotogoto_break_start",
            description = "休憩開始をCotogoto（コトゴト）またはノビィ（NOBY）に通知します。"
    )
    public String breakStart() {
        logger.info("MCP tool call: cotogotoBreakStart");
        return sendCommand("休憩開始");
    }

    @Tool(
            name = "cotogoto_break_end",
            description = "休憩終了をCotogoto（コトゴト）またはノビィ（NOBY）に通知します。"
    )
    public String breakEnd() {
        logger.info("MCP tool call: cotogotoBreakEnd");
        return sendCommand("休憩終了");
    }

    private String sendCommand(String message) {
        String response = relayService.sendConversation(message);
        JsonNode payload = objectMapper.readTree(response);
        return payload.get("commandResponse").asString();
    }
}
