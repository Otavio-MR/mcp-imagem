package br.edu.ifpr.mcp_imagens.domain.model;

import java.math.BigDecimal;

/**
 * Uma faixa do histograma de brilho.
 *
 * @param indice      posição da faixa, a partir de 0 (a mais escura)
 * @param lumaMinima  menor brilho inteiro (0 a 255) que cai na faixa
 * @param lumaMaxima  maior brilho inteiro (0 a 255) que cai na faixa
 * @param quantidade  pixels da faixa
 * @param percentual  {@code quantidade} sobre o total considerado, em %, com
 *                    2 casas
 */
public record FaixaDeBrilho(int indice, int lumaMinima, int lumaMaxima,
                            long quantidade, BigDecimal percentual) {
}
