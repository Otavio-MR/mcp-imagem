package br.edu.ifpr.mcp_imagens.infra.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import tools.jackson.databind.json.JsonMapper;

/**
 * Lê os dicionários guardados em {@code src/main/resources/imagens} (arquivos
 * JSON), para que tabelas como a de cores nomeadas não fiquem dentro das
 * classes.
 */
public class CarregadorDeRecursos {
   private static final String PASTA = "/imagens/";

   private final JsonMapper json = new JsonMapper();

   /**
    * Lê um dicionário para um record.
    *
    * @param nome  nome do arquivo sem extensão (p.ex. "cores-nomeadas")
    * @param tipo  classe do record que recebe os dados
    * @throws IllegalStateException se o arquivo não existir
    */
   public <T> T objeto(String nome, Class<T> tipo) {
      try (InputStream entrada = abrir(nome)) {
         return json.readValue(entrada, tipo);
      }
      catch (IOException ex) {
         throw new UncheckedIOException("falha ao ler o dicionário " + nome, ex);
      }
   }

   private InputStream abrir(String nome) {
      String      recurso = PASTA + nome + ".json";
      InputStream entrada = getClass().getResourceAsStream(recurso);

      if (entrada == null)
         throw new IllegalStateException("dicionário não encontrado: " +
                                         recurso);
      return entrada;
   }
}
