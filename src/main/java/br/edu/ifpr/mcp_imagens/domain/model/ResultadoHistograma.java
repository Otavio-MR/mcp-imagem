package br.edu.ifpr.mcp_imagens.domain.model;

import java.util.List;

/**
 * Distribuição dos pixels de uma imagem por faixa de brilho.
 *
 * @param totalConsiderado  pixels analisados (sem os transparentes, se eles
 *                          foram ignorados)
 * @param faixas            todas as faixas, da mais escura à mais clara, mesmo
 *                          as vazias
 */
public record ResultadoHistograma(long totalConsiderado,
                                  List<FaixaDeBrilho> faixas) {
}
