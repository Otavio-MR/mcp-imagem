package br.edu.ifpr.mcp_imagens.infra.config;

import java.nio.file.Path;
import java.util.Map;

/**
 * Configuração da aplicação: pasta de imagens (lida das variáveis de
 * ambiente) e limites de tamanho.
 * <p>
 * O construtor recebe o mapa de variáveis (em vez de ler
 * {@link System#getenv()} diretamente) para que a classe possa ser testada
 * sem alterar o ambiente do processo.
 */
public final class AppConfig {
   /** Variável de ambiente com a pasta onde ficam as imagens. */
   public static final String VARIAVEL_PASTA_IMAGENS = "IMG_DIR";
   /** Pasta usada quando a variável não é definida (relativa à pasta atual). */
   public static final String PASTA_IMAGENS_PADRAO   = "imagens";

   /** Maior arquivo de imagem aceito: 20 MB. */
   public static final long TAMANHO_MAXIMO_BYTES = 20L * 1024 * 1024;
   /** Maior imagem aceita: 25 milhões de pixels (largura x altura). */
   public static final long PIXELS_MAXIMOS       = 25_000_000L;

   private final Path pastaImagens;

   public AppConfig(Map<String, String> ambiente) {
      String pasta = ambiente.get(VARIAVEL_PASTA_IMAGENS);

      if (pasta == null || pasta.isBlank())
         pasta = PASTA_IMAGENS_PADRAO;

      this.pastaImagens = Path.of(pasta.trim()).toAbsolutePath().normalize();
   }

   /** Cria a configuração a partir das variáveis de ambiente do processo. */
   public static AppConfig doAmbiente() {
      return new AppConfig(System.getenv());
   }

   /** Pasta de imagens, sempre como caminho absoluto. */
   public Path pastaImagens() {
      return pastaImagens;
   }

   public long tamanhoMaximoBytes() {
      return TAMANHO_MAXIMO_BYTES;
   }

   public long pixelsMaximos() {
      return PIXELS_MAXIMOS;
   }
}
