package br.edu.ifpr.mcp_imagens.testutil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;

/**
 * Cria imagens pequenas de verdade (com o {@code ImageIO}) nos testes.
 * <p>
 * JPG e BMP não têm canal alfa, por isso usam RGB; PNG e GIF usam ARGB.
 */
public final class ImagensDeTeste {
   private ImagensDeTeste() {}

   /**
    * Grava uma imagem de uma cor só.
    *
    * @param pasta    pasta onde gravar
    * @param nome     nome do arquivo; a extensão define o formato
    * @param largura  largura em pixels
    * @param altura   altura em pixels
    * @param argb     cor de todos os pixels, no formato de
    *                 {@code BufferedImage.getRGB}
    * @return o caminho do arquivo gravado
    */
   public static Path criar(Path pasta, String nome, int largura, int altura,
                            int argb) {
      String         extensao = nome.substring(nome.lastIndexOf('.') + 1)
                                    .toLowerCase(Locale.ROOT);
      int            tipo     = extensao.equals("jpg") ||
                                extensao.equals("jpeg") ||
                                extensao.equals("bmp")
                                   ? BufferedImage.TYPE_INT_RGB
                                   : BufferedImage.TYPE_INT_ARGB;
      BufferedImage  imagem   = new BufferedImage(largura, altura, tipo);

      for (int y = 0; y < altura; y++)
         for (int x = 0; x < largura; x++)
            imagem.setRGB(x, y, argb);

      Path arquivo = pasta.resolve(nome);

      try {
         if (!ImageIO.write(imagem, extensao.equals("jpeg") ? "jpg"
                                                            : extensao,
                            arquivo.toFile()))
            throw new IllegalStateException("sem gravador para " + extensao);
      }
      catch (IOException ex) {
         throw new UncheckedIOException(ex);
      }
      return arquivo;
   }
}
