package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * O arquivo está corrompido ou não pôde ser decodificado como imagem.
 */
public class ImagemInvalidaException extends ImagensException {
   private static final long serialVersionUID = 1L;

   public ImagemInvalidaException(String mensagem) {
      super(CodigoErro.IMAGEM_INVALIDA, mensagem);
   }
}
