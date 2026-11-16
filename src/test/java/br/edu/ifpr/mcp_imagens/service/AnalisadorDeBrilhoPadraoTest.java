package br.edu.ifpr.mcp_imagens.service;

import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.BRANCO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.PRETO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.VERMELHO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.model.FaixaDeBrilho;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoHistograma;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;

class AnalisadorDeBrilhoPadraoTest {
   private final AnalisadorDeBrilho analisador = new AnalisadorDeBrilhoPadrao();

   /** 256 pixels, um para cada tom de cinza (brilho 0 a 255). */
   private static Imagem todosOsCinzas() {
      return ImagensSinteticas.gerar(16, 16, (x, y) -> {
         int g = y * 16 + x;

         return 0xFF000000 | (g << 16) | (g << 8) | g;
      });
   }

   @Test
   void oitoFaixasTemTrintaEDoisTonsCada() {
      ResultadoHistograma r = analisador.histograma(todosOsCinzas(), 8, true);

      assertEquals(256, r.totalConsiderado());
      assertEquals(8, r.faixas().size());
      for (FaixaDeBrilho faixa : r.faixas()) {
         assertEquals(32, faixa.quantidade());
         assertEquals(new BigDecimal("12.50"), faixa.percentual());
      }
      assertEquals(0, r.faixas().get(0).lumaMinima());
      assertEquals(31, r.faixas().get(0).lumaMaxima());
      assertEquals(224, r.faixas().get(7).lumaMinima());
      assertEquals(255, r.faixas().get(7).lumaMaxima());
   }

   @Test
   void tresFaixasTemLimitesArredondadosParaCima() {
      ResultadoHistograma r = analisador.histograma(todosOsCinzas(), 3, true);

      assertEquals(List.of(0, 86, 171),
                   r.faixas().stream().map(FaixaDeBrilho::lumaMinima).toList());
      assertEquals(List.of(85, 170, 255),
                   r.faixas().stream().map(FaixaDeBrilho::lumaMaxima).toList());
      assertEquals(List.of(86L, 85L, 85L),
                   r.faixas().stream().map(FaixaDeBrilho::quantidade).toList());
   }

   /** Para toda quantidade de faixas, os intervalos cobrem 0..255 sem falhas. */
   @ParameterizedTest
   @ValueSource(ints = { 2, 3, 5, 7, 8, 10, 13, 16, 20, 31, 32 })
   void intervalosCobremAEscalaInteiraEBatemComAContagem(int faixas) {
      ResultadoHistograma r = analisador.histograma(todosOsCinzas(), faixas,
                                                    true);
      List<FaixaDeBrilho> lista = r.faixas();

      assertEquals(faixas, lista.size());
      assertEquals(0, lista.get(0).lumaMinima());
      assertEquals(255, lista.get(faixas - 1).lumaMaxima());
      for (int i = 0; i < faixas; i++) {
         FaixaDeBrilho f = lista.get(i);

         assertEquals(i, f.indice());
         // cada tom de cinza aparece uma vez: a contagem é o tamanho do
         // intervalo
         assertEquals(f.lumaMaxima() - f.lumaMinima() + 1, f.quantidade());
         if (i > 0)
            assertEquals(lista.get(i - 1).lumaMaxima() + 1, f.lumaMinima());
      }
      assertEquals(256, lista.stream().mapToLong(FaixaDeBrilho::quantidade)
                             .sum());
   }

   @Test
   void pretoNaPrimeiraFaixaEBrancoNaUltima() {
      Imagem img = ImagensSinteticas.de(new int[] { PRETO, BRANCO, BRANCO });

      ResultadoHistograma r = analisador.histograma(img, 8, true);

      assertEquals(1, r.faixas().get(0).quantidade());
      assertEquals(2, r.faixas().get(7).quantidade());
      assertEquals(new BigDecimal("66.67"), r.faixas().get(7).percentual());
   }

   @Test
   void usaOBrilhoDaCorEOVermelhoPuroTem54() {
      // luma de #FF0000 = 54,2: faixa 1 de 8 (32 a 63)
      ResultadoHistograma r = analisador.histograma(
            ImagensSinteticas.solida(2, 2, VERMELHO), 8, true);

      assertEquals(4, r.faixas().get(1).quantidade());
   }

   @Test
   void faixasVaziasAparecemComZero() {
      ResultadoHistograma r = analisador.histograma(
            ImagensSinteticas.solida(2, 2, PRETO), 4, true);

      assertEquals(4, r.faixas().size());
      assertEquals(4, r.faixas().get(0).quantidade());
      assertEquals(0, r.faixas().get(3).quantidade());
      assertEquals(new BigDecimal("0.00"), r.faixas().get(3).percentual());
   }

   @Test
   void transparentesIgnoradosOuContados() {
      Imagem img = ImagensSinteticas.de(
            new int[] { 0x00FFFFFF, 0x00FFFFFF, BRANCO });

      ResultadoHistograma ignorando = analisador.histograma(img, 2, true);
      ResultadoHistograma contando  = analisador.histograma(img, 2, false);

      assertEquals(1, ignorando.totalConsiderado());
      assertEquals(3, contando.totalConsiderado());
      assertEquals(3, contando.faixas().get(1).quantidade());
   }

   @Test
   void imagemTodaTransparenteIgnoradaTemFaixasZeradas() {
      ResultadoHistograma r = analisador.histograma(
            ImagensSinteticas.solida(2, 2, 0x00000000), 4, true);

      assertEquals(0, r.totalConsiderado());
      assertEquals(4, r.faixas().size());
      assertEquals(new BigDecimal("0.00"), r.faixas().get(0).percentual());
   }
}
