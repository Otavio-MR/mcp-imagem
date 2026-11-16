package br.edu.ifpr.mcp_imagens.mcp.tools;

import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.chamar;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.erro;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.json;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.BRANCO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.PRETO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.VERMELHO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.infra.config.CarregadorDeRecursos;
import br.edu.ifpr.mcp_imagens.infra.config.TabelaWcag;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeCoresPadrao;
import br.edu.ifpr.mcp_imagens.service.CalculadoraDeContrastePadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import tools.jackson.databind.JsonNode;

class ContrasteDasCoresDominantesToolTest {
   private static final String NOME = "contraste_das_cores_dominantes";

   private static final int CINZA_767676 = 0xFF767676;

   private static final TabelaWcag WCAG = new CarregadorDeRecursos().objeto(
         "criterios-wcag", TabelaWcag.class);

   /** 10x10 com 70 pixels do fundo, 30 do texto (as 3 últimas linhas). */
   private static Imagem textoSobreFundo(int texto, int fundo) {
      return ImagensSinteticas.gerar(10, 10, (x, y) -> y < 3 ? texto : fundo);
   }

   private static final ContrasteDasCoresDominantesTool FERRAMENTA = criar(
         RepositorioFalso.com(
               ImagensSinteticas.aberta("pb.png",
                     textoSobreFundo(PRETO, BRANCO)),
               ImagensSinteticas.aberta("cinza.png",
                     textoSobreFundo(CINZA_767676, BRANCO)),
               ImagensSinteticas.aberta("lisa.png",
                     ImagensSinteticas.solida(4, 4, 0xFF336699)),
               ImagensSinteticas.aberta("vazia.png",
                     ImagensSinteticas.solida(4, 4, 0x00000000)),
               // 50 brancos, 30 pretos, 20 vermelhos
               ImagensSinteticas.aberta("tres.png",
                     ImagensSinteticas.gerar(10, 10, (x, y) ->
                           y < 5 ? BRANCO : y < 8 ? PRETO : VERMELHO)),
               // 3 transparentes, 2 brancos, 1 preto
               ImagensSinteticas.aberta("alfa.png",
                     ImagensSinteticas.de(new int[] { 0x00FF0000, 0x00FF0000,
                                                      0x00FF0000, BRANCO,
                                                      BRANCO, PRETO }))));

   private static ContrasteDasCoresDominantesTool criar(
                                                     RepositorioFalso repo) {
      return new ContrasteDasCoresDominantesTool(repo,
            new AnalisadorDeCoresPadrao(),
            new CalculadoraDeContrastePadrao(WCAG.criterios()));
   }

   private static JsonNode contraste(Map<String, Object> argumentos) {
      return json(FERRAMENTA, NOME, argumentos);
   }

   private static String erroCom(Map<String, Object> argumentos) {
      return erro(FERRAMENTA, NOME, argumentos);
   }

   private static String avisos(JsonNode json) {
      return String.join("\n", json.get("avisos").valueStream()
                                   .map(JsonNode::asString).toList());
   }

   @Test
   void pretoSobreBrancoDa21() {
      JsonNode json = contraste(Map.of("arquivo", "pb.png",
                                       "passo_de_agrupamento", 1));
      JsonNode c = json.get("contraste");

      assertEquals("#000000", c.get("corTexto").asString());
      assertEquals("#FFFFFF", c.get("corFundo").asString());
      assertEquals(21.0, c.get("razao").asDouble(), 0);
      assertEquals(5, c.get("vereditos").size());
      assertTrue(c.get("vereditos").valueStream()
                  .allMatch(v -> v.get("passa").asBoolean()));
   }

   @Test
   void devolveAsDuasCoresDominantesComQuantidadeEPercentual() {
      JsonNode json = contraste(Map.of("arquivo", "pb.png",
                                       "passo_de_agrupamento", 1));
      JsonNode cores = json.get("coresDominantes");

      assertEquals(100, json.get("totalConsiderado").asLong());
      assertEquals(2, cores.size());
      assertEquals("#FFFFFF", cores.get(0).get("cor").asString());
      assertEquals(70, cores.get(0).get("quantidade").asLong());
      assertEquals(30.0, cores.get(1).get("percentual").asDouble(), 0);
   }

   @Test
   void cinza767676SobreBrancoPassaAANormal() {
      JsonNode c = contraste(Map.of("arquivo", "cinza.png",
                                    "passo_de_agrupamento", 1))
                           .get("contraste");

      assertEquals(4.54, c.get("razao").asDouble(), 0.001);
      assertTrue(c.get("vereditos").get(0).get("passa").asBoolean());
   }

   @Test
   void sempreAvisaQueEAproximacao() {
      String texto = avisos(contraste(Map.of("arquivo", "pb.png",
                                             "passo_de_agrupamento", 1)));

      assertTrue(texto.contains("Aproximação"), texto);
      assertTrue(texto.contains("não detecta o texto"), texto);
   }

