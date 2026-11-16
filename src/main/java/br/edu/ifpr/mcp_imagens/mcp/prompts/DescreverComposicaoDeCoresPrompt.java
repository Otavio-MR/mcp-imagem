package br.edu.ifpr.mcp_imagens.mcp.prompts;

import java.util.List;

import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.Parametros;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeCores;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;

/**
 * Prompt {@code descrever_composicao_de_cores}: orienta a IA a descrever a
 * composição de cores de uma imagem com base em medições.
 */
public class DescreverComposicaoDeCoresPrompt extends AbstractPrompt {
   @Override
   protected String nome() {
      return "descrever_composicao_de_cores";
   }

   @Override
   protected String titulo() {
      return "Descrever a composição de cores de uma imagem";
   }

   @Override
   protected String descricao() {
      return "Pede à IA que descreva a composição de cores de uma imagem em " +
             "um parágrafo e uma tabela, com base nas medições do servidor.";
   }

   @Override
   protected List<PromptArgument> argumentos() {
      return List.of(
            new PromptArgument(Parametros.ARQUIVO,
                  "Nome do arquivo da imagem, como mostrado por " +
                  "listar_imagens", true),
            new PromptArgument(Parametros.QUANTIDADE,
                  "Quantas cores dominantes incluir (de " +
                  AnalisadorDeCores.QUANTIDADE_MINIMA + " a " +
                  AnalisadorDeCores.QUANTIDADE_MAXIMA + "; padrão " +
                  AnalisadorDeCores.QUANTIDADE_PADRAO + ")", false));
   }

   @Override
   protected String montarTexto(ArgumentReader argumentos) {
      String arquivo    = argumentos.texto(Parametros.ARQUIVO);
      int    quantidade = argumentos.inteiroOpcional(
                                 Parametros.QUANTIDADE,
                                 AnalisadorDeCores.QUANTIDADE_PADRAO,
                                 AnalisadorDeCores.QUANTIDADE_MINIMA,
                                 AnalisadorDeCores.QUANTIDADE_MAXIMA);

      return """
            Descreva a composição de cores da imagem '%s' usando as \
            ferramentas do servidor mcp-imagens. Não estime números a olho: \
            use só valores medidos pelas ferramentas.

            1. Chame obter_informacoes com arquivo '%s' para saber o \
            tamanho, o formato e se a imagem tem transparência.
            2. Chame cores_dominantes com arquivo '%s' e quantidade %d. Se \
            o passo for maior que 1, cada cor é o centro de um grupo de \
            cores parecidas; diga isso na resposta.
            3. Chame proporcao_claro_escuro com arquivo '%s' para saber se \
            a imagem é, em geral, clara ou escura.

            Formato da resposta: um parágrafo descrevendo a composição de \
            cores (tons predominantes, claridade geral, presença de \
            transparência), seguido de uma tabela com as colunas cor, \
            quantidade de pixels e percentual. Afirme apenas o que os \
            números sustentam.

            Se uma ferramenta devolver erro, informe o código e a mensagem. \
            Se o arquivo não for encontrado, lembre que a imagem precisa \
            estar na pasta de imagens do servidor (veja listar_imagens).
            """.formatted(arquivo, arquivo, arquivo, quantidade, arquivo);
   }
}
