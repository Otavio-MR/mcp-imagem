package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Dados de uma imagem da pasta.
 *
 * @param arquivo        nome do arquivo
 * @param formato        formato, pela extensão
 * @param largura        largura em pixels
 * @param altura         altura em pixels
 * @param totalPixels    largura x altura
 * @param temCanalAlfa   {@code true} se a imagem tem canal alfa
 *                       (transparência)
 * @param tamanhoBytes   tamanho do arquivo em bytes
 * @param tamanhoKb      tamanho do arquivo em KB, arredondado para cima
 */
public record InformacoesImagem(String arquivo, FormatoImagem formato,
                                int largura, int altura, long totalPixels,
                                boolean temCanalAlfa, long tamanhoBytes,
                                long tamanhoKb) {
}
