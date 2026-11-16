package br.edu.ifpr.mcp_imagens.mcp.tools;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import br.edu.ifpr.mcp_imagens.domain.criterio.CorComTolerancia;
import br.edu.ifpr.mcp_imagens.domain.criterio.CriterioDePixel;
import br.edu.ifpr.mcp_imagens.domain.criterio.ForaDaPaleta;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.CorDominante;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.Parcela;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContagem;
import br.edu.ifpr.mcp_imagens.infra.config.CorParser;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.mcp.utils.Parametros;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeCores;
import br.edu.ifpr.mcp_imagens.service.ContadorDePixels;

/**
 * Ferramenta {@code verificar_paleta}: quantos pixels da imagem usam só as
 * cores de uma paleta e quais cores fora dela mais aparecem.
 */
public class VerificarPaletaTool extends AbstractTool {
   /** Máximo de cores aceitas em {@code cores_permitidas}. */
   public static final int MAXIMO_DE_CORES = 30;

   // quantas cores fora da paleta são listadas, e sem agrupamento (passo 1):
   // o que se quer ver são as cores exatas que escaparam da paleta
   private static final int CORES_FORA_LISTADAS = 5;
   private static final int PASSO_DAS_CORES_FORA = 1;

   /** Formato da resposta de sucesso. */
   public record Resultado(String arquivo, List<String> coresPermitidas,
                           int tolerancia, boolean ignorarTransparentes,
                           long totalConsiderado, Parcela dentroDaPaleta,
                           Parcela foraDaPaleta,
                           List<CorDominante> coresForaDaPaleta) {}

   private final ImagemRepository repositorio;
   private final CorParser        parser;
   private final ContadorDePixels contador;
   private final AnalisadorDeCores analisador;

   public VerificarPaletaTool(ImagemRepository repositorio, CorParser parser,
                              ContadorDePixels contador,
                              AnalisadorDeCores analisador) {
      this.repositorio = repositorio;
      this.parser      = parser;
      this.contador    = contador;
      this.analisador  = analisador;
   }

   @Override
   protected String nome() {
      return "verificar_paleta";
   }

   @Override
   protected String titulo() {
      return "Verificar se a imagem segue uma paleta";
   }

   @Override
   protected String descricao() {
      return "Verifica se a imagem usa só as cores de uma paleta: devolve " +
             "quantos pixels (e que percentual) estão dentro da paleta e " +
             "quantos estão fora, e as 5 cores exatas que mais aparecem fora " +
             "dela. A paleta é uma lista de até " + MAXIMO_DE_CORES +
             " cores separadas por vírgula (#RRGGBB, #RGB ou nomes como " +
             "vermelho), com uma tolerância opcional por canal. Serve para " +
             "conferir se um design respeita a identidade visual. " +
             TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
            .texto(Parametros.ARQUIVO, TextosDasFerramentas.PARAMETRO_ARQUIVO)
            .texto("cores_permitidas", "Cores da paleta, separadas por " +
                   "vírgula, de 1 a " + MAXIMO_DE_CORES + " (ex.: " +
                   "#FFFFFF, #000000, azul)")
            .inteiroOpcional(Parametros.TOLERANCIA,
                             "Diferença máxima permitida por " +
                             "canal de cor para um pixel contar como uma " +
                             "cor da paleta (0 = só a cor exata)",
                             CorComTolerancia.TOLERANCIA_MINIMA,
                             CorComTolerancia.TOLERANCIA_MAXIMA,
                             CorComTolerancia.TOLERANCIA_PADRAO)
            .booleanoOpcional(Parametros.IGNORAR_TRANSPARENTES,
                              TextosDasFerramentas
                                    .PARAMETRO_IGNORAR_TRANSPARENTES, true);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      // primeiro os parâmetros, depois o arquivo
      String    arquivo    = argumentos.texto(Parametros.ARQUIVO);
      List<Cor> paleta     = parser.parseLista(
                                   argumentos.texto("cores_permitidas"),
                                   "cores_permitidas", MAXIMO_DE_CORES);
      int       tolerancia = argumentos.inteiroOpcional(
                                   Parametros.TOLERANCIA,
                                   CorComTolerancia.TOLERANCIA_PADRAO,
                                   CorComTolerancia.TOLERANCIA_MINIMA,
                                   CorComTolerancia.TOLERANCIA_MAXIMA);
      boolean   ignorar    = argumentos.booleanoOpcional(
                                   Parametros.IGNORAR_TRANSPARENTES, true);
      Imagem    imagem     = repositorio.abrir(arquivo).imagem();

      CriterioDePixel foraDaPaleta = new ForaDaPaleta(paleta, tolerancia);
      ResultadoContagem fora = contador.contar(imagem, foraDaPaleta, ignorar);
      long total = fora.totalConsiderado();

      List<CorDominante> coresFora = analisador.coresDominantes(
            imagem, CORES_FORA_LISTADAS, PASSO_DAS_CORES_FORA, ignorar,
            foraDaPaleta).cores();

      return new Resultado(arquivo, hexSemRepetir(paleta), tolerancia,
                           ignorar, total,
                           Parcela.de(total - fora.quantidade(), total),
                           Parcela.de(fora.quantidade(), total), coresFora);
   }

   /** As cores da paleta em hexadecimal, na ordem e sem repetir. */
   private List<String> hexSemRepetir(List<Cor> paleta) {
      Set<String> hexes = new LinkedHashSet<>();

      for (Cor cor : paleta)
         hexes.add(cor.hex());
      return List.copyOf(hexes);
   }
}
