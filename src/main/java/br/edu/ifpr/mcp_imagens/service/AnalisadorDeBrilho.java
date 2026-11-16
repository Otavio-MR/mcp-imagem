package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoHistograma;

/**
 * Mede como o brilho (luma) se distribui pelos pixels de uma imagem.
 */
public interface AnalisadorDeBrilho {
   int FAIXAS_MINIMO = 2;
   int FAIXAS_MAXIMO = 32;
   int FAIXAS_PADRAO = 8;

   /**
    * Divide a escala de brilho (0 a 255) em {@code faixas} partes iguais, da
    * mais escura à mais clara, e conta os pixels de cada uma. O brilho é
    * truncado para inteiro antes de escolher a faixa.
    *
    * @param ignorarTransparentes se {@code true}, pixels com alfa 0 não entram
    */
   ResultadoHistograma histograma(Imagem imagem, int faixas,
                                  boolean ignorarTransparentes);
}
