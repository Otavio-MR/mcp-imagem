package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Resultado da comparação pixel a pixel entre duas imagens do mesmo tamanho.
 *
 * @param totalPixels       pixels de cada imagem
 * @param pixelsDiferentes  pixels que diferem além da tolerância
 * @param regiaoDiferente   menor retângulo com todos os pixels diferentes, ou
 *                          {@code null} se as imagens não diferem
 */
public record ResultadoComparacao(long totalPixels, long pixelsDiferentes,
                                  CaixaDelimitadora regiaoDiferente) {
   public long pixelsIguais() {
      return totalPixels - pixelsDiferentes;
   }
}
