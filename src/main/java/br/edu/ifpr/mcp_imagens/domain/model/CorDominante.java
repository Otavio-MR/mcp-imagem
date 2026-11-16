package br.edu.ifpr.mcp_imagens.domain.model;

import java.math.BigDecimal;

/**
 * Um grupo de cores parecidas e quanto da imagem ele ocupa.
 *
 * @param cor         cor representativa do grupo (o centro da faixa de cada
 *                    canal), em {@code #RRGGBB}
 * @param quantidade  pixels que pertencem ao grupo
 * @param percentual  {@code quantidade} sobre o total considerado, em %, com
 *                    2 casas
 */
public record CorDominante(String cor, long quantidade, BigDecimal percentual) {
}
