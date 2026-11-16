package br.edu.ifpr.mcp_imagens.mcp.tools;

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
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeCoresPadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import tools.jackson.databind.JsonNode;

class CoresDominantesToolTest {
   private static final String NOME = "cores_dominantes";

   // 10x10: 30 vermelhos, 60 brancos, 10 transparentes (última linha)
   private static final Imagem IMAGEM = ImagensSinteticas.gerar(10, 10,
         (x, y) -> y == 9 ? 0x00000000 : y < 3 ? VERMELHO : BRANCO);

   private static final CoresDominantesTool FERRAMENTA =
         new CoresDominantesTool(RepositorioFalso.com(
               ImagensSinteticas.aberta("a.png", IMAGEM)),
               new AnalisadorDeCoresPadrao());

   private static JsonNode cores(Map<String, Object> argumentos) {
      return json(FERRAMENTA, NOME, argumentos);
   }

   private static String erroCom(Map<String, Object> argumentos) {
      return erro(FERRAMENTA, NOME, argumentos);
   }

   @Test
   void devolveAsCoresMaisFrequentesComPercentual() {
      JsonNode json = cores(Map.of("arquivo", "a.png", "passo", 1));
      JsonNode lista = json.get("cores");

      assertEquals(2, lista.size());
      assertEquals("#FFFFFF", lista.get(0).get("cor").asString());
      assertEquals(60, lista.get(0).get("quantidade").asLong());
      assertEquals(66.67, lista.get(0).get("percentual").asDouble(), 0.0);
      assertEquals("#FF0000", lista.get(1).get("cor").asString());
      assertEquals(30, lista.get(1).get("quantidade").asLong());
   }

   @Test
   void totalIgnoraTransparentesPorPadrao() {
      JsonNode json = cores(Map.of("arquivo", "a.png"));

      assertEquals(90, json.get("totalConsiderado").asLong());
      assertEquals(2, json.get("gruposDistintos").asInt());
      assertEquals(true, json.get("ignorarTransparentes").asBoolean());
   }

   @Test
   void semIgnorarTransparentesAparecemComoPreto() {
      JsonNode json = cores(Map.of("arquivo", "a.png", "passo", 1,
                                   "ignorar_transparentes", false));

      assertEquals(100, json.get("totalConsiderado").asLong());
      assertEquals(3, json.get("gruposDistintos").asInt());
   }

   @Test
   void padroesSaoCincoCoresEPasso32() {
      JsonNode json = cores(Map.of("arquivo", "a.png"));

      assertEquals(32, json.get("passo").asInt());
      // com passo 32 as cores saem pelo centro da faixa
      assertEquals("#F0F0F0", json.get("cores").get(0).get("cor").asString());
   }

   @Test
   void quantidadeLimitaALista() {
      assertEquals(1, cores(Map.of("arquivo", "a.png", "quantidade", 1))
                          .get("cores").size());
   }

   @ParameterizedTest
   @ValueSource(ints = { 0, 21, -1 })
   void quantidadeForaDaFaixa(int quantidade) {
      String texto = erroCom(Map.of("arquivo", "a.png",
                                    "quantidade", quantidade));

      assertTrue(texto.startsWith("PARAMETRO_INVALIDO: "), texto);
      assertTrue(texto.contains("entre 1 e 20"), texto);
   }

   @ParameterizedTest
   @ValueSource(ints = { 0, 129, -5 })
   void passoForaDaFaixa(int passo) {
      String texto = erroCom(Map.of("arquivo", "a.png", "passo", passo));

      assertTrue(texto.startsWith("PARAMETRO_INVALIDO: "), texto);
      assertTrue(texto.contains("entre 1 e 128"), texto);
   }

   @Test
   void limitesSaoAceitos() {
      cores(Map.of("arquivo", "a.png", "quantidade", 1, "passo", 1));
      cores(Map.of("arquivo", "a.png", "quantidade", 20, "passo", 128));
   }

   @Test
   void erros() {
      assertTrue(erroCom(Map.of()).startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erroCom(Map.of("arquivo", " "))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erroCom(Map.of("arquivo", "x.png"))
                    .startsWith("IMAGEM_NAO_ENCONTRADA: "));
      assertTrue(erroCom(Map.of("arquivo", "a.png", "cor", "#000"))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erroCom(Map.of("arquivo", "a.png",
                                "ignorar_transparentes", "talvez"))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void erroDeParametroVemAntesDoErroDeArquivo() {
      assertTrue(erroCom(Map.of("arquivo", "nada.png", "passo", 0))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void errosDeAcessoAImagemPropagam() {
      CoresDominantesTool invalida = new CoresDominantesTool(
            RepositorioFalso.falhando(new ImagemInvalidaException("ruim")),
            new AnalisadorDeCoresPadrao());

      assertEquals("IMAGEM_INVALIDA: ruim",
                   erro(invalida, NOME, Map.of("arquivo", "a.png")));
   }

   @Test
   void schemaEDescricao() {
      Map<?, ?> propriedades = (Map<?, ?>) FERRAMENTA.schema().comoMapa()
                                                     .get("properties");

      assertEquals(Set.of("arquivo", "quantidade", "passo",
                          "ignorar_transparentes"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("arquivo"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertEquals(5, ((Map<?, ?>) propriedades.get("quantidade"))
                            .get("default"));
      assertEquals(32, ((Map<?, ?>) propriedades.get("passo")).get("default"));
      assertTrue(FERRAMENTA.descricao().contains("pasta de imagens"));
      assertTrue(FERRAMENTA.descricao().contains("anexadas à conversa NÃO"));
      assertEquals(NOME, FERRAMENTA.nome());
   }
}
