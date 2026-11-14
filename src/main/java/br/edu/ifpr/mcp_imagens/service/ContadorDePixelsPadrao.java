package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.criterio.CriterioDePixel;
import br.edu.ifpr.mcp_imagens.domain.model.Arredondamento;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContagem;

/**
 * Implementação de {@link ContadorDePixels}: percorre a imagem linha por
 * linha e pergunta ao critério, pixel a pixel, se ele conta.
 */
public class ContadorDePixelsPadrao implements ContadorDePixels {
   @Override
   public ResultadoContagem contar(Imagem imagem, CriterioDePixel criterio,
                                   boolean ignorarTransparentes) {
      long quantidade = 0;
      long considerado = 0;

      for (int y = 0; y < imagem.altura(); y++)
         for (int x = 0; x < imagem.largura(); x++) {
            int argb = imagem.argb(x, y);

            if (ignorarTransparentes && (argb >>> 24) == 0)
               continue;

            considerado++;
            if (criterio.aceita(argb))
               quantidade++;
         }
      return new ResultadoContagem(quantidade, considerado,
                                   Arredondamento.percentual(quantidade,
                                                             considerado));
   }
}
