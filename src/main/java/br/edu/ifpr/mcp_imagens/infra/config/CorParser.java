package br.edu.ifpr.mcp_imagens.infra.config;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import br.edu.ifpr.mcp_imagens.domain.exception.CorInvalidaException;
import br.edu.ifpr.mcp_imagens.domain.exception.ParametroInvalidoException;
import br.edu.ifpr.mcp_imagens.domain.model.Cor;

/**
 * Interpreta o texto de uma cor recebido de uma ferramenta: {@code #RRGGBB},
 * {@code #RGB} ou o nome de uma cor do dicionário (p.ex. "vermelho"), sem
 * diferenciar maiúsculas nem acentos.
 */
public class CorParser {
   private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
   private static final Pattern ESPACOS = Pattern.compile("\\s+");

   private final Map<String, Cor> porNome = new HashMap<>();

   /**
    * @param nomes  dicionário de nome da cor (sem acentos, minúsculas) para
    *               "#RRGGBB"
    * @throws CorInvalidaException se algum valor do dicionário não for um hex
    *                              válido
    */
   public CorParser(Map<String, String> nomes) {
      nomes.forEach((nome, hex) -> porNome.put(normalizar(nome),
                                               Cor.deHex(hex)));
   }

   /**
    * @throws CorInvalidaException se o texto estiver vazio, não for um hex
    *                              válido nem um nome conhecido
    */
   public Cor parse(String texto) {
      if (texto == null || texto.isBlank())
         throw new CorInvalidaException("a cor não pode ser vazia");

      String cor = texto.strip();

      if (cor.startsWith("#"))
         return Cor.deHex(cor);

      Cor nomeada = porNome.get(normalizar(cor));

      if (nomeada == null)
         throw new CorInvalidaException("cor desconhecida: '" + cor + "'; " +
                                        "use #RRGGBB, #RGB ou um nome como " +
                                        "vermelho, azul-marinho ou cinza-claro");
      return nomeada;
   }

   /**
    * Igual a {@link #parse(String)}, mas a mensagem de erro indica qual
    * parâmetro da ferramenta tem a cor inválida.
    *
    * @param texto      o texto da cor
    * @param parametro  nome do parâmetro de onde veio (p.ex. "cor_texto")
    */
   public Cor parse(String texto, String parametro) {
      try {
         return parse(texto);
      }
      catch (CorInvalidaException ex) {
         throw new CorInvalidaException("parâmetro '" + parametro + "': " +
                                        ex.getMessage());
      }
   }

   /**
    * Interpreta uma lista de cores separadas por vírgula (p.ex.
    * "#FF0000, azul, #0F0"). Cada item segue as regras de
    * {@link #parse(String)}.
    *
    * @param texto      a lista, como recebida da ferramenta
    * @param parametro  nome do parâmetro de onde veio, para as mensagens
    * @param maximo     quantidade máxima de cores aceitas
    * @throws ParametroInvalidoException se a lista tiver um item vazio ou
    *                                    mais cores que o máximo
    * @throws CorInvalidaException       se algum item não for uma cor; a
    *                                    mensagem indica qual item
    */
   public List<Cor> parseLista(String texto, String parametro, int maximo) {
      String[] itens = texto.split(",", -1);

      if (itens.length > maximo)
         throw new ParametroInvalidoException("'" + parametro + "' aceita no " +
                                              "máximo " + maximo + " cores " +
                                              "(recebido: " + itens.length +
                                              ")");

      List<Cor> cores = new ArrayList<>();

      for (int i = 0; i < itens.length; i++) {
         if (itens[i].isBlank())
            throw new ParametroInvalidoException("'" + parametro + "' tem " +
                                                 "um item vazio (item " +
                                                 (i + 1) + ")");
         cores.add(parse(itens[i], parametro + " (item " + (i + 1) + ")"));
      }
      return cores;
   }

   // minúsculas, sem acentos e com espaços trocados por hífen
   private static String normalizar(String nome) {
      String semAcentos = ACENTOS.matcher(
                             Normalizer.normalize(nome, Normalizer.Form.NFD))
                                 .replaceAll("");

      return ESPACOS.matcher(semAcentos.strip().toLowerCase(Locale.ROOT))
                    .replaceAll("-");
   }
}
