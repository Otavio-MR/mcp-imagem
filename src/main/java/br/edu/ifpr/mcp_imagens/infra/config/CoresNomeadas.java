package br.edu.ifpr.mcp_imagens.infra.config;

import java.util.Map;

/**
 * Formato do arquivo {@code imagens/cores-nomeadas.json}.
 *
 * @param observacao  explicação sobre os nomes
 * @param cores       nome da cor (sem acentos, minúsculas) para "#RRGGBB"
 */
public record CoresNomeadas(String observacao, Map<String, String> cores) {
}
