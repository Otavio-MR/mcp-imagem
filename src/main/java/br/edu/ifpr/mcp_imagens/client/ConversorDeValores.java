package br.edu.ifpr.mcp_imagens.client;

/**
 * Converte o texto digitado pelo usuário para o tipo do parâmetro, conforme o
 * tipo declarado no schema da ferramenta ({@code string}, {@code integer},
 * {@code number} ou {@code boolean}).
 */
public class ConversorDeValores {
   /**
    * @param valor texto digitado (não vazio)
    * @param tipo  tipo do schema JSON
    * @throws IllegalArgumentException se o texto não serve para o tipo; a
    *                                  mensagem pode ser mostrada ao usuário
    */
   public Object converter(String valor, String tipo) {
      return switch (tipo) {
         case "string"  -> valor;
         case "integer" -> inteiro(valor);
         case "number"  -> numero(valor);
         case "boolean" -> booleano(valor);
         default -> throw new IllegalArgumentException(
               "tipo não suportado pelo modo interativo: " + tipo);
      };
   }

   private Integer inteiro(String valor) {
      try {
         return Integer.valueOf(valor);
      }
      catch (NumberFormatException e) {
         throw new IllegalArgumentException("informe um número inteiro");
      }
   }

   private Double numero(String valor) {
      try {
         return Double.valueOf(valor);
      }
      catch (NumberFormatException e) {
         throw new IllegalArgumentException("informe um número");
      }
   }

   private Boolean booleano(String valor) {
      if ("true".equalsIgnoreCase(valor) || "sim".equalsIgnoreCase(valor))
         return true;

      if ("false".equalsIgnoreCase(valor) || "não".equalsIgnoreCase(valor) ||
          "nao".equalsIgnoreCase(valor))
         return false;

      throw new IllegalArgumentException("informe true/false ou sim/não");
   }
}
