package br.edu.ifpr.mcp_imagens.testutil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import br.edu.ifpr.mcp_imagens.mcp.utils.McpToolDefinition;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.JsonNode;

/**
 * Atalhos para chamar uma ferramenta nos testes unitários, sem servidor.
 */
public final class Chamadas {
   private Chamadas() {}

   /** Chama a ferramenta e devolve o resultado como veio. */
   public static CallToolResult chamar(McpToolDefinition ferramenta,
                                       String nome,
                                       Map<String, Object> argumentos) {
      return ferramenta.run(null, new CallToolRequest(nome, argumentos));
   }

   /** Chama a ferramenta, exige sucesso e devolve o JSON da resposta. */
   public static JsonNode json(McpToolDefinition ferramenta, String nome,
                               Map<String, Object> argumentos) {
      CallToolResult resultado = chamar(ferramenta, nome, argumentos);

      assertFalse(resultado.isError(), Resultados.texto(resultado));
      return Resultados.json(resultado);
   }

   /** Chama a ferramenta, exige erro e devolve o texto {@code CODIGO: msg}. */
   public static String erro(McpToolDefinition ferramenta, String nome,
                             Map<String, Object> argumentos) {
      CallToolResult resultado = chamar(ferramenta, nome, argumentos);

      assertTrue(resultado.isError(), "deveria ser erro: " +
                                      Resultados.texto(resultado));
      return Resultados.texto(resultado);
   }
}
