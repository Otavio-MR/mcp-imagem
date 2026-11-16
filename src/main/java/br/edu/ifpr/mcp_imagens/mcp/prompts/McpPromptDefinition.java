package br.edu.ifpr.mcp_imagens.mcp.prompts;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;

/**
 * Um prompt do servidor: um modelo de instrução que o cliente pode listar e
 * pedir, preenchido com argumentos, para orientar a IA no uso das ferramentas.
 */
public interface McpPromptDefinition {
   void register(SyncSpecification<SingleSessionSyncSpecification> mcp);

   GetPromptResult get(McpSyncServerExchange exchange,
                       GetPromptRequest request);
}
