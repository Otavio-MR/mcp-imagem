package br.edu.ifpr.mcp_imagens.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.infra.config.CarregadorDeRecursos;
import br.edu.ifpr.mcp_imagens.infra.config.CoresNomeadas;
import br.edu.ifpr.mcp_imagens.infra.config.CorParser;
import br.edu.ifpr.mcp_imagens.infra.config.TabelaWcag;
import br.edu.ifpr.mcp_imagens.service.CalculadoraDeContrastePadrao;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.JsonNode;

class CalcularContrasteToolTest {
   private static final CarregadorDeRecursos RECURSOS =
                                                     new CarregadorDeRecursos();
   private static final CalcularContrasteTool FERRAMENTA =
         new CalcularContrasteTool(
               new CorParser(RECURSOS.objeto("cores-nomeadas",
                                             CoresNomeadas.class).cores()),
               new CalculadoraDeContrastePadrao(RECURSOS.objeto(
                     "criterios-wcag", TabelaWcag.class).criterios()));

   private static CallToolResult chamar(Map<String, Object> argumentos) {
      return FERRAMENTA.run(null, new CallToolRequest("calcular_contraste",
                                                      argumentos));
   }

   private static JsonNode json(String texto, String fundo) {
      CallToolResult resultado = chamar(Map.of("cor_texto", texto,
                                               "cor_fundo", fundo));

      assertFalse(resultado.isError(), Resultados.texto(resultado));
      return Resultados.json(resultado);
   }

   private static String erro(Map<String, Object> argumentos) {
      CallToolResult resultado = chamar(argumentos);

      assertTrue(resultado.isError());
      return Resultados.texto(resultado);
   }

   @Test
   void pretoSobreBrancoEh21ETudoPassa() {
      JsonNode json = json("#000000", "#FFFFFF");

      assertEquals("#000000", json.get("corTexto").asString());
      assertEquals("#FFFFFF", json.get("corFundo").asString());
      assertEquals(21.0, json.get("razao").asDouble(), 0.001);
      assertEquals(5, json.get("vereditos").size());
      assertTrue(json.get("vereditos").valueStream()
                     .allMatch(v -> v.get("passa").asBoolean()));
   }

   @Test
   void razaoSaiComDuasCasasNoTexto() {
      CallToolResult resultado = chamar(Map.of("cor_texto", "#000000",
                                               "cor_fundo", "#FFFFFF"));

      assertTrue(Resultados.texto(resultado).contains("\"razao\" : 21.00"),
                 Resultados.texto(resultado));
   }

   @Test
   void cinza777777FalhaAANormalMasPassaAAGrande() {
      JsonNode json = json("#777777", "#FFFFFF");

      assertEquals(4.48, json.get("razao").asDouble(), 0.01);
      assertFalse(json.get("vereditos").get(0).get("passa").asBoolean());
      assertEquals("aa_texto_normal",
                   json.get("vereditos").get(0).get("criterio").asString());
      assertEquals(4.5, json.get("vereditos").get(0).get("limiar").asDouble());
      assertTrue(json.get("vereditos").get(1).get("passa").asBoolean());
   }

   @Test
   void aceitaNomesHexCurtoEMaiusculas() {
      JsonNode json = json("Vermelho", "#FFF");

      assertEquals("#FF0000", json.get("corTexto").asString());
      assertEquals("#FFFFFF", json.get("corFundo").asString());
      assertEquals(4.0, json.get("razao").asDouble(), 0.01);
   }

   @Test
   void trocarTextoEFundoDaMesmaRazao() {
      assertEquals(json("#777777", "#FFFFFF").get("razao"),
                   json("#FFFFFF", "#777777").get("razao"));
   }

   @ParameterizedTest
   @ValueSource(strings = { "#GGGGGG", "#12345", "xablau", "FF0000" })
   void corTextoInvalidaIndicaOParametro(String cor) {
      String texto = erro(Map.of("cor_texto", cor, "cor_fundo", "#FFFFFF"));

      assertTrue(texto.startsWith("COR_INVALIDA: parâmetro 'cor_texto': "),
                 texto);
   }

   @Test
   void corFundoInvalidaIndicaOParametro() {
      String texto = erro(Map.of("cor_texto", "#000000",
                                 "cor_fundo", "#12345"));

      assertTrue(texto.startsWith("COR_INVALIDA: parâmetro 'cor_fundo': "),
                 texto);
   }

   @Test
   void parametrosAusentes() {
      assertTrue(erro(Map.of("cor_fundo", "#FFF"))
                    .startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erro(Map.of("cor_texto", "#000"))
                    .startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erro(Map.of()).startsWith("PARAMETRO_AUSENTE: "));
   }

   @Test
   void corEmBrancoEhParametroInvalido() {
      assertTrue(erro(Map.of("cor_texto", "  ", "cor_fundo", "#FFF"))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void corNulaEhParametroAusente() {
      Map<String, Object> argumentos = new HashMap<>();
      argumentos.put("cor_texto", null);
      argumentos.put("cor_fundo", "#FFF");

      assertTrue(erro(argumentos).startsWith("PARAMETRO_AUSENTE: "));
   }

   @Test
   void arquivoNaoEhParametroDestaFerramenta() {
      assertEquals("PARAMETRO_INVALIDO: parâmetro desconhecido: 'arquivo'",
                   erro(Map.of("cor_texto", "#000", "cor_fundo", "#FFF",
                               "arquivo", "a.png")));
   }

   @Test
   void schemaTemSoAsDuasCores() {
      assertEquals(Set.of("cor_texto", "cor_fundo"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("cor_texto", "cor_fundo"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertEquals("calcular_contraste", FERRAMENTA.nome());
   }
}
