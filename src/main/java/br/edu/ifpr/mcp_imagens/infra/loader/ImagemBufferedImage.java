package br.edu.ifpr.mcp_imagens.infra.loader;

import java.awt.image.BufferedImage;

import br.edu.ifpr.mcp_imagens.domain.model.Imagem;

/**
 * {@link Imagem} que lê os pixels de um {@link BufferedImage} do JDK, sem
 * copiá-los (uma imagem de 25 milhões de pixels ocupa uns 100 MB).
 */
public class ImagemBufferedImage implements Imagem {
   private final BufferedImage imagem;

   public ImagemBufferedImage(BufferedImage imagem) {
      this.imagem = imagem;
   }

   @Override
   public int largura() {
      return imagem.getWidth();
   }

   @Override
   public int altura() {
      return imagem.getHeight();
   }

   @Override
   public boolean temCanalAlfa() {
      return imagem.getColorModel().hasAlpha();
   }

   @Override
   public int argb(int x, int y) {
      return imagem.getRGB(x, y);
   }
}
