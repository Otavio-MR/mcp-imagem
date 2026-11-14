package br.edu.ifpr.mcp_imagens.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento dos números devolvidos pelas ferramentas: sempre 2 casas
 * decimais, com arredondamento {@code HALF_UP} (0,005 vira 0,01).
 * <p>
 * O resultado é um {@link BigDecimal}, que o JSON escreve com as duas casas
 * (21.00, e não 21.0).
 */
public final class Arredondamento {
   private Arredondamento() {}

   public static BigDecimal duasCasas(double valor) {
      return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
   }

   /**
    * Percentual de {@code parte} em {@code total}, com 2 casas. A divisão é
    * exata (sem passar por {@code double}). Total zero resulta em 0,00.
    */
   public static BigDecimal percentual(long parte, long total) {
      if (total <= 0)
         return BigDecimal.ZERO.setScale(2);

      return BigDecimal.valueOf(parte)
                       .multiply(BigDecimal.valueOf(100))
                       .divide(BigDecimal.valueOf(total), 2,
                               RoundingMode.HALF_UP);
   }
}
