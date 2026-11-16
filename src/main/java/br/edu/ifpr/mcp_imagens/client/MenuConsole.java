package br.edu.ifpr.mcp_imagens.client;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Mostra o menu de ferramentas e interpreta o que o usuário digitou para
 * escolher uma delas.
 */
public class MenuConsole {
   private static final int TAMANHO_MAXIMO_DO_RESUMO = 90;

   private final PrintStream saida;

   public MenuConsole(PrintStream saida) {
      this.saida = saida;
   }

   public void mostrar(List<Tool> ferramentas) {
      saida.println();
      saida.println("========================================");
      saida.println(" CLIENTE MCP");
      saida.println("========================================");

      if (ferramentas.isEmpty())
         saida.println("Nenhuma ferramenta disponibilizada.");
      else {
         saida.println("Ferramentas disponíveis:");
         for (int i = 0; i < ferramentas.size(); i++)
            saida.printf(" %d - %s%n", i + 1, linhaDe(ferramentas.get(i)));
      }
      saida.println();
      saida.println("Digite o número ou o nome da ferramenta. Comandos:");
      saida.println(" listar - atualizar lista de ferramentas");
      saida.println(" sair   - encerrar o cliente");
      saida.println();
   }

   /**
    * Procura a ferramenta pelo número (a partir de 1, como no menu) ou pelo
    * nome, sem diferenciar maiúsculas.
    */
   public Optional<Tool> escolher(List<Tool> ferramentas, String comando) {
      try {
         int indice = Integer.parseInt(comando) - 1;

         if (indice >= 0 && indice < ferramentas.size())
            return Optional.of(ferramentas.get(indice));
      }
      catch (NumberFormatException e) {
         // não é um número: o comando pode ser o nome da ferramenta
      }
      return ferramentas.stream()
                        .filter(f -> f.name().equalsIgnoreCase(comando))
                        .findFirst();
   }

   /** Nome da ferramenta e a primeira frase da descrição. */
   private String linhaDe(Tool ferramenta) {
      String descricao = ferramenta.description();

      if (descricao == null || descricao.isBlank())
         return ferramenta.name();

      return ferramenta.name() + ": " + resumo(descricao);
   }

   private String resumo(String descricao) {
      int fim = descricao.indexOf(". ");
      String frase = fim < 0 ? descricao : descricao.substring(0, fim + 1);

      if (frase.length() <= TAMANHO_MAXIMO_DO_RESUMO)
         return frase;
      return frase.substring(0, TAMANHO_MAXIMO_DO_RESUMO - 3) + "...";
   }
}
