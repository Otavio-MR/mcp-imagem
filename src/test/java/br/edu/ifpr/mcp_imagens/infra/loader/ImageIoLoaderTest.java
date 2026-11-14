package br.edu.ifpr.mcp_imagens.infra.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.infra.config.AppConfig;
import br.edu.ifpr.mcp_imagens.testutil.ImagensDeTeste;

class ImageIoLoaderTest {
   @TempDir
   Path pasta;

   private final ImageLoader carregador = new ImageIoLoader(
         AppConfig.TAMANHO_MAXIMO_BYTES, AppConfig.PIXELS_MAXIMOS);

   private static void assertPixelAprox(int esperado, int obtido, int folga,
                                        String onde) {
      for (int deslocamento : new int[] { 16, 8, 0 })
         assertEquals((esperado >> deslocamento) & 0xFF,
                      (obtido >> deslocamento) & 0xFF, folga,
                      onde + " (canal em deslocamento " + deslocamento + ")");
   }

   @Nested
   class LeituraPorFormato {
      @Test
      void pngPreservaOsPixelsExatamenteInclusiveOAlfa() {
         int[][] pixels = { { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF },
                            { 0x80112233, 0x00000000, 0xFFFFFFFF } };
         Path    arquivo = ImagensDeTeste.criarComPixels(pasta, "a.png",
                                                         pixels);

         Imagem imagem = carregador.carregar(arquivo);

         assertEquals(3, imagem.largura());
         assertEquals(2, imagem.altura());
         assertEquals(6, imagem.totalPixels());
         assertTrue(imagem.temCanalAlfa());
         for (int y = 0; y < 2; y++)
            for (int x = 0; x < 3; x++)
               assertEquals(pixels[y][x], imagem.argb(x, y),
                            "pixel (" + x + "," + y + ")");
      }

      @Test
      void pngSemAlfaTemPixelsOpacos() {
         Path arquivo = pasta.resolve("rgb.png");
         // grava um PNG RGB (sem canal alfa)
         gravarRgb(arquivo, 4, 3, 0x336699);

         Imagem imagem = carregador.carregar(arquivo);

         assertFalse(imagem.temCanalAlfa());
         assertEquals(0xFF336699, imagem.argb(0, 0));
         assertEquals(0xFF336699, imagem.argb(3, 2));
      }

      @Test
      void bmpPreservaAsCoresExatamente() {
         Path arquivo = ImagensDeTeste.criar(pasta, "a.bmp", 5, 4, 0xFFC08040);

         Imagem imagem = carregador.carregar(arquivo);

         assertEquals(5, imagem.largura());
         assertEquals(4, imagem.altura());
         assertFalse(imagem.temCanalAlfa());
         assertEquals(0xFFC08040, imagem.argb(4, 3));
      }

      @Test
      void gifDeCoresFlatsPreservaAsCores() {
         Path arquivo = ImagensDeTeste.criar(pasta, "a.gif", 4, 4, 0xFFFF0000);

         Imagem imagem = carregador.carregar(arquivo);

         assertEquals(4, imagem.largura());
         assertEquals(0xFFFF0000, imagem.argb(2, 2));
      }

      @Test
      void jpgEPerdaDeQualidadeEntaoOsPixelsSaoAproximados() {
         Path arquivo = ImagensDeTeste.criar(pasta, "a.jpg", 16, 16,
                                             0xFF808080);

         Imagem imagem = carregador.carregar(arquivo);

         assertEquals(16, imagem.largura());
         assertFalse(imagem.temCanalAlfa());
         assertPixelAprox(0xFF808080, imagem.argb(8, 8), 3, "centro do JPG");
      }

      @Test
      void jpegComExtensaoLongaTambemFunciona() {
         Path arquivo = ImagensDeTeste.criar(pasta, "a.jpeg", 8, 8, 0xFF404040);

         assertEquals(8, carregador.carregar(arquivo).largura());
      }

      @Test
      void oFormatoVemDoConteudoEntaoExtensaoTrocadaNaoImpede() {
         Path png = ImagensDeTeste.criar(pasta, "verdadeiro.png", 3, 3,
                                         0xFFFF0000);
         Path disfarcado = pasta.resolve("disfarcado.jpg");
         copiar(png, disfarcado);

         assertEquals(0xFFFF0000, carregador.carregar(disfarcado).argb(1, 1));
      }
   }

