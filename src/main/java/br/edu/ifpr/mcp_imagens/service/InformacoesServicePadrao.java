package br.edu.ifpr.mcp_imagens.service;

import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;
import br.edu.ifpr.mcp_imagens.domain.model.ListaDeImagens;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;

/**
 * Implementação de {@link InformacoesService} sobre um
 * {@link ImagemRepository}.
 */
public class InformacoesServicePadrao implements InformacoesService {
   private final ImagemRepository repositorio;

   public InformacoesServicePadrao(ImagemRepository repositorio) {
      this.repositorio = repositorio;
   }

   @Override
   public ListaDeImagens listarImagens() {
      List<ImagemInfo> imagens = repositorio.listar();

      return new ListaDeImagens(repositorio.pasta().toString(), imagens.size(),
                                imagens);
   }
}
