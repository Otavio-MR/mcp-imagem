package br.edu.ifpr.mcp_imagens.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contraste entre duas cores segundo o WCAG 2.x.
 *
 * @param corTexto   cor do texto, em "#RRGGBB"
 * @param corFundo   cor do fundo, em "#RRGGBB"
 * @param razao      razão de contraste, de 1,00 a 21,00 (2 casas)
 * @param vereditos  o resultado para cada critério do WCAG
 */
public record ResultadoContraste(String corTexto, String corFundo,
                                 BigDecimal razao, List<Veredito> vereditos) {
}
