package br.edu.ifpr.mcp_imagens.domain.model;

/**
 * Se uma razão de contraste atende a um critério do WCAG.
 *
 * @param criterio  identificador do critério (p.ex. "aa_texto_normal")
 * @param nome      nome legível do critério
 * @param limiar    razão mínima exigida
 * @param passa     {@code true} se a razão é maior ou igual ao limiar
 */
public record Veredito(String criterio, String nome, double limiar,
                       boolean passa) {
}
