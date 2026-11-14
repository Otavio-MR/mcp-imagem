package br.edu.ifpr.mcp_imagens.domain.criterio;

/**
 * Critério que decide se um pixel conta ou não (padrão Strategy).
 * <p>
 * O {@code ContadorDePixels} recebe um critério e conta os pixels que ele
 * aceita. Assim "pixels de uma cor", "transparentes", "claros" e "fora da
 * paleta" reutilizam o mesmo algoritmo de contagem, e um novo critério não
 * exige mudar o contador (aberto para extensão, fechado para modificação).
 */
@FunctionalInterface
public interface CriterioDePixel {
   /**
    * @param argb  o pixel no formato de {@code BufferedImage.getRGB}
    * @return {@code true} se o pixel atende ao critério
    */
   boolean aceita(int argb);

   /** Critério inverso: aceita exatamente os pixels que este recusa. */
   default CriterioDePixel negado() {
      return argb -> !aceita(argb);
   }
}
