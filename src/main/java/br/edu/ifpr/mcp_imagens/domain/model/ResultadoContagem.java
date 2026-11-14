package br.edu.ifpr.mcp_imagens.domain.model;

import java.math.BigDecimal;

/**
 * Resultado da contagem de pixels que atendem a um critério.
 *
 * @param quantidade        pixels que atendem ao critério
 * @param totalConsiderado  pixels analisados (todos, ou só os não
 *                          transparentes, se os transparentes foram ignorados)
 * @param percentual        {@code quantidade} sobre {@code totalConsiderado},
 *                          em %, com 2 casas
 */
public record ResultadoContagem(long quantidade, long totalConsiderado,
                                BigDecimal percentual) {
}
