package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.exception.DimensoesDiferentesException;
import br.edu.ifpr.mcp_imagens.domain.model.CaixaDelimitadora;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoComparacao;

/**
 * Implementação de {@link ComparadorDeImagens}.
 */
public class ComparadorDeImagensPadrao implements ComparadorDeImagens {
   @Override
   public ResultadoComparacao comparar(Imagem a, Imagem b, int tolerancia) {
      if (a.largura() != b.largura() || a.altura() != b.altura())
         throw new DimensoesDiferentesException(
               "as imagens precisam ter o mesmo tamanho: a primeira tem " +
               a.largura() + "x" + a.altura() + " e a segunda tem " +
               b.largura() + "x" + b.altura());

      long diferentes = 0;
      int  xMin = Integer.MAX_VALUE, yMin = Integer.MAX_VALUE;
      int  xMax = -1, yMax = -1;

      for (int y = 0; y < a.altura(); y++)
         for (int x = 0; x < a.largura(); x++)
            if (diferem(a.argb(x, y), b.argb(x, y), tolerancia)) {
               diferentes++;
               xMin = Math.min(xMin, x);
               yMin = Math.min(yMin, y);
               xMax = Math.max(xMax, x);
               yMax = Math.max(yMax, y);
            }

      CaixaDelimitadora regiao = diferentes == 0
            ? null
            : new CaixaDelimitadora(xMin, yMin, xMax, yMax);

      return new ResultadoComparacao(a.totalPixels(), diferentes, regiao);
   }

   /** {@code true} se algum canal (A, R, G ou B) difere além da tolerância. */
   private boolean diferem(int pixelA, int pixelB, int tolerancia) {
      for (int deslocamento = 0; deslocamento <= 24; deslocamento += 8) {
         int canalA = (pixelA >>> deslocamento) & 0xFF;
         int canalB = (pixelB >>> deslocamento) & 0xFF;

         if (Math.abs(canalA - canalB) > tolerancia)
            return true;
      }
      return false;
   }
}
