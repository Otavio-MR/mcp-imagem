package br.edu.ifpr.mcp_imagens.mcp.tools;

import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.chamar;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.erro;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeBrilhoPadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import tools.jackson.databind.JsonNode;

class HistogramaDeBrilhoToolTest {
   private static final String NOME = "histograma_de_brilho";

   // 10x10: 70 pretos, 20 brancos e 10 transparentes (última linha)
   private static final Imagem IMAGEM = ImagensSinteticas.gerar(10, 10,
         (x, y) -> y == 9 ? 0x00FFFFFF : y < 7 ? 0xFF000000 : 0xFFFFFFFF);

   private static final HistogramaDeBrilhoTool FERRAMENTA =
         new HistogramaDeBrilhoTool(RepositorioFalso.com(
               ImagensSinteticas.aberta("a.png", IMAGEM)),
               new AnalisadorDeBrilhoPadrao());

   private static JsonNode histograma(Map<String, Object> argumentos) {
      return json(FERRAMENTA, NOME, argumentos);
   }

   private static String erroCom(Map<String, Object> argumentos) {
      return erro(FERRAMENTA, NOME, argumentos);
   }

   @Test
   void padraoSaoOitoFaixas() {
      JsonNode json = histograma(Map.of("arquivo", "a.png"));

      assertEquals(8, json.get("numeroDeFaixas").asInt());
      assertEquals(8, json.get("faixas").size());
      assertEquals(90, json.get("totalConsiderado").asLong());
      assertEquals(true, json.get("ignorarTransparentes").asBoolean());
   }

   @Test
   void descreveCadaFaixa() {
      JsonNode faixas = histograma(Map.of("arquivo", "a.png")).get("faixas");
      JsonNode escura = faixas.get(0);
      JsonNode clara  = faixas.get(7);

      assertEquals(0, escura.get("indice").asInt());
      assertEquals(0, escura.get("lumaMinima").asInt());
      assertEquals(31, escura.get("lumaMaxima").asInt());
      assertEquals(70, escura.get("quantidade").asLong());
      assertEquals(77.78, escura.get("percentual").asDouble(), 0);
      assertEquals(224, clara.get("lumaMinima").asInt());
      assertEquals(255, clara.get("lumaMaxima").asInt());
      assertEquals(20, clara.get("quantidade").asLong());
      assertEquals(0, faixas.get(3).get("quantidade").asLong());
   }

   @Test
   void percentualSaiComDuasCasasNoTexto() {
      String texto = Resultados.texto(chamar(FERRAMENTA, NOME,
                                             Map.of("arquivo", "a.png")));

      assertTrue(texto.contains("\"percentual\" : 77.78"), texto);
      assertTrue(texto.contains("\"percentual\" : 0.00"), texto);
   }

   @Test
   void numeroDeFaixasPedido() {
      JsonNode json = histograma(Map.of("arquivo", "a.png", "faixas", 2));

      assertEquals(2, json.get("faixas").size());
      assertEquals(127, json.get("faixas").get(0).get("lumaMaxima").asInt());
      assertEquals(70, json.get("faixas").get(0).get("quantidade").asLong());
   }

   @Test
   void semIgnorarTransparentesContaOsDezPixelsBrancos() {
      JsonNode json = histograma(Map.of("arquivo", "a.png",
                                        "ignorar_transparentes", false));

      assertEquals(100, json.get("totalConsiderado").asLong());
      assertEquals(30, json.get("faixas").get(7).get("quantidade").asLong());
   }

   @Test
   void limitesDeFaixasSaoAceitos() {
      assertEquals(2, histograma(Map.of("arquivo", "a.png", "faixas", 2))
                          .get("faixas").size());
      assertEquals(32, histograma(Map.of("arquivo", "a.png", "faixas", 32))
                          .get("faixas").size());
   }

   @ParameterizedTest
   @ValueSource(ints = { 0, 1, 33, -4 })
   void faixasForaDoLimite(int faixas) {
      String texto = erroCom(Map.of("arquivo", "a.png", "faixas", faixas));

      assertTrue(texto.startsWith("PARAMETRO_INVALIDO: "), texto);
      assertTrue(texto.contains("entre 2 e 32"), texto);
   }

   @Test
   void erros() {
      assertTrue(erroCom(Map.of()).startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erroCom(Map.of("arquivo", "x.png"))
                    .startsWith("IMAGEM_NAO_ENCONTRADA: "));
      assertTrue(erroCom(Map.of("arquivo", "a.png", "limiar", 1))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erroCom(Map.of("arquivo", "nada.png", "faixas", 1))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void errosDeAcessoAImagemPropagam() {
      HistogramaDeBrilhoTool invalida = new HistogramaDeBrilhoTool(
            RepositorioFalso.falhando(new ImagemInvalidaException("x")),
            new AnalisadorDeBrilhoPadrao());

      assertEquals("IMAGEM_INVALIDA: x",
                   erro(invalida, NOME, Map.of("arquivo", "a.png")));
   }

   @Test
   void schemaEDescricao() {
      Map<?, ?> propriedades = (Map<?, ?>) FERRAMENTA.schema().comoMapa()
                                                     .get("properties");

      assertEquals(Set.of("arquivo", "faixas", "ignorar_transparentes"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("arquivo"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertEquals(8, ((Map<?, ?>) propriedades.get("faixas")).get("default"));
      assertEquals(32, ((Map<?, ?>) propriedades.get("faixas")).get("maximum"));
      assertTrue(FERRAMENTA.descricao().contains("anexadas à conversa NÃO"));
      assertEquals(NOME, FERRAMENTA.nome());
   }
}
