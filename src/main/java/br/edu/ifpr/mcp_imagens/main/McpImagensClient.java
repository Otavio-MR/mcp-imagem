package br.edu.ifpr.mcp_imagens.main;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_imagens.client.ArgumentPrompter;
import br.edu.ifpr.mcp_imagens.client.ConversorDeValores;
import br.edu.ifpr.mcp_imagens.client.ExploradorDeRecursosEPrompts;
import br.edu.ifpr.mcp_imagens.client.FabricaDeClienteMcp;
import br.edu.ifpr.mcp_imagens.client.MenuConsole;
import br.edu.ifpr.mcp_imagens.client.ResultPrinter;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Cliente MCP interativo (console): lista as ferramentas do servidor, pede os
 * parâmetros pelo schema da ferramenta escolhida e mostra o resultado.
 * <p>
 * Esta classe só coordena o ciclo "menu, escolha, parâmetros, execução,
 * resultado"; cada passo é feito por uma classe do pacote {@code client}.
 */
public class McpImagensClient {
   private static final Logger LOG =
                                 LoggerFactory.getLogger(McpImagensClient.class);

   private final McpSyncClient    cliente;
   private final Scanner          entrada;
   private final PrintStream      saida;
   private final MenuConsole      menu;
   private final ArgumentPrompter prompter;
   private final ResultPrinter    impressor;
   private final ExploradorDeRecursosEPrompts explorador;

   public McpImagensClient(McpSyncClient cliente, Scanner entrada,
                           PrintStream saida) {
      this.cliente   = cliente;
      this.entrada   = entrada;
      this.saida     = saida;
      this.menu      = new MenuConsole(saida);
      this.prompter  = new ArgumentPrompter(entrada, saida,
                                            new ConversorDeValores());
      this.impressor = new ResultPrinter(saida);
      this.explorador = new ExploradorDeRecursosEPrompts(cliente, prompter,
                                                         saida);
   }

   private void executar() {
      cliente.initialize();
      // nível mínimo de log que o cliente quer receber do servidor
      cliente.setLoggingLevel(LoggingLevel.INFO);
      repetirMenu();
   }

   private void encerrar() {
      LOG.info("Fechando cliente MCP...");
      cliente.closeGracefully();
      LOG.info("Cliente MCP fechado.");
   }

   /** Repete o menu até o usuário digitar "sair" ou a entrada acabar. */
   private void repetirMenu() {
      List<Tool> ferramentas = carregarFerramentas();

      while (true) {
         menu.mostrar(ferramentas);
         saida.print("> ");
         if (!entrada.hasNextLine())
            return;

         String comando = entrada.nextLine().trim();

         if (comando.isBlank())
            continue;
         if ("sair".equalsIgnoreCase(comando))
            return;
         if ("listar".equalsIgnoreCase(comando)) {
            ferramentas = carregarFerramentas();
            continue;
         }
         if (executarComandoExtra(comando))
            continue;

         Optional<Tool> escolhida = menu.escolher(ferramentas, comando);

         if (escolhida.isPresent())
            executarFerramenta(escolhida.get());
         else
            saida.println("Ferramenta não encontrada: " + comando);
      }
   }

   /**
    * Comandos opcionais (resources e prompts).
    *
    * @return {@code true} se o texto era um desses comandos
    */
   private boolean executarComandoExtra(String comando) {
      String[] partes = comando.split("\\s+", 2);
      String   nome   = partes[0].toLowerCase();
      String   alvo   = partes.length > 1 ? partes[1] : "";

      switch (nome) {
         case "recursos" -> explorador.listarRecursos();
         case "prompts"  -> explorador.listarPrompts();
         case "ler"      -> comAlvo(nome, alvo, explorador::lerRecurso);
         case "prompt"   -> comAlvo(nome, alvo, explorador::obterPrompt);
         default         -> { return false; }
      }
      return true;
   }

   private void comAlvo(String comando, String alvo,
                        Consumer<String> acao) {
      if (alvo.isBlank())
         saida.println("Informe o número ou o nome depois de '" + comando +
                       "'.");
      else
         acao.accept(alvo.trim());
   }

   private List<Tool> carregarFerramentas() {
      List<Tool> ferramentas = cliente.listTools().tools();

      LOG.info("Servidor disponibilizou {} ferramenta(s).",
               ferramentas.size());
      return ferramentas;
   }

   private void executarFerramenta(Tool ferramenta) {
      saida.println();
      saida.println("----------------------------------------");
      saida.println("Ferramenta: " + ferramenta.name());
      if (ferramenta.description() != null)
         saida.println("Descrição: " + ferramenta.description());

      Map<String, Object> argumentos = prompter.pedir(ferramenta);

      LOG.info("Executando ferramenta '{}'", ferramenta.name());
      try {
         CallToolResult resultado = cliente.callTool(
               CallToolRequest.builder(ferramenta.name())
                              .arguments(argumentos)
                              .build());

         impressor.mostrar(resultado);
      }
      catch (Exception e) {
         LOG.error("Erro executando ferramenta '{}'", ferramenta.name(), e);
      }
   }

   public static void main(String... args) {
      McpImagensClient cliente = new McpImagensClient(
            new FabricaDeClienteMcp().criar(), new Scanner(System.in),
            System.out);

      try {
         cliente.executar();
      }
      finally {
         cliente.encerrar();
      }
   }
}
