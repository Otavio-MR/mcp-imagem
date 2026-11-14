package br.edu.ifpr.mcp_imagens.infra.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AppConfigTest {
   @Test
   void semVariavelUsaPastaImagensDaPastaAtual() {
      AppConfig config = new AppConfig(Map.of());

      assertEquals(Path.of("imagens").toAbsolutePath().normalize(),
                   config.pastaImagens());
   }

   @Test
   void usaAPastaDaVariavelImgDir() {
      Path pasta = Path.of("alguma", "pasta").toAbsolutePath();

      AppConfig config = new AppConfig(
                  Map.of(AppConfig.VARIAVEL_PASTA_IMAGENS, pasta.toString()));

      assertEquals(pasta.normalize(), config.pastaImagens());
   }

   @Test
   void variavelEmBrancoEhIgnorada() {
      assertEquals(new AppConfig(Map.of()).pastaImagens(),
                   new AppConfig(Map.of("IMG_DIR", "   ")).pastaImagens());
   }

   @Test
   void removeEspacosENormalizaOCaminho() {
      Path base = Path.of("base").toAbsolutePath();

      AppConfig config = new AppConfig(
                  Map.of("IMG_DIR", "  " + base + "/a/../b  "));

      assertEquals(base.resolve("b").normalize(), config.pastaImagens());
   }

   @Test
   void pastaEhSempreAbsoluta() {
      assertTrue(new AppConfig(Map.of("IMG_DIR", "relativa"))
                       .pastaImagens().isAbsolute());
   }

   @Test
   void limitesPadraoSao20MbE25MilhoesDePixels() {
      AppConfig config = new AppConfig(Map.of());

      assertEquals(20L * 1024 * 1024, config.tamanhoMaximoBytes());
      assertEquals(25_000_000L, config.pixelsMaximos());
   }

   @Test
   void doAmbienteLeAsVariaveisDoProcesso() {
      assertNotNull(AppConfig.doAmbiente().pastaImagens());
   }

   @Test
   void nomeDaVariavelEImgDir() {
      assertEquals("IMG_DIR", AppConfig.VARIAVEL_PASTA_IMAGENS);
      assertEquals("imagens", AppConfig.PASTA_IMAGENS_PADRAO);
   }
}
