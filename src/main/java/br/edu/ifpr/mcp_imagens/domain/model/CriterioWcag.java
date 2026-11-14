package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Um critério de contraste do WCAG: a razão mínima exigida para um tipo de
 * conteúdo.
 *
 * @param id         identificador curto (p.ex. "aa_texto_normal")
 * @param nome       nome legível
 * @param limiar     razão de contraste mínima, de 1 a 21
 * @param descricao  a que tipo de conteúdo o critério se aplica
 */
public record CriterioWcag(String id, String nome, double limiar,
                           String descricao) {
}
