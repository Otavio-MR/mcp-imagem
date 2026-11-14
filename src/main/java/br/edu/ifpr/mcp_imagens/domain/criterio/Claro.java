package br.edu.ifpr.mcp_imagens.domain.criterio;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;

/**
 * Aceita os pixels claros: brilho (luma) maior ou igual ao limiar.
 * <p>
 * Os pixels escuros são os que este critério recusa; use {@link #negado()}.
 */
public final class Claro implements CriterioDePixel {
   private final int limiar;

   /**
    * @param limiar  brilho mínimo para ser considerado claro, de 0 a 255
    * @throws ParametroInvalidoException se o limiar estiver fora da faixa
    */
   public Claro(int limiar) {
      if (limiar < 0 || limiar > 255)
         throw new ParametroInvalidoException("o limiar deve estar entre 0 e " +
                                              "255 (recebido: " + limiar +
                                              ")");
      this.limiar = limiar;
   }

   @Override
   public boolean aceita(int argb) {
      return Cor.luma(argb) >= limiar;
   }
}
