package br.edu.ifpr.mcp_imagens.service;

import java.util.List;

import br.edu.ifpr.mcp_imagens.domain.model.Arredondamento;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.CriterioWcag;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContraste;
import br.edu.ifpr.mcp_imagens.domain.model.Veredito;

/**
 * Implementação de {@link CalculadoraDeContraste}.
 * <p>
 * Razão = (L clara + 0,05) / (L escura + 0,05), em que L é a luminância
 * relativa da cor ({@link Cor#luminanciaRelativa()}). O resultado fica entre
 * 1 (cores iguais) e 21 (preto sobre branco). Os vereditos usam a razão
 * <b>sem arredondar</b>, como pede o WCAG: 4,499 não atinge 4,5, mesmo que
 * apareça como 4,50 no resultado.
 */
public class CalculadoraDeContrastePadrao implements CalculadoraDeContraste {
   private static final double AJUSTE = 0.05;

   private final List<CriterioWcag> criterios;

   /** @param criterios critérios do WCAG com seus limiares, na ordem de exibição */
   public CalculadoraDeContrastePadrao(List<CriterioWcag> criterios) {
      this.criterios = List.copyOf(criterios);
   }

   @Override
   public ResultadoContraste calcular(Cor texto, Cor fundo) {
      double razao = razao(texto, fundo);

      return new ResultadoContraste(texto.hex(), fundo.hex(),
                                    Arredondamento.duasCasas(razao),
                                    vereditos(razao));
   }

   // a mais clara dividida pela mais escura, sempre entre 1 e 21
   private static double razao(Cor a, Cor b) {
      double luminanciaA = a.luminanciaRelativa();
      double luminanciaB = b.luminanciaRelativa();
      double clara       = Math.max(luminanciaA, luminanciaB);
      double escura      = Math.min(luminanciaA, luminanciaB);

      return (clara + AJUSTE) / (escura + AJUSTE);
   }

   private List<Veredito> vereditos(double razao) {
      return criterios.stream()
                      .map(criterio -> new Veredito(criterio.id(),
                                                    criterio.nome(),
                                                    criterio.limiar(),
                                                    razao >= criterio.limiar()))
                      .toList();
   }
}
