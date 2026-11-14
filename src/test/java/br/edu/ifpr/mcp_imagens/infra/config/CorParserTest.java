package br.edu.ifpr.mcp_imagens.infra.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.CorInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;

class CorParserTest {
   private static final CorParser PARSER = new CorParser(Map.of(
         "vermelho", "#FF0000",
         "azul-marinho", "#000080",
         "verde-limao", "#00FF00",
         "azul-petroleo", "#008080"));

   @Test
   void hexCurto() {
      assertEquals(new Cor(255, 255, 255), PARSER.parse("#FFF"));
   }

   @Test
   void hexLongoEmMinusculas() {
      assertEquals(new Cor(255, 255, 255), PARSER.parse("#ffffff"));
   }

   @Test
   void nomeSemDiferenciarMaiusculas() {
      assertEquals(new Cor(255, 0, 0), PARSER.parse("Vermelho"));
      assertEquals(new Cor(255, 0, 0), PARSER.parse("VERMELHO"));
      assertEquals(new Cor(255, 0, 0), PARSER.parse("vermelho"));
   }

   @Test
   void removeEspacosDasPontas() {
      assertEquals(new Cor(255, 0, 0), PARSER.parse("  vermelho \n"));
      assertEquals(new Cor(255, 255, 255), PARSER.parse("  #fff  "));
   }

   @Test
   void espacoNoMeioValeHifen() {
      assertEquals(new Cor(0, 0, 128), PARSER.parse("azul marinho"));
      assertEquals(new Cor(0, 0, 128), PARSER.parse("Azul   Marinho"));
   }

   @Test
   void ignoraAcentos() {
      assertEquals(new Cor(0, 255, 0), PARSER.parse("Verde-Limão"));
      assertEquals(new Cor(0, 128, 128), PARSER.parse("AZUL-PETRÓLEO"));
   }

   @ParameterizedTest
   @ValueSource(strings = { "#GGGGGG", "#12345", "#1234567", "#", "FF0000" })
   void hexInvalidoEhRecusado(String texto) {
      assertThrows(CorInvalidaException.class, () -> PARSER.parse(texto));
   }

   @ParameterizedTest
   @ValueSource(strings = { "", "   ", "\t\n" })
   @NullSource
   void textoVazioEhRecusado(String texto) {
      CorInvalidaException ex = assertThrows(CorInvalidaException.class,
                                             () -> PARSER.parse(texto));

      assertTrue(ex.getMessage().contains("vazia"), ex.getMessage());
   }

   @Test
   void nomeDesconhecidoEhRecusadoCitandoOQueFoiDigitado() {
      CorInvalidaException ex = assertThrows(CorInvalidaException.class,
                                             () -> PARSER.parse("Xablau"));

      assertTrue(ex.getMessage().contains("'Xablau'"), ex.getMessage());
      assertTrue(ex.getMessage().contains("#RRGGBB"), ex.getMessage());
   }

   @Test
   void dicionarioComHexInvalidoFalhaAoCriar() {
      assertThrows(CorInvalidaException.class,
                   () -> new CorParser(Map.of("estranha", "#ZZZ")));
   }

   @Test
   void dicionarioVazioSoAceitaHex() {
      CorParser semNomes = new CorParser(Map.of());

      assertEquals(new Cor(1, 2, 3), semNomes.parse("#010203"));
      assertThrows(CorInvalidaException.class,
                   () -> semNomes.parse("vermelho"));
   }
}
