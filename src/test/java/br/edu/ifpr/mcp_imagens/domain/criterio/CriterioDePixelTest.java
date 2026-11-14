package br.edu.ifpr.mcp_imagens.domain.criterio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * O padrão Strategy em si: qualquer lambda serve de critério, e todos
 * ganham o método {@code negado()}.
 */
class CriterioDePixelTest {
   @Test
   void umaLambdaEUmCriterio() {
      CriterioDePixel soVermelhoPuro = argb -> argb == 0xFFFF0000;

      assertTrue(soVermelhoPuro.aceita(0xFFFF0000));
      assertFalse(soVermelhoPuro.aceita(0xFF00FF00));
   }

   @Test
   void negadoInverteTodaResposta() {
      CriterioDePixel par     = argb -> argb % 2 == 0;
      CriterioDePixel impar   = par.negado();

      for (int n = 0; n < 6; n++)
         assertEquals(!par.aceita(n), impar.aceita(n), "n=" + n);
   }

   @Test
   void negarDuasVezesVoltaAoOriginal() {
      CriterioDePixel positivo = argb -> argb > 0;
      CriterioDePixel duplo    = positivo.negado().negado();

      for (int n = -2; n <= 2; n++)
         assertEquals(positivo.aceita(n), duplo.aceita(n), "n=" + n);
   }
}
