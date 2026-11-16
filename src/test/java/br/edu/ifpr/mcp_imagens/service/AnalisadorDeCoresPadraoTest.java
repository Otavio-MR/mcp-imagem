package br.edu.ifpr.mcp_imagens.service;

import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.AZUL;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.BRANCO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.PRETO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.VERMELHO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.model.CorDominante;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoCoresDominantes;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;

class AnalisadorDeCoresPadraoTest {
   private final AnalisadorDeCores analisador = new AnalisadorDeCoresPadrao();

   // 10 pixels: 5 vermelhos, 3 brancos, 2 pretos
   private final Imagem cinco = ImagensSinteticas.de(
         new int[] { VERMELHO, VERMELHO, VERMELHO, VERMELHO, VERMELHO },
         new int[] { BRANCO, BRANCO, BRANCO, PRETO, PRETO });

   private static List<String> hexes(ResultadoCoresDominantes r) {
      return r.cores().stream().map(CorDominante::cor).toList();
   }

   @Test
   void passoUmDevolveAsCoresExatasEmOrdemDecrescente() {
      ResultadoCoresDominantes r = analisador.coresDominantes(cinco, 5, 1,
                                                               true);

      assertEquals(List.of("#FF0000", "#FFFFFF", "#000000"), hexes(r));
      assertEquals(List.of(5L, 3L, 2L),
                   r.cores().stream().map(CorDominante::quantidade).toList());
      assertEquals(10, r.totalConsiderado());
      assertEquals(3, r.gruposDistintos());
   }

   @Test
   void percentuaisTemDuasCasas() {
      ResultadoCoresDominantes r = analisador.coresDominantes(cinco, 5, 1,
                                                               true);

      assertEquals(new BigDecimal("50.00"), r.cores().get(0).percentual());
      assertEquals(new BigDecimal("30.00"), r.cores().get(1).percentual());
   }

   @Test
   void limitaAQuantidadePedidaMasContaTodosOsGrupos() {
      ResultadoCoresDominantes r = analisador.coresDominantes(cinco, 2, 1,
                                                               true);

      assertEquals(List.of("#FF0000", "#FFFFFF"), hexes(r));
      assertEquals(3, r.gruposDistintos());
   }

   @Test
   void empateSaiEmOrdemCrescenteDoHex() {
      Imagem empate = ImagensSinteticas.de(new int[] { AZUL, VERMELHO, PRETO });

      assertEquals(List.of("#000000", "#0000FF", "#FF0000"),
                   hexes(analisador.coresDominantes(empate, 3, 1, true)));
      // com limite menor, ficam os de menor hex
      assertEquals(List.of("#000000", "#0000FF"),
                   hexes(analisador.coresDominantes(empate, 2, 1, true)));
   }

   @Test
   void passoAgrupaCoresParecidas() {
      // 0xFF0000 e 0xFE0000 caem na mesma faixa com passo 32
      Imagem parecidas = ImagensSinteticas.de(
            new int[] { 0xFFFF0000, 0xFFFE0000, 0xFFFF0000, BRANCO });

      ResultadoCoresDominantes r = analisador.coresDominantes(parecidas, 5,
                                                               32, true);

      assertEquals(2, r.gruposDistintos());
      assertEquals(3, r.cores().get(0).quantidade());
      // centro de cada faixa com passo 32: R 7*32+16 = 240, G e B 0*32+16 = 16
      assertEquals("#F01010", r.cores().get(0).cor());
   }

   @Test
   void passoUmNaoAgrupaNada() {
      Imagem parecidas = ImagensSinteticas.de(
            new int[] { 0xFFFF0000, 0xFFFE0000 });

      assertEquals(2, analisador.coresDominantes(parecidas, 5, 1, true)
                                .gruposDistintos());
   }

