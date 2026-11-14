package br.edu.ifpr.mcp_imagens.testutil;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemNaoEncontradaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ImagensException;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemAberta;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;

/**
 * Dublê de {@link ImagemRepository} para os testes das ferramentas e dos
 * serviços: guarda imagens em memória (sem disco) ou falha sempre com uma
 * exceção escolhida.
 */
public final class RepositorioFalso implements ImagemRepository {
   private final Map<String, ImagemAberta> imagens = new LinkedHashMap<>();
   private final ImagensException          falha;

   private RepositorioFalso(ImagensException falha) {
      this.falha = falha;
   }

   /** Repositório que tem as imagens informadas, pelo nome delas. */
   public static RepositorioFalso com(ImagemAberta... abertas) {
      RepositorioFalso repositorio = new RepositorioFalso(null);

      for (ImagemAberta aberta : abertas)
         repositorio.imagens.put(aberta.nome(), aberta);

      return repositorio;
   }

   /** Repositório em que abrir qualquer imagem falha com {@code falha}. */
   public static RepositorioFalso falhando(ImagensException falha) {
      return new RepositorioFalso(falha);
   }

   @Override
   public Path pasta() {
      return Path.of("pasta-falsa").toAbsolutePath();
   }

   @Override
   public List<ImagemInfo> listar() {
      return imagens.values().stream()
                    .map(a -> new ImagemInfo(a.nome(), a.formato(),
                                             (a.tamanhoBytes() + 1023) / 1024))
                    .toList();
   }

   @Override
   public ImagemAberta abrir(String nome) {
      if (falha != null)
         throw falha;

      ImagemAberta aberta = imagens.get(nome);

      if (aberta == null)
         throw new ImagemNaoEncontradaException("'" + nome + "' não está na " +
                                                "pasta falsa");
      return aberta;
   }
}
