package br.edu.ifpr.mcp_imagens.domain.criterio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;

class ForaDaPaletaTest {
   private static final List<Cor> PALETA = List.of(new Cor(255, 0, 0),
                                                   new Cor(255, 255, 255));

   @Test
   void corDaPaletaNaoEstaFora() {
      CriterioDePixel fora = new ForaDaPaleta(PALETA, 0);

      assertFalse(fora.aceita(0xFFFF0000));
      assertFalse(fora.aceita(0xFFFFFFFF));
   }

   @Test
   void corQueNaoEstaNaPaletaEstaFora() {
      CriterioDePixel fora = new ForaDaPaleta(PALETA, 0);

      assertTrue(fora.aceita(0xFF0000FF));
      assertTrue(fora.aceita(0xFF000000));
   }

   @Test
   void toleranciaTrazParaDentroAsCoresVizinhas() {
      assertTrue(new ForaDaPaleta(PALETA, 0).aceita(0xFFFE0000));
      assertFalse(new ForaDaPaleta(PALETA, 1).aceita(0xFFFE0000));
      assertTrue(new ForaDaPaleta(PALETA, 1).aceita(0xFFFC0000));
   }

   @Test
   void umaCorSoNaPaleta() {
      CriterioDePixel fora = new ForaDaPaleta(List.of(new Cor(0, 0, 255)), 0);

      assertFalse(fora.aceita(0xFF0000FF));
      assertTrue(fora.aceita(0xFFFFFFFF));
   }

   @Test
   void oAlfaNaoEComparado() {
      assertFalse(new ForaDaPaleta(PALETA, 0).aceita(0x80FF0000));
   }

   @Test
   void paletaVaziaEhRecusada() {
      ParametroInvalidoException ex = assertThrows(
            ParametroInvalidoException.class,
            () -> new ForaDaPaleta(List.of(), 0));

      assertTrue(ex.getMessage().contains("pelo menos uma cor"));
   }

   @Test
   void toleranciaForaDaFaixaEhRecusada() {
      assertThrows(ParametroInvalidoException.class,
                   () -> new ForaDaPaleta(PALETA, -1));
      assertThrows(ParametroInvalidoException.class,
                   () -> new ForaDaPaleta(PALETA, 256));
   }

   @Test
   void alterarAListaOriginalNaoMudaOCriterio() {
      ArrayList<Cor>  lista = new ArrayList<>(PALETA);
      CriterioDePixel fora  = new ForaDaPaleta(lista, 0);

      lista.add(new Cor(0, 0, 255));

      assertTrue(fora.aceita(0xFF0000FF));
   }
}
