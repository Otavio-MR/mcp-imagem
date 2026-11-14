package br.edu.ifpr.mcp_imagens.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExcecoesTest {
   private static final Map<CodigoErro, ImagensException> POR_CODIGO = Map.of(
         CodigoErro.PARAMETRO_AUSENTE,
         new ParametroAusenteException("m"),
         CodigoErro.PARAMETRO_INVALIDO,
         new ParametroInvalidoException("m"),
         CodigoErro.IMAGEM_NAO_ENCONTRADA,
         new ImagemNaoEncontradaException("m"),
         CodigoErro.FORMATO_NAO_SUPORTADO,
         new FormatoNaoSuportadoException("m"),
         CodigoErro.IMAGEM_INVALIDA,
         new ImagemInvalidaException("m"),
         CodigoErro.IMAGEM_MUITO_GRANDE,
         new ImagemMuitoGrandeException("m"),
         CodigoErro.DIMENSOES_DIFERENTES,
         new DimensoesDiferentesException("m"),
         CodigoErro.COR_INVALIDA,
         new CorInvalidaException("m"));

   @Test
   void cadaExcecaoCarregaSeuCodigoEAMensagem() {
      POR_CODIGO.forEach((codigo, excecao) -> {
         assertEquals(codigo, excecao.codigo());
         assertEquals("m", excecao.getMessage());
      });
   }

   @Test
   void todosOsCodigosDeErroDeUsuarioTemUmaExcecao() {
      // ERRO_INTERNO é o único sem exceção própria: vem de falhas inesperadas
      assertEquals(CodigoErro.values().length - 1, POR_CODIGO.size());
   }

   @Test
   void saoFalhasNaoVerificadas() {
      assertInstanceOf(RuntimeException.class,
                       new ParametroInvalidoException("m"));
   }

   @Test
   void nomesDosCodigosSaoOsDocumentados() {
      assertEquals("[PARAMETRO_AUSENTE, PARAMETRO_INVALIDO, " +
                   "IMAGEM_NAO_ENCONTRADA, FORMATO_NAO_SUPORTADO, " +
                   "IMAGEM_INVALIDA, IMAGEM_MUITO_GRANDE, " +
                   "DIMENSOES_DIFERENTES, COR_INVALIDA, ERRO_INTERNO]",
                   Arrays.toString(CodigoErro.values()));
   }
}
