package br.edu.ifpr.mcp_imagens.infra.config;

import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.CriterioWcag;

/**
 * Formato do arquivo {@code imagens/criterios-wcag.json}.
 *
 * @param fonte      critérios do WCAG em que a tabela se baseia
 * @param criterios  os critérios de contraste, com seus limiares
 */
public record TabelaWcag(String fonte, List<CriterioWcag> criterios) {
}
