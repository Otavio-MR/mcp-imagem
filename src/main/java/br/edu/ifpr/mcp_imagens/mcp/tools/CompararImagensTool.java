package br.edu.ifpr.mcp_imagens.mcp.tools;

import java.math.BigDecimal;

import br.edu.ifpr.mcp_imagens.domain.model.Arredondamento;
import br.edu.ifpr.mcp_imagens.domain.model.CaixaDelimitadora;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoComparacao;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.service.ComparadorDeImagens;

/**
 * Ferramenta {@code comparar_imagens}: compara duas imagens do mesmo tamanho
 * pixel a pixel.
 */
public class CompararImagensTool extends AbstractTool {
   /** Quantidade e percentual de um tipo de pixel. */
   public record Parte(long quantidade, BigDecimal percentual) {}

   /** Formato da resposta de sucesso. */
   public record Resultado(String arquivoA, String arquivoB, int largura,
                           int altura, long totalPixels, int tolerancia,
                           boolean identicas, Parte pixelsIguais,
                           Parte pixelsDiferentes,
                           CaixaDelimitadora regiaoDiferente) {}

   private final ImagemRepository   repositorio;
   private final ComparadorDeImagens comparador;

   public CompararImagensTool(ImagemRepository repositorio,
                              ComparadorDeImagens comparador) {
      this.repositorio = repositorio;
      this.comparador  = comparador;
   }

   @Override
   protected String nome() {
      return "comparar_imagens";
   }

   @Override
   protected String titulo() {
      return "Comparar duas imagens";
   }

   @Override
   protected String descricao() {
      return "Compara duas imagens do mesmo tamanho pixel a pixel e diz " +
             "quantos pixels são iguais e quantos são diferentes, com o " +
             "percentual, e o menor retângulo (regiaoDiferente) que contém " +
             "todas as diferenças. Dois pixels são diferentes quando algum " +
             "dos canais (alfa, vermelho, verde ou azul) difere em mais que " +
             "a tolerância. Se os tamanhos forem diferentes, devolve o erro " +
             "DIMENSOES_DIFERENTES. " + TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
            .texto("arquivo_a", "Primeira imagem: " +
                                TextosDasFerramentas.PARAMETRO_ARQUIVO)
            .texto("arquivo_b", "Segunda imagem: " +
                                TextosDasFerramentas.PARAMETRO_ARQUIVO)
            .inteiroOpcional("tolerancia", "Diferença máxima aceita em cada " +
                             "canal (0 = só pixels idênticos)",
                             ComparadorDeImagens.TOLERANCIA_MINIMA,
                             ComparadorDeImagens.TOLERANCIA_MAXIMA,
                             ComparadorDeImagens.TOLERANCIA_PADRAO);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      // primeiro os parâmetros, depois os arquivos
      String arquivoA   = argumentos.texto("arquivo_a");
      String arquivoB   = argumentos.texto("arquivo_b");
      int    tolerancia = argumentos.inteiroOpcional(
                                "tolerancia",
                                ComparadorDeImagens.TOLERANCIA_PADRAO,
                                ComparadorDeImagens.TOLERANCIA_MINIMA,
                                ComparadorDeImagens.TOLERANCIA_MAXIMA);
      Imagem a = repositorio.abrir(arquivoA).imagem();
      Imagem b = repositorio.abrir(arquivoB).imagem();

      ResultadoComparacao comparacao = comparador.comparar(a, b, tolerancia);
      long                total      = comparacao.totalPixels();

      return new Resultado(arquivoA, arquivoB, a.largura(), a.altura(), total,
                           tolerancia, comparacao.pixelsDiferentes() == 0,
                           parte(comparacao.pixelsIguais(), total),
                           parte(comparacao.pixelsDiferentes(), total),
                           comparacao.regiaoDiferente());
   }

   private static Parte parte(long quantidade, long total) {
      return new Parte(quantidade, Arredondamento.percentual(quantidade,
                                                             total));
   }
}
