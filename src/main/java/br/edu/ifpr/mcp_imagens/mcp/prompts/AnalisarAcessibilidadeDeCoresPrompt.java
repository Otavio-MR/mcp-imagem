package br.edu.ifpr.mcp_imagens.mcp.prompts;

import java.util.List;

import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.Parametros;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;

/**
 * Prompt {@code analisar_acessibilidade_de_cores}: orienta a IA a medir as
 * cores de uma imagem e interpretar o contraste pelos critérios do WCAG.
 */
public class AnalisarAcessibilidadeDeCoresPrompt extends AbstractPrompt {
   @Override
   protected String nome() {
      return "analisar_acessibilidade_de_cores";
   }

   @Override
   protected String titulo() {
      return "Analisar a acessibilidade das cores de uma imagem";
   }

   @Override
   protected String descricao() {
      return "Pede à IA que meça as cores dominantes de uma imagem, calcule " +
             "o contraste entre elas e recomende ajustes pelos critérios de " +
             "contraste do WCAG.";
   }

   @Override
   protected List<PromptArgument> argumentos() {
      return List.of(new PromptArgument(Parametros.ARQUIVO,
            "Nome do arquivo da imagem, como mostrado por listar_imagens",
            true));
   }

   @Override
   protected String montarTexto(ArgumentReader argumentos) {
      String arquivo = argumentos.texto(Parametros.ARQUIVO);

      return """
            Analise a acessibilidade das cores da imagem '%s' usando as \
            ferramentas do servidor mcp-imagens. Não estime números a olho: \
            use só valores medidos pelas ferramentas.

            1. Chame contraste_das_cores_dominantes com arquivo '%s' e \
            passo_de_agrupamento 1. Ele devolve o contraste WCAG entre as \
            duas cores mais frequentes (a mais frequente como fundo), com o \
            veredito de cada critério. É uma aproximação: o servidor não \
            detecta onde está o texto; diga isso na resposta.
            2. Chame cores_dominantes com arquivo '%s', quantidade 5 e \
            passo_de_agrupamento 1 para ver as demais cores. Se houver \
            outros pares de cores relevantes, chame calcular_contraste para \
            cada um (cor_texto e cor_fundo).
            3. Interprete os vereditos do WCAG de cada par: AA e AAA, para \
            texto normal e texto grande, e componentes de interface. Se \
            precisar dos limiares, leia o resource imagens://criterios-wcag.
            4. Recomende ajustes para os pares que não passam, por exemplo \
            escurecer o texto ou clarear o fundo, e confirme a sugestão \
            chamando calcular_contraste de novo.

            Regras:
            - Afirme apenas o que os números sustentam. Você não sabe qual \
            cor é texto e qual é fundo na imagem real: apresente o resultado \
            como contraste entre pares de cores, e não como diagnóstico do \
            texto da imagem.
            - Se uma ferramenta devolver erro, informe o código e a mensagem. \
            Se o arquivo não for encontrado, lembre que a imagem precisa \
            estar na pasta de imagens do servidor (veja listar_imagens).

            Formato da resposta: uma tabela com as colunas par de cores, \
            razão de contraste e vereditos, seguida de recomendações curtas.
            """.formatted(arquivo, arquivo, arquivo);
   }
}
