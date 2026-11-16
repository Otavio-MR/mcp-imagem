package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoCoresDominantes;

/**
 * Descobre quais são as cores que mais aparecem em uma imagem.
 */
public interface AnalisadorDeCores {
   int QUANTIDADE_MINIMA = 1;
   int QUANTIDADE_MAXIMA = 20;
   int QUANTIDADE_PADRAO = 5;

   int PASSO_MINIMO = 1;
   int PASSO_MAXIMO = 128;
   int PASSO_PADRAO = 32;

   /**
    * Agrupa as cores parecidas e devolve os grupos mais frequentes.
    * <p>
    * Cada canal (vermelho, verde, azul) é dividido em faixas de tamanho
    * {@code passo}; pixels cujos três canais caem nas mesmas faixas formam um
    * grupo. Com passo 1 não há agrupamento: cada cor exata é um grupo.
    *
    * @param quantidade           quantos grupos devolver
    * @param passo                largura de cada faixa de canal (1 a 128)
    * @param ignorarTransparentes se {@code true}, pixels com alfa 0 não entram
    */
   ResultadoCoresDominantes coresDominantes(Imagem imagem, int quantidade,
                                            int passo,
                                            boolean ignorarTransparentes);
}
