package br.edu.ifpr.mcp_imagens.mcp.tools;

import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.Parcela;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoTransparencia;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeTransparencia;

/**
 * Ferramenta {@code contar_pixels_transparentes}: quantos pixels são
 * transparentes, semitransparentes e opacos.
 */
public class ContarPixelsTransparentesTool extends AbstractTool {
   /** Formato da resposta de sucesso. */
   public record Resultado(String arquivo, boolean temCanalAlfa,
                           long totalPixels, Parcela transparentes,
                           Parcela semitransparentes, Parcela opacos) {}

   private final ImagemRepository          repositorio;
   private final AnalisadorDeTransparencia analisador;

   public ContarPixelsTransparentesTool(ImagemRepository repositorio,
                                        AnalisadorDeTransparencia analisador) {
      this.repositorio = repositorio;
      this.analisador  = analisador;
   }

   @Override
   protected String nome() {
      return "contar_pixels_transparentes";
   }

   @Override
   protected String titulo() {
      return "Contar pixels transparentes";
   }

   @Override
   protected String descricao() {
      return "Conta quantos pixels da imagem são totalmente transparentes " +
             "(alfa 0), semitransparentes (alfa de 1 a 254) e opacos (alfa " +
             "255), com o percentual de cada tipo sobre todos os pixels. " +
             "Uma imagem sem canal alfa (como JPG e BMP) tem todos os " +
             "pixels opacos. " + TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
            .texto("arquivo", TextosDasFerramentas.PARAMETRO_ARQUIVO);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      String  arquivo = argumentos.texto("arquivo");
      Imagem  imagem  = repositorio.abrir(arquivo).imagem();

      ResultadoTransparencia contagem = analisador.analisar(imagem);
      long                   total    = contagem.total();

      return new Resultado(arquivo, imagem.temCanalAlfa(), total,
                           Parcela.de(contagem.transparentes(), total),
                           Parcela.de(contagem.semitransparentes(), total),
                           Parcela.de(contagem.opacos(), total));
   }
}
