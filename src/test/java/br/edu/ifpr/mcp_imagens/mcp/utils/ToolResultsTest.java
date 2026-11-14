package br.edu.ifpr.mcp_imagens.mcp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.exception.CodigoErro;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

class ToolResultsTest {
   record Exemplo(String zeta, int alfa) {}
   record ComNumero(BigDecimal valor) {}

   @Test
   void sucessoDevolveJsonIndentadoNaOrdemDosCampos() {
      CallToolResult resultado = ToolResults.sucesso(new Exemplo("z", 1));
      String         texto     = Resultados.texto(resultado);

      assertFalse(resultado.isError());
      assertEquals(1, resultado.content().size());
      assertTrue(texto.contains("\n"), "JSON deveria estar indentado");
      assertTrue(texto.indexOf("zeta") < texto.indexOf("alfa"),
                 "campos deveriam seguir a ordem do record");
      assertEquals("z", Resultados.json(resultado).get("zeta").asString());
   }

   @Test
   void sucessoAceitaListas() {
      assertEquals(2, Resultados.json(ToolResults.sucesso(List.of(1, 2)))
                                .size());
   }

   @Test
   void sucessoMantemAsDuasCasasDosBigDecimal() {
      String texto = Resultados.texto(ToolResults.sucesso(
                           new ComNumero(new BigDecimal("21.00"))));

      assertTrue(texto.contains("21.00"), texto);
   }

   @Test
   void comoJsonTemOMesmoFormatoDoSucesso() {
      Exemplo dados = new Exemplo("z", 1);

      assertEquals(Resultados.texto(ToolResults.sucesso(dados)),
                   ToolResults.comoJson(dados));
   }

   @Test
   void erroDevolveCodigoEMensagem() {
      CallToolResult resultado = ToolResults.erro(CodigoErro.COR_INVALIDA,
                                                  "cor inválida: 'x'");

      assertTrue(resultado.isError());
      assertEquals("COR_INVALIDA: cor inválida: 'x'",
                   Resultados.texto(resultado));
   }

   @Test
   void erroTemUmUnicoConteudoDeTexto() {
      assertEquals(1, ToolResults.erro(CodigoErro.ERRO_INTERNO, "x")
                                 .content().size());
   }
}
