package br.edu.ifpr.mcp_imagens.mcp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

class ToolRegistryTest {
   /** Dublê que só anota a ordem em que foi registrado. */
   record FerramentaAnotada(String nome, List<String> registros)
          implements McpToolDefinition {
      @Override
      public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) {
         registros.add(nome);
      }

      @Override
      public CallToolResult run(McpSyncServerExchange exchange,
                                CallToolRequest request) {
         return null;
      }
   }

   @Test
   void registraTodasAsFerramentasNaOrdemRecebida() throws Exception {
      List<String> registros = new ArrayList<>();
      ToolRegistry registro  = new ToolRegistry(List.of(
                                     new FerramentaAnotada("a", registros),
                                     new FerramentaAnotada("b", registros)));

      registro.registrarEm(null);

      assertEquals(List.of("a", "b"), registros);
      assertEquals(2, registro.quantidade());
   }

   @Test
   void registroVazioNaoRegistraNada() throws Exception {
      ToolRegistry registro = new ToolRegistry(List.of());

      registro.registrarEm(null);

      assertEquals(0, registro.quantidade());
   }

   @Test
   void alterarAListaOriginalNaoAfetaORegistro() throws Exception {
      List<String>             registros = new ArrayList<>();
      List<McpToolDefinition>  lista     = new ArrayList<>(
                  List.of(new FerramentaAnotada("a", registros)));
      ToolRegistry             registro  = new ToolRegistry(lista);

      lista.add(new FerramentaAnotada("b", registros));
      registro.registrarEm(null);

      assertEquals(1, registro.quantidade());
      assertEquals(List.of("a"), registros);
   }
}
