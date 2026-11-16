package br.edu.ifpr.mcp_imagens.mcp.resources;

import br.edu.ifpr.mcp_imagens.infra.config.TabelaWcag;

/**
 * Resource {@code imagens://criterios-wcag}: os limiares de contraste do WCAG,
 * os mesmos que a {@code CalculadoraDeContraste} usa.
 */
public class CriteriosWcagResource extends AbstractResource {
   private final TabelaWcag tabela;

   public CriteriosWcagResource(TabelaWcag tabela) {
      this.tabela = tabela;
   }

   @Override
   protected String uri() {
      return "imagens://criterios-wcag";
   }

   @Override
   protected String nome() {
      return "criterios-wcag";
   }

   @Override
   protected String titulo() {
      return "Critérios de contraste do WCAG";
   }

   @Override
   protected String descricao() {
      return "Limiares mínimos de contraste do WCAG 2.x (AA e AAA, texto " +
             "normal e grande, componentes de interface), com a explicação " +
             "de cada critério. Use para interpretar os vereditos de " +
             "calcular_contraste.";
   }

   @Override
   protected Object conteudo() {
      return tabela;
   }
}
