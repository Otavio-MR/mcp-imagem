package br.edu.ifpr.mcp_imagens.infra.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import br.edu.ifpr.mcp_imagens.domain.model.FormatoImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;

/**
 * {@link ImagemRepository} que lê as imagens de uma pasta do disco.
 */
public class ImagemRepositoryEmDisco implements ImagemRepository {
   private static final long BYTES_POR_KB = 1024;

   private final Path pasta;

   /** @param pasta pasta onde ficam as imagens (pode não existir) */
   public ImagemRepositoryEmDisco(Path pasta) {
      this.pasta = pasta.toAbsolutePath().normalize();
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

   // vazio se o arquivo não tem uma extensão de imagem aceita
   private static Optional<ImagemInfo> paraInfo(Path arquivo) {
      String nome = arquivo.getFileName().toString();

      return FormatoImagem.doArquivo(nome)
                          .map(formato -> new ImagemInfo(nome, formato,
                                                         tamanhoEmKb(arquivo)));
   }

   // tamanho em KB, arredondado para cima
   private static long tamanhoEmKb(Path arquivo) {
      try {
         return (Files.size(arquivo) + BYTES_POR_KB - 1) / BYTES_POR_KB;
      }
      catch (IOException ex) {
         throw new UncheckedIOException("falha ao ler o tamanho de " + arquivo,
                                        ex);
      }
   }
}
