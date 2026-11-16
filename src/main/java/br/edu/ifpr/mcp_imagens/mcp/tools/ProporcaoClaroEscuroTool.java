package br.edu.ifpr.mcp_imagens.mcp.tools;

import br.edu.ifpr.mcp_imagens.domain.criterio.Claro;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.Parcela;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContagem;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.mcp.utils.Parametros;
import br.edu.ifpr.mcp_imagens.service.ContadorDePixels;

/**
 * Ferramenta {@code proporcao_claro_escuro}: quantos pixels são claros e
 * quantos são escuros, separados por um limiar de brilho.
 */
public class ProporcaoClaroEscuroTool extends AbstractTool {
   /** Formato da resposta de sucesso. */
   public record Resultado(String arquivo, int limiar,
                           boolean ignorarTransparentes, long totalConsiderado,
                           Parcela claros, Parcela escuros) {}

   private final ImagemRepository repositorio;
   private final ContadorDePixels contador;

   public ProporcaoClaroEscuroTool(ImagemRepository repositorio,
                                   ContadorDePixels contador) {
      this.repositorio = repositorio;
      this.contador    = contador;
   }

   @Override
   protected String nome() {
      return "proporcao_claro_escuro";
   }

   @Override
   protected String titulo() {
      return "Proporção de pixels claros e escuros";
   }

   @Override
   protected String descricao() {
      return "Conta quantos pixels da imagem são claros e quantos são " +
             "escuros, com o percentual de cada lado. Um pixel é claro quando " +
             "o seu brilho (luma, de 0 a 255) é maior ou igual ao limiar; " +
             "senão é escuro. O brilho considera só as cores, não a " +
             "transparência. Por padrão ignora os pixels totalmente " +
             "transparentes. " + TextosDasFerramentas.ARQUIVO_NA_PASTA;
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
            .texto(Parametros.ARQUIVO, TextosDasFerramentas.PARAMETRO_ARQUIVO)
            .inteiroOpcional("limiar", "Brilho mínimo (0 a 255) para um " +
                             "pixel ser considerado claro",
                             Claro.LIMIAR_MINIMO, Claro.LIMIAR_MAXIMO,
                             Claro.LIMIAR_PADRAO)
            .booleanoOpcional(Parametros.IGNORAR_TRANSPARENTES,
                              TextosDasFerramentas
                                    .PARAMETRO_IGNORAR_TRANSPARENTES, true);
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      // primeiro os parâmetros, depois o arquivo
      String  arquivo = argumentos.texto(Parametros.ARQUIVO);
      int     limiar  = argumentos.inteiroOpcional("limiar",
                                                   Claro.LIMIAR_PADRAO,
                                                   Claro.LIMIAR_MINIMO,
                                                   Claro.LIMIAR_MAXIMO);
      boolean ignorar = argumentos.booleanoOpcional(
                      Parametros.IGNORAR_TRANSPARENTES, true);
      Imagem  imagem  = repositorio.abrir(arquivo).imagem();

      ResultadoContagem contagem = contador.contar(imagem, new Claro(limiar),
                                                   ignorar);
      long total   = contagem.totalConsiderado();
      long claros  = contagem.quantidade();
      long escuros = total - claros;

      return new Resultado(arquivo, limiar, ignorar, total,
                           Parcela.de(claros, total),
                           Parcela.de(escuros, total));
   }
}
