package br.edu.ifpr.mcp_imagens.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;

/**
 * Testa a abstração {@link Imagem} (o método padrão {@code totalPixels}) e,
 * de quebra, o utilitário de imagens sintéticas usado nos demais testes.
 */
class ImagemTest {
   @Test
   void totalDePixelsEhLarguraVezesAltura() {
      assertEquals(30, ImagensSinteticas.solida(6, 5, 0xFF000000).totalPixels());
   }

   @Test
   void totalDePixelsNaoEstouraInt() {
      // 100000 x 100000 = 10 bilhões, que não cabe em int
      Imagem enorme = new Imagem() {
         @Override public int largura()      { return 100_000; }
         @Override public int altura()       { return 100_000; }
         @Override public boolean temCanalAlfa() { return false; }
         @Override public int argb(int x, int y) { return 0; }
      };

      assertEquals(10_000_000_000L, enorme.totalPixels());
   }

   @Test
   void imagemSinteticaGuardaOsPixelsNasPosicoesCertas() {
      Imagem imagem = ImagensSinteticas.de(new int[] { 1, 2, 3 },
                                           new int[] { 4, 5, 6 });

      assertEquals(3, imagem.largura());
      assertEquals(2, imagem.altura());
      assertEquals(3, imagem.argb(2, 0));
      assertEquals(4, imagem.argb(0, 1));
      assertEquals(6, imagem.argb(2, 1));
   }

   @Test
   void imagemSinteticaTemAlfaSoSeAlgumPixelNaoForOpaco() {
      assertFalse(ImagensSinteticas.solida(2, 2, 0xFFFF0000).temCanalAlfa());
      assertTrue(ImagensSinteticas.de(new int[] { 0xFFFF0000, 0x80FF0000 })
                                  .temCanalAlfa());
      assertTrue(ImagensSinteticas.solida(1, 1, 0x00000000).temCanalAlfa());
   }

   @Test
   void paraCadaPixelPercorreLinhaPorLinhaDaEsquerdaParaADireita() {
      Imagem imagem = ImagensSinteticas.de(new int[] { 1, 2, 3 },
                                           new int[] { 4, 5, 6 });
      List<Integer> vistos = new ArrayList<>();

      imagem.paraCadaPixel(false, vistos::add);

      assertEquals(List.of(1, 2, 3, 4, 5, 6), vistos);
   }

   @Test
   void paraCadaPixelPodePularOsTransparentes() {
      Imagem imagem = ImagensSinteticas.de(
            new int[] { 0x00FF0000, 0x80FF0000, 0xFFFF0000 });
      List<Integer> todos       = new ArrayList<>();
      List<Integer> semTransp   = new ArrayList<>();

      imagem.paraCadaPixel(false, todos::add);
      imagem.paraCadaPixel(true, semTransp::add);

      assertEquals(3, todos.size());
      assertEquals(List.of(0x80FF0000, 0xFFFF0000), semTransp);
   }

   @Test
   void paraCadaPixelEmImagemVaziaNaoChamaAAcao() {
      List<Integer> vistos = new ArrayList<>();

      ImagensSinteticas.de().paraCadaPixel(false, vistos::add);

      assertTrue(vistos.isEmpty());
   }

   @Test
   void geradorUsaAFuncaoDePixel() {
      Imagem imagem = ImagensSinteticas.gerar(3, 2, (x, y) -> x * 10 + y);

      assertEquals(21, imagem.argb(2, 1));
      assertEquals(0, imagem.argb(0, 0));
      assertEquals(10, imagem.argb(1, 0));
   }
}
