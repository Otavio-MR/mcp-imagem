package br.edu.ifpr.mcp_imagens.infra.loader;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;

/**
 * Valida o nome de um arquivo recebido de uma ferramenta e o resolve dentro
 * da pasta de imagens (proteção contra <i>path traversal</i>).
 * <p>
 * As ferramentas aceitam só o <b>nome</b> do arquivo. São rejeitados
 * caminhos como {@code ../segredo.png}, {@code C:\x.png}, {@code /etc/passwd}
 * e {@code pasta/imagem.png}, além de nomes vazios, {@code .} e {@code ..}.
 */
public class NomeDeArquivoSeguro {
   private final Path pasta;

   /** @param pasta pasta de imagens, dentro da qual todo nome é resolvido */
   public NomeDeArquivoSeguro(Path pasta) {
      this.pasta = pasta.toAbsolutePath().normalize();
   }

   /**
    * Caminho completo do arquivo, sempre diretamente dentro da pasta.
    *
    * @throws ParametroInvalidoException se o nome estiver vazio ou tiver
    *                                    caminho, separador ou caractere
    *                                    proibido
    */
   public Path resolver(String nome) {
      if (nome == null || nome.isBlank())
         throw new ParametroInvalidoException("o nome do arquivo não pode " +
                                              "ser vazio");
      String limpo = nome.strip();

      if (!ehSoUmNome(limpo))
         throw recusa(nome);

      try {
         Path caminho = pasta.resolve(limpo).normalize();

         // segunda barreira: o resultado precisa estar direto na pasta
         if (!pasta.equals(caminho.getParent()))
            throw recusa(nome);

         return caminho;
      }
      catch (InvalidPathException ex) {
         throw recusa(nome);
      }
   }

   // sem separadores de pasta, sem ":" (letra de unidade e fluxos alternativos
   // do NTFS), sem caractere nulo e diferente de "." e ".."
   private static boolean ehSoUmNome(String nome) {
      return !nome.equals(".") && !nome.equals("..") &&
             nome.indexOf('/') < 0 && nome.indexOf('\\') < 0 &&
             nome.indexOf(':') < 0 && nome.indexOf('\0') < 0;
   }

   private static ParametroInvalidoException recusa(String nome) {
      return new ParametroInvalidoException(
                   "informe apenas o nome do arquivo, sem pastas ou " +
                   "caminhos (recebido: '" + nome + "')");
   }
}
