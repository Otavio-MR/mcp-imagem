package br.edu.ifpr.mcp_imagens.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ArredondamentoTest {
   @ParameterizedTest
   @CsvSource({
      "4.5249,                  4.52",
      "4.525,                   4.53",
      "0.005,                   0.01",
      "0.004,                   0.00",
      "2.675,                   2.68",
      "3.9985,                  4.00",
      "21.000000000000004,      21.00",
      "1.0,                     1.00",
      "0,                       0.00",
      "100,                     100.00",
      "33.333333333,            33.33",
      "66.666666666,            66.67"
   })
   void duasCasasComHalfUp(double valor, String esperado) {
      assertEquals(esperado, Arredondamento.duasCasas(valor).toPlainString());
   }

   @ParameterizedTest
   @CsvSource({ "21, 21.00", "1, 1.00", "0.5, 0.50" })
   void semprePreservaAsDuasCasasNoTexto(double valor, String esperado) {
      assertEquals(esperado, Arredondamento.duasCasas(valor).toString());
   }
}
