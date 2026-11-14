package br.edu.ifpr.mcp_imagens.mcp.utils;

import br.edu.ifpr.mcp_imagens.domain.exception.CodigoErro;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fábrica dos resultados ({@link CallToolResult}) devolvidos pelas
 * ferramentas, garantindo o mesmo formato em todo o servidor:
 * <ul>
 * <li>sucesso: um único texto com JSON legível (indentado);</li>
 * <li>erro: {@code isError = true} e texto {@code CODIGO: mensagem}.</li>
 * </ul>
 */
public final class ToolResults {
   // mesmo Jackson usado pelo SDK; mantém a ordem dos campos dos records
   // (o Jackson 3 ordena alfabeticamente por padrão)
   private static final JsonMapper JSON =
               JsonMapper.builder()
                         .enable(SerializationFeature.INDENT_OUTPUT)
                         .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                         .build();

   private ToolResults() {}

   /**
    * {@code dados} em JSON legível (indentado). Também usado pelos resources,
    * para que tenham o mesmo formato das ferramentas.
    */
   public static String comoJson(Object dados) {
      return JSON.writeValueAsString(dados);
   }

   /** Resultado de sucesso com {@code dados} convertidos em JSON. */
   public static CallToolResult sucesso(Object dados) {
      return CallToolResult.builder()
                           .addTextContent(comoJson(dados))
                           .isError(false)
                           .build();
   }

   /** Resultado de erro no formato {@code CODIGO: mensagem}. */
   public static CallToolResult erro(CodigoErro codigo, String mensagem) {
      return CallToolResult.builder()
                           .addTextContent(codigo.name() + ": " + mensagem)
                           .isError(true)
                           .build();
   }
}
