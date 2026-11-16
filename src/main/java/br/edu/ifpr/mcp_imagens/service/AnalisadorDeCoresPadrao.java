package br.edu.ifpr.mcp_imagens.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import br.edu.ifpr.mcp_imagens.domain.criterio.CriterioDePixel;
import br.edu.ifpr.mcp_imagens.domain.model.Arredondamento;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.CorDominante;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoCoresDominantes;

/**
 * Implementação de {@link AnalisadorDeCores}: conta os pixels em uma tabela com
 * uma posição para cada grupo de cor e escolhe os grupos mais cheios.
 */
public class AnalisadorDeCoresPadrao implements AnalisadorDeCores {
   /** Um grupo (posição na tabela) e quantos pixels ele tem. */
   private record Grupo(int indice, int quantidade) {}

   /** Do "pior" para o "melhor": menos pixels e, em empate, índice maior. */
   private static final Comparator<Grupo> PIOR_PRIMEIRO =
         Comparator.comparingInt(Grupo::quantidade)
                   .thenComparing(Comparator.comparingInt(Grupo::indice)
                                            .reversed());

   @Override
   public ResultadoCoresDominantes coresDominantes(
                     Imagem imagem, int quantidade, int passo,
                     boolean ignorarTransparentes, CriterioDePixel filtro) {
      // faixas por canal: 256 / passo, arredondado para cima
      int     faixas   = (256 + passo - 1) / passo;
      int[]   contagem = new int[faixas * faixas * faixas];
      long[]  total    = new long[1];

      imagem.paraCadaPixel(ignorarTransparentes, argb -> {
         if (!filtro.aceita(argb))
            return;

         int r = ((argb >> 16) & 0xFF) / passo;
         int g = ((argb >> 8) & 0xFF) / passo;
         int b = (argb & 0xFF) / passo;

         contagem[(r * faixas + g) * faixas + b]++;
         total[0]++;
      });

      return montarResultado(contagem, faixas, passo, quantidade, total[0]);
   }

   private ResultadoCoresDominantes montarResultado(int[] contagem, int faixas,
                                                    int passo, int quantidade,
                                                    long total) {
      // guarda só os "quantidade" melhores grupos, sem ordenar a tabela toda
      PriorityQueue<Grupo> melhores = new PriorityQueue<>(PIOR_PRIMEIRO);
      int                  distintos = 0;

      for (int indice = 0; indice < contagem.length; indice++) {
         if (contagem[indice] == 0)
            continue;

         Grupo grupo = new Grupo(indice, contagem[indice]);

         distintos++;
         if (melhores.size() < quantidade)
            melhores.add(grupo);
         else if (PIOR_PRIMEIRO.compare(grupo, melhores.peek()) > 0) {
            melhores.poll();
            melhores.add(grupo);
         }
      }

      List<Grupo> ordenados = new ArrayList<>(melhores);

      ordenados.sort(PIOR_PRIMEIRO.reversed());

      List<CorDominante> cores = new ArrayList<>();

      for (Grupo grupo : ordenados)
         cores.add(new CorDominante(corDoGrupo(grupo.indice(), faixas, passo),
                                    grupo.quantidade(),
                                    Arredondamento.percentual(
                                          grupo.quantidade(), total)));

      return new ResultadoCoresDominantes(total, distintos, cores);
   }

   /** A cor do grupo é o centro da faixa em cada canal (no máximo 255). */
   private String corDoGrupo(int indice, int faixas, int passo) {
      int r = indice / (faixas * faixas);
      int g = (indice / faixas) % faixas;
      int b = indice % faixas;

      return new Cor(centro(r, passo), centro(g, passo),
                     centro(b, passo)).hex();
   }

   private int centro(int faixa, int passo) {
      return Math.min(255, faixa * passo + passo / 2);
   }
}
