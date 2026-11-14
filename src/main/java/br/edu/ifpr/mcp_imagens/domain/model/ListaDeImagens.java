package br.edu.ifpr.mcp_imagens.domain.model;

import java.util.List;

/**
 * As imagens disponíveis na pasta configurada.
 *
 * @param pasta    caminho absoluto da pasta de imagens
 * @param total    quantidade de imagens
 * @param imagens  as imagens, em ordem alfabética
 */
public record ListaDeImagens(String pasta, int total, List<ImagemInfo> imagens) {
}
