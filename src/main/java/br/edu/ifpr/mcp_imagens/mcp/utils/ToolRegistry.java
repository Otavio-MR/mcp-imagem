package br.edu.ifpr.mcp_imagens.mcp.utils;

import java.util.List;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;

/**
 * Conjunto das ferramentas oferecidas pelo servidor (padrão Registry).
 * <p>
 * Registra cada ferramenta sem conhecer as classes concretas: acrescentar
 * uma ferramenta é só incluí-la na lista montada em
 * {@code McpImagensServer} (Aberto/Fechado), sem alterar esta classe nem as
 * demais ferramentas.
 */
public final class ToolRegistry {
   private final List<McpToolDefinition> ferramentas;

   public ToolRegistry(List<McpToolDefinition> ferramentas) {
      this.ferramentas = List.copyOf(ferramentas);
   }

   /** Registra todas as ferramentas na especificação do servidor. */
   public void registrarEm(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      for (McpToolDefinition ferramenta : ferramentas)
         ferramenta.register(mcp);
   }

   public int quantidade() {
      return ferramentas.size();
   }
}
