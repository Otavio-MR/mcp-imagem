package br.edu.ifpr.mcp_imagens.mcp.tools;

import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.chamar;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.erro;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.json;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.BRANCO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.VERMELHO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.infra.config.CorParser;
import br.edu.ifpr.mcp_imagens.service.ContadorDePixelsPadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.JsonNode;

class ContarPixelsDaCorToolTest {
   private static final CorParser PARSER = new CorParser(Map.of(
         "vermelho", "#FF0000", "branco", "#FFFFFF"));

   // 10x10: 30 vermelhos (3 primeiras linhas) e 70 brancos
   private static final Imagem TRINTA_VERMELHOS = ImagensSinteticas.gerar(
         10, 10, (x, y) -> y < 3 ? VERMELHO : BRANCO);
   // igual, mas o pixel (0, 0) é #FE0000
   private static final Imagem QUASE_VERMELHO = ImagensSinteticas.gerar(
         10, 10, (x, y) -> x == 0 && y == 0 ? 0xFFFE0000
                                            : y < 3 ? VERMELHO : BRANCO);
   // alfa 0, 128 e 255, todos vermelhos
   private static final Imagem TRES_ALFAS = ImagensSinteticas.de(
         new int[] { 0x00FF0000, 0x80FF0000, 0xFFFF0000 });

   private static final ContarPixelsDaCorTool FERRAMENTA =
         new ContarPixelsDaCorTool(RepositorioFalso.com(
               ImagensSinteticas.aberta("trinta.png", TRINTA_VERMELHOS),
               ImagensSinteticas.aberta("quase.png", QUASE_VERMELHO),
               ImagensSinteticas.aberta("alfas.png", TRES_ALFAS)),
               PARSER, new ContadorDePixelsPadrao());

   private static JsonNode contar(Map<String, Object> argumentos) {
      return json(FERRAMENTA, "contar_pixels_da_cor", argumentos);
   }

   private static String erroCom(Map<String, Object> argumentos) {
      return erro(FERRAMENTA, "contar_pixels_da_cor", argumentos);
   }

   @Nested
   class Contagem {
      @Test
      void trintaVermelhosEmCem() {
         JsonNode json = contar(Map.of("arquivo", "trinta.png",
                                       "cor", "#FF0000"));

         assertEquals(30, json.get("quantidade").asLong());
         assertEquals(100, json.get("totalConsiderado").asLong());
         assertEquals(30.0, json.get("percentual").asDouble(), 0.0);
      }

      @Test
      void percentualSaiComDuasCasasNoTexto() {
         CallToolResult resultado = chamar(FERRAMENTA, "contar_pixels_da_cor",
               Map.of("arquivo", "trinta.png", "cor", "#FF0000"));

         assertTrue(Resultados.texto(resultado)
                              .contains("\"percentual\" : 30.00"),
                    Resultados.texto(resultado));
      }

      @Test
      void brancosSaoOsOutrosSetenta() {
         assertEquals(70, contar(Map.of("arquivo", "trinta.png",
                                        "cor", "#FFFFFF"))
                              .get("quantidade").asLong());
      }

      @Test
      void aceitaNomeEHexCurtoEMaiusculas() {
         for (String cor : List.of("vermelho", "Vermelho", "VERMELHO", "#F00",
                                   "#ff0000"))
            assertEquals(30, contar(Map.of("arquivo", "trinta.png",
                                           "cor", cor))
                                  .get("quantidade").asLong(), cor);
      }

      @Test
      void devolveOsParametrosUsadosParaConferencia() {
         JsonNode json = contar(Map.of("arquivo", "trinta.png",
                                       "cor", "vermelho", "tolerancia", 4));

         assertEquals("trinta.png", json.get("arquivo").asString());
         assertEquals("#FF0000", json.get("cor").asString());
         assertEquals(4, json.get("tolerancia").asInt());
         assertEquals(true, json.get("ignorarTransparentes").asBoolean());
      }

      @Test
      void corAusenteDaImagemDaZero() {
         JsonNode json = contar(Map.of("arquivo", "trinta.png",
                                       "cor", "#0000FF"));

         assertEquals(0, json.get("quantidade").asLong());
         assertEquals(0.0, json.get("percentual").asDouble(), 0.0);
      }
   }

