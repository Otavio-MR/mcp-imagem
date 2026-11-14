package br.edu.ifpr.mcp_imagens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.model.FormatoImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;
import br.edu.ifpr.mcp_imagens.domain.model.ListaDeImagens;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;

class InformacoesServicePadraoTest {
   /** Dublê do repositório com uma lista fixa. */
   record RepositorioFixo(Path pasta, List<ImagemInfo> imagens)
          implements ImagemRepository {
      @Override public List<ImagemInfo> listar() { return imagens; }
   }

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
