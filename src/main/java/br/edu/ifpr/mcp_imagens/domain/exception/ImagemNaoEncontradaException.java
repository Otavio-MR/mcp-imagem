package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * A imagem pedida não existe na pasta de imagens.
 */
public class ImagemNaoEncontradaException extends ImagensException {
   private static final long serialVersionUID = 1L;

   public ImagemNaoEncontradaException(String mensagem) {
      super(CodigoErro.IMAGEM_NAO_ENCONTRADA, mensagem);
   }
}
