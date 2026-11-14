package br.edu.ifpr.mcp_imagens.service;

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
}
