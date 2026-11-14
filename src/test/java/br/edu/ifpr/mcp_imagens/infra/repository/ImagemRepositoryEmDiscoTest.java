package br.edu.ifpr.mcp_imagens.infra.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.FormatoNaoSuportadoException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemNaoEncontradaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;
import br.edu.ifpr.mcp_imagens.domain.model.FormatoImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemAberta;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;
import br.edu.ifpr.mcp_imagens.infra.loader.ImageIoLoader;
import br.edu.ifpr.mcp_imagens.testutil.ImagensDeTeste;

class ImagemRepositoryEmDiscoTest {
   @TempDir
   Path pasta;

   private ImagemRepository repositorio() {
      return new ImagemRepositoryEmDisco(pasta, new ImageIoLoader(
            20L * 1024 * 1024, 25_000_000L));
   }

   private void criar(String nome, int bytes) throws IOException {
      Files.write(pasta.resolve(nome), new byte[bytes]);
   }

   private List<String> nomes() {
      return repositorio().listar().stream().map(ImagemInfo::nome).toList();
   }

   @Nested
   class Listar {
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
                      repositorio().listar().stream()
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
         return repositorio().listar().stream()
                             .filter(i -> i.nome().equals(nome))
                             .findFirst().orElseThrow().tamanhoKb();
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
         assertTrue(repositorio().listar().isEmpty());
      }

      @Test
      void pastaInexistenteDevolveListaVaziaEmVezDeErro() {
         ImagemRepository inexistente = new ImagemRepositoryEmDisco(
               pasta.resolve("nao-existe"), new ImageIoLoader(1, 1));

         assertTrue(inexistente.listar().isEmpty());
      }

      @Test
      void caminhoQueEhArquivoDevolveListaVazia() throws IOException {
         criar("arquivo.png", 1);

         ImagemRepository repositorio = new ImagemRepositoryEmDisco(
               pasta.resolve("arquivo.png"), new ImageIoLoader(1, 1));

         assertTrue(repositorio.listar().isEmpty());
      }

