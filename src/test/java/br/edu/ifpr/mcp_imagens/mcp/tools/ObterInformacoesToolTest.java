package br.edu.ifpr.mcp_imagens.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.exception.FormatoNaoSuportadoException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemNaoEncontradaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagensException;
import br.edu.ifpr.mcp_imagens.service.InformacoesServicePadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.JsonNode;

class ObterInformacoesToolTest {
   private static ObterInformacoesTool ferramenta(RepositorioFalso repositorio) {
      return new ObterInformacoesTool(new InformacoesServicePadrao(repositorio));
   }

   private static final ObterInformacoesTool COM_BANNER = ferramenta(
         RepositorioFalso.com(ImagensSinteticas.aberta("banner.png",
               ImagensSinteticas.solida(20, 10, 0xFF202020), 2500)));

   private static CallToolResult chamar(ObterInformacoesTool ferramenta,
                                        Map<String, Object> argumentos) {
      return ferramenta.run(null, new CallToolRequest("obter_informacoes",
                                                      argumentos));
   }

   private static String erro(ObterInformacoesTool ferramenta,
                              Map<String, Object> argumentos) {
      CallToolResult resultado = chamar(ferramenta, argumentos);

      assertTrue(resultado.isError());
      return Resultados.texto(resultado);
   }

   @Test
   void devolveDimensoesFormatoAlfaETamanho() {
      CallToolResult resultado = chamar(COM_BANNER,
                                        Map.of("arquivo", "banner.png"));
      JsonNode       json      = Resultados.json(resultado);

      assertFalse(resultado.isError(), Resultados.texto(resultado));
      assertEquals("banner.png", json.get("arquivo").asString());
      assertEquals("PNG", json.get("formato").asString());
      assertEquals(20, json.get("largura").asInt());
      assertEquals(10, json.get("altura").asInt());
      assertEquals(200, json.get("totalPixels").asLong());
      assertFalse(json.get("temCanalAlfa").asBoolean());
      assertEquals(2500, json.get("tamanhoBytes").asLong());
      assertEquals(3, json.get("tamanhoKb").asLong());
   }

   @Test
   void imagemComTransparenciaDizQueTemAlfa() {
      ObterInformacoesTool ferramenta = ferramenta(RepositorioFalso.com(
            ImagensSinteticas.aberta("icone.png",
                  ImagensSinteticas.solida(4, 4, 0x00000000))));

      assertTrue(Resultados.json(chamar(ferramenta,
            Map.of("arquivo", "icone.png"))).get("temCanalAlfa").asBoolean());
   }

   @Test
   void erroDeAcessoAImagemChegaComOCodigoDele() {
      Map<ImagensException, String> esperados = new HashMap<>();
      esperados.put(new ImagemNaoEncontradaException("sumiu"),
                    "IMAGEM_NAO_ENCONTRADA: sumiu");
      esperados.put(new FormatoNaoSuportadoException("so png"),
                    "FORMATO_NAO_SUPORTADO: so png");
      esperados.put(new ImagemInvalidaException("quebrada"),
                    "IMAGEM_INVALIDA: quebrada");
      esperados.put(new ImagemMuitoGrandeException("enorme"),
                    "IMAGEM_MUITO_GRANDE: enorme");

      esperados.forEach((falha, texto) -> assertEquals(texto,
            erro(ferramenta(RepositorioFalso.falhando(falha)),
                 Map.of("arquivo", "x.png"))));
   }

   @Test
   void imagemQueNaoExisteNoRepositorio() {
      assertTrue(erro(COM_BANNER, Map.of("arquivo", "outra.png"))
                    .startsWith("IMAGEM_NAO_ENCONTRADA: "));
   }

   @Test
   void arquivoAusenteOuEmBranco() {
      assertTrue(erro(COM_BANNER, Map.of())
                    .startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erro(COM_BANNER, Map.of("arquivo", "  "))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void parametroDesconhecidoEhRecusado() {
      assertEquals("PARAMETRO_INVALIDO: parâmetro desconhecido: 'pasta'",
                   erro(COM_BANNER, Map.of("arquivo", "banner.png",
                                           "pasta", "/")));
   }

   @Test
   void schemaTemSoOArquivoEDescricaoAvisaDaPasta() {
      assertEquals(Set.of("arquivo"), COM_BANNER.schema().nomesDosParametros());
      assertEquals("obter_informacoes", COM_BANNER.nome());
      assertTrue(COM_BANNER.descricao().contains("listar_imagens"));
      assertTrue(COM_BANNER.descricao().contains("pasta de imagens"));
      assertTrue(COM_BANNER.descricao().contains("anexadas à conversa NÃO"));
   }
}
