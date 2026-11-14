package br.edu.ifpr.mcp_imagens.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class FormatoImagemTest {
   @ParameterizedTest
   @CsvSource({
      "foto.png,        PNG",
      "FOTO.PNG,        PNG",
      "foto.jpg,        JPEG",
      "foto.JPG,        JPEG",
      "foto.jpeg,       JPEG",
      "foto.JpEg,       JPEG",
      "anim.gif,        GIF",
      "tela.bmp,        BMP",
      "arquivo.tar.png, PNG",
      "com espaco.png,  PNG"
   })
   void reconheceOFormatoPelaExtensao(String nome, FormatoImagem esperado) {
      assertEquals(Optional.of(esperado), FormatoImagem.doArquivo(nome));
   }

   @ParameterizedTest
   @ValueSource(strings = { "semextensao", "foto.", "foto.tiff", "foto.webp",
                            "foto.svg", "notas.txt", "foto.png.bak", "png",
                            "" })
   void extensaoNaoAceitaOuAusenteNaoTemFormato(String nome) {
      assertTrue(FormatoImagem.doArquivo(nome).isEmpty(), nome);
   }
}