   @Test
   void passoMaiorQueUmAvisaDosGruposESaiPeloCentroDaFaixa() {
      JsonNode json = contraste(Map.of("arquivo", "pb.png"));

      assertEquals(32, json.get("passoDeAgrupamento").asInt());
      assertEquals(2, json.get("avisos").size());
      assertTrue(avisos(json).contains("centro de um grupo"), avisos(json));
      assertEquals("#F0F0F0", json.get("contraste").get("corFundo")
                                  .asString());
      assertEquals("#101010", json.get("contraste").get("corTexto")
                                  .asString());
      // as cores aproximadas dão um contraste menor que o real (21)
      assertTrue(json.get("contraste").get("razao").asDouble() < 21.0);
   }

   @Test
   void passoUmNaoTemAvisoDeGrupos() {
      JsonNode json = contraste(Map.of("arquivo", "pb.png",
                                       "passo_de_agrupamento", 1));

      assertEquals(1, json.get("avisos").size());
      assertFalse(avisos(json).contains("centro de um grupo"));
   }

   @Test
   void usaSoAsDuasMaisFrequentesQuandoHaTres() {
      JsonNode json = contraste(Map.of("arquivo", "tres.png",
                                       "passo_de_agrupamento", 1));

      assertEquals(2, json.get("coresDominantes").size());
      assertEquals("#FFFFFF", json.get("contraste").get("corFundo")
                                  .asString());
      assertEquals("#000000", json.get("contraste").get("corTexto")
                                  .asString());
   }

   @Test
   void imagemDeUmaCorSoTemContraste1EAviso() {
      JsonNode json = contraste(Map.of("arquivo", "lisa.png",
                                       "passo_de_agrupamento", 1));

      assertEquals(1, json.get("coresDominantes").size());
      assertEquals(1.0, json.get("contraste").get("razao").asDouble(), 0);
      assertEquals(json.get("contraste").get("corTexto"),
                   json.get("contraste").get("corFundo"));
      assertTrue(avisos(json).contains("única cor dominante"), avisos(json));
      assertTrue(json.get("contraste").get("vereditos").valueStream()
                     .noneMatch(v -> v.get("passa").asBoolean()));
   }

   @Test
   void imagemTodaTransparenteNaoTemContraste() {
      JsonNode json = contraste(Map.of("arquivo", "vazia.png"));

      assertEquals(0, json.get("totalConsiderado").asLong());
      assertEquals(0, json.get("coresDominantes").size());
      assertTrue(json.get("contraste").isNull());
      assertTrue(avisos(json).contains("toda transparente"), avisos(json));
   }

   @Test
   void transparentesNaoEntramNaConta() {
      JsonNode json = contraste(Map.of("arquivo", "alfa.png",
                                       "passo_de_agrupamento", 1));

      assertEquals(3, json.get("totalConsiderado").asLong());
      assertEquals("#FFFFFF", json.get("contraste").get("corFundo")
                                  .asString());
   }

   @Test
   void razaoSaiComDuasCasasNoTexto() {
      String texto = Resultados.texto(chamar(FERRAMENTA, NOME, Map.of(
            "arquivo", "pb.png", "passo_de_agrupamento", 1)));

      assertTrue(texto.contains("\"razao\" : 21.00"), texto);
   }

   @ParameterizedTest
   @ValueSource(ints = { 0, 129, -3 })
   void passoForaDaFaixa(int passo) {
      String texto = erroCom(Map.of("arquivo", "pb.png",
                                    "passo_de_agrupamento", passo));

      assertTrue(texto.startsWith("PARAMETRO_INVALIDO: "), texto);
      assertTrue(texto.contains("entre 1 e 128"), texto);
   }

   @Test
   void limitesDoPassoSaoAceitos() {
      contraste(Map.of("arquivo", "pb.png", "passo_de_agrupamento", 1));
      contraste(Map.of("arquivo", "pb.png", "passo_de_agrupamento", 128));
   }

   @Test
   void erros() {
      assertTrue(erroCom(Map.of()).startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erroCom(Map.of("arquivo", " "))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erroCom(Map.of("arquivo", "nada.png"))
                    .startsWith("IMAGEM_NAO_ENCONTRADA: "));
      assertTrue(erroCom(Map.of("arquivo", "pb.png", "quantidade", 3))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erroCom(Map.of("arquivo", "nada.png",
                                "passo_de_agrupamento", 0))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void errosDeAcessoAImagemPropagam() {
      ContrasteDasCoresDominantesTool grande = criar(RepositorioFalso
            .falhando(new ImagemMuitoGrandeException("g")));

      assertEquals("IMAGEM_MUITO_GRANDE: g",
                   erro(grande, NOME, Map.of("arquivo", "a.png")));
   }

   @Test
   void schemaEDescricao() {
      Map<?, ?> propriedades = (Map<?, ?>) FERRAMENTA.schema().comoMapa()
                                                     .get("properties");

      assertEquals(Set.of("arquivo", "passo_de_agrupamento"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("arquivo"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertEquals(32, ((Map<?, ?>) propriedades.get("passo_de_agrupamento"))
                             .get("default"));
      assertTrue(FERRAMENTA.descricao().contains("APROXIMAÇÃO"));
      assertTrue(FERRAMENTA.descricao().contains("anexadas à conversa NÃO"));
      assertEquals(NOME, FERRAMENTA.nome());
   }
}
