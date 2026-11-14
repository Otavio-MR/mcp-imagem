package br.edu.ifpr.mcp_imagens.testutil;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Leitura do conteúdo de um {@link CallToolResult} nos testes.
 */
public final class Resultados {
   private static final JsonMapper JSON = new JsonMapper();

   private Resultados() {}

   /** Texto do único conteúdo do resultado. */
   public static String texto(CallToolResult resultado) {
      return ((TextContent) resultado.content().getFirst()).text();
   }

   /** Texto do resultado interpretado como JSON. */
   public static JsonNode json(CallToolResult resultado) {
      return JSON.readTree(texto(resultado));
   }
}
