package br.edu.ifpr.mcp_imagens.infra.loader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagensException;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;

/**
 * {@link ImageLoader} que usa o {@code ImageIO} do JDK (PNG, JPG, GIF e BMP).
 * <p>
 * Os limites são conferidos antes de decodificar: o tamanho do arquivo pelo
 * sistema de arquivos e as dimensões pelo cabeçalho da imagem (via
 * {@link ImageReader}), para que uma imagem enorme não estoure a memória. Em
 * GIFs animados, só o primeiro quadro é lido.
 */
public class ImageIoLoader implements ImageLoader {
   private static final double BYTES_POR_MB = 1024.0 * 1024.0;

   private final long tamanhoMaximoBytes;
   private final long pixelsMaximos;

   /**
    * @param tamanhoMaximoBytes maior arquivo aceito, em bytes
    * @param pixelsMaximos      maior imagem aceita (largura x altura)
    */
   public ImageIoLoader(long tamanhoMaximoBytes, long pixelsMaximos) {
      this.tamanhoMaximoBytes = tamanhoMaximoBytes;
      this.pixelsMaximos      = pixelsMaximos;
   }

   @Override
   public Imagem carregar(Path arquivo) {
      String nome = arquivo.getFileName().toString();

      verificarTamanhoDoArquivo(arquivo, nome);

      try (ImageInputStream entrada = ImageIO.createImageInputStream(
                                                         arquivo.toFile())) {
         return new ImagemBufferedImage(decodificar(entrada, nome));
      }
      catch (ImagensException ex) {
         throw ex;
      }
      catch (IOException | RuntimeException ex) {
         // arquivo truncado ou com dados inconsistentes
         throw invalida(nome);
      }
   }

   private void verificarTamanhoDoArquivo(Path arquivo, String nome) {
      long bytes;

      try {
         bytes = Files.size(arquivo);
      }
      catch (IOException ex) {
         throw new UncheckedIOException("falha ao ler o tamanho de " + arquivo,
                                        ex);
      }
      if (bytes > tamanhoMaximoBytes)
         throw new ImagemMuitoGrandeException(
                      "o arquivo '" + nome + "' tem " + bytes + " bytes (" +
                      megabytes(bytes) + "); o limite é " +
                      tamanhoMaximoBytes + " bytes (" +
                      megabytes(tamanhoMaximoBytes) + ")");
   }

   private BufferedImage decodificar(ImageInputStream entrada, String nome)
         throws IOException {
      if (entrada == null)
         throw invalida(nome);

      Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);

      if (!leitores.hasNext())
         throw invalida(nome);

      ImageReader leitor = leitores.next();

      try {
         leitor.setInput(entrada);
         verificarDimensoes(leitor, nome);
         return leitor.read(0);
      }
      finally {
         leitor.dispose();
      }
   }

   // as dimensões vêm do cabeçalho, sem decodificar os pixels
   private void verificarDimensoes(ImageReader leitor, String nome)
         throws IOException {
      int  largura = leitor.getWidth(0);
      int  altura  = leitor.getHeight(0);
      long pixels  = (long) largura * altura;

      if (pixels > pixelsMaximos)
         throw new ImagemMuitoGrandeException(
                      "a imagem '" + nome + "' tem " + largura + "x" +
                      altura + " pixels (" + pixels + "); o limite é " +
                      pixelsMaximos + " pixels");
   }

   private static ImagemInvalidaException invalida(String nome) {
      return new ImagemInvalidaException("o arquivo '" + nome + "' não é uma " +
                                         "imagem válida ou está corrompido");
   }

   private static String megabytes(long bytes) {
      return String.format(Locale.ROOT, "%.1f MB", bytes / BYTES_POR_MB);
   }
}
