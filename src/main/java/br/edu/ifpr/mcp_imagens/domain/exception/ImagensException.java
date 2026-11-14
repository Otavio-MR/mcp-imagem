package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * Base das falhas previstas do servidor (erros do usuário ou do arquivo, não
 * defeitos do programa).
 * <p>
 * Cada subclasse tem um {@link CodigoErro}. A classe {@code AbstractTool}
 * converte qualquer {@code ImagensException} em um resultado de erro
 * {@code CODIGO: mensagem}.
 */
public abstract class ImagensException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   private final CodigoErro codigo;

   protected ImagensException(CodigoErro codigo, String mensagem) {
      super(mensagem);
      this.codigo = codigo;
   }

   public CodigoErro codigo() {
      return codigo;
   }
}
