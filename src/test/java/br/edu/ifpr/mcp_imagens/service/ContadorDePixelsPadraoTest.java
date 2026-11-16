package br.edu.ifpr.mcp_imagens.service;

import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.AZUL;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.BRANCO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.PRETO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.VERMELHO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.criterio.Claro;
import br.edu.ifpr.mcp_imagens.domain.criterio.CorComTolerancia;
import br.edu.ifpr.mcp_imagens.domain.criterio.CriterioDePixel;
import br.edu.ifpr.mcp_imagens.domain.criterio.ForaDaPaleta;
import br.edu.ifpr.mcp_imagens.domain.criterio.Transparente;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContagem;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;

class ContadorDePixelsPadraoTest {
   private final ContadorDePixels contador = new ContadorDePixelsPadrao();

   private static void confere(ResultadoContagem resultado, long quantidade,
                               long total, String percentual) {
      assertEquals(quantidade, resultado.quantidade(), "quantidade");
      assertEquals(total, resultado.totalConsiderado(), "total considerado");
      assertEquals(percentual, resultado.percentual().toPlainString(),
                   "percentual");
   }

   // 10x10: as 3 primeiras linhas (30 pixels) vermelhas e as outras 70 brancas
   private static Imagem trintaVermelhosESetentaBrancos() {
      return ImagensSinteticas.gerar(10, 10,
                                     (x, y) -> y < 3 ? VERMELHO : BRANCO);
   }

   @Nested
   class PorCor {
      @Test
      void trintaVermelhosEmCem() {
         confere(contador.contar(trintaVermelhosESetentaBrancos(),
                                 new CorComTolerancia(new Cor(255, 0, 0), 0),
                                 true),
                 30, 100, "30.00");
      }

      @Test
      void brancosSaoOsOutrosSetenta() {
         confere(contador.contar(trintaVermelhosESetentaBrancos(),
                                 new CorComTolerancia(new Cor(255, 255, 255),
                                                      0), true),
                 70, 100, "70.00");
      }

      @Test
      void corAusenteDaImagemDaZero() {
         confere(contador.contar(trintaVermelhosESetentaBrancos(),
                                 new CorComTolerancia(new Cor(0, 0, 255), 0),
                                 true),
                 0, 100, "0.00");
      }

      @Test
      void pixelQuaseVermelhoEntraAPartirDaToleranciaUm() {
         // igual à imagem anterior, mas o pixel (0, 0) é #FE0000
         Imagem imagem = ImagensSinteticas.gerar(10, 10, (x, y) ->
               x == 0 && y == 0 ? 0xFFFE0000 : y < 3 ? VERMELHO : BRANCO);
         Cor    vermelho = new Cor(255, 0, 0);

         confere(contador.contar(imagem, new CorComTolerancia(vermelho, 0),
                                 true), 29, 100, "29.00");
         confere(contador.contar(imagem, new CorComTolerancia(vermelho, 1),
                                 true), 30, 100, "30.00");
      }

      @Test
      void toleranciaMaximaContaTudo() {
         confere(contador.contar(trintaVermelhosESetentaBrancos(),
                                 new CorComTolerancia(new Cor(0, 0, 0), 255),
                                 true),
                 100, 100, "100.00");
      }
   }

   @Nested
   class Transparencia {
      // alfa 0, alfa 128 e alfa 255, todos com a cor vermelha
      private final Imagem tresAlfas = ImagensSinteticas.de(
            new int[] { 0x00FF0000, 0x80FF0000, 0xFFFF0000 });

      @Test
      void contaOsTransparentes() {
         confere(contador.contar(tresAlfas, new Transparente(), false),
                 1, 3, "33.33");
      }

      @Test
      void ignorandoOsTransparentesNaoHaMaisTransparentesParaContar() {
         confere(contador.contar(tresAlfas, new Transparente(), true),
                 0, 2, "0.00");
      }

      @Test
      void ignorarTransparentesTiraDoTotalConsiderado() {
         CriterioDePixel vermelho = new CorComTolerancia(new Cor(255, 0, 0), 0);

         confere(contador.contar(tresAlfas, vermelho, false), 3, 3, "100.00");
         confere(contador.contar(tresAlfas, vermelho, true), 2, 2, "100.00");
      }

