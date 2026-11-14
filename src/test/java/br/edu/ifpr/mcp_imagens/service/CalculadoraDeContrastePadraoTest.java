package br.edu.ifpr.mcp_imagens.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_imagens.domain.model.Cor;
import br.edu.ifpr.mcp_imagens.domain.model.CriterioWcag;
import br.edu.ifpr.mcp_imagens.domain.model.ResultadoContraste;
import br.edu.ifpr.mcp_imagens.domain.model.Veredito;
import br.edu.ifpr.mcp_imagens.infra.config.CarregadorDeRecursos;
import br.edu.ifpr.mcp_imagens.infra.config.TabelaWcag;

class CalculadoraDeContrastePadraoTest {
   private static final CalculadoraDeContraste CALCULADORA =
         new CalculadoraDeContrastePadrao(new CarregadorDeRecursos()
               .objeto("criterios-wcag", TabelaWcag.class).criterios());

   private static final Cor BRANCO = new Cor(255, 255, 255);
   private static final Cor PRETO  = new Cor(0, 0, 0);

   private static ResultadoContraste contraste(String texto, Cor fundo) {
      return CALCULADORA.calcular(Cor.deHex(texto), fundo);
   }

   // aa_texto_normal, aa_texto_grande, aaa_texto_normal, aaa_texto_grande,
   // componentes_de_interface
   private static List<Boolean> passa(ResultadoContraste resultado) {
      return resultado.vereditos().stream().map(Veredito::passa).toList();
   }

   @Test
   void pretoSobreBrancoEh21() {
      ResultadoContraste resultado = CALCULADORA.calcular(PRETO, BRANCO);

      assertEquals("21.00", resultado.razao().toString());
      assertEquals(List.of(true, true, true, true, true), passa(resultado));
   }

   @Test
   void corIgualAElaMesmaEh1() {
      ResultadoContraste resultado = contraste("#336699",
                                               Cor.deHex("#336699"));

      assertEquals("1.00", resultado.razao().toString());
      assertEquals(List.of(false, false, false, false, false),
                   passa(resultado));
   }

   @Test
   void cinza767676SobreBrancoEh4_54EPassaAANormal() {
      ResultadoContraste resultado = contraste("#767676", BRANCO);

      assertEquals(4.54, resultado.razao().doubleValue(), 0.01);
      assertEquals(List.of(true, true, false, true, true), passa(resultado));
   }

   @Test
   void cinza777777SobreBrancoEh4_48EFalhaAANormalMasPassaAAGrande() {
      ResultadoContraste resultado = contraste("#777777", BRANCO);

      assertEquals(4.48, resultado.razao().doubleValue(), 0.01);
      assertEquals(List.of(false, true, false, false, true), passa(resultado));
   }

   @Test
   void vermelhoSobreBrancoEh4_00() {
      ResultadoContraste resultado = contraste("#FF0000", BRANCO);

      assertEquals(4.00, resultado.razao().doubleValue(), 0.01);
      // falha AA normal; passa AA grande e componentes de interface
      assertEquals(List.of(false, true, false, false, true), passa(resultado));
   }

   @Test
   void outrosValoresCalculadosAMao() {
      // L(#FF0000) = 0,2126: (0,2126 + 0,05) / 0,05 = 5,252
      assertEquals(5.25, contraste("#000000", Cor.deHex("#FF0000"))
                               .razao().doubleValue(), 0.005);
      // L(#0000FF) = 0,0722: 1,05 / (0,0722 + 0,05) = 8,5925
      assertEquals(8.59, contraste("#FFFFFF", Cor.deHex("#0000FF"))
                               .razao().doubleValue(), 0.005);
   }

   @Test
   void razaoEhSimetrica() {
      ResultadoContraste a = contraste("#777777", BRANCO);
      ResultadoContraste b = CALCULADORA.calcular(BRANCO, Cor.deHex("#777777"));

      assertEquals(a.razao(), b.razao());
      assertEquals(passa(a), passa(b));
   }

   @Test
   void razaoSempreEntre1E21() {
      for (int r = 0; r <= 255; r += 51)
         for (int g = 0; g <= 255; g += 51)
            for (int b = 0; b <= 255; b += 51) {
               BigDecimal razao = CALCULADORA.calcular(new Cor(r, g, b),
                                                       BRANCO).razao();

               assertTrue(razao.doubleValue() >= 1.0, razao.toString());
               assertTrue(razao.doubleValue() <= 21.0, razao.toString());
            }
   }

   @Test
   void devolveAsCoresComoHexEmMaiusculas() {
      ResultadoContraste resultado = contraste("#abc", Cor.deHex("#fff"));

      assertEquals("#AABBCC", resultado.corTexto());
      assertEquals("#FFFFFF", resultado.corFundo());
   }

   @Test
   void alfaDasCoresNaoAfetaOContraste() {
      assertEquals(CALCULADORA.calcular(new Cor(10, 20, 30, 255), BRANCO)
                              .razao(),
                   CALCULADORA.calcular(new Cor(10, 20, 30, 0), BRANCO)
                              .razao());
   }

   @Test
   void vereditosTrazemIdNomeELimiarDosCriterios() {
      List<Veredito> vereditos = contraste("#000000", BRANCO).vereditos();

      assertEquals(5, vereditos.size());
      assertEquals("aa_texto_normal", vereditos.get(0).criterio());
      assertEquals("AA texto normal", vereditos.get(0).nome());
      assertEquals(4.5, vereditos.get(0).limiar());
      assertEquals("componentes_de_interface", vereditos.get(4).criterio());
      assertEquals(3.0, vereditos.get(4).limiar());
   }

   @Test
   void vereditoUsaARazaoSemArredondar() {
      // #FF0000 sobre branco vale 3,9985: aparece como 4,00, mas não atinge
      // um limiar de 4,0
      CalculadoraDeContraste exigente = new CalculadoraDeContrastePadrao(
            List.of(new CriterioWcag("quatro", "Quatro", 4.0, "teste")));

      ResultadoContraste resultado = exigente.calcular(Cor.deHex("#FF0000"),
                                                       BRANCO);

      assertEquals("4.00", resultado.razao().toString());
      assertFalse(resultado.vereditos().getFirst().passa());
   }

   @Test
   void semCriteriosNaoHaVereditos() {
      ResultadoContraste resultado = new CalculadoraDeContrastePadrao(
            List.of()).calcular(PRETO, BRANCO);

      assertTrue(resultado.vereditos().isEmpty());
      assertEquals("21.00", resultado.razao().toString());
   }
}
