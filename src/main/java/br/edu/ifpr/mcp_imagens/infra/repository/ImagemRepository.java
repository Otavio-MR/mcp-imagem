package br.edu.ifpr.mcp_imagens.infra.repository;

import java.nio.file.Path;
import java.util.List;

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
}