      @Test
      void imagemTodaTransparenteIgnoradaNaoTemTotalNemDivisaoPorZero() {
         Imagem vazia = ImagensSinteticas.solida(4, 4, 0x00000000);

         confere(contador.contar(vazia, new Claro(0), true), 0, 0, "0.00");
      }

      @Test
      void imagemTodaTransparenteSemIgnorarContaTodos() {
         Imagem vazia = ImagensSinteticas.solida(4, 4, 0x00000000);

         confere(contador.contar(vazia, new Transparente(), false),
                 16, 16, "100.00");
      }

      @Test
      void oCriterioNaoEConsultadoParaPixelIgnorado() {
         AtomicInteger consultas = new AtomicInteger();

         contador.contar(tresAlfas, argb -> {
            consultas.incrementAndGet();
            return true;
         }, true);

         assertEquals(2, consultas.get());
      }
   }

   @Nested
   class ClaroEEscuro {
      // o contrário de Claro(128): brilho abaixo do limiar
      private final CriterioDePixel escuro128 = argb -> Cor.luma(argb) < 128;

      // metade esquerda preta, metade direita branca
      private final Imagem metadeMetade = ImagensSinteticas.gerar(10, 10,
            (x, y) -> x < 5 ? PRETO : BRANCO);

      @Test
      void cinquentaPorCentoClaros() {
         confere(contador.contar(metadeMetade, new Claro(128), true),
                 50, 100, "50.00");
      }

      @Test
      void cinquentaPorCentoEscuros() {
         confere(contador.contar(metadeMetade, escuro128, true),
                 50, 100, "50.00");
      }

      @Test
      void clarosMaisEscurosSomamOTotal() {
         Imagem imagem = ImagensSinteticas.gerar(7, 3, (x, y) ->
               (x + y) % 3 == 0 ? BRANCO : (x + y) % 3 == 1 ? 0xFF808080
                                                            : PRETO);
         ResultadoContagem claros   = contador.contar(imagem, new Claro(128),
                                                      true);
         ResultadoContagem escuros  = contador.contar(imagem, escuro128,
                                                      true);

         assertEquals(21, claros.quantidade() + escuros.quantidade());
      }
   }

   @Nested
   class ForaDaPaletaDePixels {
      @Test
      void tresQuartosForaDaPaleta() {
         // quatro cores em quatro quadrantes; a paleta só tem o vermelho
         Imagem imagem = ImagensSinteticas.gerar(4, 4, (x, y) ->
               x < 2 ? (y < 2 ? VERMELHO : BRANCO) : (y < 2 ? AZUL : PRETO));

         confere(contador.contar(imagem,
                                 new ForaDaPaleta(List.of(new Cor(255, 0, 0)),
                                                  0), true),
                 12, 16, "75.00");
      }

      @Test
      void paletaComTodasAsCoresDeixaZeroFora() {
         Imagem imagem = ImagensSinteticas.gerar(10, 10,
                                                 (x, y) -> y < 3 ? VERMELHO
                                                                 : BRANCO);

         confere(contador.contar(imagem, new ForaDaPaleta(
                     List.of(new Cor(255, 0, 0), new Cor(255, 255, 255)), 0),
                                 true),
                 0, 100, "0.00");
      }
   }

   @Test
   void arredondaOPercentualComHalfUp() {
      Imagem umTerco = ImagensSinteticas.de(new int[] { VERMELHO, BRANCO,
                                                        BRANCO });

      confere(contador.contar(umTerco, new CorComTolerancia(new Cor(255, 0, 0),
                                                            0), true),
              1, 3, "33.33");
      // os outros dois terços: os pixels brancos
      confere(contador.contar(umTerco, argb -> argb == BRANCO, true),
              2, 3, "66.67");
   }

   @Test
   void umaImagemDeUmPixel() {
      confere(contador.contar(ImagensSinteticas.solida(1, 1, VERMELHO),
                              new CorComTolerancia(new Cor(255, 0, 0), 0),
                              true), 1, 1, "100.00");
   }

   @Test
   void imagemSemPixelsDaZeroSemErro() {
      confere(contador.contar(ImagensSinteticas.de(), new Claro(0), true),
              0, 0, "0.00");
   }

   @Test
   void imagemGrandeEPercorridaPorInteiro() {
      Imagem grande = ImagensSinteticas.solida(1000, 1000, VERMELHO);

      confere(contador.contar(grande, new CorComTolerancia(new Cor(255, 0, 0),
                                                           0), true),
              1_000_000, 1_000_000, "100.00");
   }
}
