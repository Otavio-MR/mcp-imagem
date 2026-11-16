package br.edu.ifpr.mcp_imagens.mcp.tools;

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

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.service.ContadorDePixelsPadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import tools.jackson.databind.JsonNode;

class ProporcaoClaroEscuroToolTest {
   private static final String NOME = "proporcao_claro_escuro";

   // 10x10: 40 brancos (luma 255), 20 #808080 (128), 10 #7F7F7F (127),
   // 20 pretos (0) e 10 transparentes
   private static final Imagem IMAGEM = ImagensSinteticas.gerar(10, 10,
         (x, y) -> y < 4 ? 0xFFFFFFFF
                 : y < 6 ? 0xFF808080
                 : y < 7 ? 0xFF7F7F7F
                 : y < 9 ? 0xFF000000
                 : 0x00000000);

   private static final ProporcaoClaroEscuroTool FERRAMENTA =
         new ProporcaoClaroEscuroTool(RepositorioFalso.com(
               ImagensSinteticas.aberta("a.png", IMAGEM)),
               new ContadorDePixelsPadrao());

   private static JsonNode proporcao(Map<String, Object> argumentos) {
      return json(FERRAMENTA, NOME, argumentos);
   }

   private static String erroCom(Map<String, Object> argumentos) {
      return erro(FERRAMENTA, NOME, argumentos);
   }

   @Test
   void limiarPadraoE128ELumaIgualAoLimiarEClaro() {
      JsonNode json = proporcao(Map.of("arquivo", "a.png"));

      assertEquals(128, json.get("limiar").asInt());
      assertEquals(90, json.get("totalConsiderado").asLong());
      assertEquals(60, json.get("claros").get("quantidade").asLong());
      assertEquals(66.67, json.get("claros").get("percentual").asDouble(), 0);
      assertEquals(30, json.get("escuros").get("quantidade").asLong());
      assertEquals(33.33, json.get("escuros").get("percentual").asDouble(), 0);
   }

   @Test
   void limiar129DeixaOCinza808080DoLadoEscuro() {
      JsonNode json = proporcao(Map.of("arquivo", "a.png", "limiar", 129));

      assertEquals(40, json.get("claros").get("quantidade").asLong());
      assertEquals(50, json.get("escuros").get("quantidade").asLong());
   }

   @Test
   void limitesDoLimiar() {
      assertEquals(90, proporcao(Map.of("arquivo", "a.png", "limiar", 0))
                          .get("claros").get("quantidade").asLong());
      assertEquals(40, proporcao(Map.of("arquivo", "a.png", "limiar", 255))
                          .get("claros").get("quantidade").asLong());
   }

   @Test
   void clarosMaisEscurosDaOTotal() {
      for (int limiar = 0; limiar <= 255; limiar += 5) {
         JsonNode json = proporcao(Map.of("arquivo", "a.png",
                                          "limiar", limiar));

         assertEquals(json.get("totalConsiderado").asLong(),
                      json.get("claros").get("quantidade").asLong() +
                      json.get("escuros").get("quantidade").asLong());
      }
   }

   @Test
   void transparentesIgnoradosOuContadosComoEscuros() {
      JsonNode todos = proporcao(Map.of("arquivo", "a.png",
                                        "ignorar_transparentes", false));

      assertEquals(100, todos.get("totalConsiderado").asLong());
      // o pixel transparente 0x00000000 tem brilho 0
      assertEquals(40, todos.get("escuros").get("quantidade").asLong());
      assertEquals(false, todos.get("ignorarTransparentes").asBoolean());
   }

   @Test
   void imagemTodaTransparenteIgnoradaDaZeroSemErro() {
      ProporcaoClaroEscuroTool vazia = new ProporcaoClaroEscuroTool(
            RepositorioFalso.com(ImagensSinteticas.aberta("v.png",
                  ImagensSinteticas.solida(2, 2, 0x00FFFFFF))),
            new ContadorDePixelsPadrao());

      JsonNode json = json(vazia, NOME, Map.of("arquivo", "v.png"));

      assertEquals(0, json.get("totalConsiderado").asLong());
      assertEquals(0.0, json.get("claros").get("percentual").asDouble(), 0);
      assertEquals(0.0, json.get("escuros").get("percentual").asDouble(), 0);
   }

   @ParameterizedTest
   @ValueSource(ints = { -1, 256 })
   void limiarForaDaFaixa(int limiar) {
      String texto = erroCom(Map.of("arquivo", "a.png", "limiar", limiar));

      assertTrue(texto.startsWith("PARAMETRO_INVALIDO: "), texto);
      assertTrue(texto.contains("entre 0 e 255"), texto);
   }

   @Test
   void erros() {
      assertTrue(erroCom(Map.of()).startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erroCom(Map.of("arquivo", "x.png"))
                    .startsWith("IMAGEM_NAO_ENCONTRADA: "));
      assertTrue(erroCom(Map.of("arquivo", "a.png", "passo", 1))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erroCom(Map.of("arquivo", "x.png", "limiar", 300))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void errosDeAcessoAImagemPropagam() {
      ProporcaoClaroEscuroTool grande = new ProporcaoClaroEscuroTool(
            RepositorioFalso.falhando(new ImagemMuitoGrandeException("g")),
            new ContadorDePixelsPadrao());

      assertEquals("IMAGEM_MUITO_GRANDE: g",
                   erro(grande, NOME, Map.of("arquivo", "a.png")));
   }

   @Test
   void schemaEDescricao() {
      Map<?, ?> propriedades = (Map<?, ?>) FERRAMENTA.schema().comoMapa()
                                                     .get("properties");

      assertEquals(Set.of("arquivo", "limiar", "ignorar_transparentes"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("arquivo"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertEquals(128, ((Map<?, ?>) propriedades.get("limiar"))
                              .get("default"));
      assertTrue(FERRAMENTA.descricao().contains("anexadas à conversa NÃO"));
      assertEquals(NOME, FERRAMENTA.nome());
   }
}