   @Test
   void passoMaximoTemDuasFaixasPorCanal() {
      // passo 128: faixas 0..127 (centro 64) e 128..255 (centro 192)
      Imagem img = ImagensSinteticas.de(new int[] { PRETO, BRANCO, BRANCO });

      ResultadoCoresDominantes r = analisador.coresDominantes(img, 5, 128,
                                                               true);

      assertEquals(List.of("#C0C0C0", "#404040"), hexes(r));
   }

   @Test
   void passoQueNaoDivide256NaoPassaDe255() {
      // passo 100: faixas 0..99, 100..199, 200..255 (centro 250)
      Imagem img = ImagensSinteticas.de(new int[] { BRANCO });

      assertEquals(List.of("#FAFAFA"),
                   hexes(analisador.coresDominantes(img, 5, 100, true)));
      // passo 200: segunda faixa 200..255, centro 300 limitado a 255
      assertEquals(List.of("#FFFFFF"),
                   hexes(analisador.coresDominantes(img, 5, 200, true)));
   }

   @Test
   void transparentesSaoIgnoradosOuContados() {
      Imagem img = ImagensSinteticas.de(
            new int[] { 0x00FF0000, 0x00FF0000, 0x00FF0000, BRANCO });

      ResultadoCoresDominantes ignorando = analisador.coresDominantes(img, 5,
                                                                      1, true);
      ResultadoCoresDominantes contando = analisador.coresDominantes(img, 5, 1,
                                                                     false);

      assertEquals(1, ignorando.totalConsiderado());
      assertEquals(List.of("#FFFFFF"), hexes(ignorando));
      assertEquals(4, contando.totalConsiderado());
      assertEquals("#FF0000", contando.cores().get(0).cor());
      assertEquals(3, contando.cores().get(0).quantidade());
   }

   @Test
   void imagemTodaTransparenteIgnoradaNaoTemCores() {
      ResultadoCoresDominantes r = analisador.coresDominantes(
            ImagensSinteticas.solida(3, 3, 0x00000000), 5, 1, true);

      assertEquals(0, r.totalConsiderado());
      assertEquals(0, r.gruposDistintos());
      assertEquals(List.of(), r.cores());
   }

   @Test
   void imagemDeUmaCorSoTemUmGrupoDeCemPorCento() {
      ResultadoCoresDominantes r = analisador.coresDominantes(
            ImagensSinteticas.solida(4, 4, AZUL), 5, 1, true);

      assertEquals(1, r.cores().size());
      assertEquals(new BigDecimal("100.00"), r.cores().get(0).percentual());
   }

   @Test
   void gradienteGeraGruposDeMesmoTamanho() {
      // R = x*16 e G = y*16: com passo 32 são 8 x 8 grupos de 4 pixels
      Imagem gradiente = ImagensSinteticas.gerar(16, 16,
            (x, y) -> 0xFF000000 | (x * 16) << 16 | (y * 16) << 8);

      ResultadoCoresDominantes r = analisador.coresDominantes(gradiente, 20,
                                                               32, true);

      assertEquals(256, r.totalConsiderado());
      assertEquals(64, r.gruposDistintos());
      assertEquals(20, r.cores().size());
      assertEquals(4, r.cores().get(0).quantidade());
      assertEquals(new BigDecimal("1.56"), r.cores().get(0).percentual());
   }

   @Test
   void filtroContaSoOsPixelsAceitos() {
      // só os pixels que não são brancos
      ResultadoCoresDominantes r = analisador.coresDominantes(cinco, 5, 1,
            true, argb -> argb != BRANCO);

      assertEquals(7, r.totalConsiderado());
      assertEquals(List.of("#FF0000", "#000000"), hexes(r));
      assertEquals(new BigDecimal("71.43"), r.cores().get(0).percentual());
   }

   @Test
   void filtroQueNaoAceitaNadaDeixaOResultadoVazio() {
      ResultadoCoresDominantes r = analisador.coresDominantes(cinco, 5, 1,
            true, argb -> false);

      assertEquals(0, r.totalConsiderado());
      assertEquals(0, r.gruposDistintos());
      assertEquals(List.of(), r.cores());
   }
}
