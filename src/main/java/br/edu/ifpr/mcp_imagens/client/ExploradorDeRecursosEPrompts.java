package br.edu.ifpr.mcp_imagens.client;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

/**
 * Comandos opcionais do cliente: listar e ler os resources do servidor, e
 * listar e obter os prompts.
 */
public class ExploradorDeRecursosEPrompts {
   private static final Logger LOG =
                      LoggerFactory.getLogger(ExploradorDeRecursosEPrompts.class);

   private final McpSyncClient    cliente;
   private final ArgumentPrompter prompter;
   private final PrintStream      saida;

   // listas mostradas por último, para escolher por número
   private List<Resource> recursos = List.of();
   private List<Prompt>   prompts  = List.of();

   public ExploradorDeRecursosEPrompts(McpSyncClient cliente,
                                       ArgumentPrompter prompter,
                                       PrintStream saida) {
      this.cliente  = cliente;
      this.prompter = prompter;
      this.saida    = saida;
   }

   /** Comando "recursos": mostra o endereço e a descrição de cada resource. */
   public void listarRecursos() {
      recursos = cliente.listResources().resources();

      saida.println();
      saida.println("Resources disponíveis (use: ler <número ou endereço>):");
      if (recursos.isEmpty())
         saida.println(" Nenhum resource disponibilizado.");
      for (int i = 0; i < recursos.size(); i++)
         saida.printf(" %d - %s%n     %s%n", i + 1, recursos.get(i).uri(),
                      recursos.get(i).description());
   }

   /** Comando "ler": mostra o conteúdo do resource escolhido. */
   public void lerRecurso(String alvo) {
      if (recursos.isEmpty())
         recursos = cliente.listResources().resources();

      Optional<Resource> escolhido = escolher(recursos, alvo,
                                              Resource::uri);
      // se o usuário digitou um endereço que não está na lista, tenta mesmo
      // assim: o servidor responde com erro se ele não existir
      String uri = escolhido.map(Resource::uri).orElse(alvo);

      try {
         ReadResourceResult resultado = cliente.readResource(
                                             new ReadResourceRequest(uri));

         saida.println();
         saida.println("=============== RESOURCE ===============");
         for (ResourceContents conteudo : resultado.contents())
            saida.println(textoDe(conteudo));
         saida.println("========================================");
      }
      catch (Exception e) {
         avisarErro("ler o resource '" + uri + "'", e);
      }
   }

   /** Comando "prompts": mostra o nome e a descrição de cada prompt. */
   public void listarPrompts() {
      prompts = cliente.listPrompts().prompts();

      saida.println();
      saida.println("Prompts disponíveis (use: prompt <número ou nome>):");
      if (prompts.isEmpty())
         saida.println(" Nenhum prompt disponibilizado.");
      for (int i = 0; i < prompts.size(); i++)
         saida.printf(" %d - %s%n     %s%n", i + 1, prompts.get(i).name(),
                      prompts.get(i).description());
   }

   /** Comando "prompt": pede os argumentos e mostra a instrução montada. */
   public void obterPrompt(String alvo) {
      if (prompts.isEmpty())
         prompts = cliente.listPrompts().prompts();

      Optional<Prompt> escolhido = escolher(prompts, alvo, Prompt::name);

      if (escolhido.isEmpty()) {
         saida.println("Prompt não encontrado: " + alvo +
                       " (use o comando prompts para ver a lista)");
         return;
      }

      Prompt              prompt    = escolhido.get();
      Map<String, Object> argumentos = prompter.pedir(prompt);

      try {
         GetPromptResult resultado = cliente.getPrompt(
               new GetPromptRequest(prompt.name(), argumentos));

         saida.println();
         saida.println("================ PROMPT ================");
         for (PromptMessage mensagem : resultado.messages())
            saida.println(mensagem.role() + ": " + textoDe(mensagem.content()));
         saida.println("========================================");
      }
      catch (Exception e) {
         avisarErro("obter o prompt '" + prompt.name() + "'", e);
      }
   }

   /** Escolhe pelo número (a partir de 1) ou pelo texto de identificação. */
   private <T> Optional<T> escolher(List<T> itens, String alvo,
                                    Function<T, String> identificador) {
      try {
         int indice = Integer.parseInt(alvo) - 1;

         if (indice >= 0 && indice < itens.size())
            return Optional.of(itens.get(indice));
      }
      catch (NumberFormatException e) {
         // não é um número: pode ser o endereço ou o nome
      }
      return itens.stream()
                  .filter(item -> identificador.apply(item)
                                               .equalsIgnoreCase(alvo))
                  .findFirst();
   }

   private String textoDe(ResourceContents conteudo) {
      if (conteudo instanceof TextResourceContents texto)
         return texto.text();
      return String.valueOf(conteudo);
   }

   private String textoDe(Content conteudo) {
      if (conteudo instanceof TextContent texto)
         return texto.text();
      return String.valueOf(conteudo);
   }

   private void avisarErro(String acao, Exception e) {
      saida.println("Não foi possível " + acao + ": " + e.getMessage());
      LOG.warn("Falha ao {}", acao, e);
   }
}
