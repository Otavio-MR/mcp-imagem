package br.edu.ifpr.mcp_imagens.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.Tool;

class ArgumentPrompterTest {
   private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
   private final PrintStream saida = new PrintStream(bytes, true,
                                                     StandardCharsets.UTF_8);

   /** Ferramenta com arquivo (obrigatório), tolerancia e ignorar (opcionais). */
   private static Tool ferramentaCompleta() {
      Map<String, Object> propriedades = new LinkedHashMap<>();

      propriedades.put("arquivo", Map.of("type", "string",
                                         "description", "Nome do arquivo"));
      propriedades.put("tolerancia", Map.of("type", "integer",
                                            "description", "Tolerância",
                                            "minimum", 0, "maximum", 255,
                                            "default", 0));
      propriedades.put("ignorar", Map.of("type", "boolean",
                                         "description", "Ignorar"));

      return Tool.builder("t", Map.of("type", "object",
                                      "properties", propriedades,
                                      "required", List.of("arquivo")))
                 .description("d")
                 .build();
   }

   private Map<String, Object> pedir(Tool ferramenta, String digitado) {
      ArgumentPrompter prompter = new ArgumentPrompter(new Scanner(digitado),
            saida, new ConversorDeValores());

      return prompter.pedir(ferramenta);
   }

   private String texto() {
      return bytes.toString(StandardCharsets.UTF_8);
   }

   @Test
   void convertePeloTipoDoSchemaEMantemAOrdem() {
      Map<String, Object> argumentos = pedir(ferramentaCompleta(),
                                             "banner.png\n10\nsim\n");

      assertEquals(List.of("arquivo", "tolerancia", "ignorar"),
                   List.copyOf(argumentos.keySet()));
      assertEquals("banner.png", argumentos.get("arquivo"));
      assertEquals(10, argumentos.get("tolerancia"));
      assertEquals(true, argumentos.get("ignorar"));
   }

   @Test
   void opcionalEmBrancoNaoEntraNosArgumentos() {
      Map<String, Object> argumentos = pedir(ferramentaCompleta(),
                                             "banner.png\n\n\n");

      assertEquals(Map.of("arquivo", "banner.png"), argumentos);
   }

   @Test
   void obrigatorioEmBrancoPerguntaDeNovo() {
      Map<String, Object> argumentos = pedir(ferramentaCompleta(),
                                             "\n  \nbanner.png\n\n\n");

      assertEquals("banner.png", argumentos.get("arquivo"));
      assertTrue(texto().contains("Este parâmetro é obrigatório."), texto());
   }

   @Test
   void valorInvalidoMostraMensagemEPerguntaDeNovo() {
      Map<String, Object> argumentos = pedir(ferramentaCompleta(),
                                             "a.png\nabc\n7\n\n");

      assertEquals(7, argumentos.get("tolerancia"));
      assertTrue(texto().contains("Valor inválido: informe um número inteiro"),
                 texto());
   }

   @Test
   void mostraTipoDescricaoPadraoEFaixa() {
      pedir(ferramentaCompleta(), "a.png\n\n\n");

      assertTrue(texto().contains("'arquivo' [string] (obrigatório)"),
                 texto());
      assertTrue(texto().contains("'tolerancia' [integer] (opcional) " +
                                  "[padrão: 0] [de 0 a 255]"), texto());
      assertTrue(texto().contains("Descrição: Nome do arquivo"), texto());
   }

   @Test
   void fimDaEntradaEncerraSemTravar() {
      Map<String, Object> argumentos = pedir(ferramentaCompleta(),
                                             "banner.png\n");

      assertEquals(Map.of("arquivo", "banner.png"), argumentos);
   }

   @Test
   void ferramentaSemParametrosNaoPergunta() {
      Tool semParametros = Tool.builder("t", Map.of("type", "object",
                                        "properties", Map.of()))
                               .description("d").build();

      assertEquals(Map.of(), pedir(semParametros, ""));
      assertEquals("", texto());
   }

   @Test
   void schemaSemPropriedadesNaoPergunta() {
      Tool vazia = Tool.builder("t", Map.of("type", "object"))
                       .description("d").build();

      assertEquals(Map.of(), pedir(vazia, ""));
   }

   @Test
   void pedeOsArgumentosDeUmPromptComoTexto() {
      Prompt prompt = new Prompt("p", "d", List.of(
            new PromptArgument("arquivo", "Nome do arquivo", true),
            new PromptArgument("quantidade", "Quantas cores", false)));
      ArgumentPrompter prompter = new ArgumentPrompter(
            new Scanner("banner.png\n7\n"), saida, new ConversorDeValores());

      Map<String, Object> argumentos = prompter.pedir(prompt);

      // argumentos de prompt sempre seguem como texto
      assertEquals("banner.png", argumentos.get("arquivo"));
      assertEquals("7", argumentos.get("quantidade"));
      assertTrue(texto().contains("'arquivo' [string] (obrigatório)"), texto());
      assertTrue(texto().contains("'quantidade' [string] (opcional)"),
                 texto());
   }

   @Test
   void promptSemArgumentosNaoPergunta() {
      ArgumentPrompter prompter = new ArgumentPrompter(new Scanner(""), saida,
                                                       new ConversorDeValores());

      assertEquals(Map.of(), prompter.pedir(new Prompt("p", "d", List.of())));
      assertEquals("", texto());
   }
}
