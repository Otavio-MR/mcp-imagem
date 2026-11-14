package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.criterio.CriterioDePixel;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContagem;

/**
 * Conta os pixels de uma imagem que atendem a um critério.
 */
public interface ContadorDePixels {
   /**
    * @param imagem               imagem a percorrer
    * @param criterio             decide quais pixels contam
    * @param ignorarTransparentes se {@code true}, os pixels totalmente
    *                             transparentes (alfa 0) não entram nem na
    *                             contagem nem no total considerado
    */
   ResultadoContagem contar(Imagem imagem, CriterioDePixel criterio,
                            boolean ignorarTransparentes);
}
