package br.edu.ifpr.mcp_imagens.mcp.tools;

import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.chamar;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.erro;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.json;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.BRANCO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.VERMELHO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.infra.config.CorParser;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeCoresPadrao;
import br.edu.ifpr.mcp_imagens.service.ContadorDePixelsPadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import tools.jackson.databind.JsonNode;

class VerificarPaletaToolTest {
   private static final String NOME = "verificar_paleta";

   private static final CorParser PARSER = new CorParser(Map.of(
         "vermelho", "#FF0000", "branco", "#FFFFFF", "azul", "#0000FF"));

   // 10x10: 30 vermelhos, 50 brancos, 10 azuis e 10 #FE0000 (quase vermelho)
   private static final Imagem IMAGEM = ImagensSinteticas.gerar(10, 10,
         (x, y) -> y < 3 ? VERMELHO
                 : y < 8 ? BRANCO
                 : y == 8 ? 0xFF0000FF
                 : 0xFFFE0000);

   // 4 pixels: dois vermelhos transparentes, um branco e um azul
   private static final Imagem COM_ALFA = ImagensSinteticas.de(
         new int[] { 0x00FF0000, 0x00FF0000, BRANCO, 0xFF0000FF });

   private static final VerificarPaletaTool FERRAMENTA = criar(
         RepositorioFalso.com(
               ImagensSinteticas.aberta("a.png", IMAGEM),
               ImagensSinteticas.aberta("alfa.png", COM_ALFA)));

   private static VerificarPaletaTool criar(RepositorioFalso repositorio) {
      return new VerificarPaletaTool(repositorio, PARSER,
            new ContadorDePixelsPadrao(), new AnalisadorDeCoresPadrao());
   }

   private static JsonNode verificar(String paleta) {
      return json(FERRAMENTA, NOME, Map.of("arquivo", "a.png",
                                           "cores_permitidas", paleta));
   }

   private static String erroCom(Map<String, Object> argumentos) {
      return erro(FERRAMENTA, NOME, argumentos);
   }

   @Test
   void percentualDentroEForaDaPaleta() {
      JsonNode json = verificar("vermelho, branco");

      assertEquals(100, json.get("totalConsiderado").asLong());
      assertEquals(80, json.get("dentroDaPaleta").get("quantidade").asLong());
      assertEquals(80.0, json.get("dentroDaPaleta").get("percentual")
                           .asDouble(), 0);
      assertEquals(20, json.get("foraDaPaleta").get("quantidade").asLong());
      assertEquals(20.0, json.get("foraDaPaleta").get("percentual")
                          .asDouble(), 0);
   }

   @Test
   void listaAsCoresExatasForaDaPaletaEmOrdem() {
      JsonNode fora = verificar("vermelho, branco").get("coresForaDaPaleta");

      // empate de 10 pixels: vem primeiro o hex menor
      assertEquals(2, fora.size());
      assertEquals("#0000FF", fora.get(0).get("cor").asString());
      assertEquals(10, fora.get(0).get("quantidade").asLong());
      assertEquals("#FE0000", fora.get(1).get("cor").asString());
      // percentual sobre os pixels fora da paleta (20), e não sobre a imagem
      assertEquals(50.0, fora.get(0).get("percentual").asDouble(), 0);
   }

   @Test
   void toleranciaUmIncluiOPixelQuaseVermelho() {
      JsonNode json = json(FERRAMENTA, NOME, Map.of(
            "arquivo", "a.png", "cores_permitidas", "vermelho, branco",
            "tolerancia", 1));

      assertEquals(90, json.get("dentroDaPaleta").get("quantidade").asLong());
      assertEquals(1, json.get("coresForaDaPaleta").size());
      assertEquals(1, json.get("tolerancia").asInt());
   }

   @Test
   void devolveAPaletaEmHexSemRepetirEmMaiusculas() {
      JsonNode json = verificar("Vermelho, #F00, #ffffff, branco");

      assertEquals(List.of("#FF0000", "#FFFFFF"),
                   json.get("coresPermitidas").valueStream()
                       .map(JsonNode::asString).toList());
   }

   @Test
   void todaAImagemDentroDaPaletaNaoTemCoresFora() {
      JsonNode json = verificar("vermelho, branco, azul, #FE0000");

      assertEquals(100.0, json.get("dentroDaPaleta").get("percentual")
                          .asDouble(), 0);
      assertEquals(0, json.get("foraDaPaleta").get("quantidade").asLong());
      assertEquals(0, json.get("coresForaDaPaleta").size());
   }

   @Test
   void percentualSaiComDuasCasasNoTexto() {
      String texto = Resultados.texto(chamar(FERRAMENTA, NOME, Map.of(
            "arquivo", "a.png", "cores_permitidas", "vermelho, branco")));

      assertTrue(texto.contains("\"percentual\" : 80.00"), texto);
   }

