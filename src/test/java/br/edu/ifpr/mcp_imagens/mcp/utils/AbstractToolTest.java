package br.edu.ifpr.mcp_imagens.mcp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.exception.CorInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemNaoEncontradaException;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

class AbstractToolTest {
   record Saida(String eco) {}

   /** Ferramenta de mentira: o comportamento vem de uma função. */
   static class FerramentaFalsa extends AbstractTool {
      private final Function<ArgumentReader, Object> comportamento;

      FerramentaFalsa(Function<ArgumentReader, Object> comportamento) {
         this.comportamento = comportamento;
      }

      @Override protected String nome()      { return "ferramenta_falsa"; }
      @Override protected String titulo()    { return "Ferramenta falsa"; }
      @Override protected String descricao() { return "Só para testes."; }

      @Override
      protected InputSchema schema() {
         return InputSchema.novo().texto("valor", "Um valor");
      }

      @Override
      protected Object executar(ArgumentReader argumentos) {
         return comportamento.apply(argumentos);
      }
   }

   private static CallToolResult chamar(AbstractTool ferramenta,
                                        Map<String, Object> argumentos) {
      return ferramenta.run(null, new CallToolRequest("ferramenta_falsa",
                                                      argumentos));
   }

   @Test
   void sucessoConverteORetornoEmJson() {
      CallToolResult resultado = chamar(
            new FerramentaFalsa(a -> new Saida(a.texto("valor"))),
            Map.of("valor", " oi "));

      assertFalse(resultado.isError());
      assertEquals("oi", Resultados.json(resultado).get("eco").asString());
   }

   @Test
   void excecaoDeDominioViraErroComOCodigoDela() {
      CallToolResult resultado = chamar(new FerramentaFalsa(a -> {
         throw new ImagemNaoEncontradaException("sumiu");
      }), Map.of("valor", "x"));

      assertTrue(resultado.isError());
      assertEquals("IMAGEM_NAO_ENCONTRADA: sumiu", Resultados.texto(resultado));
   }

   @Test
   void outroCodigoDeDominio() {
      CallToolResult resultado = chamar(new FerramentaFalsa(a -> {
         throw new CorInvalidaException("cor ruim");
      }), Map.of("valor", "x"));

      assertEquals("COR_INVALIDA: cor ruim", Resultados.texto(resultado));
   }

   @Test
   void falhaInesperadaViraErroInternoSemVazarDetalhes() {
      CallToolResult resultado = chamar(new FerramentaFalsa(a -> {
         throw new IllegalStateException("segredo interno");
      }), Map.of("valor", "x"));
      String texto = Resultados.texto(resultado);

      assertTrue(resultado.isError());
      assertTrue(texto.startsWith("ERRO_INTERNO: "), texto);
      assertFalse(texto.contains("segredo interno"), texto);
      assertTrue(texto.contains("ferramenta_falsa"), texto);
   }

   @Test
   void parametroAusenteViraErroPadronizado() {
      CallToolResult resultado = chamar(
            new FerramentaFalsa(a -> new Saida(a.texto("valor"))), Map.of());

      assertEquals("PARAMETRO_AUSENTE: 'valor' é obrigatório",
                   Resultados.texto(resultado));
   }

   @Test
   void parametroDesconhecidoViraErroPadronizado() {
      CallToolResult resultado = chamar(
            new FerramentaFalsa(a -> new Saida("x")),
            Map.of("valor", "x", "outro", 1));

      assertEquals("PARAMETRO_INVALIDO: parâmetro desconhecido: 'outro'",
                   Resultados.texto(resultado));
   }

   @Test
   void argumentosNulosSaoTratadosComoVazios() {
      CallToolResult resultado = chamar(
            new FerramentaFalsa(a -> new Saida("ok")), null);

      assertFalse(resultado.isError());
   }

   @Test
   void especificacaoTemNomeTituloDescricaoESchemaFechado() {
      SyncToolSpecification especificacao =
            new FerramentaFalsa(a -> null).especificacao();
      Tool                  tool          = especificacao.tool();

      assertEquals("ferramenta_falsa", tool.name());
      assertEquals("Ferramenta falsa", tool.title());
      assertEquals("Só para testes.", tool.description());
      assertEquals(false, tool.inputSchema().get("additionalProperties"));
      assertEquals(List.of("valor"), tool.inputSchema().get("required"));
   }
}
