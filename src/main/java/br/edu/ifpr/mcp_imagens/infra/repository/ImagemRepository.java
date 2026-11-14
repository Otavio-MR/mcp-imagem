package br.edu.ifpr.mcp_imagens.infra.repository;

import java.nio.file.Path;
import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.ImagemAberta;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;

/**
 * Acesso às imagens guardadas na pasta de imagens configurada.
 */
public interface ImagemRepository {
   /** Pasta onde as imagens são procuradas, como caminho absoluto. */
   Path pasta();

   /**
    * Imagens suportadas na pasta, em ordem alfabética. Pasta inexistente ou
    * vazia resulta em lista vazia.
    */
   List<ImagemInfo> listar();

   /**
    * Lê uma imagem da pasta.
    *
    * @param nome nome do arquivo (nunca um caminho)
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException
    *         se o nome tiver caminho
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagemNaoEncontradaException
    *         se o arquivo não existir na pasta
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.FormatoNaoSuportadoException
    *         se a extensão não for PNG, JPG/JPEG, GIF ou BMP
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException
    *         se o arquivo estiver corrompido
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException
    *         se o arquivo ou a imagem passar dos limites
    */
   ImagemAberta abrir(String nome);
}
