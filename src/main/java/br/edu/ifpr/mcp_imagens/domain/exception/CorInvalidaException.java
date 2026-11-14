package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * Uma cor não pôde ser interpretada (formato errado, nome desconhecido ou
 * componente fora de 0 a 255).
 */
public class CorInvalidaException extends ImagensException {
   private static final long serialVersionUID = 1L;

   public CorInvalidaException(String mensagem) {
      super(CodigoErro.COR_INVALIDA, mensagem);
   }
}
