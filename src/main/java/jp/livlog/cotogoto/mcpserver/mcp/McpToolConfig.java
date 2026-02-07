package jp.livlog.cotogoto.mcpserver.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider cotogotoTools(ConversationToolService service) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(service)
                .build();
    }
}
