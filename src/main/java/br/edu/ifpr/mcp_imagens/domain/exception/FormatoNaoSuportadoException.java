package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * O arquivo não está em um formato aceito (PNG, JPG/JPEG, GIF ou BMP).
 */
public class FormatoNaoSuportadoException extends ImagensException {
   private static final long serialVersionUID = 1L;

   public FormatoNaoSuportadoException(String mensagem) {
      super(CodigoErro.FORMATO_NAO_SUPORTADO, mensagem);
   }
}
