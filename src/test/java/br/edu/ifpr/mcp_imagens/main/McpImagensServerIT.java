package br.edu.ifpr.mcp_imagens.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_imagens.testutil.ImagensDeTeste;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testes de integração: sobem o JAR real do servidor (gerado por
 * {@code mvn clean verify -Pserver}) e chamam as ferramentas pelo protocolo
 * MCP, como fariam o Claude Desktop ou o MCP Inspector.
 * <p>
 * As imagens usadas são geradas com o {@code ImageIO} em uma pasta temporária
 * e passadas ao servidor pela variável de ambiente {@code IMG_DIR}.
 */
class McpImagensServerIT {
   private static final Logger LOG =
                                  LoggerFactory.getLogger(McpImagensServerIT.class);
   // nome e caminho para o servidor MCP
   private static final String SERVER_PATH = "server/mcp_imagens_server.jar";

   private static Path          pastaDeImagens;
   private static McpSyncClient client;

   @BeforeAll
   static void iniciarServidor() throws Exception {
      pastaDeImagens = Files.createTempDirectory("mcp-imagens-it");

      ImagensDeTeste.criar(pastaDeImagens, "banner.png", 20, 10, 0xFF202020);
      ImagensDeTeste.criar(pastaDeImagens, "icone.PNG", 8, 8, 0x00000000);
      ImagensDeTeste.criar(pastaDeImagens, "foto.jpg", 16, 16, 0xFF808080);
      ImagensDeTeste.criar(pastaDeImagens, "animacao.gif", 4, 4, 0xFFFF0000);
      ImagensDeTeste.criar(pastaDeImagens, "tela.bmp", 6, 6, 0xFFFFFFFF);
      Files.writeString(pastaDeImagens.resolve("notas.txt"), "não é imagem");

      File serverJar = new File(SERVER_PATH);
      assertTrue(serverJar.isFile(), "JAR do servidor não encontrado: " +
                                     serverJar.getAbsolutePath());

      ServerParameters params = ServerParameters.builder("java")
            .args("-jar", serverJar.getAbsolutePath())
            .addEnvVar("IMG_DIR", pastaDeImagens.toString())
            .build();
      StdioClientTransport transport = new StdioClientTransport(
                         params, new JacksonMcpJsonMapper(new JsonMapper()));

      transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] {}", line));

