package br.edu.ifpr.mcp_imagens.domain.criterio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TransparenteTest {
   private final CriterioDePixel criterio = new Transparente();

   @Test
   void alfaZeroEhTransparenteQualquerQueSejaACor() {
      assertTrue(criterio.aceita(0x00000000));
      assertTrue(criterio.aceita(0x00FF0000));
      assertTrue(criterio.aceita(0x00FFFFFF));
   }

   @Test
   void alfaUmJaNaoEhTransparente() {
      assertFalse(criterio.aceita(0x01000000));
   }

   @Test
   void semitransparenteNaoEhTransparente() {
      assertFalse(criterio.aceita(0x80FF0000));
      assertFalse(criterio.aceita(0xFEFFFFFF));
   }

   @Test
   void opacoNaoEhTransparente() {
      assertFalse(criterio.aceita(0xFF000000));
      assertFalse(criterio.aceita(0xFFFFFFFF));
   }

   @Test
   void negadoAceitaTodoPixelNaoTransparente() {
      CriterioDePixel visivel = criterio.negado();

      assertFalse(visivel.aceita(0x00FF0000));
      assertTrue(visivel.aceita(0x01000000));
      assertTrue(visivel.aceita(0xFF000000));
   }
}
