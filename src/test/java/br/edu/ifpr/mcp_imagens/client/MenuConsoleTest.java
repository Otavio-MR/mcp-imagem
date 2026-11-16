package br.edu.ifpr.mcp_imagens.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema.Tool;

class MenuConsoleTest {
   private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
   private final MenuConsole menu = new MenuConsole(
         new PrintStream(bytes, true, StandardCharsets.UTF_8));

   private static Tool ferramenta(String nome, String descricao) {
      return Tool.builder(nome, Map.of("type", "object"))
                 .description(descricao).build();
   }

   private final Tool primeira = ferramenta("listar_imagens",
         "Lista as imagens. Segunda frase que não deve aparecer.");
   private final Tool segunda  = ferramenta("Calcular_Contraste", "Contraste.");

   private String texto() {
      return bytes.toString(StandardCharsets.UTF_8);
   }

   @Test
   void mostraNumeroNomeEPrimeiraFraseDaDescricao() {
      menu.mostrar(List.of(primeira, segunda));

      assertTrue(texto().contains(" 1 - listar_imagens: Lista as imagens."),
                 texto());
      assertTrue(texto().contains(" 2 - Calcular_Contraste: Contraste."),
                 texto());
      assertTrue(!texto().contains("Segunda frase"), texto());
      assertTrue(texto().contains("listar          - atualizar"), texto());
      assertTrue(texto().contains("recursos        - listar"), texto());
      assertTrue(texto().contains("prompt <n/nome>  - montar"), texto());
      assertTrue(texto().contains("sair            - encerrar"), texto());
   }

   @Test
   void descricaoLongaSemPontoViraResumoComReticencias() {
      menu.mostrar(List.of(ferramenta("t", "x".repeat(200))));

      String linha = texto().lines().filter(l -> l.startsWith(" 1 - "))
                            .findFirst().orElseThrow();

      assertTrue(linha.endsWith("..."), linha);
      assertEquals(" 1 - t: ".length() + 90, linha.length());
   }

   @Test
   void ferramentaSemDescricaoMostraSoONome() {
      menu.mostrar(List.of(Tool.builder("t", Map.of("type", "object"))
                               .build()));

      assertTrue(texto().contains(" 1 - t\n") || texto().contains(" 1 - t\r\n"),
                 texto());
   }

   @Test
   void listaVaziaAvisa() {
      menu.mostrar(List.of());

      assertTrue(texto().contains("Nenhuma ferramenta disponibilizada."),
                 texto());
   }

   @Test
   void escolhePeloNumeroAPartirDeUm() {
      List<Tool> ferramentas = List.of(primeira, segunda);

      assertEquals(primeira, menu.escolher(ferramentas, "1").orElseThrow());
      assertEquals(segunda, menu.escolher(ferramentas, "2").orElseThrow());
   }

   @Test
   void numeroForaDaListaNaoEscolhe() {
      List<Tool> ferramentas = List.of(primeira, segunda);

      assertTrue(menu.escolher(ferramentas, "0").isEmpty());
      assertTrue(menu.escolher(ferramentas, "3").isEmpty());
      assertTrue(menu.escolher(ferramentas, "-1").isEmpty());
   }

   @Test
   void escolhePeloNomeSemDiferenciarMaiusculas() {
      List<Tool> ferramentas = List.of(primeira, segunda);

      assertEquals(segunda,
                   menu.escolher(ferramentas, "calcular_contraste")
                       .orElseThrow());
      assertEquals(primeira,
                   menu.escolher(ferramentas, "LISTAR_IMAGENS").orElseThrow());
   }

   @Test
   void nomeDesconhecidoNaoEscolhe() {
      assertTrue(menu.escolher(List.of(primeira), "xablau").isEmpty());
   }
}
