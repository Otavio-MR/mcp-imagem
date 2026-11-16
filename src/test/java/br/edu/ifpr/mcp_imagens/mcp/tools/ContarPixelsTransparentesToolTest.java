package br.edu.ifpr.mcp_imagens.mcp.tools;

import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.chamar;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.erro;
import static br.edu.ifpr.mcp_imagens.testutil.Chamadas.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.service.AnalisadorDeTransparenciaPadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import br.edu.ifpr.mcp_imagens.testutil.Resultados;
import tools.jackson.databind.JsonNode;

class ContarPixelsTransparentesToolTest {
   private static final String NOME = "contar_pixels_transparentes";

   private static final ContarPixelsTransparentesTool FERRAMENTA =
         new ContarPixelsTransparentesTool(RepositorioFalso.com(
               // alfa 0, 128 e 255
               ImagensSinteticas.aberta("alfas.png", ImagensSinteticas.de(
                     new int[] { 0x00FF0000, 0x80FF0000, 0xFFFF0000 })),
               // quatro pixels, um transparente
               ImagensSinteticas.aberta("quarto.png", ImagensSinteticas.de(
                     new int[] { 0x00000000, 0xFF000000 },
                     new int[] { 0xFF000000, 0xFF000000 })),
               ImagensSinteticas.aberta("opaca.jpg",
                     ImagensSinteticas.solida(2, 2, 0xFF808080))),
               new AnalisadorDeTransparenciaPadrao());

   @Test
   void contaOsTresTipos() {
      JsonNode json = json(FERRAMENTA, NOME, Map.of("arquivo", "alfas.png"));

      assertEquals("alfas.png", json.get("arquivo").asString());
      assertEquals(3, json.get("totalPixels").asLong());
      assertEquals(true, json.get("temCanalAlfa").asBoolean());
      assertEquals(1, json.get("transparentes").get("quantidade").asLong());
      assertEquals(1, json.get("semitransparentes").get("quantidade")
                        .asLong());
      assertEquals(1, json.get("opacos").get("quantidade").asLong());
      assertEquals(33.33, json.get("transparentes").get("percentual")
                              .asDouble(), 0);
   }

   @Test
   void percentualDeUmQuarto() {
      JsonNode json = json(FERRAMENTA, NOME, Map.of("arquivo", "quarto.png"));

      assertEquals(25.0, json.get("transparentes").get("percentual")
                             .asDouble(), 0);
      assertEquals(75.0, json.get("opacos").get("percentual").asDouble(), 0);
      assertEquals(0.0, json.get("semitransparentes").get("percentual")
                            .asDouble(), 0);
   }

   @Test
   void percentualSaiComDuasCasasNoTexto() {
      String texto = Resultados.texto(chamar(FERRAMENTA, NOME,
                                             Map.of("arquivo", "quarto.png")));

      assertTrue(texto.contains("\"percentual\" : 25.00"), texto);
      assertTrue(texto.contains("\"percentual\" : 0.00"), texto);
   }

   @Test
   void imagemSemAlfaTemTudoOpaco() {
      JsonNode json = json(FERRAMENTA, NOME, Map.of("arquivo", "opaca.jpg"));

      assertEquals(false, json.get("temCanalAlfa").asBoolean());
      assertEquals(4, json.get("opacos").get("quantidade").asLong());
      assertEquals(100.0, json.get("opacos").get("percentual").asDouble(), 0);
   }

   @Test
   void erros() {
      assertTrue(erro(FERRAMENTA, NOME, Map.of())
                    .startsWith("PARAMETRO_AUSENTE: "));
      assertTrue(erro(FERRAMENTA, NOME, Map.of("arquivo", " "))
                    .startsWith("PARAMETRO_INVALIDO: "));
      assertTrue(erro(FERRAMENTA, NOME, Map.of("arquivo", "nada.png"))
                    .startsWith("IMAGEM_NAO_ENCONTRADA: "));
      assertTrue(erro(FERRAMENTA, NOME,
                      Map.of("arquivo", "alfas.png",
                             "ignorar_transparentes", true))
                    .startsWith("PARAMETRO_INVALIDO: "));
   }

   @Test
   void errosDeAcessoAImagemPropagam() {
      ContarPixelsTransparentesTool invalida =
            new ContarPixelsTransparentesTool(
                  RepositorioFalso.falhando(new ImagemInvalidaException("x")),
                  new AnalisadorDeTransparenciaPadrao());

      assertEquals("IMAGEM_INVALIDA: x",
                   erro(invalida, NOME, Map.of("arquivo", "a.png")));
   }

   @Test
   void schemaEDescricao() {
      assertEquals(Set.of("arquivo"),
                   FERRAMENTA.schema().nomesDosParametros());
      assertEquals(List.of("arquivo"),
                   FERRAMENTA.schema().comoMapa().get("required"));
      assertTrue(FERRAMENTA.descricao().contains("anexadas à conversa NÃO"));
      assertEquals(NOME, FERRAMENTA.nome());
   }
}
