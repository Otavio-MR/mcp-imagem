package br.edu.ifpr.mcp_imagens.domain.criterio;

/**
 * Aceita os pixels totalmente transparentes (alfa 0).
 */
public final class Transparente implements CriterioDePixel {
   @Override
   public boolean aceita(int argb) {
      return (argb >>> 24) == 0;
   }
}
