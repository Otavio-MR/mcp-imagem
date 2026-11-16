package br.edu.ifpr.mcp_imagens.client;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Pergunta ao usuário o valor de cada parâmetro de uma ferramenta, lendo os
 * nomes, os tipos e as descrições do schema JSON dela. Por isso funciona com
 * qualquer ferramenta do servidor.
 */
public class ArgumentPrompter {
   private final Scanner            entrada;
   private final PrintStream        saida;
   private final ConversorDeValores conversor;

   public ArgumentPrompter(Scanner entrada, PrintStream saida,
                           ConversorDeValores conversor) {
      this.entrada   = entrada;
      this.saida     = saida;
      this.conversor = conversor;
   }

   /**
    * Pede os parâmetros da ferramenta. Parâmetros opcionais deixados em branco
    * não entram no resultado (o servidor usa o valor padrão).
    */
   public Map<String, Object> pedir(Tool ferramenta) {
      return pedir(ferramenta.inputSchema());
   }

   /**
    * Pede os argumentos de um prompt. Todos são texto; o que muda é só se são
    * obrigatórios.
    */
   public Map<String, Object> pedir(Prompt prompt) {
      Map<String, Object> propriedades = new LinkedHashMap<>();
      List<String>        obrigatorios = new ArrayList<>();

      for (PromptArgument argumento : prompt.arguments()) {
         propriedades.put(argumento.name(), Map.of(
               "type", "string",
               "description", String.valueOf(argumento.description())));
         if (Boolean.TRUE.equals(argumento.required()))
            obrigatorios.add(argumento.name());
      }
      return pedir(Map.of("properties", propriedades,
                          "required", obrigatorios));
   }

   private Map<String, Object> pedir(Map<?, ?> schema) {
      Map<String, Object> argumentos   = new LinkedHashMap<>();
      Map<?, ?>           propriedades = mapaEm(schema, "properties");

      if (propriedades.isEmpty())
         return argumentos;

      List<?> obrigatorios = listaEm(schema, "required");

      saida.println();
      saida.println("Informe os parâmetros:");
      for (Map.Entry<?, ?> item : propriedades.entrySet()) {
         String nome = String.valueOf(item.getKey());

         if (!(item.getValue() instanceof Map<?, ?> propriedade))
            continue;

         Object valor = pedirValor(nome, propriedade,
                                   obrigatorios.contains(nome));

         if (valor != null)
            argumentos.put(nome, valor);
      }
      return argumentos;
   }

   /** Repete a pergunta até receber um valor válido (ou o fim da entrada). */
   private Object pedirValor(String nome, Map<?, ?> propriedade,
                             boolean obrigatorio) {
      String tipo = textoEm(propriedade, "type", "string");

      while (true) {
         saida.println();
         saida.println("'" + nome + "' [" + tipo + "] " +
                       (obrigatorio ? "(obrigatório)" : "(opcional)") +
                       dicas(propriedade));
         saida.println("Descrição: " + textoEm(propriedade, "description",
                                               ""));
         saida.print("Valor: ");

         if (!entrada.hasNextLine())
            return null;

         String digitado = entrada.nextLine().trim();

         if (digitado.isBlank()) {
            if (!obrigatorio)
               return null;

            saida.println("Este parâmetro é obrigatório.");
            continue;
         }
         try {
            return conversor.converter(digitado, tipo);
         }
         catch (IllegalArgumentException e) {
            saida.println("Valor inválido: " + e.getMessage());
         }
      }
   }

   /** Texto com o valor padrão e a faixa permitida, se o schema os tem. */
   private String dicas(Map<?, ?> propriedade) {
      StringBuilder dicas = new StringBuilder();

      if (propriedade.containsKey("default"))
         dicas.append(" [padrão: ").append(propriedade.get("default"))
              .append("]");
      if (propriedade.containsKey("minimum") &&
          propriedade.containsKey("maximum"))
         dicas.append(" [de ").append(propriedade.get("minimum"))
              .append(" a ").append(propriedade.get("maximum")).append("]");
      return dicas.toString();
   }

   private Map<?, ?> mapaEm(Map<?, ?> mapa, String chave) {
      if (mapa != null && mapa.get(chave) instanceof Map<?, ?> encontrado)
         return encontrado;
      return Map.of();
   }

   private List<?> listaEm(Map<?, ?> mapa, String chave) {
      if (mapa != null && mapa.get(chave) instanceof List<?> encontrada)
         return encontrada;
      return List.of();
   }

   private String textoEm(Map<?, ?> mapa, String chave, String padrao) {
      Object valor = mapa.get(chave);

      return valor != null ? String.valueOf(valor) : padrao;
   }
}
