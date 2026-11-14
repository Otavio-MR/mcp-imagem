package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * Um parâmetro foi informado com tipo errado, fora dos limites ou com valor
 * que não pode ser usado (por exemplo, um nome de arquivo com caminho).
 */
public class ParametroInvalidoException extends ImagensException {
   private static final long serialVersionUID = 1L;

   public ParametroInvalidoException(String mensagem) {
      super(CodigoErro.PARAMETRO_INVALIDO, mensagem);
   }
}
