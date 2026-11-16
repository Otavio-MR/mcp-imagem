package br.edu.ifpr.mcp_imagens.domain.criterio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * O padrão Strategy em si: qualquer lambda serve de critério.
 */
class CriterioDePixelTest {
   @Test
   void umaLambdaEUmCriterio() {
      CriterioDePixel soVermelhoPuro = argb -> argb == 0xFFFF0000;

      assertTrue(soVermelhoPuro.aceita(0xFFFF0000));
      assertFalse(soVermelhoPuro.aceita(0xFF00FF00));
   }
}
