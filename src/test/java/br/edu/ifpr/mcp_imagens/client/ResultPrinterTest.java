package br.edu.ifpr.mcp_imagens.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

class ResultPrinterTest {
   private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
   private final ResultPrinter impressor = new ResultPrinter(
         new PrintStream(bytes, true, StandardCharsets.UTF_8));

   private String texto() {
      return bytes.toString(StandardCharsets.UTF_8);
   }

   @Test
   void sucessoMostraOTextoPuroENaoARepresentacaoDoObjeto() {
      impressor.mostrar(CallToolResult.builder()
                                      .addTextContent("{\"total\": 3}")
                                      .isError(false).build());

      assertTrue(texto().contains("Erro: Não"), texto());
      assertTrue(texto().contains("{\"total\": 3}"), texto());
      assertFalse(texto().contains("TextContent"), texto());
      assertTrue(texto().contains("RESULTADO"), texto());
   }

   @Test
   void erroMostraSimEOCodigo() {
      impressor.mostrar(CallToolResult.builder()
                                      .addTextContent("COR_INVALIDA: xablau")
                                      .isError(true).build());

      assertTrue(texto().contains("Erro: Sim"), texto());
      assertTrue(texto().contains("COR_INVALIDA: xablau"), texto());
   }

   @Test
   void variosConteudosSaemUmPorLinha() {
      impressor.mostrar(CallToolResult.builder()
                                      .addTextContent("um")
                                      .addTextContent("dois")
                                      .isError(false).build());

      assertTrue(texto().contains("um"), texto());
      assertTrue(texto().contains("dois"), texto());
   }

   @Test
   void erroNaoInformadoContaComoSemErro() {
      impressor.mostrar(CallToolResult.builder().addTextContent("x").build());

      assertTrue(texto().contains("Erro: Não"), texto());
   }
}
