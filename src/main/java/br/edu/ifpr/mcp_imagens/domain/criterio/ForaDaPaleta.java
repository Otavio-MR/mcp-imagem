package br.edu.ifpr.mcp_imagens.domain.criterio;

import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;

/**
 * Aceita os pixels que não estão em nenhuma cor de uma paleta (com a mesma
 * tolerância por canal de {@link CorComTolerancia}).
 */
public final class ForaDaPaleta implements CriterioDePixel {
   private final List<CorComTolerancia> coresDaPaleta;

   /**
    * @param paleta      cores permitidas (pelo menos uma)
    * @param tolerancia  diferença máxima por canal, de 0 a 255
    * @throws ParametroInvalidoException se a paleta estiver vazia ou a
    *                                    tolerância fora da faixa
    */
   public ForaDaPaleta(List<Cor> paleta, int tolerancia) {
      if (paleta.isEmpty())
         throw new ParametroInvalidoException("a paleta precisa ter pelo " +
                                              "menos uma cor");
      this.coresDaPaleta = paleta.stream()
                                 .map(cor -> new CorComTolerancia(cor,
                                                                  tolerancia))
                                 .toList();
   }

   @Override
   public boolean aceita(int argb) {
      return coresDaPaleta.stream().noneMatch(cor -> cor.aceita(argb));
   }
}
