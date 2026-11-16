package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoTransparencia;

/**
 * Classifica os pixels de uma imagem pela transparência.
 */
public interface AnalisadorDeTransparencia {
   /**
    * Conta os pixels transparentes (alfa 0), semitransparentes (1 a 254) e
    * opacos (255). Uma imagem sem canal alfa tem todos os pixels opacos.
    */
   ResultadoTransparencia analisar(Imagem imagem);
}
