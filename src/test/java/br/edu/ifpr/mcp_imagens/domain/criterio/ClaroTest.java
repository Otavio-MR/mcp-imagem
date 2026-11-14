package br.edu.ifpr.mcp_imagens.domain.criterio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;

class ClaroTest {
   @Test
   void brancoEClaroEPretoEEscuro() {
      CriterioDePixel claro = new Claro(128);

      assertTrue(claro.aceita(0xFFFFFFFF));
      assertFalse(claro.aceita(0xFF000000));
   }

   @Test
   void limiarEInclusivo() {
      CriterioDePixel claro = new Claro(128);

      // cinza 128 tem luma exatamente 128: conta como claro (luma >= limiar)
      assertTrue(claro.aceita(0xFF808080));
      assertFalse(claro.aceita(0xFF7F7F7F));
   }

   @Test
   void limitesDoLimiar() {
      assertTrue(new Claro(0).aceita(0xFF000000));
      assertTrue(new Claro(255).aceita(0xFFFFFFFF));
      assertFalse(new Claro(255).aceita(0xFFFEFEFE));
   }

   @Test
   void todoCinzaFicaDoLadoCertoDoLimiar() {
      // com a fórmula em double, alguns cinzas ficariam um pouco abaixo do
      // valor e cairiam do lado errado do limiar
      for (int v = 0; v <= 255; v++) {
         int pixel = 0xFF000000 | (v << 16) | (v << 8) | v;

         assertTrue(new Claro(v).aceita(pixel), "cinza " + v + " >= " + v);
         if (v < 255)
            assertFalse(new Claro(v + 1).aceita(pixel),
                        "cinza " + v + " < " + (v + 1));
      }
   }

   @Test
   void oVerdeEMaisClaroQueOAzulPorCausaDosPesos() {
      CriterioDePixel claro = new Claro(128);

      assertTrue(claro.aceita(0xFF00FF00));    // luma 182,4
      assertFalse(claro.aceita(0xFFFF0000));   // luma  54,2
      assertFalse(claro.aceita(0xFF0000FF));   // luma  18,4
   }

   @Test
   void oAlfaNaoInfluenciaOBrilho() {
      CriterioDePixel claro = new Claro(128);

      assertTrue(claro.aceita(0x00FFFFFF));
      assertFalse(claro.aceita(0x00000000));
   }

   @Test
   void negadoSaoOsEscuros() {
      CriterioDePixel escuro = new Claro(128).negado();

      assertTrue(escuro.aceita(0xFF000000));
      assertTrue(escuro.aceita(0xFF7F7F7F));
      assertFalse(escuro.aceita(0xFF808080));
      assertFalse(escuro.aceita(0xFFFFFFFF));
   }

   @ParameterizedTest
   @ValueSource(ints = { -1, 256, 1000 })
   void limiarForaDaFaixaEhRecusado(int limiar) {
      ParametroInvalidoException ex = assertThrows(
            ParametroInvalidoException.class, () -> new Claro(limiar));

      assertTrue(ex.getMessage().contains("0 e 255"));
   }
}
