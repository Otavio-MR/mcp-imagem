package br.edu.ifpr.mcp_imagens.client;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cria o cliente MCP: localiza o JAR do servidor, inicia o servidor como
 * subprocesso (transporte STDIO) e repassa a ele a pasta de imagens.
 */
public class FabricaDeClienteMcp {
   private static final Logger LOG =
                              LoggerFactory.getLogger(FabricaDeClienteMcp.class);

   // o cliente roda a partir de target/dist-client, por isso o "../../"
   static final String SERVER_PATH = "../../server/mcp_imagens_server.jar";

   // pasta de imagens usada quando IMG_DIR não está definida: a pasta
   // "imagens" na raiz do projeto
   static final String IMG_DIR_VAR    = "IMG_DIR";
   static final String IMG_DIR_PADRAO = "../../imagens";

   /** @throws IllegalStateException se o JAR do servidor não for encontrado */
   public McpSyncClient criar() {
      File jarDoServidor = new File(SERVER_PATH);

      if (!jarDoServidor.isFile())
         throw new IllegalStateException("JAR do servidor não encontrado: " +
                                         jarDoServidor.getAbsolutePath());

      String caminhoDoJar = jarDoServidor.getAbsolutePath();

      LOG.info("Servidor MCP: {}", caminhoDoJar);

      // comando que executa o servidor
      ServerParameters parametros = ServerParameters.builder("java")
            .args("-jar", caminhoDoJar)
            .addEnvVar(IMG_DIR_VAR, pastaDeImagens())
            .build();
      StdioClientTransport transporte = new StdioClientTransport(parametros,
            new JacksonMcpJsonMapper(new JsonMapper()));

      // o que o servidor escreve no stderr (logs) vai para o log do cliente
      transporte.setStdErrorHandler(
                                   linha -> LOG.info("[SERVER STDERR] {}", linha));

      return McpClient.sync(transporte)
                      .loggingConsumer(this::registrarNotificacao)
                      .build();
   }

   /** Pasta de IMG_DIR (ou a padrão), como caminho absoluto. */
   private String pastaDeImagens() {
      String pasta = System.getenv(IMG_DIR_VAR);

      if (pasta == null || pasta.isBlank())
         pasta = IMG_DIR_PADRAO;

      String absoluta = new File(pasta).getAbsolutePath();

      LOG.info("Pasta de imagens: {}", absoluta);
      return absoluta;
   }

   /** Chamado quando o servidor envia uma mensagem de log ao cliente. */
   private void registrarNotificacao(LoggingMessageNotification notificacao) {
      LOG.info("[NOTIFICAÇÃO MCP] Origem: {} | Nível: {} | Mensagem: {}",
               notificacao.logger(), notificacao.level(), notificacao.data());
   }
}
