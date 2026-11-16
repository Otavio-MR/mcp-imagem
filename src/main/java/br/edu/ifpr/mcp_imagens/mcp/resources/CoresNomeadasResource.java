package br.edu.ifpr.mcp_imagens.mcp.resources;

import br.edu.ifpr.mcp_imagens.infra.config.CoresNomeadas;

/**
 * Resource {@code imagens://cores-nomeadas}: o dicionário de nomes de cores
 * em português para hexadecimal, o mesmo que o {@code CorParser} usa.
 */
public class CoresNomeadasResource extends AbstractResource {
   private final CoresNomeadas cores;

   public CoresNomeadasResource(CoresNomeadas cores) {
      this.cores = cores;
   }

   @Override
   protected String uri() {
      return "imagens://cores-nomeadas";
   }

   @Override
   protected String nome() {
      return "cores-nomeadas";
   }

   @Override
   protected String titulo() {
      return "Cores nomeadas";
   }

   @Override
   protected String descricao() {
      return "Nomes de cores em português (sem acentos) e o valor " +
             "hexadecimal de cada uma. São os nomes aceitos nos parâmetros " +
             "de cor das ferramentas, como vermelho ou azul-marinho.";
   }

   @Override
   protected Object conteudo() {
      return cores;
   }
}
