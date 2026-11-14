package br.edu.ifpr.mcp_imagens.domain.exception;

/**
 * Códigos de erro padronizados devolvidos pelas ferramentas do servidor.
 * <p>
 * O texto de um resultado de erro sempre tem o formato
 * {@code CODIGO: mensagem}, para que a IA (ou o cliente) identifique a causa
 * sem precisar interpretar a mensagem.
 */
public enum CodigoErro {
   PARAMETRO_AUSENTE,
   PARAMETRO_INVALIDO,
   IMAGEM_NAO_ENCONTRADA,
   FORMATO_NAO_SUPORTADO,
   IMAGEM_INVALIDA,
   IMAGEM_MUITO_GRANDE,
   DIMENSOES_DIFERENTES,
   COR_INVALIDA,
   ERRO_INTERNO
}
