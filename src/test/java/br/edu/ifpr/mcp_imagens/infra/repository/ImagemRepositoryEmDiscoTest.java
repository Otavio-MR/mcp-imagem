package br.edu.ifpr.mcp_imagens.infra.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_imagens.domain.model.FormatoImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;

class ImagemRepositoryEmDiscoTest {
   @TempDir
   Path pasta;

   private void criar(String nome, int bytes) throws IOException {
      Files.write(pasta.resolve(nome), new byte[bytes]);
   }

   private List<String> nomes() {
      return new ImagemRepositoryEmDisco(pasta).listar().stream()
                                               .map(ImagemInfo::nome).toList();
   }

   @Test
   void listaSoOsFormatosAceitosEmOrdemAlfabeticaSemDiferenciarCaixa()
         throws IOException {
      criar("b.png", 10);
      criar("A.jpg", 10);
      criar("c.GIF", 10);
      criar("d.bmp", 10);
      criar("e.jpeg", 10);
      criar("notas.txt", 10);
      criar("sem-extensao", 10);
      criar("foto.tiff", 10);

      assertEquals(List.of("A.jpg", "b.png", "c.GIF", "d.bmp", "e.jpeg"),
                   nomes());
   }

   @Test
   void informaOFormatoPelaExtensao() throws IOException {
      criar("a.png", 1);
      criar("b.JPG", 1);
      criar("c.jpeg", 1);
      criar("d.gif", 1);
      criar("e.bmp", 1);

      assertEquals(List.of(FormatoImagem.PNG, FormatoImagem.JPEG,
                           FormatoImagem.JPEG, FormatoImagem.GIF,
                           FormatoImagem.BMP),
                   new ImagemRepositoryEmDisco(pasta).listar().stream()
                         .map(ImagemInfo::formato).toList());
   }

   @Test
   void tamanhoEmKbEhArredondadoParaCima() throws IOException {
      criar("vazio.png", 0);
      criar("um-byte.png", 1);
      criar("exato.png", 1024);
      criar("passa-um.png", 1025);
      criar("dois-kb.png", 2048);

      assertEquals(0L, tamanhoKb("vazio.png"));
      assertEquals(1L, tamanhoKb("um-byte.png"));
      assertEquals(1L, tamanhoKb("exato.png"));
      assertEquals(2L, tamanhoKb("passa-um.png"));
      assertEquals(2L, tamanhoKb("dois-kb.png"));
   }

   private long tamanhoKb(String nome) {
      return new ImagemRepositoryEmDisco(pasta).listar().stream()
                                               .filter(i -> i.nome().equals(nome))
                                               .findFirst().orElseThrow()
                                               .tamanhoKb();
   }

   @Test
   void pastaComNomeDeImagemNaoContaComoImagem() throws IOException {
      Files.createDirectory(pasta.resolve("pasta.png"));
      criar("real.png", 1);

      assertEquals(List.of("real.png"), nomes());
   }

   @Test
   void naoDesceEmSubpastas() throws IOException {
      Path sub = Files.createDirectory(pasta.resolve("sub"));
      Files.write(sub.resolve("escondida.png"), new byte[1]);
      criar("visivel.png", 1);

      assertEquals(List.of("visivel.png"), nomes());
   }

   @Test
   void pastaVaziaDevolveListaVazia() {
      assertTrue(new ImagemRepositoryEmDisco(pasta).listar().isEmpty());
   }

   @Test
   void pastaInexistenteDevolveListaVaziaEmVezDeErro() {
      ImagemRepository repositorio = new ImagemRepositoryEmDisco(
                                             pasta.resolve("nao-existe"));

      assertTrue(repositorio.listar().isEmpty());
   }

   @Test
   void caminhoQueEhArquivoDevolveListaVazia() throws IOException {
      criar("arquivo.png", 1);

      assertTrue(new ImagemRepositoryEmDisco(pasta.resolve("arquivo.png"))
                       .listar().isEmpty());
   }

   @Test
   void pastaEhAbsolutaENormalizada() {
      Path informada = pasta.resolve("a").resolve("..");

      assertEquals(pasta.toAbsolutePath().normalize(),
                   new ImagemRepositoryEmDisco(informada).pasta());
   }
}
