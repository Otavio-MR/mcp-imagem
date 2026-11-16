package br.edu.ifpr.mcp_imagens.mcp.resources;

import java.util.List;

import br.edu.ifpr.mcp_imagens.mcp.utils.ToolResults;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

/**
 * Base dos resources (padrão Template Method, como {@code AbstractTool}).
 * <p>
 * Cada resource concreto informa o endereço, o nome, o título e a descrição, e
 * devolve o conteúdo em {@link #conteudo()}. Aqui ficam a definição, o registro
 * e a conversão do conteúdo em JSON.
 */
public abstract class AbstractResource implements McpResourceDefinition {
   private static final String TIPO_JSON = "application/json";

   /** Endereço do resource, por exemplo {@code imagens://cores-nomeadas}. */
   protected abstract String uri();

   /** Nome curto. */
   protected abstract String nome();

   /** Título, exibido por clientes como o MCP Inspector. */
   protected abstract String titulo();

   /** Descrição pensada para a IA saber quando ler o resource. */
   protected abstract String descricao();

   /** Objeto (record, lista, mapa) que será devolvido como JSON. */
   protected abstract Object conteudo();

   @Override
   public final void register(
                         SyncSpecification<SingleSessionSyncSpecification> mcp) {
      mcp.resources(especificacao());
   }

   @Override
   public final ReadResourceResult read(McpSyncServerExchange exchange,
                                        ReadResourceRequest request) {
      String json = ToolResults.comoJson(conteudo());

      return new ReadResourceResult(List.of(
            new TextResourceContents(uri(), TIPO_JSON, json)));
   }

   private SyncResourceSpecification especificacao() {
      Resource resource = Resource.builder(uri(), nome())
                                  .title(titulo())
                                  .description(descricao())
                                  .mimeType(TIPO_JSON)
                                  .build();

      return new SyncResourceSpecification(resource, this::read);
   }
}
