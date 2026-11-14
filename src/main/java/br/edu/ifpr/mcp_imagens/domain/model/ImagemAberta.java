package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Uma imagem da pasta já lida, com os dados do arquivo.
 *
 * @param nome          nome do arquivo
 * @param formato       formato, pela extensão
 * @param tamanhoBytes  tamanho do arquivo em bytes
 * @param imagem        os pixels
 */
public record ImagemAberta(String nome, FormatoImagem formato,
                           long tamanhoBytes, Imagem imagem) {
}
