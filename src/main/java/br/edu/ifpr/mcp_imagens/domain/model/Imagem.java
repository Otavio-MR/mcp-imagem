package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Abstração de leitura dos pixels de uma imagem.
 * <p>
 * Os serviços de análise dependem desta interface, e não de
 * {@code BufferedImage}. Assim eles podem ser testados com imagens sintéticas
 * criadas em memória, sem arquivos e sem {@code ImageIO}.
 * <p>
 * Os pixels seguem a convenção de {@code BufferedImage.getRGB}: valor ARGB em
 * sRGB, com alfa 0 (transparente) a 255 (opaco).
 */
public interface Imagem {
   /** Largura em pixels. */
   int largura();

   /** Altura em pixels. */
   int altura();

   /** {@code true} se a imagem tem canal alfa (transparência). */
   boolean temCanalAlfa();

   /**
    * Pixel na coluna {@code x} e linha {@code y}, a partir de (0, 0) no canto
    * superior esquerdo.
    */
   int argb(int x, int y);

   /** Total de pixels (largura x altura). */
   default long totalPixels() {
      return (long) largura() * altura();
   }
}
