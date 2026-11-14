package br.edu.ifpr.mcp_imagens.infra.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;

class NomeDeArquivoSeguroTest {
   @TempDir
   Path pasta;

   private NomeDeArquivoSeguro seguro() {
      return new NomeDeArquivoSeguro(pasta);
   }

   @ParameterizedTest
   @ValueSource(strings = { "foto.png", "FOTO.PNG", "foto ação.png",
                            "a.b.c.png", "sem-extensao", "2024_banner-v2.jpg",
                            "..png", "a..b.png" })
   void nomeSimplesEResolvidoDentroDaPasta(String nome) {
      Path caminho = seguro().resolver(nome);

      assertEquals(pasta.resolve(nome).normalize(), caminho);
      assertEquals(pasta.toAbsolutePath().normalize(), caminho.getParent());
   }

   @Test
   void removeEspacosDasPontas() {
      assertEquals(pasta.resolve("foto.png"), seguro().resolver("  foto.png "));
   }

   @ParameterizedTest
   @ValueSource(strings = {
      "../segredo.png",         // sobe uma pasta
      "..\\segredo.png",        // sobe uma pasta, estilo Windows
      "../../etc/passwd",
      "C:\\x.png",              // caminho absoluto do Windows
      "C:x.png",                // letra de unidade sem barra
      "c:/windows/system32/a.png",
      "/etc/passwd",            // caminho absoluto do Linux
      "\\\\servidor\\pasta\\a.png",  // caminho de rede
      "sub/a.png",              // subpasta
      "sub\\a.png",
      "./a.png",
      "a/../a.png",
      "..",
      ".",
      "a.png:fluxo",            // fluxo alternativo de dados do NTFS
      "a.png\u0000.txt"         // caractere nulo
   })
   void caminhosSaoRecusados(String nome) {
      ParametroInvalidoException ex = assertThrows(
            ParametroInvalidoException.class, () -> seguro().resolver(nome));

      assertTrue(ex.getMessage().contains("apenas o nome do arquivo"),
                 ex.getMessage());
      assertTrue(ex.getMessage().contains("(recebido: '" + nome + "')"),
                 ex.getMessage());
   }

   @ParameterizedTest
   @ValueSource(strings = { "", "   ", "\t\n" })
   @NullSource
   void nomeVazioEhRecusado(String nome) {
      ParametroInvalidoException ex = assertThrows(
            ParametroInvalidoException.class, () -> seguro().resolver(nome));

      assertTrue(ex.getMessage().contains("vazio"), ex.getMessage());
   }

   @Test
   void pastaRelativaViraAbsoluta() {
      Path caminho = new NomeDeArquivoSeguro(Path.of("imagens"))
                           .resolver("a.png");

      assertTrue(caminho.isAbsolute());
      assertEquals(Path.of("imagens").toAbsolutePath().normalize(),
                   caminho.getParent());
   }

   @Test
   void pastaComPontosNoCaminhoEhNormalizada() {
      Path caminho = new NomeDeArquivoSeguro(pasta.resolve("x").resolve(".."))
                           .resolver("a.png");

      assertEquals(pasta.resolve("a.png"), caminho);
   }
}
