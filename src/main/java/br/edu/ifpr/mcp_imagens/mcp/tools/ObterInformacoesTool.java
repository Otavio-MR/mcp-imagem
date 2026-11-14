package br.edu.ifpr.mcp_imagens.mcp.tools;

import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.service.InformacoesService;

/**
 * Ferramenta {@code obter_informacoes}: largura, altura, total de pixels,
 * formato, canal alfa e tamanho do arquivo de uma imagem.
 */
public class ObterInformacoesTool extends AbstractTool {
   private final InformacoesService informacoes;

   public ObterInformacoesTool(InformacoesService informacoes) {
      this.informacoes = informacoes;
   }

   @Override
   protected String nome() {
      return "obter_informacoes";
   }

   @Override
   protected String titulo() {
      return "Obter informações da imagem";
   }

   @Override
   protected String descricao() {
      return "Informa largura, altura, total de pixels, formato, se a imagem " +
             "tem canal alfa (transparência) e o tamanho do arquivo. " +
             "Medidas exatas, lidas do arquivo. " +
             TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
                        .texto("arquivo", TextosDasFerramentas.PARAMETRO_ARQUIVO);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      return informacoes.obter(argumentos.texto("arquivo"));
   }
}
