package br.edu.ifpr.mcp_imagens.infra.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import br.edu.ifpr.mcp_imagens.domain.exception.FormatoNaoSuportadoException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagemNaoEncontradaException;
import br.edu.ifpr.mcp_imagens.domain.model.FormatoImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemAberta;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;
import br.edu.ifpr.mcp_imagens.infra.loader.ImageLoader;
import br.edu.ifpr.mcp_imagens.infra.loader.NomeDeArquivoSeguro;

/**
 * {@link ImagemRepository} que lê as imagens de uma pasta do disco.
 */
public class ImagemRepositoryEmDisco implements ImagemRepository {
   private static final long BYTES_POR_KB = 1024;
   /** Quantas imagens a mensagem de "não encontrada" cita, no máximo. */
   static final int MAXIMO_NOMES_NA_MENSAGEM = 20;

   private final Path                pasta;
   private final NomeDeArquivoSeguro nomeSeguro;
   private final ImageLoader         carregador;

   /**
    * @param pasta       pasta onde ficam as imagens (pode não existir)
    * @param carregador  lê os pixels de um arquivo
    */
   public ImagemRepositoryEmDisco(Path pasta, ImageLoader carregador) {
      this.pasta      = pasta.toAbsolutePath().normalize();
      this.nomeSeguro = new NomeDeArquivoSeguro(this.pasta);
      this.carregador = carregador;
   }

   @Override
   public Path pasta() {
      return pasta;
   }

   @Override
   public List<ImagemInfo> listar() {
      if (!Files.isDirectory(pasta))
         return List.of();

      try (Stream<Path> arquivos = Files.list(pasta)) {
         return arquivos.filter(Files::isRegularFile)
                        .map(ImagemRepositoryEmDisco::paraInfo)
                        .flatMap(Optional::stream)
                        .sorted(Comparator.comparing(
                                      (ImagemInfo imagem) -> imagem.nome()
                                                                   .toLowerCase())
                                          .thenComparing(ImagemInfo::nome))
                        .toList();
      }
      catch (IOException ex) {
         throw new UncheckedIOException("falha ao listar a pasta " + pasta, ex);
      }
   }

   @Override
   public ImagemAberta abrir(String nome) {
      Path caminho = nomeSeguro.resolver(nome);

      if (!Files.isRegularFile(caminho))
         throw new ImagemNaoEncontradaException(naoEncontrada(nome.strip()));

      String        arquivo = caminho.getFileName().toString();
      FormatoImagem formato = FormatoImagem.doArquivo(arquivo)
            .orElseThrow(() -> new FormatoNaoSuportadoException(
                  "'" + arquivo + "' não é um formato suportado; use PNG, " +
                  "JPG/JPEG, GIF ou BMP"));

      return new ImagemAberta(arquivo, formato, tamanhoEmBytes(caminho),
                              carregador.carregar(caminho));
   }

   // texto didático: a IA consegue orientar o usuário em vez de só falhar
   private String naoEncontrada(String nome) {
      return "'" + nome + "' não está na pasta '" + pasta + "'. Imagens " +
             "anexadas à conversa não são visíveis ao servidor: copie o " +
             "arquivo para essa pasta. Disponíveis: " + disponiveis();
   }

   // até 20 nomes; se houver mais, informa quantas faltam
   private String disponiveis() {
      List<String> nomes = listar().stream().map(ImagemInfo::nome).toList();

      if (nomes.isEmpty())
         return "nenhuma";
      if (nomes.size() <= MAXIMO_NOMES_NA_MENSAGEM)
         return String.join(", ", nomes);

      return String.join(", ", nomes.subList(0, MAXIMO_NOMES_NA_MENSAGEM)) +
             " e mais " + (nomes.size() - MAXIMO_NOMES_NA_MENSAGEM);
   }

   // vazio se o arquivo não tem uma extensão de imagem aceita
   private static Optional<ImagemInfo> paraInfo(Path arquivo) {
      String nome = arquivo.getFileName().toString();

      return FormatoImagem.doArquivo(nome)
                          .map(formato -> new ImagemInfo(
                                  nome, formato,
                                  (tamanhoEmBytes(arquivo) + BYTES_POR_KB - 1) /
                                  BYTES_POR_KB));
   }

   private static long tamanhoEmBytes(Path arquivo) {
      try {
         return Files.size(arquivo);
      }
      catch (IOException ex) {
         throw new UncheckedIOException("falha ao ler o tamanho de " + arquivo,
                                        ex);
      }
   }
}
