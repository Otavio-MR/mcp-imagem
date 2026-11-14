package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.model.InformacoesImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ListaDeImagens;

/**
 * Informações sobre as imagens disponíveis.
 */
public interface InformacoesService {
   /**
    * As imagens da pasta configurada. Pasta inexistente ou vazia não é erro:
    * a lista vem vazia.
    */
   ListaDeImagens listarImagens();

   /**
    * Largura, altura, formato, canal alfa e tamanho de uma imagem.
    *
    * @param arquivo nome do arquivo (nunca um caminho)
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagemNaoEncontradaException
    *         se o arquivo não existir na pasta
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.FormatoNaoSuportadoException
    *         se a extensão não for aceita
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException
    *         se o arquivo estiver corrompido
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException
    *         se o arquivo ou a imagem passar dos limites
    */
   InformacoesImagem obter(String arquivo);
}