      @Test
      void pastaEhAbsolutaENormalizada() {
         ImagemRepository repositorio = new ImagemRepositoryEmDisco(
               pasta.resolve("a").resolve(".."), new ImageIoLoader(1, 1));

         assertEquals(pasta.toAbsolutePath().normalize(), repositorio.pasta());
      }
   }

   @Nested
   class Abrir {
      @Test
      void abreUmaImagemComNomeFormatoTamanhoEPixels() {
         Path arquivo = ImagensDeTeste.criar(pasta, "banner.png", 20, 10,
                                             0xFF202020);

         ImagemAberta aberta = repositorio().abrir("banner.png");

         assertEquals("banner.png", aberta.nome());
         assertEquals(FormatoImagem.PNG, aberta.formato());
         assertEquals(arquivo.toFile().length(), aberta.tamanhoBytes());
         assertEquals(20, aberta.imagem().largura());
         assertEquals(10, aberta.imagem().altura());
         assertEquals(0xFF202020, aberta.imagem().argb(5, 5));
      }

      @Test
      void abreCadaFormato() {
         ImagensDeTeste.criar(pasta, "a.png", 2, 2, 0xFFFF0000);
         ImagensDeTeste.criar(pasta, "b.jpg", 2, 2, 0xFFFF0000);
         ImagensDeTeste.criar(pasta, "c.gif", 2, 2, 0xFFFF0000);
         ImagensDeTeste.criar(pasta, "d.bmp", 2, 2, 0xFFFF0000);

         assertEquals(FormatoImagem.PNG, repositorio().abrir("a.png").formato());
         assertEquals(FormatoImagem.JPEG, repositorio().abrir("b.jpg").formato());
         assertEquals(FormatoImagem.GIF, repositorio().abrir("c.gif").formato());
         assertEquals(FormatoImagem.BMP, repositorio().abrir("d.bmp").formato());
      }

      @Test
      void extensaoEmMaiusculasFunciona() {
         ImagensDeTeste.criar(pasta, "Foto.PNG", 2, 2, 0xFFFF0000);

         assertEquals(FormatoImagem.PNG, repositorio().abrir("Foto.PNG")
                                                      .formato());
      }

      @Test
      void removeEspacosDasPontasDoNome() {
         ImagensDeTeste.criar(pasta, "banner.png", 2, 2, 0xFFFF0000);

         assertEquals("banner.png", repositorio().abrir("  banner.png ").nome());
      }

      @Test
      void imagemInvalidaPropagaOErro() throws IOException {
         Files.writeString(pasta.resolve("falsa.png"), "não sou imagem");

         assertThrows(ImagemInvalidaException.class,
                      () -> repositorio().abrir("falsa.png"));
      }

      @Test
      void imagemMuitoGrandePropagaOErro() {
         ImagensDeTeste.criar(pasta, "grande.png", 20, 20, 0xFFFF0000);
         ImagemRepository pequeno = new ImagemRepositoryEmDisco(pasta,
               new ImageIoLoader(1_000_000, 100));

         assertThrows(ImagemMuitoGrandeException.class,
                      () -> pequeno.abrir("grande.png"));
      }
   }

   @Nested
   class NaoEncontrada {
      private String mensagemPara(String nome) {
         return assertThrows(ImagemNaoEncontradaException.class,
                             () -> repositorio().abrir(nome)).getMessage();
      }

      @Test
      void mensagemDidaticaComPastaEImagensDisponiveis() {
         ImagensDeTeste.criar(pasta, "icone.png", 2, 2, 0xFFFF0000);
         ImagensDeTeste.criar(pasta, "banner.png", 2, 2, 0xFFFF0000);

         assertEquals("'foto.png' não está na pasta '" + pasta.toAbsolutePath()
                      .normalize() + "'. Imagens anexadas à conversa não são " +
                      "visíveis ao servidor: copie o arquivo para essa " +
                      "pasta. Disponíveis: banner.png, icone.png",
                      mensagemPara("foto.png"));
      }

      @Test
      void pastaVaziaDizNenhuma() {
         assertTrue(mensagemPara("foto.png").endsWith("Disponíveis: nenhuma"),
                    mensagemPara("foto.png"));
      }

      @Test
      void pastaInexistenteTambemDizNenhuma() {
         ImagemRepository repositorio = new ImagemRepositoryEmDisco(
               pasta.resolve("nao-existe"), new ImageIoLoader(1, 1));

         String mensagem = assertThrows(ImagemNaoEncontradaException.class,
               () -> repositorio.abrir("foto.png")).getMessage();

         assertTrue(mensagem.endsWith("Disponíveis: nenhuma"), mensagem);
      }

      @Test
      void citaNoMaximoVinteImagens() {
         for (int i = 1; i <= 25; i++)
            ImagensDeTeste.criar(pasta, String.format("img%02d.png", i), 1, 1,
                                 0xFFFF0000);

         String mensagem = mensagemPara("outra.png");

         assertTrue(mensagem.contains("img20.png e mais 5"), mensagem);
         assertFalse(mensagem.contains("img21.png"), mensagem);
      }

      @Test
      void comExatamenteVinteImagensNaoHaEMais() {
         for (int i = 1; i <= 20; i++)
            ImagensDeTeste.criar(pasta, String.format("img%02d.png", i), 1, 1,
                                 0xFFFF0000);

         String mensagem = mensagemPara("outra.png");

         assertTrue(mensagem.endsWith("img20.png"), mensagem);
         assertFalse(mensagem.contains("e mais"), mensagem);
      }

      @Test
      void naoListaArquivosQueNaoSaoImagens() throws IOException {
         criar("notas.txt", 1);
         ImagensDeTeste.criar(pasta, "a.png", 1, 1, 0xFFFF0000);

         assertTrue(mensagemPara("foto.png").endsWith("Disponíveis: a.png"));
      }

      @Test
      void pastaComNomeDeImagemNaoEUmaImagem() throws IOException {
         Files.createDirectory(pasta.resolve("pasta.png"));

         assertThrows(ImagemNaoEncontradaException.class,
                      () -> repositorio().abrir("pasta.png"));
      }

      @Test
      void aMensagemCitaONomeSemEspacosNasPontas() {
         assertTrue(mensagemPara("  foto.png ").startsWith("'foto.png' "));
      }
   }

   @Nested
   class FormatoNaoSuportado {
      @Test
      void arquivoExistenteComExtensaoNaoAceita() throws IOException {
         criar("notas.txt", 5);

         FormatoNaoSuportadoException ex = assertThrows(
               FormatoNaoSuportadoException.class,
               () -> repositorio().abrir("notas.txt"));

         assertTrue(ex.getMessage().contains("'notas.txt'"), ex.getMessage());
         assertTrue(ex.getMessage().contains("PNG, JPG/JPEG, GIF ou BMP"));
      }

      @Test
      void arquivoSemExtensao() throws IOException {
         criar("semextensao", 5);

         assertThrows(FormatoNaoSuportadoException.class,
                      () -> repositorio().abrir("semextensao"));
      }

      @Test
      void arquivoInexistenteComExtensaoNaoAceitaEhNaoEncontrado() {
         // a existência é conferida antes: a mensagem ajuda mais
         assertThrows(ImagemNaoEncontradaException.class,
                      () -> repositorio().abrir("foto.webp"));
      }
   }

   @Nested
   class Seguranca {
      @ParameterizedTest
      @ValueSource(strings = { "../segredo.png", "..\\segredo.png",
                               "C:\\x.png", "/etc/passwd", "sub/a.png",
                               ".." })
      void caminhosSaoRecusadosEOArquivoDeForaNuncaEhLido(String nome)
            throws IOException {
         // "segredo.png" existe, mas na pasta de cima da pasta de imagens
         ImagensDeTeste.criar(pasta, "segredo.png", 2, 2, 0xFFFF0000);
         Path imagens = Files.createDirectory(pasta.resolve("imagens"));
         ImagemRepository repositorio = new ImagemRepositoryEmDisco(imagens,
               new ImageIoLoader(1_000_000, 1_000_000));

         assertThrows(ParametroInvalidoException.class,
                      () -> repositorio.abrir(nome));
      }

      @Test
      void nomeVazioEhParametroInvalido() {
         assertThrows(ParametroInvalidoException.class,
                      () -> repositorio().abrir("  "));
         assertThrows(ParametroInvalidoException.class,
                      () -> repositorio().abrir(null));
      }
   }
}
