package br.edu.ifpr.mcp_imagens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.model.ResultadoTransparencia;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;

class AnalisadorDeTransparenciaPadraoTest {
   private final AnalisadorDeTransparencia analisador =
                                          new AnalisadorDeTransparenciaPadrao();

   @Test
   void separaTransparentesSemitransparentesEOpacos() {
      ResultadoTransparencia r = analisador.analisar(ImagensSinteticas.de(
            new int[] { 0x00FF0000, 0x01FF0000, 0x80FF0000, 0xFEFF0000,
                        0xFFFF0000, 0xFF000000 }));

      assertEquals(1, r.transparentes());
      assertEquals(3, r.semitransparentes());
      assertEquals(2, r.opacos());
      assertEquals(6, r.total());
   }

   @Test
   void oAlfaNaoDependeDaCor() {
      // alfa 0 com qualquer cor continua transparente
      ResultadoTransparencia r = analisador.analisar(ImagensSinteticas.de(
            new int[] { 0x00000000, 0x00FFFFFF, 0x00123456 }));

      assertEquals(3, r.transparentes());
   }

   @Test
   void imagemSemAlfaTemTodosOsPixelsOpacos() {
      ResultadoTransparencia r = analisador.analisar(
            ImagensSinteticas.solida(4, 5, 0xFF336699));

      assertEquals(0, r.transparentes());
      assertEquals(0, r.semitransparentes());
      assertEquals(20, r.opacos());
   }

   @Test
   void imagemTodaTransparente() {
      ResultadoTransparencia r = analisador.analisar(
            ImagensSinteticas.solida(3, 3, 0x00000000));

      assertEquals(9, r.transparentes());
      assertEquals(9, r.total());
   }
}
