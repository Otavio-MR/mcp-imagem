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
      BufferedImage imagem = nova(nome, largura, altura);

      for (int y = 0; y < altura; y++)
         for (int x = 0; x < largura; x++)
            imagem.setRGB(x, y, argb);

      return gravar(imagem, pasta, nome);
   }

   /**
    * Grava uma imagem com os pixels informados (linhas de cima para baixo).
    */
   public static Path criarComPixels(Path pasta, String nome,
                                     int[]... linhas) {
      BufferedImage imagem = nova(nome, linhas[0].length, linhas.length);

      for (int y = 0; y < linhas.length; y++)
         for (int x = 0; x < linhas[y].length; x++)
            imagem.setRGB(x, y, linhas[y][x]);

      return gravar(imagem, pasta, nome);
   }

   /**
    * Grava uma imagem em tons de cinza de 8 bits ({@code TYPE_BYTE_GRAY}), de
    * uma linha, com as amostras informadas (0 a 255).
    */
   public static Path criarCinza(Path pasta, String nome, int... amostras) {
      BufferedImage imagem = new BufferedImage(amostras.length, 1,
                                               BufferedImage.TYPE_BYTE_GRAY);

      for (int x = 0; x < amostras.length; x++)
         imagem.getRaster().setSample(x, 0, 0, amostras[x]);

      return gravar(imagem, pasta, nome);
   }

   private static BufferedImage nova(String nome, int largura, int altura) {
      String extensao = extensao(nome);
      int    tipo     = extensao.equals("jpg") || extensao.equals("jpeg") ||
                        extensao.equals("bmp")
                           ? BufferedImage.TYPE_INT_RGB
                           : BufferedImage.TYPE_INT_ARGB;

      return new BufferedImage(largura, altura, tipo);
   }

   private static Path gravar(BufferedImage imagem, Path pasta, String nome) {
      String extensao = extensao(nome);
      Path   arquivo  = pasta.resolve(nome);

      try {
         if (!ImageIO.write(imagem, extensao.equals("jpeg") ? "jpg" : extensao,
                            arquivo.toFile()))
            throw new IllegalStateException("sem gravador para " + extensao);
      }
      catch (IOException ex) {
         throw new UncheckedIOException(ex);
      }
      return arquivo;
   }

   private static String extensao(String nome) {
      return nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
   }
}
