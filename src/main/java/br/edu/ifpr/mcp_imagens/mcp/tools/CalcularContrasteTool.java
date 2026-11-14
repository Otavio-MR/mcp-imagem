package br.edu.ifpr.mcp_imagens.mcp.tools;

import br.edu.ifpr.mcp_imagens.domain.exception.CorInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.infra.config.CorParser;
import br.edu.ifpr.mcp_imagens.mcp.utils.AbstractTool;
import br.edu.ifpr.mcp_imagens.mcp.utils.ArgumentReader;
import br.edu.ifpr.mcp_imagens.mcp.utils.InputSchema;
import br.edu.ifpr.mcp_imagens.service.CalculadoraDeContraste;

/**
 * Ferramenta {@code calcular_contraste}: razão de contraste WCAG 2.x entre
 * duas cores, com o veredito para cada critério. Não usa arquivo de imagem.
 */
public class CalcularContrasteTool extends AbstractTool {
   private final CorParser               parser;
   private final CalculadoraDeContraste  calculadora;

   public CalcularContrasteTool(CorParser parser,
                                CalculadoraDeContraste calculadora) {
      this.parser      = parser;
      this.calculadora = calculadora;
   }

   @Override
   protected String nome() {
      return "calcular_contraste";
   }

   @Override
   protected String titulo() {
      return "Calcular contraste entre cores";
   }

   @Override
   protected String descricao() {
      return "Calcula a razão de contraste (de 1 a 21) entre a cor do texto " +
             "e a do fundo, pela fórmula do WCAG 2.x, e informa se passa em " +
             "AA e AAA para texto normal e grande e para componentes de " +
             "interface. Cálculo exato, sem estimar. Não usa arquivo de " +
             "imagem. As cores podem ser #RRGGBB, #RGB ou um nome como " +
             "vermelho.";
   }

   @Override
   protected InputSchema schema() {
      return InputSchema.novo()
                        .texto("cor_texto", "Cor do texto: #RRGGBB, #RGB ou " +
                                            "nome (ex.: #777777, vermelho)")
                        .texto("cor_fundo", "Cor do fundo: #RRGGBB, #RGB ou " +
                                            "nome (ex.: #FFFFFF, branco)");
   }

   @Override
   protected Object executar(ArgumentReader argumentos) {
      Cor texto = lerCor(argumentos, "cor_texto");
      Cor fundo = lerCor(argumentos, "cor_fundo");

      return calculadora.calcular(texto, fundo);
   }

   // lê o parâmetro e indica qual deles tem a cor inválida
   private Cor lerCor(ArgumentReader argumentos, String parametro) {
      String texto = argumentos.texto(parametro);

      try {
         return parser.parse(texto);
      }
      catch (CorInvalidaException ex) {
         throw new CorInvalidaException("parâmetro '" + parametro + "': " +
                                        ex.getMessage());
      }
   }
}
