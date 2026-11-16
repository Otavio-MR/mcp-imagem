package br.edu.ifpr.mcp_imagens.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConversorDeValoresTest {
   private final ConversorDeValores conversor = new ConversorDeValores();

   @Test
   void stringFicaComoFoiDigitada() {
      assertEquals("banner.png", conversor.converter("banner.png", "string"));
      assertEquals("#FF0000", conversor.converter("#FF0000", "string"));
   }

   @Test
   void inteiro() {
      assertEquals(42, conversor.converter("42", "integer"));
      assertEquals(-3, conversor.converter("-3", "integer"));
   }

   @ParameterizedTest
   @ValueSource(strings = { "abc", "1.5", "1,5", "99999999999" })
   void inteiroInvalido(String texto) {
      IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> conversor.converter(texto, "integer"));

      assertEquals("informe um número inteiro", e.getMessage());
   }

   @Test
   void numero() {
      assertEquals(1.5, conversor.converter("1.5", "number"));
      assertEquals(2.0, conversor.converter("2", "number"));
   }

   @Test
   void numeroInvalido() {
      IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> conversor.converter("x", "number"));

      assertEquals("informe um número", e.getMessage());
   }

   @ParameterizedTest
   @ValueSource(strings = { "true", "TRUE", "sim", "Sim" })
   void booleanoVerdadeiro(String texto) {
      assertEquals(true, conversor.converter(texto, "boolean"));
   }

   @ParameterizedTest
   @ValueSource(strings = { "false", "False", "não", "nao", "NÃO" })
   void booleanoFalso(String texto) {
      assertEquals(false, conversor.converter(texto, "boolean"));
   }

   @Test
   void booleanoInvalido() {
      IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> conversor.converter("talvez", "boolean"));

      assertEquals("informe true/false ou sim/não", e.getMessage());
   }

   @Test
   void tipoNaoSuportado() {
      IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> conversor.converter("[1]", "array"));

      assertEquals("tipo não suportado pelo modo interativo: array",
                   e.getMessage());
   }
}
