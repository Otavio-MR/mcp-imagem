package br.edu.ifpr.mcp_imagens.testutil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Gera as imagens de exemplo da pasta {@code imagens/}, para testar o servidor
 * no Claude Desktop e no MCP Inspector sem precisar de arquivos próprios.
 * <p>
 * As imagens são pequenas e originais, desenhadas aqui com Java 2D, e usam
 * poucas cores exatas (o desenho não usa suavização de bordas), o que torna
 * fáceis de conferir os números devolvidos pelas ferramentas.
 * <p>
 * Para gerar de novo: execute esta classe (Run As, Java Application, no
 * Eclipse), ou depois de {@code mvn test-compile}:
 *
 * <pre>
 * java -cp target/test-classes br.edu.ifpr.mcp_imagens.testutil.GeradorDeImagensDeExemplo imagens
 * </pre>
 *
 * O argumento é a pasta de saída (padrão: {@code imagens}).
 */
public final class GeradorDeImagensDeExemplo {
   // paleta usada nas imagens
   private static final Color AZUL_ESCURO = new Color(0x1B2A41);
   private static final Color CREME       = new Color(0xF5F1E8);
   private static final Color LARANJA     = new Color(0xE4572E);
   private static final Color AZUL        = new Color(0x2E86AB);
   private static final Color AZUL_MARINHO = new Color(0x1F3A5F);
   private static final Color VERDE       = new Color(0x1F7A4D);
   private static final Color BRANCO      = Color.WHITE;
   private static final Color CINZA_ESCURO = new Color(0x333333);
   private static final Color CINZA_CLARO = new Color(0xAAAAAA);
   private static final Color CINZA_MEDIO = new Color(0x767676);

   private GeradorDeImagensDeExemplo() {}

   public static void main(String[] args) throws IOException {
      System.setProperty("java.awt.headless", "true");

      Path pasta = Path.of(args.length > 0 ? args[0] : "imagens");

      gerar(pasta);
      System.out.println("Imagens de exemplo geradas em " +
                         pasta.toAbsolutePath());
   }

   /** Gera as quatro imagens de exemplo na pasta (que é criada se preciso). */
   public static void gerar(Path pasta) throws IOException {
      Files.createDirectories(pasta);

      gravar(pasta, "banner.png", banner());
      gravar(pasta, "icone.png", icone());
      gravar(pasta, "tela-v1.png", tela(AZUL, CINZA_CLARO));
      gravar(pasta, "tela-v2.png", tela(VERDE, CINZA_MEDIO));
   }

   /** Banner 400x120: texto claro sobre fundo escuro e uma barra laranja. */
   private static BufferedImage banner() {
      return desenhar(400, 120, g -> {
         g.setColor(AZUL_ESCURO);
         g.fillRect(0, 0, 400, 120);
         g.setColor(CREME);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
         g.drawString("Promoção de Verão", 24, 62);
         g.setColor(LARANJA);
         g.fillRect(24, 84, 120, 6);
      });
   }

   /**
    * Ícone 64x64 com fundo transparente: um círculo azul com um sinal de
    * "certo" branco, envolto em um anel semitransparente.
    */
   private static BufferedImage icone() {
      return desenhar(64, 64, g -> {
         g.setColor(new Color(0x2E, 0x86, 0xAB, 90));
         g.fillOval(0, 0, 64, 64);
         g.setColor(AZUL);
         g.fillOval(8, 8, 48, 48);
         g.setColor(BRANCO);
         g.setStroke(new BasicStroke(5));
         g.drawPolyline(new int[] { 20, 28, 44 }, new int[] { 34, 42, 24 },
                        3);
      });
   }

   /**
    * Tela 320x200 de um aplicativo. As duas variações (v1 e v2) diferem na cor
    * do botão e na cor do texto de apoio, o que permite testar
    * {@code comparar_imagens}; o cinza claro do texto de apoio da v1 não passa
    * nos critérios de contraste do WCAG, e o da v2 passa.
    */
   private static BufferedImage tela(Color botao, Color textoDeApoio) {
      return desenhar(320, 200, g -> {
         g.setColor(BRANCO);
         g.fillRect(0, 0, 320, 200);
         g.setColor(AZUL_MARINHO);
         g.fillRect(0, 0, 320, 36);
         g.setColor(BRANCO);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
         g.drawString("Minha conta", 12, 24);
         g.setColor(CINZA_ESCURO);
         g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
         g.drawString("Bem-vindo de volta", 16, 70);
         g.setColor(textoDeApoio);
         g.drawString("Último acesso hoje", 16, 92);
         g.setColor(botao);
         g.fillRect(16, 130, 120, 36);
         g.setColor(BRANCO);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
         g.drawString("Entrar", 52, 153);
      });
   }

   /** Cria a imagem, desenha nela sem suavização e a devolve. */
   private static BufferedImage desenhar(int largura, int altura,
                                         Desenho desenho) {
      BufferedImage imagem = new BufferedImage(largura, altura,
                                               BufferedImage.TYPE_INT_ARGB);
      Graphics2D    g      = imagem.createGraphics();

      try {
         // sem suavização: cada pixel é uma das cores da paleta, sem tons
         // intermediários
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
         desenho.desenhar(g);
      }
      finally {
         g.dispose();
      }
      return imagem;
   }

   private static void gravar(Path pasta, String nome, BufferedImage imagem)
               throws IOException {
      ImageIO.write(imagem, "png", pasta.resolve(nome).toFile());
   }

   /** O que desenhar em uma imagem nova. */
   @FunctionalInterface
   private interface Desenho {
      void desenhar(Graphics2D g);
   }
}
