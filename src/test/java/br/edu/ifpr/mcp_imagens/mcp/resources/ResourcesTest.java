package br.edu.ifpr.mcp_imagens.mcp.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.exception.ImagemInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.model.CriterioWcag;
import br.edu.ifpr.mcp_imagens.domain.model.InformacoesImagem;
import br.edu.ifpr.mcp_imagens.domain.model.ListaDeImagens;
import br.edu.ifpr.mcp_imagens.infra.config.CarregadorDeRecursos;
import br.edu.ifpr.mcp_imagens.infra.config.CoresNomeadas;
import br.edu.ifpr.mcp_imagens.infra.config.TabelaWcag;
import br.edu.ifpr.mcp_imagens.service.InformacoesService;
import br.edu.ifpr.mcp_imagens.service.InformacoesServicePadrao;
import br.edu.ifpr.mcp_imagens.testutil.ImagensSinteticas;
import br.edu.ifpr.mcp_imagens.testutil.RepositorioFalso;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ResourcesTest {
   private static final JsonMapper JSON = new JsonMapper();

   private static ReadResourceResult ler(McpResourceDefinition resource,
                                         String uri) {
      return resource.read(null, new ReadResourceRequest(uri));
   }

   private static TextResourceContents unico(ReadResourceResult resultado) {
      assertEquals(1, resultado.contents().size());
      return (TextResourceContents) resultado.contents().get(0);
   }

   @Test
   void coresNomeadasDevolveODicionarioComoJson() throws Exception {
      CoresNomeadasResource resource = new CoresNomeadasResource(
            new CoresNomeadas("obs", Map.of("vermelho", "#FF0000")));

      TextResourceContents conteudo = unico(ler(resource,
                                                "imagens://cores-nomeadas"));
      JsonNode json = JSON.readTree(conteudo.text());

      assertEquals("imagens://cores-nomeadas", conteudo.uri());
      assertEquals("application/json", conteudo.mimeType());
      assertEquals("#FF0000", json.get("cores").get("vermelho").asString());
      assertEquals("obs", json.get("observacao").asString());
   }

   @Test
   void coresNomeadasReaisTemAsCoresBasicas() throws Exception {
      CoresNomeadas reais = new CarregadorDeRecursos().objeto(
            "cores-nomeadas", CoresNomeadas.class);

      JsonNode json = JSON.readTree(unico(ler(new CoresNomeadasResource(reais),
            "imagens://cores-nomeadas")).text());

      assertEquals("#FF0000", json.get("cores").get("vermelho").asString());
      assertEquals("#000000", json.get("cores").get("preto").asString());
      assertEquals("#FFFFFF", json.get("cores").get("branco").asString());
   }

   @Test
   void criteriosWcagReaisTemCincoCriteriosComLimiares() throws Exception {
      TabelaWcag reais = new CarregadorDeRecursos().objeto("criterios-wcag",
                                                           TabelaWcag.class);

      TextResourceContents conteudo = unico(ler(new CriteriosWcagResource(reais),
            "imagens://criterios-wcag"));
      JsonNode criterios = JSON.readTree(conteudo.text()).get("criterios");

      assertEquals("imagens://criterios-wcag", conteudo.uri());
      assertEquals(5, criterios.size());
      assertEquals("aa_texto_normal", criterios.get(0).get("id").asString());
      assertEquals(4.5, criterios.get(0).get("limiar").asDouble(), 0);
   }

   @Test
   void criteriosWcagDevolveOQueRecebeu() throws Exception {
      TabelaWcag tabela = new TabelaWcag("fonte", List.of(
            new CriterioWcag("x", "X", 2.5, "descricao")));

      JsonNode json = JSON.readTree(unico(ler(new CriteriosWcagResource(tabela),
            "imagens://criterios-wcag")).text());

      assertEquals("fonte", json.get("fonte").asString());
      assertEquals(2.5, json.get("criterios").get(0).get("limiar").asDouble(),
                   0);
   }

   @Test
   void imagensListaAsImagensDaPasta() throws Exception {
      ImagensResource resource = new ImagensResource(
            new InformacoesServicePadrao(RepositorioFalso.com(
                  ImagensSinteticas.aberta("a.png",
                        ImagensSinteticas.solida(2, 2, 0xFFFFFFFF)),
                  ImagensSinteticas.aberta("b.jpg",
                        ImagensSinteticas.solida(2, 2, 0xFF000000)))));

      JsonNode json = JSON.readTree(unico(ler(resource,
                                              "imagens://imagens")).text());

      assertEquals(2, json.get("total").asInt());
      assertEquals("a.png", json.get("imagens").get(0).get("nome")
                                .asString());
      assertEquals("JPEG", json.get("imagens").get(1).get("formato")
                               .asString());
   }

   @Test
   void imagensComPastaVaziaDevolveListaVazia() throws Exception {
      ImagensResource resource = new ImagensResource(
            new InformacoesServicePadrao(RepositorioFalso.com()));

      JsonNode json = JSON.readTree(unico(ler(resource,
                                              "imagens://imagens")).text());

      assertEquals(0, json.get("total").asInt());
      assertEquals(0, json.get("imagens").size());
   }

   @Test
   void falhaAoListarPropagaComOCodigoDoErro() {
      // serviço que falha ao listar as imagens
      ImagensResource resource = new ImagensResource(new InformacoesService() {
         @Override
         public ListaDeImagens listarImagens() {
            throw new ImagemInvalidaException("ruim");
         }

         @Override
         public InformacoesImagem obter(String arquivo) {
            throw new UnsupportedOperationException();
         }
      });

      Exception e = org.junit.jupiter.api.Assertions.assertThrows(
            ImagemInvalidaException.class,
            () -> ler(resource, "imagens://imagens"));

      assertTrue(e.getMessage().contains("ruim"));
   }
}