   @Nested
   class Tolerancia {
      @Test
      void padraoEZeroEEntaoPixelQuaseVermelhoNaoEntra() {
         JsonNode json = contar(Map.of("arquivo", "quase.png",
                                       "cor", "#FF0000"));

         assertEquals(29, json.get("quantidade").asLong());
         assertEquals(0, json.get("tolerancia").asInt());
      }

      @Test
      void aPartirDeUmEntraOPixelFE0000() {
         assertEquals(30, contar(Map.of("arquivo", "quase.png",
                                        "cor", "#FF0000", "tolerancia", 1))
                               .get("quantidade").asLong());
      }

      @Test
      void limitesMinimoEMaximoSaoAceitos() {
         assertEquals(29, contar(Map.of("arquivo", "quase.png",
                                        "cor", "#FF0000", "tolerancia", 0))
                               .get("quantidade").asLong());
         assertEquals(100, contar(Map.of("arquivo", "quase.png",
                                         "cor", "#FF0000", "tolerancia", 255))
                                .get("quantidade").asLong());
      }

      @ParameterizedTest
      @ValueSource(ints = { -1, 256, 1000 })
      void foraDaFaixaEhParametroInvalido(int tolerancia) {
         String texto = erroCom(Map.of("arquivo", "quase.png",
                                       "cor", "#FF0000",
                                       "tolerancia", tolerancia));

         assertTrue(texto.startsWith("PARAMETRO_INVALIDO: "), texto);
         assertTrue(texto.contains("entre 0 e 255"), texto);
      }

      @Test
      void quePareceNumeroEscritoComoTextoEAceito() {
         assertEquals(30, contar(Map.of("arquivo", "quase.png",
                                        "cor", "#FF0000", "tolerancia", "1"))
                               .get("quantidade").asLong());
      }

      @ParameterizedTest
      @ValueSource(strings = { "abc", "1.5", "" })
      void naoNumericaEhParametroInvalido(String tolerancia) {
         assertTrue(erroCom(Map.of("arquivo", "quase.png", "cor", "#FF0000",
                                   "tolerancia", tolerancia))
                       .startsWith("PARAMETRO_INVALIDO: "));
      }
   }

   @Nested
   class Transparencia {
      @Test
      void porPadraoIgnoraOsTransparentes() {
         JsonNode json = contar(Map.of("arquivo", "alfas.png",
                                       "cor", "#FF0000"));

         assertEquals(2, json.get("quantidade").asLong());
         assertEquals(2, json.get("totalConsiderado").asLong());
         assertEquals(100.0, json.get("percentual").asDouble(), 0.0);
      }

      @Test
      void semIgnorarContaTodosPoisOAlfaNaoEntraNaComparacao() {
         JsonNode json = contar(Map.of("arquivo", "alfas.png",
                                       "cor", "#FF0000",
                                       "ignorar_transparentes", false));

         assertEquals(3, json.get("quantidade").asLong());
         assertEquals(3, json.get("totalConsiderado").asLong());
         assertEquals(false, json.get("ignorarTransparentes").asBoolean());
      }

      @Test
      void aceitaBooleanoEscritoComoTexto() {
         assertEquals(3, contar(Map.of("arquivo", "alfas.png", "cor", "#FF0000",
                                       "ignorar_transparentes", "false"))
                               .get("totalConsiderado").asLong());
      }

      @Test
      void booleanoInvalidoEhParametroInvalido() {
         assertTrue(erroCom(Map.of("arquivo", "alfas.png", "cor", "#FF0000",
                                   "ignorar_transparentes", "talvez"))
                       .startsWith("PARAMETRO_INVALIDO: "));
      }

      @Test
      void imagemTodaTransparenteIgnoradaDaTotalZeroSemErro() {
         ContarPixelsDaCorTool vazia = new ContarPixelsDaCorTool(
               RepositorioFalso.com(ImagensSinteticas.aberta("v.png",
                     ImagensSinteticas.solida(3, 3, 0x00000000))),
               PARSER, new ContadorDePixelsPadrao());

         JsonNode json = json(vazia, "contar_pixels_da_cor",
                              Map.of("arquivo", "v.png", "cor", "#FF0000"));

         assertEquals(0, json.get("totalConsiderado").asLong());
         assertEquals(0.0, json.get("percentual").asDouble(), 0.0);
      }
   }

