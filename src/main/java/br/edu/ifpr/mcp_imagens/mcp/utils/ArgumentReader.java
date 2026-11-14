package br.edu.ifpr.mcp_imagens.mcp.utils;

import java.util.Map;
import java.util.Set;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroAusenteException;
import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;

/**
 * Lê e valida os argumentos de uma chamada de ferramenta.
 * <p>
 * Centraliza as conversões (números JSON podem chegar como qualquer
 * {@link Number}, e alguns clientes enviam números e booleanos como texto) e
 * as mensagens de erro, para que cada ferramenta apenas peça o valor de que
 * precisa.
 */
public final class ArgumentReader {
   private final Map<String, Object> argumentos;

   /**
    * @param argumentos  argumentos recebidos (pode ser {@code null})
    * @param permitidos  nomes declarados no schema da ferramenta; qualquer
    *                    outro nome é rejeitado
    * @throws ParametroInvalidoException se houver parâmetro não declarado
    */
   public ArgumentReader(Map<String, Object> argumentos,
                         Set<String> permitidos) {
      this.argumentos = argumentos == null ? Map.of() : argumentos;
      rejeitarDesconhecidos(permitidos);
   }

   /**
    * Texto obrigatório, sem espaços nas pontas.
    *
    * @throws ParametroAusenteException  se ausente ou nulo
    * @throws ParametroInvalidoException se não for texto ou estiver em branco
    */
   public String texto(String nome) {
      Object valor = argumentos.get(nome);

      if (valor == null)
         throw new ParametroAusenteException("'" + nome + "' é obrigatório");
      if (!(valor instanceof String texto))
         throw new ParametroInvalidoException("'" + nome +
                                              "' deve ser um texto");
      if (texto.isBlank())
         throw new ParametroInvalidoException("'" + nome +
                                              "' não pode ser vazio");
      return texto.trim();
   }

   /**
    * Inteiro opcional dentro de [minimo, maximo]: devolve {@code padrao} se
    * ausente ou nulo.
    *
    * @throws ParametroInvalidoException se não for inteiro ou estiver fora
    *                                    dos limites
    */
   public int inteiroOpcional(String nome, int padrao, int minimo,
                              int maximo) {
      Object valor = argumentos.get(nome);

      if (valor == null)
         return padrao;

      int inteiro = paraInteiro(nome, valor);

      if (inteiro < minimo || inteiro > maximo)
         throw new ParametroInvalidoException("'" + nome + "' deve estar " +
                                              "entre " + minimo + " e " +
                                              maximo + " (recebido: " +
                                              inteiro + ")");
      return inteiro;
   }

   /**
    * Booleano opcional: devolve {@code padrao} se ausente ou nulo. Aceita o
    * booleano JSON e os textos "true" e "false" (sem diferenciar maiúsculas).
    *
    * @throws ParametroInvalidoException se o valor não for um booleano
    */
   public boolean booleanoOpcional(String nome, boolean padrao) {
      Object valor = argumentos.get(nome);

      if (valor == null)
         return padrao;
      if (valor instanceof Boolean booleano)
         return booleano;
      if (valor instanceof String texto) {
         if ("true".equalsIgnoreCase(texto.trim()))
            return true;
         if ("false".equalsIgnoreCase(texto.trim()))
            return false;
      }
      throw new ParametroInvalidoException("'" + nome + "' deve ser true " +
                                           "ou false");
   }

   private void rejeitarDesconhecidos(Set<String> permitidos) {
      argumentos.keySet()
                .stream()
                .filter(nome -> !permitidos.contains(nome))
                .findFirst()
                .ifPresent(nome -> {
                   throw new ParametroInvalidoException(
                                "parâmetro desconhecido: '" + nome + "'");
                });
   }

   private static int paraInteiro(String nome, Object valor) {
      if (valor instanceof Number numero && ehInteiro(numero))
         return numero.intValue();

      if (valor instanceof String texto)
         try { return Integer.parseInt(texto.trim()); }
         catch (NumberFormatException ex) { /* tratado abaixo */ }

      throw new ParametroInvalidoException("'" + nome +
                                           "' deve ser um número inteiro");
   }

   // números JSON como 5.0 valem 5; 5.5 não é inteiro
   private static boolean ehInteiro(Number numero) {
      double valor = numero.doubleValue();

      return valor == Math.rint(valor) &&
             valor >= Integer.MIN_VALUE && valor <= Integer.MAX_VALUE;
   }
}
