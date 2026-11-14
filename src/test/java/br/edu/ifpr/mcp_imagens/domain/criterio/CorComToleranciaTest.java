package br.edu.ifpr.mcp_imagens.domain.criterio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;

class CorComToleranciaTest {
   private static final Cor VERMELHO = new Cor(255, 0, 0);

   @Test
   void toleranciaZeroSoAceitaACorExata() {
      CriterioDePixel criterio = new CorComTolerancia(VERMELHO, 0);

      assertTrue(criterio.aceita(0xFFFF0000));
      assertFalse(criterio.aceita(0xFFFE0000));
      assertFalse(criterio.aceita(0xFFFF0001));
      assertFalse(criterio.aceita(0xFF00FF00));
   }

   @Test
   void toleranciaUmAceitaDiferencaDeUmEmCadaCanal() {
      CriterioDePixel criterio = new CorComTolerancia(VERMELHO, 1);

      assertTrue(criterio.aceita(0xFFFE0000));   // R diferente em 1
      assertTrue(criterio.aceita(0xFFFF0101));   // G e B diferentes em 1
      assertTrue(criterio.aceita(0xFFFF0000));
      assertFalse(criterio.aceita(0xFFFD0000));  // R diferente em 2
      assertFalse(criterio.aceita(0xFFFF0200));  // G diferente em 2
   }

   @Test
   void toleranciaEPorCanalENaoSomada() {
      CriterioDePixel criterio = new CorComTolerancia(new Cor(100, 100, 100), 5);

      // cada canal difere em 5 (soma das diferenças: 15), e ainda é aceito
      assertTrue(criterio.aceita(argb(105, 105, 105)));
      // um canal difere em 6
      assertFalse(criterio.aceita(argb(105, 105, 106)));
   }

   @Test
   void toleranciaPorCanalEmLimitesExatos() {
      CriterioDePixel criterio = new CorComTolerancia(new Cor(100, 100, 100), 5);

      assertTrue(criterio.aceita(argb(105, 95, 100)));
      assertFalse(criterio.aceita(argb(106, 100, 100)));
      assertFalse(criterio.aceita(argb(100, 94, 100)));
      assertFalse(criterio.aceita(argb(100, 100, 106)));
   }

   @Test
   void toleranciaMaximaAceitaQualquerCor() {
      CriterioDePixel criterio = new CorComTolerancia(VERMELHO, 255);

      assertTrue(criterio.aceita(0xFF000000));
      assertTrue(criterio.aceita(0xFFFFFFFF));
      assertTrue(criterio.aceita(0xFF00FF00));
   }

   @Test
   void oAlfaNaoEComparado() {
      CriterioDePixel criterio = new CorComTolerancia(VERMELHO, 0);

      assertTrue(criterio.aceita(0x80FF0000));  // semitransparente
      assertTrue(criterio.aceita(0x00FF0000));  // transparente
   }

   @Test
   void corPretaComToleranciaZero() {
      CriterioDePixel criterio = new CorComTolerancia(new Cor(0, 0, 0), 0);

      assertTrue(criterio.aceita(0xFF000000));
      assertFalse(criterio.aceita(0xFF000001));
   }

   @ParameterizedTest
   @ValueSource(ints = { -1, 256, 1000, Integer.MIN_VALUE })
   void toleranciaForaDaFaixaEhRecusada(int tolerancia) {
      ParametroInvalidoException ex = assertThrows(
            ParametroInvalidoException.class,
            () -> new CorComTolerancia(VERMELHO, tolerancia));

      assertTrue(ex.getMessage().contains("0 e 255"));
   }

   @Test
   void limitesDaToleranciaSaoAceitos() {
      new CorComTolerancia(VERMELHO, 0);
      new CorComTolerancia(VERMELHO, 255);
   }

   private static int argb(int r, int g, int b) {
      return new Cor(r, g, b).argb();
   }
}
