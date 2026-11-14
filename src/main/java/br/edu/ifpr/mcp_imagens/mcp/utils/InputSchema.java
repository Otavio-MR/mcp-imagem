package br.edu.ifpr.mcp_imagens.mcp.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Monta o JSON Schema de entrada de uma ferramenta (padrão Builder).
 * <p>
 * Evita repetir em cada ferramenta os {@code Map.of(...)} aninhados do
 * exemplo da disciplina e garante que todo schema tenha
 * {@code "additionalProperties": false} e a lista {@code required} correta.
 * Só há parâmetros de tipos simples (texto, inteiro e booleano), que são os
 * que o cliente interativo sabe perguntar.
 */
public final class InputSchema {
   private final Map<String, Object> propriedades = new LinkedHashMap<>();
   private final List<String>        obrigatorios = new ArrayList<>();

   private InputSchema() {}

   /** Inicia um schema sem parâmetros. */
   public static InputSchema novo() {
      return new InputSchema();
   }

   /** Acrescenta um parâmetro texto obrigatório. */
   public InputSchema texto(String nome, String descricao) {
      return adicionar(nome, propriedade("string", descricao), true);
   }

   /** Acrescenta um parâmetro inteiro opcional, limitado, com valor padrão. */
   public InputSchema inteiroOpcional(String nome, String descricao,
                                      int minimo, int maximo, int padrao) {
      Map<String, Object> propriedade = propriedade("integer", descricao);

      propriedade.put("minimum", minimo);
      propriedade.put("maximum", maximo);
      propriedade.put("default", padrao);

      return adicionar(nome, propriedade, false);
   }

   /** Acrescenta um parâmetro booleano opcional, com valor padrão. */
   public InputSchema booleanoOpcional(String nome, String descricao,
                                       boolean padrao) {
      Map<String, Object> propriedade = propriedade("boolean", descricao);

      propriedade.put("default", padrao);

      return adicionar(nome, propriedade, false);
   }

   /** Schema no formato esperado por {@code Tool.builder(nome, schema)}. */
   public Map<String, Object> comoMapa() {
      Map<String, Object> schema = new LinkedHashMap<>();

      schema.put("type", "object");
      schema.put("properties", propriedades);
      schema.put("required", obrigatorios);
      schema.put("additionalProperties", false);

      return schema;
   }

   /** Nomes de todos os parâmetros declarados. */
   public Set<String> nomesDosParametros() {
      return propriedades.keySet();
   }

   private InputSchema adicionar(String nome, Map<String, Object> propriedade,
                                 boolean obrigatorio) {
      propriedades.put(nome, propriedade);
      if (obrigatorio)
         obrigatorios.add(nome);

      return this;
   }

   private static Map<String, Object> propriedade(String tipo,
                                                  String descricao) {
      Map<String, Object> propriedade = new LinkedHashMap<>();

      propriedade.put("type", tipo);
      propriedade.put("description", descricao);

      return propriedade;
   }
}
