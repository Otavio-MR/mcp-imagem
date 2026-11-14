package br.edu.ifpr.mcp_imagens.domain.model;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Formatos de imagem aceitos pelo servidor (os que o {@code ImageIO} do JDK
 * lê), identificados pela extensão do arquivo.
 */
public enum FormatoImagem {
   PNG("png"),
   JPEG("jpg", "jpeg"),
   GIF("gif"),
   BMP("bmp");

   private final List<String> extensoes;

   FormatoImagem(String... extensoes) {
      this.extensoes = List.of(extensoes);
   }

   /**
    * Formato de um arquivo, pela extensão (sem diferenciar maiúsculas).
    *
    * @param nomeArquivo nome do arquivo, como "foto.PNG"
    * @return o formato, ou vazio se a extensão não for aceita ou não houver
    */
   public static Optional<FormatoImagem> doArquivo(String nomeArquivo) {
      int ponto = nomeArquivo.lastIndexOf('.');

      if (ponto < 0 || ponto == nomeArquivo.length() - 1)
         return Optional.empty();

      String extensao = nomeArquivo.substring(ponto + 1)
                                   .toLowerCase(Locale.ROOT);

      for (FormatoImagem formato : values())
         if (formato.extensoes.contains(extensao))
            return Optional.of(formato);

      return Optional.empty();
   }
}