      client = McpClient.sync(transport).build();
      client.initialize();
   }

   @AfterAll
   static void encerrarServidor() throws Exception {
      if (client != null)
         client.closeGracefully();

      try (var arquivos = Files.walk(pastaDeImagens)) {
         arquivos.sorted(Comparator.reverseOrder())
                 .forEach(caminho -> caminho.toFile().delete());
      }
   }

   private static Tool ferramenta(String nome) {
      return client.listTools()
                   .tools()
                   .stream()
                   .filter(tool -> nome.equals(tool.name()))
                   .findFirst()
                   .orElse(null);
   }

   private static CallToolResult chamar(String nome,
                                        Map<String, Object> argumentos) {
      return client.callTool(CallToolRequest.builder(nome)
                                            .arguments(argumentos)
                                            .build());
   }

   /** Confere que o resultado é um erro que começa com o código esperado. */
   private static void assertErro(String codigo, CallToolResult resultado) {
      assertTrue(resultado.isError(), "deveria ser erro: " +
                                      Resultados.texto(resultado));
      assertTrue(Resultados.texto(resultado).startsWith(codigo + ": "),
                 "esperado " + codigo + " mas veio: " +
                 Resultados.texto(resultado));
   }

   /** Confere que a ferramenta existe, com descrição e schema fechado. */
   private static void assertFerramentaDisponivel(String nome) {
      Tool tool = ferramenta(nome);

      assertNotNull(tool, nome + " não foi encontrada");
      assertNotNull(tool.description());
      assertFalse(tool.description().isBlank());
      assertNotNull(tool.inputSchema());
      assertEquals(false, tool.inputSchema().get("additionalProperties"));
   }

   @Test
   void servidorDisponibilizaAsFerramentasDaFase2() {
      assertEquals(Set.of("listar_imagens", "calcular_contraste"),
                   client.listTools().tools().stream().map(Tool::name)
                         .collect(Collectors.toSet()));
   }

   @Nested
   class ListarImagens {
      @Test
      void estaDisponivelComDescricaoESchema() {
         assertFerramentaDisponivel("listar_imagens");
      }

      @Test
      void descricaoAvisaQueAnexosNaoChegamAoServidor() {
         String descricao = ferramenta("listar_imagens").description();

         assertTrue(descricao.contains("anexadas à conversa NÃO"), descricao);
         assertTrue(descricao.contains("primeiro"), descricao);
      }

      @Test
      void listaSoAsImagensDaPastaConfiguradaEmOrdemAlfabetica() {
         CallToolResult resultado = chamar("listar_imagens", Map.of());
         JsonNode       json      = Resultados.json(resultado);

         assertFalse(resultado.isError(), Resultados.texto(resultado));
         assertEquals(pastaDeImagens.toString(), json.get("pasta").asString());
         assertEquals(5, json.get("total").asInt());
         assertEquals(List.of("animacao.gif", "banner.png", "foto.jpg",
                              "icone.PNG", "tela.bmp"),
                      json.get("imagens").valueStream()
                          .map(i -> i.get("nome").asString()).toList());
      }

      @Test
      void informaFormatoETamanho() {
         JsonNode json = Resultados.json(chamar("listar_imagens", Map.of()));

         assertEquals(List.of("GIF", "PNG", "JPEG", "PNG", "BMP"),
                      json.get("imagens").valueStream()
                          .map(i -> i.get("formato").asString()).toList());
         assertTrue(json.get("imagens").valueStream()
                        .allMatch(i -> i.get("tamanhoKb").asLong() >= 1));
      }

      @Test
      void naoListaArquivosDeOutrosTipos() {
         JsonNode json = Resultados.json(chamar("listar_imagens", Map.of()));

         assertTrue(json.get("imagens").valueStream()
                        .noneMatch(i -> i.get("nome").asString()
                                         .endsWith(".txt")));
      }

      @Test
      void parametroInesperadoResultaEmErroPadronizado() {
         CallToolResult resultado = chamar("listar_imagens",
                                           Map.of("pasta", "/etc"));

         assertErro("PARAMETRO_INVALIDO", resultado);
         assertEquals("PARAMETRO_INVALIDO: parâmetro desconhecido: 'pasta'",
                      Resultados.texto(resultado),
                      "mensagem (com acentos) deveria chegar intacta pelo " +
                      "protocolo");
      }
   }

   @Nested
   class CalcularContraste {
      private JsonNode calcular(String texto, String fundo) {
         CallToolResult resultado = chamar("calcular_contraste",
               Map.of("cor_texto", texto, "cor_fundo", fundo));

         assertFalse(resultado.isError(), Resultados.texto(resultado));
         return Resultados.json(resultado);
      }

      @Test
      void estaDisponivelComDescricaoESchema() {
         assertFerramentaDisponivel("calcular_contraste");

         Tool tool = ferramenta("calcular_contraste");

         assertEquals(List.of("cor_texto", "cor_fundo"),
                      tool.inputSchema().get("required"));
      }

      @Test
      void pretoSobreBrancoEh21() {
         JsonNode json = calcular("#000000", "#FFFFFF");

         assertEquals(21.0, json.get("razao").asDouble(), 0.001);
         assertEquals(5, json.get("vereditos").size());
         assertTrue(json.get("vereditos").valueStream()
                        .allMatch(v -> v.get("passa").asBoolean()));
      }

      @Test
      void cinza777777SobreBrancoFalhaAANormal() {
         JsonNode json = calcular("#777777", "#FFFFFF");

         assertEquals(4.48, json.get("razao").asDouble(), 0.01);
         assertFalse(json.get("vereditos").get(0).get("passa").asBoolean());
         assertTrue(json.get("vereditos").get(1).get("passa").asBoolean());
      }

      @Test
      void cinza767676SobreBrancoPassaAANormal() {
         assertTrue(calcular("#767676", "#FFFFFF").get("vereditos").get(0)
                        .get("passa").asBoolean());
      }

      @Test
      void aceitaNomeDeCorEHexCurto() {
         JsonNode json = calcular("Vermelho", "#FFF");

         assertEquals("#FF0000", json.get("corTexto").asString());
         assertEquals("#FFFFFF", json.get("corFundo").asString());
         assertEquals(4.0, json.get("razao").asDouble(), 0.01);
      }

      @Test
      void corIgualAElaMesmaEh1() {
         assertEquals(1.0, calcular("#336699", "#336699").get("razao")
                                  .asDouble(), 0.001);
      }

      @Test
      void corInvalida() {
         assertErro("COR_INVALIDA",
                    chamar("calcular_contraste",
                           Map.of("cor_texto", "#GGGGGG",
                                  "cor_fundo", "#FFFFFF")));
      }

      @Test
      void nomeDeCorDesconhecido() {
         assertErro("COR_INVALIDA",
                    chamar("calcular_contraste",
                           Map.of("cor_texto", "xablau",
                                  "cor_fundo", "#FFFFFF")));
      }

      @Test
      void parametroAusente() {
         assertErro("PARAMETRO_AUSENTE",
                    chamar("calcular_contraste", Map.of("cor_texto", "#000")));
      }

      @Test
      void arquivoNaoEhParametroDestaFerramenta() {
         assertErro("PARAMETRO_INVALIDO",
                    chamar("calcular_contraste",
                           Map.of("cor_texto", "#000", "cor_fundo", "#FFF",
                                  "arquivo", "banner.png")));
      }
   }
}
