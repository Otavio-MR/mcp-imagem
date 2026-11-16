package br.edu.ifpr.mcp_imagens.mcp.utils;

/**
 * Nomes dos parâmetros que se repetem em várias ferramentas e prompts. Estar
 * em um só lugar evita que o nome no schema e o nome lido em
 * {@code executar} fiquem diferentes por um erro de digitação.
 */
public final class Parametros {
   /** Nome do arquivo da imagem, na pasta de imagens do servidor. */
   public static final String ARQUIVO = "arquivo";

   /** Se os pixels totalmente transparentes ficam fora da análise. */
   public static final String IGNORAR_TRANSPARENTES = "ignorar_transparentes";

   /** Diferença máxima aceita por canal de cor. */
   public static final String TOLERANCIA = "tolerancia";

   /** Quantas cores devolver. */
   public static final String QUANTIDADE = "quantidade";

   /** Tamanho da faixa de cada canal usada para agrupar cores parecidas. */
   public static final String PASSO_DE_AGRUPAMENTO = "passo_de_agrupamento";

   private Parametros() {}
}
