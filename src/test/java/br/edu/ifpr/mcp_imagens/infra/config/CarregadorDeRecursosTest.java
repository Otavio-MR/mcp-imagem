package br.edu.ifpr.mcp_imagens.infra.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.Normalizer;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.CriterioWcag;

/**
 * Lê os dicionários reais de {@code src/main/resources/imagens} e confere a
 * qualidade deles: cores com hex válido e nomes sem acento, e a tabela WCAG
 * com todos os critérios.
 */
class CarregadorDeRecursosTest {
   private static final CarregadorDeRecursos CARREGADOR =
                                                     new CarregadorDeRecursos();

   @Test
   void dicionarioInexistenteEhErro() {
      IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> CARREGADOR.objeto("nao-existe", CoresNomeadas.class));

      assertTrue(ex.getMessage().contains("nao-existe"));
   }

   @Nested
   class Cores {
      private final CoresNomeadas cores = CARREGADOR.objeto("cores-nomeadas",
                                                            CoresNomeadas.class);

      @Test
      void temAsDezesseisCoresBasicasEAlgumasExtras() {
         assertTrue(cores.cores().size() >= 20, "só " + cores.cores().size());
         assertEquals("#FF0000", cores.cores().get("vermelho"));
         assertEquals("#FFFFFF", cores.cores().get("branco"));
         assertEquals("#000000", cores.cores().get("preto"));
      }

      @Test
      void todoValorEhUmHexValido() {
         cores.cores().forEach((nome, hex) -> assertEquals(
               hex.toUpperCase(), Cor.deHex(hex).hex(),
               "hex inválido ou fora do padrão: " + nome));
      }

      @Test
      void nomesSemAcentoEmMinusculasESemEspaco() {
         cores.cores().keySet().forEach(nome -> {
            String simples = Normalizer.normalize(nome, Normalizer.Form.NFD)
                                       .replaceAll("\\p{M}", "")
                                       .toLowerCase()
                                       .replace(' ', '-');

            assertEquals(simples, nome, "nome fora do padrão: " + nome);
         });
      }

      @Test
      void trazObservacaoExplicativa() {
         assertFalse(cores.observacao().isBlank());
      }

      @Test
      void oParserAceitaTodosOsNomesDoDicionario() {
         CorParser parser = new CorParser(cores.cores());

         cores.cores().keySet().forEach(nome -> assertEquals(
               Cor.deHex(cores.cores().get(nome)), parser.parse(nome)));
      }
   }

   @Nested
   class Wcag {
      private final TabelaWcag tabela = CARREGADOR.objeto("criterios-wcag",
                                                          TabelaWcag.class);

      @Test
      void temOsCincoCriteriosNaOrdemDaEspecificacao() {
         assertEquals(List.of("aa_texto_normal", "aa_texto_grande",
                              "aaa_texto_normal", "aaa_texto_grande",
                              "componentes_de_interface"),
                      tabela.criterios().stream().map(CriterioWcag::id)
                            .toList());
      }

      @Test
      void limiaresSaoOsDoWcag() {
         assertEquals(List.of(4.5, 3.0, 7.0, 4.5, 3.0),
                      tabela.criterios().stream().map(CriterioWcag::limiar)
                            .toList());
      }

      @Test
      void todoCriterioTemNomeEDescricao() {
         tabela.criterios().forEach(criterio -> {
            assertFalse(criterio.nome().isBlank(), criterio.id());
            assertFalse(criterio.descricao().isBlank(), criterio.id());
         });
      }

      @Test
      void citaAFonteWcag() {
         assertTrue(tabela.fonte().contains("WCAG"));
      }
   }
}
