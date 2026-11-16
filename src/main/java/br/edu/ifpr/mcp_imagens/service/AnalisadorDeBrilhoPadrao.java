package br.edu.ifpr.mcp_imagens.service;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.Arredondamento;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.FaixaDeBrilho;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoHistograma;

/**
 * Implementação de {@link AnalisadorDeBrilho}.
 */
public class AnalisadorDeBrilhoPadrao implements AnalisadorDeBrilho {
   @Override
   public ResultadoHistograma histograma(Imagem imagem, int faixas,
                                         boolean ignorarTransparentes) {
      long[] contagem = new long[faixas];
      long[] total    = new long[1];

      imagem.paraCadaPixel(ignorarTransparentes, argb -> {
         int brilho = (int) Cor.luma(argb);   // 0 a 255

         contagem[brilho * faixas / 256]++;
         total[0]++;
      });

      List<FaixaDeBrilho> resultado = new ArrayList<>();

      for (int faixa = 0; faixa < faixas; faixa++)
         resultado.add(new FaixaDeBrilho(faixa,
                                         inicioDaFaixa(faixa, faixas),
                                         inicioDaFaixa(faixa + 1, faixas) - 1,
                                         contagem[faixa],
                                         Arredondamento.percentual(
                                               contagem[faixa], total[0])));
      return new ResultadoHistograma(total[0], resultado);
   }

   /**
    * Menor brilho inteiro que cai na faixa: o menor {@code b} com
    * {@code b * faixas / 256 >= faixa}, ou seja, {@code faixa * 256 / faixas}
    * arredondado para cima.
    */
   private int inicioDaFaixa(int faixa, int faixas) {
      return (faixa * 256 + faixas - 1) / faixas;
   }
}
