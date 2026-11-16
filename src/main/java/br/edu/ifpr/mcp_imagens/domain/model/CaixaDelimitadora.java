package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Menor retângulo que contém todos os pixels que diferem entre duas imagens.
 * As coordenadas contam a partir de (0, 0) no canto superior esquerdo e os
 * quatro extremos fazem parte do retângulo.
 */
public record CaixaDelimitadora(int xMin, int yMin, int xMax, int yMax) {
}
