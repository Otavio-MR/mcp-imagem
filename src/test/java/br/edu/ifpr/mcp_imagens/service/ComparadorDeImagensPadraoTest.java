package br.edu.ifpr.mcp_imagens.service;

import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.BRANCO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.PRETO;
import static br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas.VERMELHO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.exception.DimensoesDiferentesException;
import br.edu.ifpr.mcp_imagens.domain.model.CaixaDelimitadora;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoComparacao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;

class ComparadorDeImagensPadraoTest {
   private final ComparadorDeImagens comparador =
                                                new ComparadorDeImagensPadrao();

   /** Imagem 5x4 branca, com os pixels dados trocados por vermelho. */
   private static Imagem brancaCom(int... posicoes) {
      return ImagensSinteticas.gerar(5, 4, (x, y) -> {
         for (int i = 0; i < posicoes.length; i += 2)
            if (posicoes[i] == x && posicoes[i + 1] == y)
               return VERMELHO;
         return BRANCO;
      });
   }

   @Test
   void imagensIdenticasNaoTemDiferencas() {
      ResultadoComparacao r = comparador.comparar(brancaCom(1, 1),
                                                  brancaCom(1, 1), 0);

      assertEquals(20, r.totalPixels());
      assertEquals(0, r.pixelsDiferentes());
      assertEquals(20, r.pixelsIguais());
      assertNull(r.regiaoDiferente());
   }

   @Test
   void umPixelDiferenteTemRegiaoDeUmPixel() {
      ResultadoComparacao r = comparador.comparar(brancaCom(),
                                                  brancaCom(3, 2), 0);

      assertEquals(1, r.pixelsDiferentes());
      assertEquals(19, r.pixelsIguais());
      assertEquals(new CaixaDelimitadora(3, 2, 3, 2), r.regiaoDiferente());
   }

   @Test
   void regiaoEOMenorRetanguloQueContemTodasAsDiferencas() {
      // cantos opostos: a caixa é a imagem inteira
      ResultadoComparacao r = comparador.comparar(
            brancaCom(), brancaCom(0, 0, 4, 3), 0);

      assertEquals(2, r.pixelsDiferentes());
      assertEquals(new CaixaDelimitadora(0, 0, 4, 3), r.regiaoDiferente());
   }

   @Test
   void regiaoNaoDependeDaOrdemDosArquivos() {
      ResultadoComparacao ab = comparador.comparar(brancaCom(),
                                                   brancaCom(1, 1, 3, 2), 0);
      ResultadoComparacao ba = comparador.comparar(brancaCom(1, 1, 3, 2),
                                                   brancaCom(), 0);

      assertEquals(ab, ba);
      assertEquals(new CaixaDelimitadora(1, 1, 3, 2), ab.regiaoDiferente());
   }

   @Test
   void toleranciaAceitaPequenasDiferencasPorCanal() {
      Imagem a = ImagensSinteticas.de(new int[] { 0xFF102030, 0xFF000000 });
      Imagem b = ImagensSinteticas.de(new int[] { 0xFF112233, 0xFF000000 });

      // maior diferença é 3 (azul: 0x30 contra 0x33)
      assertEquals(1, comparador.comparar(a, b, 0).pixelsDiferentes());
      assertEquals(1, comparador.comparar(a, b, 2).pixelsDiferentes());
      assertEquals(0, comparador.comparar(a, b, 3).pixelsDiferentes());
   }

   @Test
   void oAlfaTambemEComparado() {
      Imagem a = ImagensSinteticas.de(new int[] { 0xFFFF0000 });
      Imagem b = ImagensSinteticas.de(new int[] { 0x80FF0000 });

      assertEquals(1, comparador.comparar(a, b, 0).pixelsDiferentes());
      assertEquals(1, comparador.comparar(a, b, 126).pixelsDiferentes());
      assertEquals(0, comparador.comparar(a, b, 127).pixelsDiferentes());
   }

   @Test
   void pixelTransparenteComCoresDiferentesContaComoDiferente() {
      Imagem a = ImagensSinteticas.de(new int[] { 0x00FF0000 });
      Imagem b = ImagensSinteticas.de(new int[] { 0x000000FF });

      assertEquals(1, comparador.comparar(a, b, 0).pixelsDiferentes());
   }

   @Test
   void toleranciaMaximaIgualaTudo() {
      Imagem preta  = ImagensSinteticas.solida(3, 3, PRETO);
      Imagem branca = ImagensSinteticas.solida(3, 3, BRANCO);

      assertEquals(9, comparador.comparar(preta, branca, 0)
                                .pixelsDiferentes());
      assertEquals(9, comparador.comparar(preta, branca, 254)
                                .pixelsDiferentes());
      assertEquals(0, comparador.comparar(preta, branca, 255)
                                .pixelsDiferentes());
   }

   @Test
   void tamanhosDiferentesDaoErroComAsDuasDimensoes() {
      DimensoesDiferentesException e = assertThrows(
            DimensoesDiferentesException.class,
            () -> comparador.comparar(ImagensSinteticas.solida(5, 4, BRANCO),
                                      ImagensSinteticas.solida(4, 5, BRANCO),
                                      0));

      assertEquals("as imagens precisam ter o mesmo tamanho: a primeira tem " +
                   "5x4 e a segunda tem 4x5", e.getMessage());
   }

   @Test
   void mesmaAreaComLarguraEAlturaTrocadasEDiferente() {
      assertThrows(DimensoesDiferentesException.class,
                   () -> comparador.comparar(
                         ImagensSinteticas.solida(2, 6, BRANCO),
                         ImagensSinteticas.solida(3, 4, BRANCO), 0));
   }
}
