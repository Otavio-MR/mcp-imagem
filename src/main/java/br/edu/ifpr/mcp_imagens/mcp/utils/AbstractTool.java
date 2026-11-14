package br.edu.ifpr.mcp_imagens.mcp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_imagens.domain.exception.CodigoErro;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagensException;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Base de todas as ferramentas (padrão Template Method).
 * <p>
 * Os passos fixos ficam aqui: montar e registrar a definição da ferramenta,
 * ler os argumentos, converter o retorno em JSON e transformar qualquer
 * exceção em um resultado de erro padronizado. Cada ferramenta concreta só
 * informa nome, título, descrição e schema, e implementa
 * {@link #executar(ArgumentReader)}.
 */
public abstract class AbstractTool implements McpToolDefinition {
   private static final Logger LOG = LoggerFactory.getLogger(AbstractTool.class);

   /** Nome da ferramenta, em snake_case. */
   protected abstract String nome();

   /** Título curto, exibido por clientes como o MCP Inspector. */
   protected abstract String titulo();

   /** Descrição pensada para a IA decidir quando usar a ferramenta. */
   protected abstract String descricao();

   /** Parâmetros aceitos pela ferramenta. */
   protected abstract InputSchema schema();

   /**
    * Realiza a tarefa da ferramenta.
    *
    * @return objeto (record, lista, mapa) que será devolvido como JSON
    * @throws ImagensException em falhas previstas do domínio
    */
   protected abstract Object executar(ArgumentReader argumentos);

   @Override
   public final void register(
                         SyncSpecification<SingleSessionSyncSpecification> mcp) {
      mcp.tools(especificacao());
   }

   @Override
   public final CallToolResult run(McpSyncServerExchange exchange,
                                   CallToolRequest request) {
      try {
         ArgumentReader argumentos = new ArgumentReader(
                                               request.arguments(),
                                               schema().nomesDosParametros());

         return ToolResults.sucesso(executar(argumentos));
      }
      catch (ImagensException ex) {
         LOG.warn("'{}' recusou a chamada: {} {}", nome(), ex.codigo(),
                  ex.getMessage());
         return ToolResults.erro(ex.codigo(), ex.getMessage());
      }
      catch (RuntimeException ex) {
         LOG.error("Falha inesperada em '{}'", nome(), ex);
         return ToolResults.erro(CodigoErro.ERRO_INTERNO,
                                 "falha inesperada ao executar '" + nome() +
                                 "'");
      }
   }

   /** Definição da ferramenta vinculada ao método {@link #run}. */
   SyncToolSpecification especificacao() {
      Tool tool = Tool.builder(nome(), schema().comoMapa())
                      .title(titulo())
                      .description(descricao())
                      .build();

      return SyncToolSpecification.builder()
                                  .tool(tool)
                                  .callHandler(this::run)
                                  .build();
   }
}
