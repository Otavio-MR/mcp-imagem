package br.edu.ifpr.mcp_imagens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.exception.FormatoNaoSuportadoException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemNaoEncontradaException;
import br.edu.ifpr.mcp_imagens.domain.model.FormatoImagem;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemAberta;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;
import br.edu.ifpr.mcp_imagens.domain.model.InformacoesImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ListaDeImagens;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;

class InformacoesServicePadraoTest {
   /** Dublê do repositório com uma lista fixa. */
   record RepositorioFixo(Path pasta, List<ImagemInfo> imagens)
          implements ImagemRepository {
      @Override public List<ImagemInfo> listar() { return imagens; }

      @Override
      public ImagemAberta abrir(String nome) {
         throw new UnsupportedOperationException("este teste só lista");
      }
   }

   @Nested
   class Listar {
      @Test
      void listaAsImagensDoRepositorioComAPastaEOTotal() {
         Path pasta = Path.of("imagens").toAbsolutePath();
         InformacoesService servico = new InformacoesServicePadrao(
               new RepositorioFixo(pasta, List.of(
                     new ImagemInfo("a.png", FormatoImagem.PNG, 3),
                     new ImagemInfo("b.jpg", FormatoImagem.JPEG, 40))));

         ListaDeImagens lista = servico.listarImagens();

         assertEquals(pasta.toString(), lista.pasta());
         assertEquals(2, lista.total());
         assertEquals("a.png", lista.imagens().get(0).nome());
         assertEquals(FormatoImagem.JPEG, lista.imagens().get(1).formato());
         assertEquals(40, lista.imagens().get(1).tamanhoKb());
      }

      @Test
      void semImagensEhListaVaziaComTotalZero() {
         ListaDeImagens lista = new InformacoesServicePadrao(
               new RepositorioFixo(Path.of("x"), List.of())).listarImagens();

         assertEquals(0, lista.total());
         assertTrue(lista.imagens().isEmpty());
      }
   }

   @Nested
   class Obter {
      private InformacoesService servicoCom(ImagemAberta aberta) {
         return new InformacoesServicePadrao(RepositorioFalso.com(aberta));
      }

      @Test
      void informaDimensoesFormatoAlfaETamanho() {
         InformacoesImagem info = servicoCom(ImagensSinteticas.aberta(
               "banner.png", ImagensSinteticas.solida(20, 10, 0xFF202020),
               3000)).obter("banner.png");

         assertEquals("banner.png", info.arquivo());
         assertEquals(FormatoImagem.PNG, info.formato());
         assertEquals(20, info.largura());
         assertEquals(10, info.altura());
         assertEquals(200, info.totalPixels());
         assertFalse(info.temCanalAlfa());
         assertEquals(3000, info.tamanhoBytes());
         assertEquals(3, info.tamanhoKb());
      }

      @Test
      void imagemComTransparenciaTemCanalAlfa() {
         assertTrue(servicoCom(ImagensSinteticas.aberta("icone.png",
               ImagensSinteticas.solida(8, 8, 0x00000000)))
               .obter("icone.png").temCanalAlfa());
      }

      @Test
      void tamanhoEmKbArredondaParaCima() {
         assertEquals(1, kb(1));
         assertEquals(1, kb(1024));
         assertEquals(2, kb(1025));
         assertEquals(0, kb(0));
      }

      private long kb(long bytes) {
         return servicoCom(ImagensSinteticas.aberta("a.png",
               ImagensSinteticas.solida(1, 1, 0xFF000000), bytes))
               .obter("a.png").tamanhoKb();
      }

      @Test
      void totalDePixelsNaoEstouraInt() {
         InformacoesImagem info = servicoCom(ImagensSinteticas.aberta("a.png",
               new Imagem() {
                  @Override public int largura()           { return 100_000; }
                  @Override public int altura()            { return 100_000; }
                  @Override public boolean temCanalAlfa()  { return false; }
                  @Override public int argb(int x, int y)  { return 0; }
               })).obter("a.png");

         assertEquals(10_000_000_000L, info.totalPixels());
      }

      @Test
      void repassaOsErrosDeAcessoAImagem() {
         assertThrows(ImagemNaoEncontradaException.class,
               () -> servicoCom(ImagensSinteticas.aberta("a.png",
                     ImagensSinteticas.solida(1, 1, 0))).obter("outra.png"));

         for (var falha : List.of(new FormatoNaoSuportadoException("f"),
                                  new ImagemInvalidaException("i"),
                                  new ImagemMuitoGrandeException("g")))
            assertThrows(falha.getClass(),
                         () -> new InformacoesServicePadrao(
                               RepositorioFalso.falhando(falha)).obter("a.png"));
      }
   }
}
