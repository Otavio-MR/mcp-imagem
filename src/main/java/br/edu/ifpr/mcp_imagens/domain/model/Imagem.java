package br.edu.ifpr.mcp_imagens.domain.model;

import java.util.function.IntConsumer;

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

   /**
    * Percorre os pixels linha por linha, da esquerda para a direita, e entrega
    * cada um (ARGB) à ação.
    *
    * @param ignorarTransparentes se {@code true}, os pixels totalmente
    *                             transparentes (alfa 0) são pulados
    * @param acao                 o que fazer com cada pixel
    */
   default void paraCadaPixel(boolean ignorarTransparentes, IntConsumer acao) {
      for (int y = 0; y < altura(); y++)
         for (int x = 0; x < largura(); x++) {
            int argb = argb(x, y);

            if (!ignorarTransparentes || (argb >>> 24) != 0)
               acao.accept(argb);
         }
   }
}
