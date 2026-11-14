package br.edu.ifpr.mcp_imagens.infra.loader;

import java.nio.file.Path;

import br.edu.ifpr.mcp_imagens.domain.model.Imagem;

/**
 * Lê um arquivo de imagem e devolve seus pixels.
 */
public interface ImageLoader {
   /**
    * @param arquivo caminho de um arquivo que existe
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException
    *         se o arquivo estiver corrompido ou não for uma imagem
    * @throws br.edu.ifpr.mcp_imagens.domain.exception.ImagemMuitoGrandeException
    *         se o arquivo ou a imagem passar dos limites de tamanho
    */
   Imagem carregar(Path arquivo);
}