   @Test
   void transparentesIgnoradosPorPadraoOuContados() {
      JsonNode ignorando = json(FERRAMENTA, NOME, Map.of(
            "arquivo", "alfa.png", "cores_permitidas", "branco"));
      JsonNode contando = json(FERRAMENTA, NOME, Map.of(
            "arquivo", "alfa.png", "cores_permitidas", "branco",
            "ignorar_transparentes", false));

      assertEquals(2, ignorando.get("totalConsiderado").asLong());
      assertEquals(1, ignorando.get("dentroDaPaleta").get("quantidade")
                              .asLong());
      assertEquals(4, contando.get("totalConsiderado").asLong());
      assertEquals(3, contando.get("foraDaPaleta").get("quantidade").asLong());
      // o vermelho transparente aparece como cor fora da paleta
      assertEquals("#FF0000", contando.get("coresForaDaPaleta").get(0)
                                      .get("cor").asString());
   }

   @Test
   void imagemTodaTransparenteIgnoradaDaZeroSemErro() {
      VerificarPaletaTool vazia = criar(RepositorioFalso.com(
            ImagensSinteticas.aberta("v.png",
                  ImagensSinteticas.solida(2, 2, 0x00000000))));

      JsonNode json = json(vazia, NOME, Map.of(
            "arquivo", "v.png", "cores_permitidas", "branco"));

      assertEquals(0, json.get("totalConsiderado").asLong());
      assertEquals(0.0, json.get("dentroDaPaleta").get("percentual")
                        .asDouble(), 0);
      assertEquals(0, json.get("coresForaDaPaleta").size());
   }

   @Test
   void corInvalidaIndicaQualItemDaLista() {
      String texto = erroCom(Map.of("arquivo", "a.png",
                                    "cores_permitidas", "vermelho, xablau"));

      assertTrue(texto.startsWith(
            "COR_INVALIDA: parâmetro 'cores_permitidas (item 2)': "), texto);
      assertTrue(texto.contains("'xablau'"), texto);
   }

   @ParameterizedTest
   @ValueSource(strings = { "vermelho,,branco", "vermelho,", ",", " , " })
   void itemVazioNaListaEParametroInvalido(String lista) {
      assertTrue(erroCom(Map.of("arquivo", "a.png",
                                "cores_permitidas", lista))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void limiteDeTrintaCores() {
      String trinta = String.join(",", Collections.nCopies(30, "#FFF"));

      verificar(trinta);

      String texto = erroCom(Map.of("arquivo", "a.png",
                                    "cores_permitidas", trinta + ",#FFF"));

      assertEquals("PARAMETRO_INVALIDO: 'cores_permitidas' aceita no máximo " +
                   "30 cores (recebido: 31)", texto);
   }

   @Test
   void paletaEmBrancoOuAusente() {
      assertTrue(erroCom(Map.of("arquivo", "a.png", "cores_permitidas", " "))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erroCom(Map.of("arquivo", "a.png"))
                    .startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erroCom(Map.of("cores_permitidas", "branco"))
                    .startsWith("PARAMETRO_AUSENTE: "));
   }

   @ParameterizedTest
   @ValueSource(ints = { -1, 256 })
   void toleranciaForaDaFaixa(int tolerancia) {
      String texto = erroCom(Map.of("arquivo", "a.png",
                                    "cores_permitidas", "branco",
                                    "tolerancia", tolerancia));

      assertTrue(texto.startsWith("PARAMETRO_INVALIDO: "), texto);
      assertTrue(texto.contains("entre 0 e 255"), texto);
   }

   @Test
   void parametroDesconhecidoEhRecusado() {
      assertEquals("PARAMETRO_INVALIDO: parâmetro desconhecido: 'cor'",
                   erroCom(Map.of("arquivo", "a.png",
                                  "cores_permitidas", "branco",
                                  "cor", "#FFF")));
   }

   @Test
   void erroDeParametroVemAntesDoErroDeArquivo() {
      assertTrue(erroCom(Map.of("arquivo", "nada.png",
                                "cores_permitidas", "xablau"))
                    .startsWith("COR_INVALIDA: "));
      assertTrue(erroCom(Map.of("arquivo", "nada.png",
                                "cores_permitidas", "branco"))
                    .startsWith("IMAGEM_NAO_ENCONTRADA: "));
   }

   @Test
   void errosDeAcessoAImagemPropagam() {
      VerificarPaletaTool invalida = criar(RepositorioFalso.falhando(
            new ImagemInvalidaException("ruim")));
      Map<String, Object> args = new HashMap<>(Map.of(
            "arquivo", "a.png", "cores_permitidas", "branco"));

      assertEquals("IMAGEM_INVALIDA: ruim", erro(invalida, NOME, args));
   }

   @Test
   void schemaEDescricao() {
      Map<?, ?> propriedades = (Map<?, ?>) FERRAMENTA.schema().comoMapa()
                                                     .get("properties");

      assertEquals(Set.of("arquivo", "cores_permitidas", "tolerancia",
                          "ignorar_transparentes"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("arquivo", "cores_permitidas"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertEquals(0, ((Map<?, ?>) propriedades.get("tolerancia"))
                            .get("default"));
      assertTrue(FERRAMENTA.descricao().contains("anexadas à conversa NÃO"));
      assertTrue(FERRAMENTA.descricao().contains("30"));
      assertEquals(NOME, FERRAMENTA.nome());
   }
}
