package br.edu.ifpr.mcp_imagens.mcp.tools;

import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.CorDominante;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoCoresDominantes;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeCores;

/**
 * Ferramenta {@code cores_dominantes}: as cores que mais aparecem na imagem,
 * agrupando cores parecidas.
 */
public class CoresDominantesTool extends AbstractTool {
   /** Formato da resposta de sucesso. */
   public record Resultado(String arquivo, int passoDeAgrupamento,
                           boolean ignorarTransparentes, long totalConsiderado,
                           int gruposDistintos, List<CorDominante> cores) {}

   private final ImagemRepository repositorio;
   private final AnalisadorDeCores analisador;

   public CoresDominantesTool(ImagemRepository repositorio,
                              AnalisadorDeCores analisador) {
      this.repositorio = repositorio;
      this.analisador  = analisador;
   }

   @Override
   protected String nome() {
      return "cores_dominantes";
   }

   @Override
   protected String titulo() {
      return "Cores dominantes da imagem";
   }

   @Override
   protected String descricao() {
      return "Lista as cores que mais aparecem na imagem, com a quantidade " +
             "exata de pixels e o percentual de cada uma. Cores parecidas são " +
             "agrupadas (parâmetro passo_de_agrupamento) e cada grupo é " +
             "mostrado pela cor central da sua faixa; com passo 1 não há " +
             "agrupamento e saem as cores exatas. Por padrão ignora os " +
             "pixels totalmente transparentes. " +
             TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
            .texto("arquivo", TextosDasFerramentas.PARAMETRO_ARQUIVO)
            .inteiroOpcional("quantidade", "Quantas cores devolver",
                             AnalisadorDeCores.QUANTIDADE_MINIMA,
                             AnalisadorDeCores.QUANTIDADE_MAXIMA,
                             AnalisadorDeCores.QUANTIDADE_PADRAO)
            .inteiroOpcional("passo_de_agrupamento", "Tamanho da faixa de " +
                             "cada canal de cor usada para agrupar cores " +
                             "parecidas (1 = cores exatas; maior = grupos " +
                             "mais amplos)",
                             AnalisadorDeCores.PASSO_MINIMO,
                             AnalisadorDeCores.PASSO_MAXIMO,
                             AnalisadorDeCores.PASSO_PADRAO)
            .booleanoOpcional("ignorar_transparentes",
                              TextosDasFerramentas
                                    .PARAMETRO_IGNORAR_TRANSPARENTES, true);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      // primeiro os parâmetros, depois o arquivo
      String  arquivo    = argumentos.texto("arquivo");
      int     quantidade = argumentos.inteiroOpcional(
                                 "quantidade",
                                 AnalisadorDeCores.QUANTIDADE_PADRAO,
                                 AnalisadorDeCores.QUANTIDADE_MINIMA,
                                 AnalisadorDeCores.QUANTIDADE_MAXIMA);
      int     passo      = argumentos.inteiroOpcional(
                                 "passo_de_agrupamento",
                                 AnalisadorDeCores.PASSO_PADRAO,
                                 AnalisadorDeCores.PASSO_MINIMO,
                                 AnalisadorDeCores.PASSO_MAXIMO);
      boolean ignorar    = argumentos.booleanoOpcional("ignorar_transparentes",
                                                       true);
      Imagem  imagem     = repositorio.abrir(arquivo).imagem();

      ResultadoCoresDominantes resultado = analisador.coresDominantes(
                                       imagem, quantidade, passo, ignorar);

      return new Resultado(arquivo, passo, ignorar,
                           resultado.totalConsiderado(),
                           resultado.gruposDistintos(), resultado.cores());
   }
}
