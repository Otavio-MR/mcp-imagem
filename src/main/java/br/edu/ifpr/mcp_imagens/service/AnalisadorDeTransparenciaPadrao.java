package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoTransparencia;

/**
 * Implementação de {@link AnalisadorDeTransparencia}: uma única passada pelos
 * pixels, olhando o canal alfa de cada um.
 */
public class AnalisadorDeTransparenciaPadrao
      implements AnalisadorDeTransparencia {
   @Override
   public ResultadoTransparencia analisar(Imagem imagem) {
      long[] contagem = new long[3]; // transparentes, semitransparentes, opacos

      // false: aqui os transparentes precisam ser percorridos para serem contados
      imagem.paraCadaPixel(false, argb -> {
         int alfa = argb >>> 24;

         if (alfa == 0)
            contagem[0]++;
         else if (alfa == 255)
            contagem[2]++;
         else
            contagem[1]++;
      });
      return new ResultadoTransparencia(contagem[0], contagem[1], contagem[2]);
   }
}
