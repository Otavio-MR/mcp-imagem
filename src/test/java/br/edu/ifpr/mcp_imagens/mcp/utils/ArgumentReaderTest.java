package br.edu.ifpr.mcp_imagens.mcp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroAusenteException;
import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;

class ArgumentReaderTest {
   private static final Set<String> PERMITIDOS = Set.of("arquivo", "limiar",
                                                        "ignorar");

   private static ArgumentReader leitor(Map<String, Object> argumentos) {
      return new ArgumentReader(argumentos, PERMITIDOS);
   }

   private static Map<String, Object> com(String nome, Object valor) {
      Map<String, Object> argumentos = new HashMap<>();
      argumentos.put(nome, valor);
      return argumentos;
   }

   @Nested
   class Construcao {
      @Test
      void aceitaArgumentosNulos() {
         assertEquals(7, leitor(null).inteiroOpcional("limiar", 7, 0, 255));
      }

      @Test
      void aceitaArgumentosVazios() {
         assertEquals(7, leitor(Map.of()).inteiroOpcional("limiar", 7, 0, 255));
      }

      @Test
      void recusaParametroNaoDeclarado() {
         ParametroInvalidoException ex = assertThrows(
               ParametroInvalidoException.class,
               () -> leitor(Map.of("arquivo", "a.png", "extra", 1)));

         assertTrue(ex.getMessage().contains("'extra'"));
      }
   }

   @Nested
   class Texto {
      @Test
      void devolveTextoSemEspacosNasPontas() {
         assertEquals("a.png", leitor(com("arquivo", "  a.png ")).texto(
                                                                    "arquivo"));
      }

      @Test
      void ausenteGeraParametroAusente() {
         ParametroAusenteException ex = assertThrows(
               ParametroAusenteException.class,
               () -> leitor(Map.of()).texto("arquivo"));

         assertTrue(ex.getMessage().contains("'arquivo'"));
      }

      @Test
      void nuloGeraParametroAusente() {
         assertThrows(ParametroAusenteException.class,
                      () -> leitor(com("arquivo", null)).texto("arquivo"));
      }

      @ParameterizedTest
      @ValueSource(strings = { "", "   ", "\t\n" })
      void emBrancoGeraParametroInvalido(String valor) {
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("arquivo", valor)).texto("arquivo"));
      }

      @Test
      void valorQueNaoEhTextoGeraParametroInvalido() {
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("arquivo", 42)).texto("arquivo"));
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("arquivo", true)).texto("arquivo"));
      }
   }

   @Nested
   class InteiroOpcional {
      @Test
      void ausenteOuNuloDevolveOPadrao() {
         assertEquals(128, leitor(Map.of()).inteiroOpcional("limiar", 128, 0,
                                                            255));
         assertEquals(128, leitor(com("limiar", null))
                              .inteiroOpcional("limiar", 128, 0, 255));
      }

      @Test
      void devolveOValorInformado() {
         assertEquals(200, leitor(com("limiar", 200))
                              .inteiroOpcional("limiar", 128, 0, 255));
      }

      @Test
      void limitesMinimoEMaximoSaoAceitos() {
         assertEquals(0, leitor(com("limiar", 0))
                            .inteiroOpcional("limiar", 128, 0, 255));
         assertEquals(255, leitor(com("limiar", 255))
                              .inteiroOpcional("limiar", 128, 0, 255));
      }

      @ParameterizedTest
      @ValueSource(ints = { -1, 256, 1000, Integer.MIN_VALUE })
      void foraDosLimitesGeraParametroInvalido(int valor) {
         ParametroInvalidoException ex = assertThrows(
               ParametroInvalidoException.class,
               () -> leitor(com("limiar", valor))
                           .inteiroOpcional("limiar", 128, 0, 255));

         assertTrue(ex.getMessage().contains("entre 0 e 255"));
      }

      @Test
      void aceitaQualquerNumberQueSejaInteiro() {
         assertEquals(5, leitor(com("limiar", 5L))
                            .inteiroOpcional("limiar", 1, 0, 10));
         assertEquals(5, leitor(com("limiar", 5.0))
                            .inteiroOpcional("limiar", 1, 0, 10));
         assertEquals(5, leitor(com("limiar", (short) 5))
                            .inteiroOpcional("limiar", 1, 0, 10));
      }

      @Test
      void aceitaInteiroEscritoComoTexto() {
         assertEquals(7, leitor(com("limiar", " 7 "))
                            .inteiroOpcional("limiar", 1, 0, 10));
      }

      @Test
      void recusaNumeroComParteFracionaria() {
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("limiar", 5.5))
                                  .inteiroOpcional("limiar", 1, 0, 10));
      }

      @Test
      void recusaNumeroGrandeDemaisParaInt() {
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("limiar", 1e12))
                                  .inteiroOpcional("limiar", 1, 0, 10));
      }

      @ParameterizedTest
      @ValueSource(strings = { "abc", "5.5", "", "1e3" })
      void recusaTextoQueNaoEhInteiro(String valor) {
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("limiar", valor))
                                  .inteiroOpcional("limiar", 1, 0, 10));
      }

      @Test
      void recusaBooleanoComoInteiro() {
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("limiar", true))
                                  .inteiroOpcional("limiar", 1, 0, 10));
      }
   }

   @Nested
   class BooleanoOpcional {
      @Test
      void ausenteOuNuloDevolveOPadrao() {
         assertTrue(leitor(Map.of()).booleanoOpcional("ignorar", true));
         assertFalse(leitor(Map.of()).booleanoOpcional("ignorar", false));
         assertTrue(leitor(com("ignorar", null))
                          .booleanoOpcional("ignorar", true));
      }

      @Test
      void devolveOBooleanoInformado() {
         assertTrue(leitor(com("ignorar", true))
                          .booleanoOpcional("ignorar", false));
         assertFalse(leitor(com("ignorar", false))
                           .booleanoOpcional("ignorar", true));
      }

      @ParameterizedTest
      @ValueSource(strings = { "true", "TRUE", " True " })
      void aceitaTrueComoTexto(String valor) {
         assertTrue(leitor(com("ignorar", valor))
                          .booleanoOpcional("ignorar", false));
      }

      @ParameterizedTest
      @ValueSource(strings = { "false", "FALSE", " False " })
      void aceitaFalseComoTexto(String valor) {
         assertFalse(leitor(com("ignorar", valor))
                           .booleanoOpcional("ignorar", true));
      }

      @ParameterizedTest
      @ValueSource(strings = { "sim", "1", "", "yes" })
      void recusaTextoQueNaoEhBooleano(String valor) {
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("ignorar", valor))
                                  .booleanoOpcional("ignorar", true));
      }

      @Test
      void recusaNumeroComoBooleano() {
         assertThrows(ParametroInvalidoException.class,
                      () -> leitor(com("ignorar", 1))
                                  .booleanoOpcional("ignorar", true));
      }
   }
}
