package br.edu.ifpr.mcp_imagens.domain.model;

import java.util.regex.Pattern;

import br.edu.ifpr.mcp_imagens.domain.exception.CorInvalidaException;

/**
 * Uma cor em sRGB, com canal alfa (transparência). Objeto de valor imutável:
 * os quatro componentes são validados na criação (0 a 255).
 *
 * @param r  vermelho
 * @param g  verde
 * @param b  azul
 * @param a  alfa: 0 = transparente, 255 = opaco
 */
public record Cor(int r, int g, int b, int a) {
   /** Valor do alfa de uma cor totalmente opaca. */
   public static final int OPACO = 255;

   // "#RGB" ou "#RRGGBB", com letras maiúsculas ou minúsculas
   private static final Pattern HEX = Pattern.compile(
                                          "^#(?:[0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$");

   public Cor {
      validar("r", r);
      validar("g", g);
      validar("b", b);
      validar("a", a);
   }

   /** Cor opaca. */
   public Cor(int r, int g, int b) {
      this(r, g, b, OPACO);
   }

   private static void validar(String nome, int componente) {
      if (componente < 0 || componente > 255)
         throw new CorInvalidaException("o componente '" + nome +
                                        "' deve estar entre 0 e 255 " +
                                        "(recebido: " + componente + ")");
   }

   /**
    * Cor opaca a partir de "#RRGGBB" ou "#RGB" (cada dígito do formato curto
    * vale duas vezes: "#F80" é "#FF8800").
    *
    * @throws CorInvalidaException se o texto não estiver nesses formatos
    */
   public static Cor deHex(String texto) {
      if (texto == null || !HEX.matcher(texto).matches())
         throw new CorInvalidaException("'" + texto + "' não é uma cor " +
                                        "hexadecimal válida; use #RRGGBB " +
                                        "ou #RGB");

      String digitos = texto.substring(1);

      if (digitos.length() == 3)
         digitos = "" + digitos.charAt(0) + digitos.charAt(0) +
                   digitos.charAt(1) + digitos.charAt(1) +
                   digitos.charAt(2) + digitos.charAt(2);

      return new Cor(Integer.parseInt(digitos.substring(0, 2), 16),
                     Integer.parseInt(digitos.substring(2, 4), 16),
                     Integer.parseInt(digitos.substring(4, 6), 16));
   }

   /** "#RRGGBB" em maiúsculas; o alfa não aparece. */
   public String hex() {
      return String.format("#%02X%02X%02X", r, g, b);
   }

   /**
    * Brilho (luma) de um pixel no formato ARGB de
    * {@code BufferedImage.getRGB}, sem criar um objeto {@code Cor} (as
    * análises percorrem milhões de pixels).
    */
   public static double luma(int argb) {
      return luma((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
   }

   // Os pesos 0,2126, 0,7152 e 0,0722 viram 2126, 7152 e 722 sobre 10000. Em
   // inteiros a soma é exata: com double, 67 dos 256 tons de cinza dariam um
   // brilho um pouco diferente do valor (o cinza 5 daria 4,999999999999999) e
   // um pixel poderia cair do lado errado de um limiar.
   private static double luma(int r, int g, int b) {
      return (2126 * r + 7152 * g + 722 * b) / 10000.0;
   }

   /**
    * Luminância relativa do WCAG 2.x, de 0 (preto) a 1 (branco): cada
    * componente é linearizado e ponderado por {@code 0,2126}, {@code 0,7152}
    * e {@code 0,0722}.
    */
   public double luminanciaRelativa() {
      return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b);
   }

   // componente de 0 a 255 para o valor linear usado pelo WCAG
   private static double linear(int componente) {
      double s = componente / 255.0;

      return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
   }
}
