package br.edu.ifpr.mcp_imagens.mcp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class InputSchemaTest {
   @Test
   void schemaSemParametros() {
      Map<String, Object> schema = InputSchema.novo().comoMapa();

      assertEquals("object", schema.get("type"));
      assertEquals(Map.of(), schema.get("properties"));
      assertEquals(List.of(), schema.get("required"));
      assertEquals(false, schema.get("additionalProperties"));
      assertEquals(Set.of(), InputSchema.novo().nomesDosParametros());
   }

   @Test
   void soTextoEhObrigatorio() {
      InputSchema schema = InputSchema.novo()
            .texto("arquivo", "Nome do arquivo")
            .inteiroOpcional("tolerancia", "Tolerância", 0, 255, 0)
            .booleanoOpcional("ignorar", "Ignorar transparentes", true);

      assertEquals(List.of("arquivo"), schema.comoMapa().get("required"));
      assertEquals(List.of("arquivo", "tolerancia", "ignorar"),
                   List.copyOf(schema.nomesDosParametros()));
   }

   @Test
   void propriedadesTrazemTipoDescricaoLimitesEPadrao() {
      Map<?, ?> propriedades = (Map<?, ?>) InputSchema.novo()
            .texto("arquivo", "Nome do arquivo")
            .inteiroOpcional("tolerancia", "Tolerância", 0, 255, 0)
            .booleanoOpcional("ignorar", "Ignorar transparentes", true)
            .comoMapa()
            .get("properties");

      assertEquals(Map.of("type", "string", "description", "Nome do arquivo"),
                   propriedades.get("arquivo"));
      assertEquals(Map.of("type", "integer", "description", "Tolerância",
                          "minimum", 0, "maximum", 255, "default", 0),
                   propriedades.get("tolerancia"));
      assertEquals(Map.of("type", "boolean",
                          "description", "Ignorar transparentes",
                          "default", true),
                   propriedades.get("ignorar"));
      assertFalse(((Map<?, ?>) propriedades.get("arquivo"))
                                                 .containsKey("default"));
   }

   @Test
   void todoSchemaProibePropriedadesExtras() {
      assertEquals(false, InputSchema.novo().texto("a", "A").comoMapa()
                                     .get("additionalProperties"));
   }
}