   @Nested
   class Erros {
      @Test
      void corInvalidaIndicaOParametroCor() {
         String texto = erroCom(Map.of("arquivo", "trinta.png",
                                       "cor", "#GGGGGG"));

         assertTrue(texto.startsWith("COR_INVALIDA: parâmetro 'cor': "), texto);
      }

      @Test
      void nomeDeCorDesconhecido() {
         assertTrue(erroCom(Map.of("arquivo", "trinta.png", "cor", "xablau"))
                       .startsWith("COR_INVALIDA: "));
      }

      @Test
      void parametrosObrigatoriosAusentes() {
         assertTrue(erroCom(Map.of("cor", "#FF0000"))
                       .startsWith("PARAMETRO_AUSENTE: "));
         assertTrue(erroCom(Map.of("arquivo", "trinta.png"))
                       .startsWith("PARAMETRO_AUSENTE: "));
      }

      @Test
      void obrigatoriosEmBranco() {
         assertTrue(erroCom(Map.of("arquivo", " ", "cor", "#FF0000"))
                       .startsWith("PARAMETRO_INVALIDO: "));
         assertTrue(erroCom(Map.of("arquivo", "trinta.png", "cor", " "))
                       .startsWith("PARAMETRO_INVALIDO: "));
      }

      @Test
      void parametroDesconhecidoEhRecusado() {
         assertEquals("PARAMETRO_INVALIDO: parâmetro desconhecido: 'limiar'",
                      erroCom(Map.of("arquivo", "trinta.png", "cor", "#FF0000",
                                     "limiar", 1)));
      }

      @Test
      void imagemInexistente() {
         assertTrue(erroCom(Map.of("arquivo", "nada.png", "cor", "#FF0000"))
                       .startsWith("IMAGEM_NAO_ENCONTRADA: "));
      }

      @Test
      void erroDeParametroVemAntesDoErroDeArquivo() {
         // a cor é inválida e o arquivo não existe: vale o erro da cor
         assertTrue(erroCom(Map.of("arquivo", "nada.png", "cor", "#GGGGGG"))
                       .startsWith("COR_INVALIDA: "));
      }

      @Test
      void errosDeAcessoAImagemChegamComOCodigoDeles() {
         ContarPixelsDaCorTool invalida = new ContarPixelsDaCorTool(
               RepositorioFalso.falhando(new ImagemInvalidaException("ruim")),
               PARSER, new ContadorDePixelsPadrao());
         ContarPixelsDaCorTool grande = new ContarPixelsDaCorTool(
               RepositorioFalso.falhando(new ImagemMuitoGrandeException("g")),
               PARSER, new ContadorDePixelsPadrao());
         Map<String, Object> args = new HashMap<>(Map.of("arquivo", "a.png",
                                                         "cor", "#FF0000"));

         assertEquals("IMAGEM_INVALIDA: ruim",
                      erro(invalida, "contar_pixels_da_cor", args));
         assertEquals("IMAGEM_MUITO_GRANDE: g",
                      erro(grande, "contar_pixels_da_cor", args));
      }
   }

   @Test
   void schemaTemOsQuatroParametrosComPadroes() {
      Map<?, ?> propriedades = (Map<?, ?>) FERRAMENTA.schema().comoMapa()
                                                     .get("properties");

      assertEquals(Set.of("arquivo", "cor", "tolerancia",
                          "ignorar_transparentes"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("arquivo", "cor"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertEquals(0, ((Map<?, ?>) propriedades.get("tolerancia"))
                            .get("default"));
      assertEquals(255, ((Map<?, ?>) propriedades.get("tolerancia"))
                              .get("maximum"));
      assertEquals(true, ((Map<?, ?>) propriedades
                             .get("ignorar_transparentes")).get("default"));
   }

   @Test
   void descricaoAvisaDaPastaDeImagens() {
      String descricao = FERRAMENTA.descricao();

      assertTrue(descricao.contains("pasta de imagens"));
      assertTrue(descricao.contains("listar_imagens"));
      assertTrue(descricao.contains("anexadas à conversa NÃO"));
      assertEquals("contar_pixels_da_cor", FERRAMENTA.nome());
   }
}
