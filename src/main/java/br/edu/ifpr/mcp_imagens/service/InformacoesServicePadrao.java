package br.edu.ifpr.mcp_imagens.service;

import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemAberta;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemInfo;
import br.edu.ifpr.mcp_imagens.domain.model.InformacoesImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ListaDeImagens;
import br.edu.ifpr.mcp_imagens.infra.repository.ImagemRepository;

/**
 * Implementação de {@link InformacoesService} sobre um
 * {@link ImagemRepository}.
 */
public class InformacoesServicePadrao implements InformacoesService {
   private static final long BYTES_POR_KB = 1024;

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

   @Override
   public InformacoesImagem obter(String arquivo) {
      ImagemAberta aberta = repositorio.abrir(arquivo);
      Imagem       imagem = aberta.imagem();

      return new InformacoesImagem(aberta.nome(), aberta.formato(),
                                   imagem.largura(), imagem.altura(),
                                   imagem.totalPixels(),
                                   imagem.temCanalAlfa(),
                                   aberta.tamanhoBytes(),
                                   (aberta.tamanhoBytes() + BYTES_POR_KB - 1) /
                                   BYTES_POR_KB);
   }
}
