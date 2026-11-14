package br.edu.ifpr.mcp_imagens.mcp.tools;

import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.service.InformacoesService;

/**
 * Ferramenta {@code listar_imagens}: mostra a pasta de imagens configurada
 * ({@code IMG_DIR}) e as imagens que existem nela.
 */
public class ListarImagensTool extends AbstractTool {
   private final InformacoesService informacoes;

   public ListarImagensTool(InformacoesService informacoes) {
      this.informacoes = informacoes;
   }

   @Override
   protected String nome() {
      return "listar_imagens";
   }

   @Override
   protected String titulo() {
      return "Listar imagens";
   }

   @Override
   protected String descricao() {
      return "Lista as imagens (PNG, JPG/JPEG, GIF e BMP) disponíveis na " +
             "pasta de imagens do servidor, com nome, formato e tamanho em " +
             "KB, e informa o caminho da pasta. Use esta ferramenta " +
             "primeiro, para descobrir o nome exato do arquivo pedido pelas " +
             "demais. IMPORTANTE: imagens anexadas à conversa NÃO ficam " +
             "disponíveis ao servidor; só as que estão nessa pasta. Para " +
             "analisar uma imagem, ela precisa ser copiada para a pasta.";
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo();
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      return informacoes.listarImagens();
   }
}
