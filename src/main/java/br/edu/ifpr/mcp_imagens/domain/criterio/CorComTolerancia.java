package br.edu.ifpr.mcp_imagens.domain.criterio;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;

/**
 * Aceita os pixels de uma cor, com uma tolerância.
 * <p>
 * A tolerância é a diferença máxima permitida <b>por canal</b> (vermelho,
 * verde e azul): com tolerância 1, o pixel {@code #FE0000} conta como
 * {@code #FF0000}. O alfa não é comparado: um pixel semitransparente com a
 * mesma cor conta. Para desconsiderar os transparentes, use a opção de
 * ignorá-los na contagem.
 */
public final class CorComTolerancia implements CriterioDePixel {
   /** Menor tolerância: só a cor exata. */
   public static final int TOLERANCIA_MINIMA = 0;
   /** Maior tolerância: qualquer cor serve. */
   public static final int TOLERANCIA_MAXIMA = 255;
   /** Tolerância usada quando a ferramenta não recebe uma. */
   public static final int TOLERANCIA_PADRAO = 0;

   private final int r;
   private final int g;
   private final int b;
   private final int tolerancia;

   /**
    * @param alvo        a cor procurada
    * @param tolerancia  diferença máxima por canal, de 0 a 255
    * @throws ParametroInvalidoException se a tolerância estiver fora da faixa
    */
   public CorComTolerancia(Cor alvo, int tolerancia) {
      if (tolerancia < TOLERANCIA_MINIMA || tolerancia > TOLERANCIA_MAXIMA)
         throw new ParametroInvalidoException("a tolerância deve estar entre " +
                                              TOLERANCIA_MINIMA + " e " +
                                              TOLERANCIA_MAXIMA +
                                              " (recebido: " + tolerancia +
                                              ")");
      this.r          = alvo.r();
      this.g          = alvo.g();
      this.b          = alvo.b();
      this.tolerancia = tolerancia;
   }

   @Override
   public boolean aceita(int argb) {
      return Math.abs(((argb >> 16) & 0xFF) - r) <= tolerancia &&
             Math.abs(((argb >> 8) & 0xFF) - g) <= tolerancia &&
             Math.abs((argb & 0xFF) - b) <= tolerancia;
   }
}
