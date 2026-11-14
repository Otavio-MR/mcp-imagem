package br.edu.ifpr.mcp_imagens.testutil;

import java.util.function.IntBinaryOperator;

import br.edu.ifpr.mcp_imagens.domain.model.FormatoImagem;
import br.edu.ifpr.mcp_imagens.domain.model.Imagem;
import br.edu.ifpr.mcp_imagens.domain.model.ImagemAberta;

/**
 * Cria imagens pequenas <b>em memória</b> para testar os serviços de análise,
 * sem arquivos e sem {@code ImageIO}.
 */
public final class ImagensSinteticas {
   public static final int VERMELHO = 0xFFFF0000;
   public static final int BRANCO   = 0xFFFFFFFF;
   public static final int PRETO    = 0xFF000000;
   public static final int AZUL     = 0xFF0000FF;

   private ImagensSinteticas() {}

   /** Imagem de uma cor só. */
   public static Imagem solida(int largura, int altura, int argb) {
      return gerar(largura, altura, (x, y) -> argb);
   }

   /**
    * Imagem cujo pixel em (x, y) é calculado por uma função.
    *
    * @param pixel  recebe (x, y) e devolve o pixel ARGB
    */
   public static Imagem gerar(int largura, int altura, IntBinaryOperator pixel) {
      int[][] linhas = new int[altura][largura];

      for (int y = 0; y < altura; y++)
         for (int x = 0; x < largura; x++)
            linhas[y][x] = pixel.applyAsInt(x, y);

      return de(linhas);
   }

   /** Imagem a partir das linhas de pixels ARGB, de cima para baixo. */
   public static Imagem de(int[]... linhas) {
      boolean alfa = false;

      for (int[] linha : linhas)
         for (int argb : linha)
            if ((argb >>> 24) != 0xFF)
               alfa = true;

      return new EmMemoria(linhas, alfa);
   }

   /**
    * Imagem "aberta" de mentira, com o formato dado pela extensão do nome
    * (PNG se não houver) e 1500 bytes de tamanho.
    */
   public static ImagemAberta aberta(String nome, Imagem imagem) {
      return aberta(nome, imagem, 1500);
   }

   /** Igual a {@link #aberta(String, Imagem)}, com o tamanho informado. */
   public static ImagemAberta aberta(String nome, Imagem imagem,
                                     long tamanhoBytes) {
      return new ImagemAberta(nome,
                              FormatoImagem.doArquivo(nome)
                                           .orElse(FormatoImagem.PNG),
                              tamanhoBytes, imagem);
   }

   /** Imagem na memória: o canal alfa existe se algum pixel não for opaco. */
   private record EmMemoria(int[][] linhas, boolean alfa) implements Imagem {
      @Override public int     largura()       { return linhas.length == 0 ? 0 : linhas[0].length; }
      @Override public int     altura()        { return linhas.length; }
      @Override public boolean temCanalAlfa()  { return alfa; }
      @Override public int     argb(int x, int y) { return linhas[y][x]; }
   }
}
