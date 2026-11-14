package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Uma imagem encontrada na pasta de imagens.
 *
 * @param nome        nome do arquivo
 * @param formato     formato, pela extensão
 * @param tamanhoKb   tamanho do arquivo em KB, arredondado para cima
 */
public record ImagemInfo(String nome, FormatoImagem formato, long tamanhoKb) {
}
