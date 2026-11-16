package br.edu.ifpr.mcp_imagens.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_imagens.infra.config.AppConfig;
import br.edu.ifpr.mcp_imagens.infra.config.CarregadorDeRecursos;
import br.edu.ifpr.mcp_imagens.infra.config.CoresNomeadas;
import br.edu.ifpr.mcp_imagens.infra.config.CorParser;
import br.edu.ifpr.mcp_imagens.infra.config.TabelaWcag;
import br.edu.ifpr.mcp_imagens.infra.loader.ImageIoLoader;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepositoryEmDisco;
import br.edu.ifpr.mcp_imagens.mcp.tools.CalcularContrasteTool;
import br.edu.ifpr.mcp_imagens.mcp.tools.ContarPixelsDaCorTool;
import br.edu.ifpr.mcp_imagens.mcp.tools.ContarPixelsTransparentesTool;
import br.edu.ifpr.mcp_imagens.mcp.tools.CoresDominantesTool;
import br.edu.ifpr.mcp_imagens.mcp.tools.ListarImagensTool;
import br.edu.ifpr.mcp_imagens.mcp.tools.ObterInformacoesTool;
import br.edu.ifpr.mcp_imagens.mcp.tools.ProporcaoClaroEscuroTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_imagens.mcp.utils.ToolRegistry;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeCoresPadrao;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeTransparenciaPadrao;
import br.edu.ifpr.mcp_imagens.service.CalculadoraDeContrastePadrao;
import br.edu.ifpr.mcp_imagens.service.ContadorDePixels;
import br.edu.ifpr.mcp_imagens.service.ContadorDePixelsPadrao;
import br.edu.ifpr.mcp_imagens.service.InformacoesService;
import br.edu.ifpr.mcp_imagens.service.InformacoesServicePadrao;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

/**
 * Servidor MCP (transporte STDIO) de análise de imagens e acessibilidade de
 * cores.
 * <p>
 * Esta classe é a raiz de composição: cria a configuração, os serviços e as
 * ferramentas (injeção de dependência manual, pelos construtores) e inicia o
 * servidor. Não há lógica de negócio aqui.
 * <p>
 * IMPORTANTE: nada pode ser escrito em {@code System.out}, que é o canal do
 * protocolo; os logs vão para o stderr (ver {@code logback.xml}).
 */
public class McpImagensServer {
   private static final Logger LOG =
                              LoggerFactory.getLogger(McpImagensServer.class);

   private static final String NOME   = "mcp-imagens";
   private static final String VERSAO = "1.0.0";

   private static List<McpToolDefinition> criarFerramentas(AppConfig config) {
      CarregadorDeRecursos carregador  = new CarregadorDeRecursos();
      CorParser            parser      = new CorParser(carregador.objeto(
                              "cores-nomeadas", CoresNomeadas.class).cores());
      TabelaWcag           wcag        = carregador.objeto("criterios-wcag",
                                                           TabelaWcag.class);

      ImagemRepository     repositorio = new ImagemRepositoryEmDisco(
                                      config.pastaImagens(),
                                      new ImageIoLoader(
                                            config.tamanhoMaximoBytes(),
                                            config.pixelsMaximos()));
      InformacoesService   informacoes = new InformacoesServicePadrao(
                                                                 repositorio);

      ContadorDePixels     contador    = new ContadorDePixelsPadrao();

      return List.of(
         new ListarImagensTool(informacoes),
         new ObterInformacoesTool(informacoes),
         new ContarPixelsDaCorTool(repositorio, parser, contador),
         new CoresDominantesTool(repositorio, new AnalisadorDeCoresPadrao()),
         new ProporcaoClaroEscuroTool(repositorio, contador),
         new ContarPixelsTransparentesTool(repositorio,
               new AnalisadorDeTransparenciaPadrao()),
         new CalcularContrasteTool(parser,
               new CalculadoraDeContrastePadrao(wcag.criterios())));
   }

   private static SyncSpecification<SingleSessionSyncSpecification>
                  configurarServidor() {
      JacksonMcpJsonMapper         jsonMapper        =
                                     new JacksonMcpJsonMapper(new JsonMapper());
      StdioServerTransportProvider transportProvider =
                                   new StdioServerTransportProvider(jsonMapper);
      ServerCapabilities           capabilities      =
                                   ServerCapabilities.builder()
                                                     .tools(true)
                                                     .logging()
                                                     .build();

      return McpServer.sync(transportProvider)
                      .serverInfo(Implementation.builder(NOME, VERSAO).build())
                      .capabilities(capabilities)
                      // a validação de argumentos é feita pelas próprias
                      // ferramentas (ArgumentReader), que devolvem os códigos
                      // de erro padronizados (PARAMETRO_AUSENTE, ...); a
                      // validação do SDK responderia antes, com outro formato
                      .validateToolInputs(false);
   }

   public static void main(String[] args) throws Exception {
      // o ImageIO usa classes gráficas do JDK (AWT); em modo "headless" elas
      // não precisam de tela nem de ambiente gráfico
      System.setProperty("java.awt.headless", "true");

      AppConfig                                         config   =
                                                       AppConfig.doAmbiente();
      ToolRegistry                                      registro =
                                    new ToolRegistry(criarFerramentas(config));
      SyncSpecification<SingleSessionSyncSpecification> servidor =
                                                          configurarServidor();

      registro.registrarEm(servidor);
      servidor.build();

      LOG.info("Servidor MCP Imagens iniciado via STDIO: {} ferramenta(s), " +
               "imagens em {}", registro.quantidade(), config.pastaImagens());
   }
}
