package br.edu.ifpr.mcp_imagens.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeradorDeImagensDeExemploTest {
   @TempDir
   Path pasta;

   private BufferedImage ler(String nome) throws IOException {
      return ImageIO.read(pasta.resolve(nome).toFile());
   }

   private static Set<Integer> cores(BufferedImage imagem) {
      Set<Integer> cores = new HashSet<>();

      for (int y = 0; y < imagem.getHeight(); y++)
         for (int x = 0; x < imagem.getWidth(); x++)
            cores.add(imagem.getRGB(x, y));
      return cores;
   }

   @Test
   void geraAsQuatroImagensComOsTamanhosEsperados() throws IOException {
      GeradorDeImagensDeExemplo.gerar(pasta);

      assertEquals(400, ler("banner.png").getWidth());
      assertEquals(120, ler("banner.png").getHeight());
      assertEquals(64, ler("icone.png").getWidth());
      assertEquals(320, ler("tela-v1.png").getWidth());
      assertEquals(200, ler("tela-v1.png").getHeight());
      assertEquals(320, ler("tela-v2.png").getWidth());
      assertEquals(200, ler("tela-v2.png").getHeight());
   }

   @Test
   void criaAPastaDeSaidaSeNaoExistir() throws IOException {
      Path nova = pasta.resolve("nova/imagens");

      GeradorDeImagensDeExemplo.gerar(nova);

      assertTrue(nova.resolve("banner.png").toFile().isFile());
   }

   @Test
   void bannerUsaSoTresCoresSemSuavizacao() throws IOException {
      GeradorDeImagensDeExemplo.gerar(pasta);

      assertEquals(Set.of(0xFF1B2A41, 0xFFF5F1E8, 0xFFE4572E),
                   cores(ler("banner.png")));
   }

   @Test
   void iconeTemPixelsTransparentesSemitransparentesEOpacos()
               throws IOException {
      GeradorDeImagensDeExemplo.gerar(pasta);

      BufferedImage icone = ler("icone.png");
      boolean transparente = false, semitransparente = false, opaco = false;

      for (int y = 0; y < icone.getHeight(); y++)
         for (int x = 0; x < icone.getWidth(); x++) {
            int alfa = icone.getRGB(x, y) >>> 24;

            transparente     |= alfa == 0;
            semitransparente |= alfa > 0 && alfa < 255;
            opaco            |= alfa == 255;
         }
      assertTrue(transparente && semitransparente && opaco);
      // os cantos ficam fora do círculo
      assertEquals(0, icone.getRGB(0, 0) >>> 24);
   }

   @Test
   void asDuasTelasTemOMesmoTamanhoEDiferemNoBotaoENoTextoDeApoio()
               throws IOException {
      GeradorDeImagensDeExemplo.gerar(pasta);

      BufferedImage v1 = ler("tela-v1.png");
      BufferedImage v2 = ler("tela-v2.png");

      assertEquals(v1.getWidth(), v2.getWidth());
      // o botão tem cores diferentes e o cabeçalho é igual
      assertEquals(v1.getRGB(5, 5), v2.getRGB(5, 5));
      assertNotEquals(v1.getRGB(20, 134), v2.getRGB(20, 134));
      assertEquals(0xFF2E86AB, v1.getRGB(20, 134));
      assertEquals(0xFF1F7A4D, v2.getRGB(20, 134));
   }
}
