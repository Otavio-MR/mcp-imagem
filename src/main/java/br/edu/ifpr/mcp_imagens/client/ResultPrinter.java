package br.edu.ifpr.mcp_imagens.client;

import java.io.PrintStream;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Mostra no console o resultado da execução de uma ferramenta.
 */
public class ResultPrinter {
   private final PrintStream saida;

   public ResultPrinter(PrintStream saida) {
      this.saida = saida;
   }

   public void mostrar(CallToolResult resultado) {
      saida.println();
      saida.println("=============== RESULTADO ===============");
      saida.println("Erro: " + (Boolean.TRUE.equals(resultado.isError())
                                ? "Sim" : "Não"));
      saida.println();
      for (Content conteudo : resultado.content())
         saida.println(textoDe(conteudo));
      saida.println("==========================================");
   }

   /** Texto puro quando o conteúdo é texto; senão, a representação dele. */
   private String textoDe(Content conteudo) {
      if (conteudo instanceof TextContent texto)
         return texto.text();
      return String.valueOf(conteudo);
   }
}
