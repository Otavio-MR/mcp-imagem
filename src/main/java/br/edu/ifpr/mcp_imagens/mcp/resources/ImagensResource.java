package br.edu.ifpr.mcp_imagens.mcp.resources;

import br.edu.ifpr.mcp_imagens.service.InformacoesService;

/**
 * Resource {@code imagens://imagens}: as imagens disponíveis na pasta do
 * servidor, a mesma lista que a ferramenta {@code listar_imagens} devolve.
 */
public class ImagensResource extends AbstractResource {
   private final InformacoesService informacoes;

   public ImagensResource(InformacoesService informacoes) {
      this.informacoes = informacoes;
   }

   @Override
   protected String uri() {
      return "imagens://imagens";
   }

   @Override
   protected String nome() {
      return "imagens";
   }

   @Override
   protected String titulo() {
      return "Imagens disponíveis";
   }

   @Override
   protected String descricao() {
      return "Lista as imagens (PNG, JPG/JPEG, GIF e BMP) que estão na pasta " +
             "de imagens do servidor, com o nome de cada arquivo e o " +
             "formato. Só estas imagens podem ser analisadas; imagens " +
             "anexadas à conversa NÃO ficam disponíveis ao servidor.";
   }

   @Override
   protected Object conteudo() {
      return informacoes.listarImagens();
   }
}
