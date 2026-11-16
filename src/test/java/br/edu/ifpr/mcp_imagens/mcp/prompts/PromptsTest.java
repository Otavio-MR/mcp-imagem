package br.edu.ifpr.mcp_imagens.mcp.prompts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroAusenteException;
import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class PromptsTest {
   private final AnalisarAcessibilidadeDeCoresPrompt analisar =
                                        new AnalisarAcessibilidadeDeCoresPrompt();
   private final DescreverComposicaoDeCoresPrompt    descrever =
                                           new DescreverComposicaoDeCoresPrompt();

   private static GetPromptResult obter(McpPromptDefinition prompt,
                                        String nome,
                                        Map<String, Object> argumentos) {
      return prompt.get(null, new GetPromptRequest(nome, argumentos));
   }

   private static String texto(GetPromptResult resultado) {
      assertEquals(1, resultado.messages().size());
      assertEquals(Role.USER, resultado.messages().get(0).role());
      return ((TextContent) resultado.messages().get(0).content()).text();
   }

   @Test
   void analisarUsaOArquivoEAsFerramentasCertas() {
      GetPromptResult resultado = obter(analisar,
            "analisar_acessibilidade_de_cores", Map.of("arquivo", "banner.png"));
      String texto = texto(resultado);

      assertTrue(texto.contains("'banner.png'"), texto);
      assertTrue(texto.contains("cores_dominantes"), texto);
      assertTrue(texto.contains("calcular_contraste"), texto);
      assertTrue(texto.contains("imagens://criterios-wcag"), texto);
      assertTrue(texto.contains("Afirme apenas o que os números sustentam"),
                 texto);
      assertFalse(resultado.description().isBlank());
   }

   @Test
   void analisarRepeteOArquivoNaChamadaDeCoresDominantes() {
      String texto = texto(obter(analisar, "analisar_acessibilidade_de_cores",
                                 Map.of("arquivo", "foto.jpg")));

      assertTrue(texto.contains("cores_dominantes com arquivo 'foto.jpg'"),
                 texto);
   }

   @Test
   void descreverUsaQuantidadePadraoCinco() {
      String texto = texto(obter(descrever, "descrever_composicao_de_cores",
                                 Map.of("arquivo", "banner.png")));

      assertTrue(texto.contains("quantidade 5"), texto);
      assertTrue(texto.contains("obter_informacoes"), texto);
      assertTrue(texto.contains("cores_dominantes"), texto);
      assertTrue(texto.contains("proporcao_claro_escuro"), texto);
      assertTrue(texto.contains("parágrafo"), texto);
      assertTrue(texto.contains("tabela"), texto);
   }

   @Test
   void descreverAceitaQuantidadeComoTextoOuNumero() {
      assertTrue(texto(obter(descrever, "d", Map.of("arquivo", "a.png",
                                                     "quantidade", "8")))
                    .contains("quantidade 8"));
      assertTrue(texto(obter(descrever, "d", Map.of("arquivo", "a.png",
                                                     "quantidade", 20)))
                    .contains("quantidade 20"));
      assertTrue(texto(obter(descrever, "d", Map.of("arquivo", "a.png",
                                                     "quantidade", 1)))
                    .contains("quantidade 1"));
   }

   @ParameterizedTest
   @ValueSource(strings = { "0", "21", "-1", "abc" })
   void descreverRecusaQuantidadeInvalida(String quantidade) {
      assertThrows(ParametroInvalidoException.class,
                   () -> obter(descrever, "d",
                               Map.of("arquivo", "a.png",
                                      "quantidade", quantidade)));
   }

   @Test
   void arquivoEObrigatorioEnaoPodeSerVazio() {
      assertThrows(ParametroAusenteException.class,
                   () -> obter(analisar, "a", Map.of()));
      assertThrows(ParametroAusenteException.class,
                   () -> obter(descrever, "d", new HashMap<>()));
      assertThrows(ParametroInvalidoException.class,
                   () -> obter(analisar, "a", Map.of("arquivo", " ")));
   }

   @Test
   void argumentoDesconhecidoEhRecusado() {
      ParametroInvalidoException e = assertThrows(
            ParametroInvalidoException.class,
            () -> obter(analisar, "a", Map.of("arquivo", "a.png",
                                              "quantidade", 3)));

      assertEquals("parâmetro desconhecido: 'quantidade'", e.getMessage());
   }

   @Test
   void semArgumentosNenhumDaErroDeArquivoAusente() {
      assertThrows(ParametroAusenteException.class,
                   () -> analisar.get(null, new GetPromptRequest("a", null)));
   }

   @Test
   void argumentosDeclaradosNosPrompts() {
      assertEquals(List.of("arquivo"),
                   analisar.argumentos().stream()
                           .map(a -> a.name()).toList());
      assertEquals(List.of("arquivo", "quantidade"),
                   descrever.argumentos().stream()
                            .map(a -> a.name()).toList());
      assertTrue(descrever.argumentos().get(0).required());
      assertFalse(descrever.argumentos().get(1).required());
   }
}
