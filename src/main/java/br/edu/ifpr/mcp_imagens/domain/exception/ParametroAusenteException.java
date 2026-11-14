package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * Um parâmetro obrigatório não foi informado.
 */
public class ParametroAusenteException extends ImagensException {
   private static final long serialVersionUID = 1L;

   public ParametroAusenteException(String mensagem) {
      super(CodigoErro.PARAMETRO_AUSENTE, mensagem);
   }
}
