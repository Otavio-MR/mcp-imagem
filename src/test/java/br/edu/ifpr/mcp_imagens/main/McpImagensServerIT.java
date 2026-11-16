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
import java.util.TreeMap;
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

   private static final int VERMELHO = 0xFFFF0000;
   private static final int BRANCO   = 0xFFFFFFFF;
   private static final int PRETO    = 0xFF000000;

   /** Imagens geradas para os testes e o formato que o servidor deve dar. */
   private static final Map<String, String> IMAGENS = new TreeMap<>(
                                                 String.CASE_INSENSITIVE_ORDER);

   static {
      IMAGENS.put("animacao.gif", "GIF");
      IMAGENS.put("banner.png", "PNG");
      IMAGENS.put("foto.jpg", "JPEG");
      IMAGENS.put("icone.PNG", "PNG");
      IMAGENS.put("tela.bmp", "BMP");
      IMAGENS.put("falsa.png", "PNG");
      IMAGENS.put("gigante.png", "PNG");
      IMAGENS.put("vermelho-branco.png", "PNG");
      IMAGENS.put("quase-vermelho.png", "PNG");
      IMAGENS.put("alfas.png", "PNG");
   }

   private static Path          pastaDeImagens;
   private static McpSyncClient client;

   @BeforeAll
   static void iniciarServidor() throws Exception {
      pastaDeImagens = Files.createTempDirectory("mcp-imagens-it");

      criarImagens();

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

   // as imagens têm cores e contagens conhecidas, usadas pelos testes
   private static void criarImagens() throws Exception {
      ImagensDeTeste.criar(pastaDeImagens, "banner.png", 20, 10, 0xFF202020);
      ImagensDeTeste.criar(pastaDeImagens, "icone.PNG", 8, 8, 0x00000000);
      ImagensDeTeste.criar(pastaDeImagens, "foto.jpg", 16, 16, 0xFF808080);
      ImagensDeTeste.criar(pastaDeImagens, "animacao.gif", 4, 4, VERMELHO);
      ImagensDeTeste.criar(pastaDeImagens, "tela.bmp", 6, 6, BRANCO);
      // 10x10: 30 pixels vermelhos (3 primeiras linhas) e 70 brancos
      int[] linhaVermelha = new int[10];
      int[] linhaBranca   = new int[10];
      java.util.Arrays.fill(linhaVermelha, VERMELHO);
      java.util.Arrays.fill(linhaBranca, BRANCO);
      int[][] trintaVermelhos = new int[10][];
      for (int y = 0; y < 10; y++)
         trintaVermelhos[y] = y < 3 ? linhaVermelha : linhaBranca;
      ImagensDeTeste.criarComPixels(pastaDeImagens, "vermelho-branco.png",
                                    trintaVermelhos);
      // igual, mas o pixel (0, 0) é #FE0000 (quase vermelho)
      int[][] quase = new int[10][];
      for (int y = 0; y < 10; y++)
         quase[y] = trintaVermelhos[y].clone();
      quase[0][0] = 0xFFFE0000;
      ImagensDeTeste.criarComPixels(pastaDeImagens, "quase-vermelho.png", quase);
      // três pixels vermelhos com alfa 0, 128 e 255
      ImagensDeTeste.criarComPixels(pastaDeImagens, "alfas.png",
            new int[] { 0x00FF0000, 0x80FF0000, 0xFFFF0000 });
      // arquivo com extensão de imagem, mas que não é uma imagem
      Files.writeString(pastaDeImagens.resolve("falsa.png"), "não sou imagem");
      // cabeçalho que declara 6000 x 6000 pixels (acima do limite de 25 milhões)
      ImagensDeTeste.criarPngSoComCabecalho(pastaDeImagens, "gigante.png",
                                            6000, 6000);
      // não é imagem pelo formato: não deve aparecer na lista
      Files.writeString(pastaDeImagens.resolve("notas.txt"), "texto");
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

   /**
    * Confere que a descrição de uma ferramenta que recebe {@code arquivo}
    * diz que o arquivo precisa estar na pasta de imagens.
    */
   private static void assertDescricaoAvisaDaPasta(String nome) {
      String descricao = ferramenta(nome).description();

      assertTrue(descricao.contains("pasta de imagens"), nome + ": " + descricao);
      assertTrue(descricao.contains("listar_imagens"), nome + ": " + descricao);
      assertTrue(descricao.contains("anexadas à conversa NÃO"),
                 nome + ": " + descricao);
   }

   @Test
   void servidorDisponibilizaAsFerramentasEsperadas() {
      assertEquals(Set.of("listar_imagens", "obter_informacoes",
                          "contar_pixels_da_cor", "cores_dominantes",
                          "proporcao_claro_escuro",
                          "contar_pixels_transparentes", "histograma_de_brilho",
                          "calcular_contraste"),
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
         assertEquals(IMAGENS.size(), json.get("total").asInt());
         assertEquals(List.copyOf(IMAGENS.keySet()),
                      json.get("imagens").valueStream()
                          .map(i -> i.get("nome").asString()).toList());
      }

      @Test
      void informaFormatoETamanho() {
         JsonNode json = Resultados.json(chamar("listar_imagens", Map.of()));

         assertEquals(List.copyOf(IMAGENS.values()),
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
   class ObterInformacoes {
      private JsonNode obter(String arquivo) {
         CallToolResult resultado = chamar("obter_informacoes",
                                           Map.of("arquivo", arquivo));

         assertFalse(resultado.isError(), Resultados.texto(resultado));
         return Resultados.json(resultado);
      }

      @Test
      void estaDisponivelComDescricaoESchema() {
         assertFerramentaDisponivel("obter_informacoes");
         assertDescricaoAvisaDaPasta("obter_informacoes");
         assertEquals(List.of("arquivo"),
                      ferramenta("obter_informacoes").inputSchema()
                                                     .get("required"));
      }

      @Test
      void informaAsMedidasExatasDeUmPng() {
         JsonNode json = obter("banner.png");

         assertEquals("banner.png", json.get("arquivo").asString());
         assertEquals("PNG", json.get("formato").asString());
         assertEquals(20, json.get("largura").asInt());
         assertEquals(10, json.get("altura").asInt());
         assertEquals(200, json.get("totalPixels").asLong());
         assertEquals(pastaDeImagens.resolve("banner.png").toFile().length(),
                      json.get("tamanhoBytes").asLong());
         assertTrue(json.get("tamanhoKb").asLong() >= 1);
      }

      @Test
      void pngComAlfaDizQueTemCanalAlfa() {
         JsonNode json = obter("icone.PNG");

         assertEquals(8, json.get("largura").asInt());
         assertTrue(json.get("temCanalAlfa").asBoolean());
      }

      @Test
      void jpgEBmpNaoTemCanalAlfa() {
         JsonNode jpg = obter("foto.jpg");
         JsonNode bmp = obter("tela.bmp");

         assertEquals("JPEG", jpg.get("formato").asString());
         assertFalse(jpg.get("temCanalAlfa").asBoolean());
         assertEquals("BMP", bmp.get("formato").asString());
         assertFalse(bmp.get("temCanalAlfa").asBoolean());
         assertEquals(6, bmp.get("altura").asInt());
      }

      @Test
      void gifTemFormatoGif() {
         assertEquals("GIF", obter("animacao.gif").get("formato").asString());
      }

      @Test
      void imagemInexistenteTrazMensagemDidatica() {
         CallToolResult resultado = chamar("obter_informacoes",
               Map.of("arquivo", "nao-existe.png"));
         String         texto     = Resultados.texto(resultado);

         assertErro("IMAGEM_NAO_ENCONTRADA", resultado);
         assertTrue(texto.contains("'nao-existe.png' não está na pasta '" +
                                   pastaDeImagens + "'"), texto);
         assertTrue(texto.contains("Imagens anexadas à conversa não são " +
                                   "visíveis ao servidor"), texto);
         assertTrue(texto.contains("Disponíveis: " +
                                   String.join(", ", IMAGENS.keySet())), texto);
      }

      @Test
      void formatoNaoSuportado() {
         assertErro("FORMATO_NAO_SUPORTADO",
                    chamar("obter_informacoes", Map.of("arquivo", "notas.txt")));
      }

      @Test
      void imagemInvalida() {
         assertErro("IMAGEM_INVALIDA",
                    chamar("obter_informacoes", Map.of("arquivo", "falsa.png")));
      }

      @Test
      void imagemMuitoGrande() {
         CallToolResult resultado = chamar("obter_informacoes",
               Map.of("arquivo", "gigante.png"));

         assertErro("IMAGEM_MUITO_GRANDE", resultado);
         assertTrue(Resultados.texto(resultado).contains("6000x6000"),
                    Resultados.texto(resultado));
      }

      @Test
      void caminhosSaoRecusados() {
         for (String caminho : new String[] { "../segredo.png", "C:\\x.png",
                                              "/etc/passwd" })
            assertErro("PARAMETRO_INVALIDO",
                       chamar("obter_informacoes", Map.of("arquivo", caminho)));
      }

      @Test
      void arquivoAusente() {
         assertErro("PARAMETRO_AUSENTE", chamar("obter_informacoes", Map.of()));
      }
   }

   @Nested
   class ContarPixelsDaCor {
      private JsonNode contar(Map<String, Object> argumentos) {
         CallToolResult resultado = chamar("contar_pixels_da_cor", argumentos);

         assertFalse(resultado.isError(), Resultados.texto(resultado));
         return Resultados.json(resultado);
      }

      @Test
      void estaDisponivelComDescricaoESchema() {
         assertFerramentaDisponivel("contar_pixels_da_cor");
         assertDescricaoAvisaDaPasta("contar_pixels_da_cor");
         assertEquals(List.of("arquivo", "cor"),
                      ferramenta("contar_pixels_da_cor").inputSchema()
                                                        .get("required"));
      }

      @Test
      void trintaVermelhosEmCem() {
         JsonNode json = contar(Map.of("arquivo", "vermelho-branco.png",
                                       "cor", "vermelho"));

         assertEquals(30, json.get("quantidade").asLong());
         assertEquals(100, json.get("totalConsiderado").asLong());
         assertEquals(30.0, json.get("percentual").asDouble(), 0.0);
      }

      @Test
      void toleranciaIncluiOPixelQuaseVermelho() {
         assertEquals(29, contar(Map.of("arquivo", "quase-vermelho.png",
                                        "cor", "#FF0000"))
                               .get("quantidade").asLong());
         assertEquals(30, contar(Map.of("arquivo", "quase-vermelho.png",
                                        "cor", "#FF0000", "tolerancia", 1))
                               .get("quantidade").asLong());
      }

      @Test
      void transparentesSaoIgnoradosPorPadrao() {
         JsonNode padrao = contar(Map.of("arquivo", "alfas.png",
                                         "cor", "#F00"));
         JsonNode todos  = contar(Map.of("arquivo", "alfas.png",
                                         "cor", "#F00",
                                         "ignorar_transparentes", false));

         assertEquals(2, padrao.get("totalConsiderado").asLong());
         assertEquals(3, todos.get("totalConsiderado").asLong());
      }

      @Test
      void toleranciaForaDaFaixa() {
         assertErro("PARAMETRO_INVALIDO",
                    chamar("contar_pixels_da_cor",
                           Map.of("arquivo", "banner.png", "cor", "#000",
                                  "tolerancia", 256)));
      }

      @Test
      void corInvalida() {
         assertErro("COR_INVALIDA",
                    chamar("contar_pixels_da_cor",
                           Map.of("arquivo", "banner.png", "cor", "xablau")));
      }

      @Test
      void parametroAusente() {
         assertErro("PARAMETRO_AUSENTE",
                    chamar("contar_pixels_da_cor", Map.of("arquivo", "banner.png")));
      }

      @Test
      void imagemInexistenteEImagemInvalida() {
         assertErro("IMAGEM_NAO_ENCONTRADA",
                    chamar("contar_pixels_da_cor",
                           Map.of("arquivo", "nada.png", "cor", "#000")));
         assertErro("IMAGEM_INVALIDA",
                    chamar("contar_pixels_da_cor",
                           Map.of("arquivo", "falsa.png", "cor", "#000")));
      }
   }

   @Nested
   class CoresDominantes {
      private JsonNode dominantes(Map<String, Object> argumentos) {
         CallToolResult resultado = chamar("cores_dominantes", argumentos);

         assertFalse(resultado.isError(), Resultados.texto(resultado));
         return Resultados.json(resultado);
      }

      @Test
      void estaDisponivelComDescricaoESchema() {
         assertFerramentaDisponivel("cores_dominantes");
         assertDescricaoAvisaDaPasta("cores_dominantes");
         assertEquals(List.of("arquivo"),
                      ferramenta("cores_dominantes").inputSchema()
                                                    .get("required"));
      }

      @Test
      void coresExatasComPassoUm() {
         JsonNode json  = dominantes(Map.of("arquivo", "vermelho-branco.png",
                                            "passo", 1));
         JsonNode lista = json.get("cores");

         assertEquals(100, json.get("totalConsiderado").asLong());
         assertEquals(2, json.get("gruposDistintos").asInt());
         assertEquals("#FFFFFF", lista.get(0).get("cor").asString());
         assertEquals(70, lista.get(0).get("quantidade").asLong());
         assertEquals("#FF0000", lista.get(1).get("cor").asString());
         assertEquals(30, lista.get(1).get("quantidade").asLong());
      }

      @Test
      void passoAgrupaOPixelQuaseVermelho() {
         assertEquals(3, dominantes(Map.of("arquivo", "quase-vermelho.png",
                                           "passo", 1))
                             .get("gruposDistintos").asInt());
         assertEquals(2, dominantes(Map.of("arquivo", "quase-vermelho.png"))
                             .get("gruposDistintos").asInt());
      }

      @Test
      void quantidadeLimitaALista() {
         assertEquals(1, dominantes(Map.of("arquivo", "vermelho-branco.png",
                                           "quantidade", 1))
                             .get("cores").size());
      }

      @Test
      void transparentesSaoIgnoradosPorPadrao() {
         assertEquals(2, dominantes(Map.of("arquivo", "alfas.png"))
                             .get("totalConsiderado").asLong());
      }

      @Test
      void passoForaDaFaixa() {
         assertErro("PARAMETRO_INVALIDO",
                    chamar("cores_dominantes",
                           Map.of("arquivo", "banner.png", "passo", 129)));
      }

      @Test
      void errosDeArquivo() {
         assertErro("PARAMETRO_AUSENTE", chamar("cores_dominantes", Map.of()));
         assertErro("IMAGEM_NAO_ENCONTRADA",
                    chamar("cores_dominantes", Map.of("arquivo", "nada.png")));
         assertErro("IMAGEM_INVALIDA",
                    chamar("cores_dominantes", Map.of("arquivo", "falsa.png")));
      }
   }

   @Nested
   class ProporcaoClaroEscuro {
      private JsonNode proporcao(Map<String, Object> argumentos) {
         CallToolResult resultado = chamar("proporcao_claro_escuro",
                                           argumentos);

         assertFalse(resultado.isError(), Resultados.texto(resultado));
         return Resultados.json(resultado);
      }

      @Test
      void estaDisponivelComDescricaoESchema() {
         assertFerramentaDisponivel("proporcao_claro_escuro");
         assertDescricaoAvisaDaPasta("proporcao_claro_escuro");
         assertEquals(List.of("arquivo"),
                      ferramenta("proporcao_claro_escuro").inputSchema()
                                                          .get("required"));
      }

      @Test
      void vermelhoEscuroEBrancoClaro() {
         // vermelho puro tem brilho 54, branco 255
         JsonNode json = proporcao(Map.of("arquivo", "vermelho-branco.png"));

         assertEquals(70, json.get("claros").get("quantidade").asLong());
         assertEquals(30, json.get("escuros").get("quantidade").asLong());
         assertEquals(30.0, json.get("escuros").get("percentual").asDouble(),
                      0);
      }

      @Test
      void limiarMudaOResultado() {
         assertEquals(100, proporcao(Map.of("arquivo", "vermelho-branco.png",
                                            "limiar", 54))
                                 .get("claros").get("quantidade").asLong());
         assertEquals(70, proporcao(Map.of("arquivo", "vermelho-branco.png",
                                           "limiar", 55))
                                .get("claros").get("quantidade").asLong());
      }

      @Test
      void transparentesSaoIgnoradosPorPadrao() {
         assertEquals(2, proporcao(Map.of("arquivo", "alfas.png"))
                             .get("totalConsiderado").asLong());
      }

      @Test
      void limiarForaDaFaixa() {
         assertErro("PARAMETRO_INVALIDO",
                    chamar("proporcao_claro_escuro",
                           Map.of("arquivo", "banner.png", "limiar", 256)));
      }

      @Test
      void errosDeArquivo() {
         assertErro("PARAMETRO_AUSENTE",
                    chamar("proporcao_claro_escuro", Map.of()));
         assertErro("IMAGEM_NAO_ENCONTRADA",
                    chamar("proporcao_claro_escuro",
                           Map.of("arquivo", "nada.png")));
      }
   }

   @Nested
   class ContarPixelsTransparentes {
      private JsonNode contar(String arquivo) {
         CallToolResult resultado = chamar("contar_pixels_transparentes",
                                           Map.of("arquivo", arquivo));

         assertFalse(resultado.isError(), Resultados.texto(resultado));
         return Resultados.json(resultado);
      }

      @Test
      void estaDisponivelComDescricaoESchema() {
         assertFerramentaDisponivel("contar_pixels_transparentes");
         assertDescricaoAvisaDaPasta("contar_pixels_transparentes");
         assertEquals(List.of("arquivo"),
                      ferramenta("contar_pixels_transparentes").inputSchema()
                                                               .get("required"));
      }

      @Test
      void tresAlfasTresTipos() {
         JsonNode json = contar("alfas.png");

         assertEquals(3, json.get("totalPixels").asLong());
         assertEquals(1, json.get("transparentes").get("quantidade").asLong());
         assertEquals(1, json.get("semitransparentes").get("quantidade")
                           .asLong());
         assertEquals(1, json.get("opacos").get("quantidade").asLong());
      }

      @Test
      void iconeTodoTransparente() {
         JsonNode json = contar("icone.PNG");

         assertEquals(64, json.get("transparentes").get("quantidade").asLong());
         assertEquals(100.0, json.get("transparentes").get("percentual")
                                 .asDouble(), 0);
      }

      @Test
      void jpgTemTudoOpaco() {
         JsonNode json = contar("foto.jpg");

         assertFalse(json.get("temCanalAlfa").asBoolean());
         assertEquals(256, json.get("opacos").get("quantidade").asLong());
      }

      @Test
      void errosDeArquivo() {
         assertErro("PARAMETRO_AUSENTE",
                    chamar("contar_pixels_transparentes", Map.of()));
         assertErro("IMAGEM_NAO_ENCONTRADA",
                    chamar("contar_pixels_transparentes",
                           Map.of("arquivo", "nada.png")));
         assertErro("IMAGEM_INVALIDA",
                    chamar("contar_pixels_transparentes",
                           Map.of("arquivo", "falsa.png")));
      }
   }

   @Nested
   class HistogramaDeBrilho {
      private JsonNode histograma(Map<String, Object> argumentos) {
         CallToolResult resultado = chamar("histograma_de_brilho", argumentos);

         assertFalse(resultado.isError(), Resultados.texto(resultado));
         return Resultados.json(resultado);
      }

      @Test
      void estaDisponivelComDescricaoESchema() {
         assertFerramentaDisponivel("histograma_de_brilho");
         assertDescricaoAvisaDaPasta("histograma_de_brilho");
         assertEquals(List.of("arquivo"),
                      ferramenta("histograma_de_brilho").inputSchema()
                                                        .get("required"));
      }

      @Test
      void vermelhoNaFaixaUmEBrancoNaUltima() {
         // brilho do vermelho puro: 54 (faixa 32..63); do branco: 255
         JsonNode faixas = histograma(Map.of("arquivo", "vermelho-branco.png"))
                                     .get("faixas");

         assertEquals(8, faixas.size());
         assertEquals(30, faixas.get(1).get("quantidade").asLong());
         assertEquals(70, faixas.get(7).get("quantidade").asLong());
         assertEquals(0, faixas.get(0).get("quantidade").asLong());
      }

      @Test
      void imagemDeUmTomSoPreencheUmaFaixa() {
         // banner.png é todo #202020 (brilho 32)
         JsonNode json = histograma(Map.of("arquivo", "banner.png"));

         assertEquals(200, json.get("totalConsiderado").asLong());
         assertEquals(200, json.get("faixas").get(1).get("quantidade")
                              .asLong());
      }

      @Test
      void quantidadeDeFaixasPedida() {
         assertEquals(4, histograma(Map.of("arquivo", "banner.png",
                                           "faixas", 4))
                                .get("faixas").size());
      }

      @Test
      void transparentesSaoIgnoradosPorPadrao() {
         assertEquals(2, histograma(Map.of("arquivo", "alfas.png"))
                             .get("totalConsiderado").asLong());
      }

      @Test
      void faixasForaDoLimite() {
         assertErro("PARAMETRO_INVALIDO",
                    chamar("histograma_de_brilho",
                           Map.of("arquivo", "banner.png", "faixas", 33)));
      }

      @Test
      void errosDeArquivo() {
         assertErro("PARAMETRO_AUSENTE",
                    chamar("histograma_de_brilho", Map.of()));
         assertErro("IMAGEM_NAO_ENCONTRADA",
                    chamar("histograma_de_brilho",
                           Map.of("arquivo", "nada.png")));
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
