package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * Duas imagens que deveriam ter o mesmo tamanho têm dimensões diferentes.
 */
public class DimensoesDiferentesException extends ImagensException {
   private static final long serialVersionUID = 1L;

   public DimensoesDiferentesException(String mensagem) {
      super(CodigoErro.DIMENSOES_DIFERENTES, mensagem);
   }
}
