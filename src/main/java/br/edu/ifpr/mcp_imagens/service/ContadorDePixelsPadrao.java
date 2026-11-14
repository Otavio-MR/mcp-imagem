package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.criterio.CriterioDePixel;
import br.edu.ifpr.mcp_imagens.domain.model.Arredondamento;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContagem;

/**
 * Implementação de {@link ContadorDePixels}: percorre a imagem e pergunta ao
 * critério, pixel a pixel, se ele conta.
 */
public class ContadorDePixelsPadrao implements ContadorDePixels {
   /** Acumuladores da contagem em andamento. */
   private static final class Contagem {
      long considerado;
      long quantidade;
   }

   @Override
   public ResultadoContagem contar(Imagem imagem, CriterioDePixel criterio,
                                   boolean ignorarTransparentes) {
      Contagem contagem = new Contagem();

      imagem.paraCadaPixel(ignorarTransparentes, argb -> {
         contagem.considerado++;
         if (criterio.aceita(argb))
            contagem.quantidade++;
      });
      return new ResultadoContagem(contagem.quantidade, contagem.considerado,
                                   Arredondamento.percentual(
                                         contagem.quantidade,
                                         contagem.considerado));
   }
}