   @Nested
   class TonsDeCinza {
      private final int[] amostras = { 0, 64, 128, 192, 255 };

      @Test
      void cinzaDe8BitsSaiMaisClaroQueOMesmoCinzaEmRgb() {
         // Comportamento do JDK, documentado no README: o Java trata o cinza
         // como linear e converte para sRGB em getRGB.
         Path   arquivo = ImagensDeTeste.criarCinza(pasta, "cinza.png",
                                                    amostras);
         Imagem imagem  = carregador.carregar(arquivo);

         assertEquals(0xFF000000, imagem.argb(0, 0));
         assertEquals(0xFF898989, imagem.argb(1, 0));
         assertEquals(0xFFBCBCBC, imagem.argb(2, 0));
         assertEquals(0xFFE1E1E1, imagem.argb(3, 0));
         assertEquals(0xFFFFFFFF, imagem.argb(4, 0));
         assertFalse(imagem.temCanalAlfa());
      }

      @Test
      void omesmoCinzaGravadoComoRgbSaiExato() {
         int[] linha = Arrays.stream(amostras)
                             .map(v -> 0xFF000000 | (v << 16) | (v << 8) | v)
                             .toArray();
         Path   arquivo = ImagensDeTeste.criarComPixels(pasta, "rgb.bmp",
                                                        linha);
         Imagem imagem  = carregador.carregar(arquivo);

         for (int x = 0; x < amostras.length; x++)
            assertEquals(linha[x], imagem.argb(x, 0), "amostra " + amostras[x]);
      }
   }

   @Nested
   class ArquivosInvalidos {
      private ImagemInvalidaException recusa(Path arquivo) {
         return assertThrows(ImagemInvalidaException.class,
                             () -> carregador.carregar(arquivo));
      }

      @Test
      void textoComExtensaoDeImagemEInvalido() throws IOException {
         Path arquivo = pasta.resolve("falso.png");
         Files.writeString(arquivo, "isto não é uma imagem");

         ImagemInvalidaException ex = recusa(arquivo);

         assertTrue(ex.getMessage().contains("'falso.png'"), ex.getMessage());
         assertTrue(ex.getMessage().contains("não é uma imagem válida"));
      }

      @Test
      void arquivoVazioEInvalido() throws IOException {
         Path arquivo = pasta.resolve("vazio.png");
         Files.write(arquivo, new byte[0]);

         recusa(arquivo);
      }

      @Test
      void bytesAleatoriosSaoInvalidos() throws IOException {
         Path arquivo = pasta.resolve("lixo.jpg");
         byte[] lixo = new byte[500];
         new Random(7).nextBytes(lixo);
         Files.write(arquivo, lixo);

         recusa(arquivo);
      }

      @Test
      void pngTruncadoEInvalido() throws IOException {
         Path completo = ImagensDeTeste.criar(pasta, "completo.png", 64, 64,
                                              0xFFFF0000);
         byte[] bytes = Files.readAllBytes(completo);
         Path cortado = pasta.resolve("cortado.png");
         Files.write(cortado, Arrays.copyOf(bytes, bytes.length / 2));

         recusa(cortado);
      }

      @Test
      void apenasACabecaDoPngEInvalida() throws IOException {
         Path completo = ImagensDeTeste.criar(pasta, "completo.png", 32, 32,
                                              0xFF00FF00);
         Path cabeca = pasta.resolve("cabeca.png");
         Files.write(cabeca, Arrays.copyOf(Files.readAllBytes(completo), 30));

         recusa(cabeca);
      }

      @Test
      void jpgTruncadoEInvalido() throws IOException {
         Path completo = ImagensDeTeste.criar(pasta, "completo.jpg", 64, 64,
                                              0xFF336699);
         byte[] bytes = Files.readAllBytes(completo);
         Path cortado = pasta.resolve("cortado.jpg");
         Files.write(cortado, Arrays.copyOf(bytes, bytes.length / 2));

         recusa(cortado);
      }

      @Test
      void arquivoInexistenteNaoEErroDeDominio() {
         // o contrato é receber um arquivo que existe; quem chama confere antes
         assertThrows(UncheckedIOException.class,
                      () -> carregador.carregar(pasta.resolve("nada.png")));
      }
   }

