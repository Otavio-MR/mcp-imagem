package br.edu.ifpr.mcp_imagens.mcp.prompts;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Base dos prompts (padrão Template Method, como {@code AbstractTool}).
 * <p>
 * Cada prompt concreto informa nome, título, descrição e argumentos, e monta o
 * texto da instrução em {@link #montarTexto(ArgumentReader)}. Aqui ficam a
 * definição, o registro, a leitura dos argumentos e o formato da resposta (uma
 * mensagem do usuário com o texto).
 */
public abstract class AbstractPrompt implements McpPromptDefinition {
   /** Nome do prompt, em snake_case. */
   protected abstract String nome();

   /** Título, exibido por clientes como o MCP Inspector. */
   protected abstract String titulo();

   /** Descrição do que o prompt faz. */
   protected abstract String descricao();

   /** Argumentos aceitos pelo prompt (todos chegam como texto). */
   protected abstract List<PromptArgument> argumentos();

   /**
    * Monta a instrução enviada à IA.
    *
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagensException se um
    *         argumento estiver ausente ou inválido
    */
   protected abstract String montarTexto(ArgumentReader argumentos);

   @Override
   public final void register(
                         SyncSpecification<SingleSessionSyncSpecification> mcp) {
      mcp.prompts(especificacao());
   }

   @Override
   public final GetPromptResult get(McpSyncServerExchange exchange,
                                    GetPromptRequest request) {
      ArgumentReader argumentos = new ArgumentReader(request.arguments(),
                                                     nomesDosArgumentos());
      String         texto      = montarTexto(argumentos);

      return new GetPromptResult(descricao(), List.of(
            new PromptMessage(Role.USER, new TextContent(texto))));
   }

   private Set<String> nomesDosArgumentos() {
      return argumentos().stream()
                         .map(PromptArgument::name)
                         .collect(Collectors.toSet());
   }

   private SyncPromptSpecification especificacao() {
      Prompt prompt = Prompt.builder(nome())
                            .title(titulo())
                            .description(descricao())
                            .arguments(argumentos())
                            .build();

      return new SyncPromptSpecification(prompt, this::get);
   }
}
