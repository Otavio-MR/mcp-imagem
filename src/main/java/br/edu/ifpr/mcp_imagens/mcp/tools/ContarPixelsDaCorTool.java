package br.edu.ifpr.mcp_imagens.mcp.tools;

import java.math.BigDecimal;

import br.edu.ifpr.mcp_imagens.domain.criterio.CorComTolerancia;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContagem;
import br.edu.ifpr.mcp_imagens.infra.config.CorParser;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.mcp.utils.Parametros;
import br.edu.ifpr.mcp_imagens.service.ContadorDePixels;

/**
 * Ferramenta {@code contar_pixels_da_cor}: quantos pixels da imagem têm uma
 * cor, com uma tolerância por canal.
 */
public class ContarPixelsDaCorTool extends AbstractTool {
   /** Formato da resposta de sucesso. */
   public record Resultado(String arquivo, String cor, int tolerancia,
                           boolean ignorarTransparentes, long quantidade,
                           long totalConsiderado, BigDecimal percentual) {}

   private final ImagemRepository repositorio;
   private final CorParser        parser;
   private final ContadorDePixels contador;

   public ContarPixelsDaCorTool(ImagemRepository repositorio, CorParser parser,
                                ContadorDePixels contador) {
      this.repositorio = repositorio;
      this.parser      = parser;
      this.contador    = contador;
   }

   @Override
   protected String nome() {
      return "contar_pixels_da_cor";
   }

   @Override
   protected String titulo() {
      return "Contar pixels de uma cor";
   }

   @Override
   protected String descricao() {
      return "Conta exatamente quantos pixels da imagem têm uma cor (e o " +
             "percentual sobre o total considerado), com uma tolerância " +
             "opcional por canal de cor (vermelho, verde e azul). Por " +
             "padrão ignora os pixels totalmente transparentes. A cor pode " +
             "ser #RRGGBB, #RGB ou um nome como vermelho. Use quando for " +
             "preciso um número exato, e não uma estimativa visual. " +
             TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
            .texto(Parametros.ARQUIVO, TextosDasFerramentas.PARAMETRO_ARQUIVO)
            .texto("cor", "Cor procurada: #RRGGBB, #RGB ou nome (ex.: " +
                          "#FF0000, vermelho)")
            .inteiroOpcional(Parametros.TOLERANCIA,
                             "Diferença máxima permitida por " +
                             "canal de cor (0 = só a cor exata)",
                             CorComTolerancia.TOLERANCIA_MINIMA,
                             CorComTolerancia.TOLERANCIA_MAXIMA,
                             CorComTolerancia.TOLERANCIA_PADRAO)
            .booleanoOpcional(Parametros.IGNORAR_TRANSPARENTES,
                              TextosDasFerramentas
                                    .PARAMETRO_IGNORAR_TRANSPARENTES, true);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      // primeiro os parâmetros, depois o arquivo: erro de parâmetro não
      // precisa abrir a imagem
      String  arquivo    = argumentos.texto(Parametros.ARQUIVO);
      Cor     cor        = parser.parse(argumentos.texto("cor"), "cor");
      int     tolerancia = argumentos.inteiroOpcional(
                                 Parametros.TOLERANCIA,
                                 CorComTolerancia.TOLERANCIA_PADRAO,
                                 CorComTolerancia.TOLERANCIA_MINIMA,
                                 CorComTolerancia.TOLERANCIA_MAXIMA);
      boolean ignorar    = argumentos.booleanoOpcional(
                      Parametros.IGNORAR_TRANSPARENTES, true);
      Imagem  imagem     = repositorio.abrir(arquivo).imagem();

      ResultadoContagem contagem = contador.contar(
                  imagem, new CorComTolerancia(cor, tolerancia), ignorar);

      return new Resultado(arquivo, cor.hex(), tolerancia, ignorar,
                           contagem.quantidade(), contagem.totalConsiderado(),
                           contagem.percentual());
   }
}
