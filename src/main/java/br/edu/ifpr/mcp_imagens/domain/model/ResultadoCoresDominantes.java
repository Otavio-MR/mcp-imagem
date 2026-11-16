package br.edu.ifpr.mcp_imagens.domain.model;

import java.util.List;

/**
 * Resultado da busca pelas cores dominantes de uma imagem.
 *
 * @param totalConsiderado  pixels analisados (sem os transparentes, se eles
 *                          foram ignorados)
 * @param gruposDistintos   quantos grupos de cores existem na imagem (pode ser
 *                          maior que o tamanho de {@code cores})
 * @param cores             os grupos mais frequentes, do maior para o menor
 */
public record ResultadoCoresDominantes(long totalConsiderado,
                                       int gruposDistintos,
                                       List<CorDominante> cores) {
}