   @Nested
   class Limites {
      @Test
      void arquivoMaiorQueOLimiteEmBytesEhRecusado() {
         Path arquivo = ImagensDeTeste.criar(pasta, "a.png", 50, 50,
                                             0xFFFF0000);
         long tamanho = arquivo.toFile().length();
         ImageLoader pequeno = new ImageIoLoader(tamanho - 1, 1_000_000);

         ImagemMuitoGrandeException ex = assertThrows(
               ImagemMuitoGrandeException.class,
               () -> pequeno.carregar(arquivo));

         assertTrue(ex.getMessage().contains("'a.png'"), ex.getMessage());
         assertTrue(ex.getMessage().contains(tamanho + " bytes"),
                    ex.getMessage());
         assertTrue(ex.getMessage().contains((tamanho - 1) + " bytes"),
                    ex.getMessage());
      }

      @Test
      void arquivoExatamenteNoLimiteDeBytesEhAceito() {
         Path arquivo = ImagensDeTeste.criar(pasta, "a.png", 50, 50,
                                             0xFFFF0000);

         ImageLoader noLimite = new ImageIoLoader(arquivo.toFile().length(),
                                                  1_000_000);

         assertEquals(50, noLimite.carregar(arquivo).largura());
      }

      @Test
      void imagemComMaisPixelsQueOLimiteEhRecusada() {
         Path arquivo = ImagensDeTeste.criar(pasta, "a.png", 11, 10,
                                             0xFFFF0000);
         ImageLoader limitado = new ImageIoLoader(1_000_000, 100);

         ImagemMuitoGrandeException ex = assertThrows(
               ImagemMuitoGrandeException.class,
               () -> limitado.carregar(arquivo));

         assertTrue(ex.getMessage().contains("11x10"), ex.getMessage());
         assertTrue(ex.getMessage().contains("(110)"), ex.getMessage());
         assertTrue(ex.getMessage().contains("100 pixels"), ex.getMessage());
      }

      @Test
      void imagemComExatamenteOLimiteDePixelsEhAceita() {
         Path arquivo = ImagensDeTeste.criar(pasta, "a.png", 10, 10,
                                             0xFFFF0000);

         assertEquals(100, new ImageIoLoader(1_000_000, 100).carregar(arquivo)
                                                            .totalPixels());
      }

      @Test
      void oLimiteDePixelsVaiPelaAreaENaoSoPelaLargura() {
         // 1001 x 1001 = 1.002.001 > 1.000.000, ainda que cada lado seja pequeno
         Path arquivo = ImagensDeTeste.criar(pasta, "a.png", 1001, 1001,
                                             0xFFFF0000);

         assertThrows(ImagemMuitoGrandeException.class,
                      () -> new ImageIoLoader(10_000_000, 1_000_000)
                                  .carregar(arquivo));
      }

      @Test
      void limitesPadraoDaAplicacaoSao20MbE25MilhoesDePixels() {
         assertEquals(20L * 1024 * 1024, AppConfig.TAMANHO_MAXIMO_BYTES);
         assertEquals(25_000_000L, AppConfig.PIXELS_MAXIMOS);
      }

      @Test
      void tamanhoEmMegabytesApareceNaMensagem() throws IOException {
         Path arquivo = pasta.resolve("grande.png");
         Files.write(arquivo, new byte[3 * 1024 * 1024]);

         ImagemMuitoGrandeException ex = assertThrows(
               ImagemMuitoGrandeException.class,
               () -> new ImageIoLoader(2 * 1024 * 1024, 100)
                           .carregar(arquivo));

         assertTrue(ex.getMessage().contains("3.0 MB"), ex.getMessage());
         assertTrue(ex.getMessage().contains("2.0 MB"), ex.getMessage());
      }
   }

   private static void gravarRgb(Path arquivo, int largura, int altura,
                                 int rgb) {
      BufferedImage imagem = new BufferedImage(largura, altura,
                                               BufferedImage.TYPE_INT_RGB);

      for (int y = 0; y < altura; y++)
         for (int x = 0; x < largura; x++)
            imagem.setRGB(x, y, rgb);
      try {
         ImageIO.write(imagem, "png", arquivo.toFile());
      }
      catch (IOException ex) {
         throw new UncheckedIOException(ex);
      }
   }

   private static void copiar(Path de, Path para) {
      try {
         Files.copy(de, para);
      }
      catch (IOException ex) {
         throw new UncheckedIOException(ex);
      }
   }
}
