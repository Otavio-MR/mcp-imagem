package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.exception.DimensoesDiferentesException;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoComparacao;

/**
 * Compara duas imagens pixel a pixel.
 */
public interface ComparadorDeImagens {
   int TOLERANCIA_MINIMA = 0;
   int TOLERANCIA_MAXIMA = 255;
   int TOLERANCIA_PADRAO = 0;

   /**
    * Dois pixels são diferentes quando algum dos quatro canais (alfa,
    * vermelho, verde ou azul) difere em mais que a tolerância. Um pixel
    * totalmente transparente é comparado como os outros: as cores dele contam.
    *
    * @param tolerancia diferença máxima aceita por canal (0 = só pixels
    *                   idênticos)
    * @throws DimensoesDiferentesException se as imagens não têm a mesma
    *                                      largura e altura
    */
   ResultadoComparacao comparar(Imagem a, Imagem b, int tolerancia);
}
