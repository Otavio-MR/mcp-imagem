package br.edu.ifpr.mcp_imagens.mcp.tools;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.CorDominante;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContraste;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoCoresDominantes;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.mcp.utils.Parametros;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeCores;
import br.edu.ifpr.mcp_imagens.service.CalculadoraDeContraste;

/**
 * Ferramenta {@code contraste_das_cores_dominantes}: o contraste WCAG entre as
 * duas cores que mais aparecem na imagem.
 * <p>
 * É uma aproximação: serve para telas e banners com texto sobre um fundo liso,
 * mas o servidor não detecta onde está o texto.
 */
public class ContrasteDasCoresDominantesTool extends AbstractTool {
   private static final int QUANTIDADE_DE_CORES = 2;

   private static final String AVISO_APROXIMACAO =
         "Aproximação: o servidor não detecta o texto da imagem. O contraste " +
         "é calculado entre as duas cores mais frequentes, a mais frequente " +
         "como fundo e a outra como texto; só faz sentido se a imagem for um " +
         "texto sobre um fundo liso.";

   private static final String AVISO_GRUPOS =
         "Com passo_de_agrupamento maior que 1, cada cor é o centro de um " +
         "grupo de cores parecidas, e não uma cor exata da imagem. Use " +
         "passo_de_agrupamento 1 para as cores exatas.";

   private static final String AVISO_UMA_COR =
         "A imagem tem uma única cor dominante: não há contraste a medir " +
         "(a razão é 1,00).";

   private static final String AVISO_SEM_PIXELS =
         "Não há pixels para analisar: a imagem é toda transparente.";

   /** Formato da resposta de sucesso. */
   public record Resultado(String arquivo, int passoDeAgrupamento,
                           long totalConsiderado,
                           List<CorDominante> coresDominantes,
                           ResultadoContraste contraste, List<String> avisos) {}

   private final ImagemRepository      repositorio;
   private final AnalisadorDeCores     analisador;
   private final CalculadoraDeContraste calculadora;

   public ContrasteDasCoresDominantesTool(ImagemRepository repositorio,
                                          AnalisadorDeCores analisador,
                                          CalculadoraDeContraste calculadora) {
      this.repositorio = repositorio;
      this.analisador  = analisador;
      this.calculadora = calculadora;
   }

   @Override
   protected String nome() {
      return "contraste_das_cores_dominantes";
   }

   @Override
   protected String titulo() {
      return "Contraste entre as duas cores dominantes";
   }

   @Override
   protected String descricao() {
      return "Pega as duas cores que mais aparecem na imagem e calcula o " +
             "contraste entre elas pelo WCAG 2.x, com o veredito para cada " +
             "critério (AA, AAA, texto normal e grande, componentes de " +
             "interface). É uma APROXIMAÇÃO, útil para telas e banners com " +
             "texto sobre um fundo liso: o servidor não detecta onde está o " +
             "texto. Ignora os pixels totalmente transparentes. " +
             TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
            .texto(Parametros.ARQUIVO, TextosDasFerramentas.PARAMETRO_ARQUIVO)
            .inteiroOpcional(Parametros.PASSO_DE_AGRUPAMENTO,
                             "Tamanho da faixa de " +
                             "cada canal de cor usada para agrupar cores " +
                             "parecidas (1 = cores exatas; maior = grupos " +
                             "mais amplos)",
                             AnalisadorDeCores.PASSO_MINIMO,
                             AnalisadorDeCores.PASSO_MAXIMO,
                             AnalisadorDeCores.PASSO_PADRAO);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      // primeiro os parâmetros, depois o arquivo
      String arquivo = argumentos.texto(Parametros.ARQUIVO);
      int    passo   = argumentos.inteiroOpcional(
                             Parametros.PASSO_DE_AGRUPAMENTO,
                             AnalisadorDeCores.PASSO_PADRAO,
                             AnalisadorDeCores.PASSO_MINIMO,
                             AnalisadorDeCores.PASSO_MAXIMO);
      Imagem imagem  = repositorio.abrir(arquivo).imagem();

      ResultadoCoresDominantes dominantes = analisador.coresDominantes(
                        imagem, QUANTIDADE_DE_CORES, passo, true);
      List<CorDominante> cores = dominantes.cores();

      return new Resultado(arquivo, passo, dominantes.totalConsiderado(),
                           cores, contrasteDe(cores), avisosDe(cores, passo));
   }

   /**
    * Contraste entre as duas cores, ou {@code null} se não há cores. A cor mais
    * frequente é o fundo; sem uma segunda cor, o contraste é o da cor com ela
    * mesma (1,00).
    */
   private ResultadoContraste contrasteDe(List<CorDominante> cores) {
      if (cores.isEmpty())
         return null;

      Cor fundo = Cor.deHex(cores.get(0).cor());
      Cor texto = cores.size() > 1 ? Cor.deHex(cores.get(1).cor()) : fundo;

      return calculadora.calcular(texto, fundo);
   }

   private List<String> avisosDe(List<CorDominante> cores, int passo) {
      List<String> avisos = new ArrayList<>();

      avisos.add(AVISO_APROXIMACAO);
      if (passo > 1)
         avisos.add(AVISO_GRUPOS);
      if (cores.isEmpty())
         avisos.add(AVISO_SEM_PIXELS);
      else if (cores.size() == 1)
         avisos.add(AVISO_UMA_COR);
      return avisos;
   }
}
