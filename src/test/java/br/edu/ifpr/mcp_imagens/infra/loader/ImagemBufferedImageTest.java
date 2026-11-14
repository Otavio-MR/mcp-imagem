package br.edu.ifpr.mcp_imagens.infra.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class ImagemBufferedImageTest {
   @Test
   void informaDimensoesETotalDePixels() {
      ImagemBufferedImage imagem = new ImagemBufferedImage(
            new BufferedImage(7, 3, BufferedImage.TYPE_INT_ARGB));

      assertEquals(7, imagem.largura());
      assertEquals(3, imagem.altura());
      assertEquals(21, imagem.totalPixels());
   }

   @Test
   void leOsPixelsNasPosicoesCertas() {
      BufferedImage original = new BufferedImage(3, 2,
                                                 BufferedImage.TYPE_INT_ARGB);
      original.setRGB(2, 0, 0xFFAA0000);
      original.setRGB(0, 1, 0x8000BB00);

      ImagemBufferedImage imagem = new ImagemBufferedImage(original);

      assertEquals(0xFFAA0000, imagem.argb(2, 0));
      assertEquals(0x8000BB00, imagem.argb(0, 1));
      assertEquals(0x00000000, imagem.argb(1, 1));
   }

   @Test
   void argbTemCanalAlfaEIntRgbNao() {
      assertTrue(new ImagemBufferedImage(
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
            .temCanalAlfa());
      assertFalse(new ImagemBufferedImage(
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB))
            .temCanalAlfa());
      assertFalse(new ImagemBufferedImage(
            new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY))
            .temCanalAlfa());
   }

   @Test
   void imagemSemAlfaTemPixelsOpacos() {
      BufferedImage original = new BufferedImage(1, 1,
                                                 BufferedImage.TYPE_INT_RGB);
      original.setRGB(0, 0, 0x123456);

      assertEquals(0xFF123456, new ImagemBufferedImage(original).argb(0, 0));
   }

   @Test
   void pixelForaDaImagemEErroDoJdk() {
      ImagemBufferedImage imagem = new ImagemBufferedImage(
            new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));

      assertThrows(ArrayIndexOutOfBoundsException.class,
                   () -> imagem.argb(2, 0));
   }
}
