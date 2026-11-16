package br.edu.ifpr.mcp_imagens.mcp.tools;

import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.chamar;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.erro;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.json;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.BRANCO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.VERMELHO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.service.ComparadorDeImagensPadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import tools.jackson.databind.JsonNode;

class CompararImagensToolTest {
   private static final String NOME = "comparar_imagens";

   private static final CompararImagensTool FERRAMENTA =
         new CompararImagensTool(RepositorioFalso.com(
               // 4x2 branca
               ImagensSinteticas.aberta("base.png",
                     ImagensSinteticas.solida(4, 2, BRANCO)),
               // igual a base, mas com dois pixels vermelhos
               ImagensSinteticas.aberta("dois.png",
                     ImagensSinteticas.gerar(4, 2, (x, y) ->
                           (x == 1 && y == 0) || (x == 2 && y == 1)
                                 ? VERMELHO : BRANCO)),
               // quase branca: um pixel #FEFEFE
               ImagensSinteticas.aberta("quase.png",
                     ImagensSinteticas.gerar(4, 2, (x, y) ->
                           x == 0 && y == 0 ? 0xFFFEFEFE : BRANCO)),
               ImagensSinteticas.aberta("outra.png",
                     ImagensSinteticas.solida(2, 4, BRANCO))),
               new ComparadorDeImagensPadrao());

   private static JsonNode comparar(Map<String, Object> argumentos) {
      return json(FERRAMENTA, NOME, argumentos);
   }

   private static String erroCom(Map<String, Object> argumentos) {
      return erro(FERRAMENTA, NOME, argumentos);
   }

   @Test
   void imagensIdenticas() {
      JsonNode json = comparar(Map.of("arquivo_a", "base.png",
                                      "arquivo_b", "base.png"));

      assertEquals(true, json.get("identicas").asBoolean());
      assertEquals(8, json.get("totalPixels").asLong());
      assertEquals(4, json.get("largura").asInt());
      assertEquals(2, json.get("altura").asInt());
      assertEquals(0, json.get("pixelsDiferentes").get("quantidade").asLong());
      assertEquals(100.0, json.get("pixelsIguais").get("percentual")
                              .asDouble(), 0);
      assertTrue(json.get("regiaoDiferente").isNull());
   }

   @Test
   void doisPixelsDiferentesComRegiao() {
      JsonNode json = comparar(Map.of("arquivo_a", "base.png",
                                      "arquivo_b", "dois.png"));

      assertEquals("base.png", json.get("arquivoA").asString());
      assertEquals("dois.png", json.get("arquivoB").asString());
      assertEquals(false, json.get("identicas").asBoolean());
      assertEquals(2, json.get("pixelsDiferentes").get("quantidade").asLong());
      assertEquals(25.0, json.get("pixelsDiferentes").get("percentual")
                             .asDouble(), 0);
      assertEquals(6, json.get("pixelsIguais").get("quantidade").asLong());
      assertEquals(75.0, json.get("pixelsIguais").get("percentual")
                            .asDouble(), 0);
      assertEquals(1, json.get("regiaoDiferente").get("xMin").asInt());
      assertEquals(0, json.get("regiaoDiferente").get("yMin").asInt());
      assertEquals(2, json.get("regiaoDiferente").get("xMax").asInt());
      assertEquals(1, json.get("regiaoDiferente").get("yMax").asInt());
   }

   @Test
   void percentualSaiComDuasCasasNoTexto() {
      String texto = Resultados.texto(chamar(FERRAMENTA, NOME,
            Map.of("arquivo_a", "base.png", "arquivo_b", "dois.png")));

      assertTrue(texto.contains("\"percentual\" : 25.00"), texto);
   }

   @Test
   void toleranciaPadraoEZeroEUmCanalAMenosJaDiferente() {
      JsonNode json = comparar(Map.of("arquivo_a", "base.png",
                                      "arquivo_b", "quase.png"));

      assertEquals(0, json.get("tolerancia").asInt());
      assertEquals(1, json.get("pixelsDiferentes").get("quantidade").asLong());
   }

   @Test
   void toleranciaUmAceitaOPixelQuaseBranco() {
      JsonNode json = comparar(Map.of("arquivo_a", "base.png",
                                      "arquivo_b", "quase.png",
                                      "tolerancia", 1));

      assertEquals(true, json.get("identicas").asBoolean());
   }

   @Test
   void tamanhosDiferentes() {
      String texto = erroCom(Map.of("arquivo_a", "base.png",
                                    "arquivo_b", "outra.png"));

      assertEquals("DIMENSOES_DIFERENTES: as imagens precisam ter o mesmo " +
                   "tamanho: a primeira tem 4x2 e a segunda tem 2x4", texto);
   }

   @ParameterizedTest
   @ValueSource(ints = { -1, 256 })
   void toleranciaForaDaFaixa(int tolerancia) {
      String texto = erroCom(Map.of("arquivo_a", "base.png",
                                    "arquivo_b", "base.png",
                                    "tolerancia", tolerancia));

      assertTrue(texto.startsWith("PARAMETRO_INVALIDO: "), texto);
      assertTrue(texto.contains("entre 0 e 255"), texto);
   }

   @Test
   void parametrosAusentesOuEmBranco() {
      assertTrue(erroCom(Map.of("arquivo_a", "base.png"))
                    .startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erroCom(Map.of("arquivo_b", "base.png"))
                    .startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erroCom(Map.of("arquivo_a", "base.png", "arquivo_b", " "))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erroCom(Map.of("arquivo", "base.png"))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void cadaArquivoInexistenteEApontado() {
      String a = erroCom(Map.of("arquivo_a", "nada.png",
                                "arquivo_b", "base.png"));
      String b = erroCom(Map.of("arquivo_a", "base.png",
                                "arquivo_b", "sumiu.png"));

      assertTrue(a.startsWith("IMAGEM_NAO_ENCONTRADA: ") &&
                 a.contains("'nada.png'"), a);
      assertTrue(b.startsWith("IMAGEM_NAO_ENCONTRADA: ") &&
                 b.contains("'sumiu.png'"), b);
   }

   @Test
   void erroDeParametroVemAntesDoErroDeArquivo() {
      assertTrue(erroCom(Map.of("arquivo_a", "nada.png",
                                "arquivo_b", "nada.png", "tolerancia", 300))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void errosDeAcessoAImagemPropagam() {
      CompararImagensTool invalida = new CompararImagensTool(
            RepositorioFalso.falhando(new ImagemInvalidaException("x")),
            new ComparadorDeImagensPadrao());

      assertEquals("IMAGEM_INVALIDA: x",
                   erro(invalida, NOME, Map.of("arquivo_a", "a.png",
                                               "arquivo_b", "b.png")));
   }

   @Test
   void schemaEDescricao() {
      assertEquals(Set.of("arquivo_a", "arquivo_b", "tolerancia"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("arquivo_a", "arquivo_b"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertTrue(FERRAMENTA.descricao().contains("anexadas à conversa NÃO"));
      assertTrue(FERRAMENTA.descricao().contains("DIMENSOES_DIFERENTES"));
      assertEquals(NOME, FERRAMENTA.nome());
   }
}
