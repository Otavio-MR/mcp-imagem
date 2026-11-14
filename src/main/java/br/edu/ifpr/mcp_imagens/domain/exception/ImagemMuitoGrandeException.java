package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * O arquivo ou a imagem passa dos limites de tamanho do servidor.
 */
public class ImagemMuitoGrandeException extends ImagensException {
   private static final long serialVersionUID = 1L;

   public ImagemMuitoGrandeException(String mensagem) {
      super(CodigoErro.IMAGEM_MUITO_GRANDE, mensagem);
   }
}
