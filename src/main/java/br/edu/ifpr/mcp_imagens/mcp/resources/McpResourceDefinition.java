package br.edu.ifpr.mcp_imagens.mcp.resources;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;

/**
 * Um resource do servidor: um conteúdo que o cliente pode listar e ler pelo
 * endereço (URI), sem chamar uma ferramenta.
 */
public interface McpResourceDefinition {
   void register(SyncSpecification<SingleSessionSyncSpecification> mcp);

   ReadResourceResult read(McpSyncServerExchange exchange,
                           ReadResourceRequest request);
}
