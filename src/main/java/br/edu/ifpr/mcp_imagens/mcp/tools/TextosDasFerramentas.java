package br.edu.ifpr.mcp_imagens.mcp.tools;

/**
 * Textos que se repetem nas descrições das ferramentas que recebem um arquivo
 * de imagem, para que todas digam a mesma coisa.
 */
public final class TextosDasFerramentas {
   /** Descrição do parâmetro {@code arquivo}. */
   public static final String PARAMETRO_ARQUIVO =
         "Nome do arquivo da imagem, como mostrado por listar_imagens (só o " +
         "nome, sem caminho)";

   /** Aviso, no fim da descrição da ferramenta, sobre onde a imagem fica. */
   public static final String ARQUIVO_NA_PASTA =
         "O arquivo precisa estar na pasta de imagens do servidor (veja " +
         "listar_imagens): imagens anexadas à conversa NÃO ficam " +
         "disponíveis ao servidor.";

   private TextosDasFerramentas() {}
}
