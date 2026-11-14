package br.edu.ifpr.mcp_imagens.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.model.FormatoImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;
import br.edu.ifpr.mcp_imagens.domain.model.ListaDeImagens;
import br.edu.ifpr.mcp_imagens.service.InformacoesService;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.JsonNode;

class ListarImagensToolTest {
   /** Dublê do serviço com uma lista fixa. */
   record ServicoFixo(ListaDeImagens lista) implements InformacoesService {
      @Override public ListaDeImagens listarImagens() { return lista; }
   }

   private static CallToolResult chamar(ListaDeImagens lista,
                                        Map<String, Object> argumentos) {
      return new ListarImagensTool(new ServicoFixo(lista)).run(
                  null, new CallToolRequest("listar_imagens", argumentos));
   }

   @Test
   void listaAsImagensComPastaTotalNomeFormatoETamanho() {
      CallToolResult resultado = chamar(new ListaDeImagens("/img", 2, List.of(
               new ImagemInfo("a.png", FormatoImagem.PNG, 12),
               new ImagemInfo("b.jpg", FormatoImagem.JPEG, 340))), Map.of());
      JsonNode       json      = Resultados.json(resultado);

      assertFalse(resultado.isError());
      assertEquals("/img", json.get("pasta").asString());
      assertEquals(2, json.get("total").asInt());
      assertEquals("a.png", json.get("imagens").get(0).get("nome").asString());
      assertEquals("PNG", json.get("imagens").get(0).get("formato").asString());
      assertEquals(12, json.get("imagens").get(0).get("tamanhoKb").asLong());
      assertEquals("JPEG", json.get("imagens").get(1).get("formato").asString());
   }

   @Test
   void pastaSemImagensEhSucessoComListaVazia() {
      CallToolResult resultado = chamar(new ListaDeImagens("/img", 0,
                                                           List.of()), null);
      JsonNode       json      = Resultados.json(resultado);

      assertFalse(resultado.isError());
      assertEquals(0, json.get("total").asInt());
      assertTrue(json.get("imagens").isEmpty());
      assertEquals("/img", json.get("pasta").asString());
   }

   @Test
   void parametroInesperadoEhRecusado() {
      CallToolResult resultado = chamar(new ListaDeImagens("/img", 0,
            List.of()), Map.of("pasta", "C:\\"));

      assertTrue(resultado.isError());
      assertTrue(Resultados.texto(resultado)
                           .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void naoTemParametros() {
      ListarImagensTool ferramenta = new ListarImagensTool(
            new ServicoFixo(new ListaDeImagens("/", 0, List.of())));

      assertTrue(ferramenta.schema().nomesDosParametros().isEmpty());
      assertEquals("listar_imagens", ferramenta.nome());
   }

   @Test
   void descricaoOrientaAUsarPrimeiroEAvisaDosAnexos() {
      String descricao = new ListarImagensTool(
            new ServicoFixo(new ListaDeImagens("/", 0, List.of())))
            .descricao();

      assertTrue(descricao.contains("primeiro"), descricao);
      assertTrue(descricao.contains("anexadas à conversa NÃO"), descricao);
      assertTrue(descricao.contains("pasta"), descricao);
   }
}
