package br.edu.ifpr.mcp_imagens.domain.model;

import java.math.BigDecimal;

/**
 * Uma parte de um total de pixels: a quantidade e o percentual dela.
 *
 * @param quantidade  pixels da parte
 * @param percentual  {@code quantidade} sobre o total, em %, com 2 casas
 */
public record Parcela(long quantidade, BigDecimal percentual) {
   /** A parcela de {@code quantidade} pixels em um total de {@code total}. */
   public static Parcela de(long quantidade, long total) {
      return new Parcela(quantidade,
                         Arredondamento.percentual(quantidade, total));
   }
}
