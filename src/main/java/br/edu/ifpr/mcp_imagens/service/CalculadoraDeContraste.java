package br.edu.ifpr.mcp_imagens.service;

import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContraste;

/**
 * Cálculo do contraste entre duas cores, segundo o WCAG 2.x.
 */
public interface CalculadoraDeContraste {
   /**
    * Razão de contraste entre o texto e o fundo, com o veredito para cada
    * critério do WCAG. A razão é simétrica: trocar texto e fundo dá o mesmo
    * valor.
    */
   ResultadoContraste calcular(Cor texto, Cor fundo);
}
