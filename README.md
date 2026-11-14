# MCP Imagens: análise de imagens e acessibilidade de cores

Servidor e cliente MCP (Model Context Protocol) em Java para **medir** imagens
e cores com exatidão. Modelos de IA enxergam imagens, mas **estimam**
quantidades e cores: não contam pixels nem calculam contraste com precisão. O
servidor faz essas medições de forma exata e determinística, e a IA usa os
números para responder e recomendar. É voltado a pessoas de design, front-end
e UX que querem checar imagens, paletas e contraste (critérios WCAG).

O servidor **não chama nenhuma IA**: ele mede e calcula. A interpretação ("isso
é um problema de acessibilidade?") fica com a IA.

> **Status:** em construção. Este arquivo documenta, por enquanto, o
> esqueleto e as duas primeiras ferramentas (`listar_imagens` e
> `calcular_contraste`). As demais ferramentas, resources, prompts, o cliente
> refatorado, o Claude Desktop e o MCP Inspector serão acrescentados nas
> próximas fases.

## Convenções

Valem para todas as ferramentas que analisam imagens:

* **Pixels:** todas as análises usam o valor ARGB em sRGB devolvido por
  `BufferedImage.getRGB`.
* **Transparência:** **transparente** é alfa 0; **semitransparente** é alfa de
  1 a 254; **opaco** é alfa 255. Por padrão, as análises de cor **ignoram os
  pixels totalmente transparentes** (parâmetro `ignorar_transparentes`, padrão
  `true`).
* **Brilho (luma)** de um pixel: `0,2126*R + 0,7152*G + 0,0722*B`, de 0 a 255,
  sobre os componentes sRGB, **sem linearizar**.
* **Percentuais e razões:** 2 casas decimais, com arredondamento `HALF_UP`
  (0,005 vira 0,01). No JSON aparecem com as duas casas (por exemplo, `21.00`).
* **Cores** aceitas: `#RRGGBB`, `#RGB` ou um nome em português do dicionário
  (por exemplo `vermelho`, `azul-marinho`, `cinza-claro`), sem diferenciar
  maiúsculas nem acentos. Os nomes ficam em
  `src/main/resources/imagens/cores-nomeadas.json`; os 16 primeiros seguem as
  cores básicas do CSS. Para precisão, use o hexadecimal.
* **Arquivos:** as ferramentas recebem só o **nome do arquivo**, nunca um
  caminho. As imagens ficam na pasta indicada pela variável de ambiente
  `IMG_DIR` (padrão: `./imagens`). Formatos aceitos: PNG, JPG/JPEG, GIF e BMP.

> **Importante:** o servidor lê as imagens **da pasta configurada**, não os
> anexos da conversa. Para analisar uma imagem, copie-a para a pasta `IMG_DIR`
> e cite o nome do arquivo.

## Ferramentas do servidor

Todas as ferramentas:

* respondem, em caso de sucesso, com um texto em **JSON**;
* respondem, em caso de erro, com `isError = true` e o texto
  `CODIGO: mensagem`;
* rejeitam parâmetros que não estejam no schema (`PARAMETRO_INVALIDO`);
* usam só parâmetros de tipos simples (texto, inteiro e booleano).

### Códigos de erro

| Código | Quando acontece |
|---|---|
| `PARAMETRO_AUSENTE` | falta um parâmetro obrigatório |
| `PARAMETRO_INVALIDO` | parâmetro com tipo errado, vazio, fora dos limites, desconhecido, ou nome de arquivo com caminho |
| `IMAGEM_NAO_ENCONTRADA` | o arquivo não existe na pasta de imagens |
| `FORMATO_NAO_SUPORTADO` | a extensão não é PNG, JPG/JPEG, GIF nem BMP |
| `IMAGEM_INVALIDA` | o arquivo está corrompido ou não pode ser decodificado |
| `IMAGEM_MUITO_GRANDE` | o arquivo passa de 20 MB ou a imagem de 25 milhões de pixels |
| `DIMENSOES_DIFERENTES` | duas imagens que deveriam ter o mesmo tamanho têm dimensões diferentes |
| `COR_INVALIDA` | cor com formato errado, nome desconhecido ou componente fora de 0 a 255 |
| `ERRO_INTERNO` | falha inesperada no servidor |

Nas tabelas de cada ferramenta, "erros específicos" lista só o que é próprio
dela.

---

### `listar_imagens`

Lista as imagens disponíveis na pasta de imagens. **Use primeiro**, para
descobrir o nome exato do arquivo pedido pelas demais ferramentas.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| (nenhum) | | | |

Resultado: `pasta` (caminho absoluto da pasta configurada), `total` e
`imagens` (nome, formato e tamanho em KB, arredondado para cima), em ordem
alfabética. Arquivos de outros tipos não aparecem. Pasta inexistente ou vazia
**não é erro**: volta lista vazia.

```json
{
  "pasta" : "C:\\imagens",
  "total" : 2,
  "imagens" : [ {
    "nome" : "banner.png",
    "formato" : "PNG",
    "tamanhoKb" : 1
  }, {
    "nome" : "icone.png",
    "formato" : "PNG",
    "tamanhoKb" : 1
  } ]
}
```

Erros específicos: nenhum (só `PARAMETRO_INVALIDO` se algum parâmetro for
enviado).

---

### `calcular_contraste`

Calcula a razão de contraste entre a cor do texto e a do fundo pela fórmula do
**WCAG 2.x**, e diz se passa em cada critério. **Não usa arquivo de imagem.**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `cor_texto` | string | sim | cor do texto: `#RRGGBB`, `#RGB` ou nome |
| `cor_fundo` | string | sim | cor do fundo: `#RRGGBB`, `#RGB` ou nome |

**Fórmula.** Para cada componente `c` de 0 a 255: `s = c/255`; `lin = s/12,92`
se `s <= 0,03928`, senão `((s + 0,055)/1,055)^2,4`. A luminância relativa é
`L = 0,2126*R_lin + 0,7152*G_lin + 0,0722*B_lin`. A razão é
`(L_clara + 0,05) / (L_escura + 0,05)`, sempre entre 1 e 21, e é simétrica:
trocar texto e fundo dá o mesmo valor.

**Critérios** (em `src/main/resources/imagens/criterios-wcag.json`):

| Critério | Limiar |
|---|---|
| `aa_texto_normal` | 4,5 |
| `aa_texto_grande` | 3,0 |
| `aaa_texto_normal` | 7,0 |
| `aaa_texto_grande` | 4,5 |
| `componentes_de_interface` | 3,0 |

Os vereditos usam a razão **sem arredondar**, como pede o WCAG: 4,499 não
atinge 4,5, mesmo que apareça como 4,50.

Exemplo de chamada: `{"cor_texto": "#777777", "cor_fundo": "branco"}`

```json
{
  "corTexto" : "#777777",
  "corFundo" : "#FFFFFF",
  "razao" : 4.48,
  "vereditos" : [ {
    "criterio" : "aa_texto_normal",
    "nome" : "AA texto normal",
    "limiar" : 4.5,
    "passa" : false
  }, {
    "criterio" : "aa_texto_grande",
    "nome" : "AA texto grande",
    "limiar" : 3.0,
    "passa" : true
  } ]
}
```

(Exemplo abreviado: a resposta real traz os 5 critérios.)

Valores de referência conferidos nos testes: preto sobre branco = 21,00; cor
igual a ela mesma = 1,00; `#767676` sobre branco = 4,54 (passa AA normal);
`#777777` sobre branco = 4,48 (falha AA normal, passa AA grande); `#FF0000`
sobre branco = 4,00 (falha AA normal, passa AA grande e componentes de
interface).

Erros específicos:

| Código | Causa |
|---|---|
| `COR_INVALIDA` | cor com formato errado ou nome desconhecido; a mensagem indica qual parâmetro (`parâmetro 'cor_texto': ...`) |
| `PARAMETRO_AUSENTE` | falta `cor_texto` ou `cor_fundo` |
| `PARAMETRO_INVALIDO` | cor em branco, ou parâmetro que não existe (por exemplo `arquivo`) |

---

## Pré-requisitos

* JDK 25 e Apache Maven 3.9 ou superior.
* Docker Desktop, apenas para testar no MCP Inspector (ver as próximas fases).

## Builds

| Comando | O que faz |
|---|---|
| `mvn test` | executa os testes unitários |
| `mvn clean verify -Pserver` | gera `server/mcp_imagens_server.jar` (com `server/libs`) e executa os testes unitários e os de integração |
| `mvn clean package -Pserver -DskipTests` | só gera o servidor |
| `mvn clean package -Pclient -DskipTests` | gera `target/dist-client/mcp_imagens_client.jar` (com `libs`) |

> Gerar o servidor executa `clean` e apaga o `target`, inclusive o cliente. Por
> isso o servidor vai primeiro e o cliente depois.

Os testes de integração geram as próprias imagens, com o `ImageIO`, em uma
pasta temporária, e sobem o JAR do servidor com `IMG_DIR` apontando para ela.
