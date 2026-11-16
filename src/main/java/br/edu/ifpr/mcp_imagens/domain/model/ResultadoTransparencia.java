package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Quantos pixels de uma imagem são transparentes, semitransparentes e opacos.
 * A soma dos três é o total de pixels.
 *
 * @param transparentes      pixels com alfa 0
 * @param semitransparentes  pixels com alfa de 1 a 254
 * @param opacos             pixels com alfa 255
 */
public record ResultadoTransparencia(long transparentes,
                                     long semitransparentes, long opacos) {
   public long total() {
      return transparentes + semitransparentes + opacos;
   }
}
