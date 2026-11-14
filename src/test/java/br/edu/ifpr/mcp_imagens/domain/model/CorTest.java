package br.edu.ifpr.mcp_imagens.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.CorInvalidaException;

class CorTest {
   @Nested
   class Criacao {
      @Test
      void guardaOsComponentes() {
         Cor cor = new Cor(10, 20, 30, 40);

         assertEquals(10, cor.r());
         assertEquals(20, cor.g());
         assertEquals(30, cor.b());
         assertEquals(40, cor.a());
      }

      @Test
      void semAlfaEhOpaca() {
         assertEquals(255, new Cor(1, 2, 3).a());
         assertEquals(Cor.OPACO, new Cor(1, 2, 3).a());
      }

      @Test
      void limitesDe0Ate255SaoAceitos() {
         new Cor(0, 0, 0, 0);
         new Cor(255, 255, 255, 255);
      }

      @ParameterizedTest
      @ValueSource(ints = { -1, 256, 1000 })
      void componenteForaDaFaixaEhRecusado(int invalido) {
         assertThrows(CorInvalidaException.class,
                      () -> new Cor(invalido, 0, 0));
         assertThrows(CorInvalidaException.class,
                      () -> new Cor(0, invalido, 0));
         assertThrows(CorInvalidaException.class,
                      () -> new Cor(0, 0, invalido));
         assertThrows(CorInvalidaException.class,
                      () -> new Cor(0, 0, 0, invalido));
      }

      @Test
      void mensagemIndicaOComponenteEOValor() {
         CorInvalidaException ex = assertThrows(CorInvalidaException.class,
                                                () -> new Cor(0, 300, 0));

         assertTrue(ex.getMessage().contains("'g'"));
         assertTrue(ex.getMessage().contains("300"));
      }
   }

   @Nested
   class DeHex {
      @Test
      void formatoLongo() {
         assertEquals(new Cor(255, 0, 0), Cor.deHex("#FF0000"));
         assertEquals(new Cor(18, 171, 205), Cor.deHex("#12ABCD"));
      }

      @Test
      void formatoCurtoDobraCadaDigito() {
         assertEquals(new Cor(255, 255, 255), Cor.deHex("#FFF"));
         assertEquals(new Cor(255, 136, 0), Cor.deHex("#F80"));
      }

      @Test
      void aceitaMinusculasEMaiusculas() {
         assertEquals(Cor.deHex("#ABCDEF"), Cor.deHex("#abcdef"));
         assertEquals(new Cor(255, 255, 255), Cor.deHex("#ffffff"));
      }

      @ParameterizedTest
      @ValueSource(strings = { "FF0000", "#GGGGGG", "#12345", "#1234567",
                               "#12", "#", "", "  ", "#FF 000", "#FF00000x" })
      @NullSource
      void formatoInvalidoEhRecusado(String texto) {
         assertThrows(CorInvalidaException.class, () -> Cor.deHex(texto));
      }
   }

   @Nested
   class Conversoes {
      @Test
      void hexEmMaiusculasSemAlfa() {
         assertEquals("#FF0000", new Cor(255, 0, 0).hex());
         assertEquals("#00AB0F", new Cor(0, 171, 15, 77).hex());
         assertEquals("#000000", new Cor(0, 0, 0).hex());
      }

      @Test
      void hexVoltaAoMesmoValor() {
         assertEquals("#12ABCD", Cor.deHex("#12abcd").hex());
      }

      @Test
      void argbNoFormatoDoBufferedImage() {
         assertEquals(0xFFFF0000, new Cor(255, 0, 0).argb());
         assertEquals(0x80112233, new Cor(0x11, 0x22, 0x33, 0x80).argb());
         assertEquals(0x00000000, new Cor(0, 0, 0, 0).argb());
      }

      @Test
      void deArgbSeparaOsComponentes() {
         assertEquals(new Cor(0x11, 0x22, 0x33, 0x80),
                      Cor.deArgb(0x80112233));
         assertEquals(new Cor(255, 255, 255, 255), Cor.deArgb(0xFFFFFFFF));
         assertEquals(new Cor(0, 0, 0, 0), Cor.deArgb(0));
      }

      @Test
      void argbEDeArgbSaoInversos() {
         Cor cor = new Cor(200, 100, 50, 25);

         assertEquals(cor, Cor.deArgb(cor.argb()));
      }
   }

   @Nested
   class Brilho {
      @Test
      void lumaDoBrancoEDoPreto() {
         assertEquals(255.0, new Cor(255, 255, 255).luma(), 1e-9);
         assertEquals(0.0, new Cor(0, 0, 0).luma(), 1e-9);
      }

      @Test
      void lumaPesaOsCanaisComoNaEspecificacao() {
         assertEquals(0.2126 * 255, new Cor(255, 0, 0).luma(), 1e-9);
         assertEquals(0.7152 * 255, new Cor(0, 255, 0).luma(), 1e-9);
         assertEquals(0.0722 * 255, new Cor(0, 0, 255).luma(), 1e-9);
      }

      @Test
      void lumaIgnoraOAlfa() {
         assertEquals(new Cor(10, 20, 30, 255).luma(),
                      new Cor(10, 20, 30, 0).luma(), 1e-9);
      }
   }

   @Nested
   class LuminanciaRelativa {
      @Test
      void brancoValeUmEPretoValeZero() {
         assertEquals(1.0, new Cor(255, 255, 255).luminanciaRelativa(), 1e-9);
         assertEquals(0.0, new Cor(0, 0, 0).luminanciaRelativa(), 1e-9);
      }

      @Test
      void vermelhoPuroVale0_2126() {
         assertEquals(0.2126, new Cor(255, 0, 0).luminanciaRelativa(), 1e-9);
      }

      @Test
      void trechoLinearParaComponentesMuitoEscuros() {
         // 10/255 = 0,03922 não passa de 0,03928, então o componente é só
         // dividido por 12,92
         double esperado = (10 / 255.0) / 12.92;

         assertEquals(esperado, new Cor(10, 10, 10).luminanciaRelativa(), 1e-9);
      }

      @Test
      void cinza767676ValeCercaDe0_1812() {
         assertEquals(0.1812, Cor.deHex("#767676").luminanciaRelativa(), 1e-4);
      }
   }
}
