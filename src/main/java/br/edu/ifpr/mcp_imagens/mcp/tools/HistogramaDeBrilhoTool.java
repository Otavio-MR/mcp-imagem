package br.edu.ifpr.mcp_imagens.mcp.tools;

import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.FaixaDeBrilho;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoHistograma;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.mcp.utils.Parametros;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeBrilho;

/**
 * Ferramenta {@code histograma_de_brilho}: quantos pixels há em cada faixa de
 * brilho, da mais escura à mais clara.
 */
public class HistogramaDeBrilhoTool extends AbstractTool {
   /** Formato da resposta de sucesso. */
   public record Resultado(String arquivo, int numeroDeFaixas,
                           boolean ignorarTransparentes, long totalConsiderado,
                           List<FaixaDeBrilho> faixas) {}

   private final ImagemRepository repositorio;
   private final AnalisadorDeBrilho analisador;

   public HistogramaDeBrilhoTool(ImagemRepository repositorio,
                                 AnalisadorDeBrilho analisador) {
      this.repositorio = repositorio;
      this.analisador  = analisador;
   }

   @Override
   protected String nome() {
      return "histograma_de_brilho";
   }

   @Override
   protected String titulo() {
      return "Histograma de brilho";
   }

   @Override
   protected String descricao() {
      return "Mostra como o brilho (luma, de 0 a 255) se distribui pelos " +
             "pixels da imagem: a escala é dividida em faixas iguais, da " +
             "mais escura à mais clara, e para cada faixa vêm o intervalo de " +
             "brilho, a quantidade exata de pixels e o percentual. Serve " +
             "para saber se a imagem é escura, clara ou tem pouco contraste. " +
             "Por padrão ignora os pixels totalmente transparentes. " +
             TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
            .texto(Parametros.ARQUIVO, TextosDasFerramentas.PARAMETRO_ARQUIVO)
            .inteiroOpcional("faixas", "Em quantas faixas dividir a escala " +
                             "de brilho",
                             AnalisadorDeBrilho.FAIXAS_MINIMO,
                             AnalisadorDeBrilho.FAIXAS_MAXIMO,
                             AnalisadorDeBrilho.FAIXAS_PADRAO)
            .booleanoOpcional(Parametros.IGNORAR_TRANSPARENTES,
                              TextosDasFerramentas
                                    .PARAMETRO_IGNORAR_TRANSPARENTES, true);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      // primeiro os parâmetros, depois o arquivo
      String  arquivo = argumentos.texto(Parametros.ARQUIVO);
      int     faixas  = argumentos.inteiroOpcional(
                              "faixas", AnalisadorDeBrilho.FAIXAS_PADRAO,
                              AnalisadorDeBrilho.FAIXAS_MINIMO,
                              AnalisadorDeBrilho.FAIXAS_MAXIMO);
      boolean ignorar = argumentos.booleanoOpcional(
                      Parametros.IGNORAR_TRANSPARENTES, true);
      Imagem  imagem  = repositorio.abrir(arquivo).imagem();

      ResultadoHistograma histograma = analisador.histograma(imagem, faixas,
                                                             ignorar);

      return new Resultado(arquivo, faixas, ignorar,
                           histograma.totalConsiderado(),
                           histograma.faixas());
   }
}
