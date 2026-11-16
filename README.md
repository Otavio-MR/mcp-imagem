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

* **Limites:** arquivo de até **20 MB** e imagem de até **25 milhões de
  pixels** (largura x altura). As dimensões são lidas do cabeçalho da imagem
  **antes** de decodificá-la, para não estourar a memória; acima do limite o
  erro é `IMAGEM_MUITO_GRANDE`. Em GIF animado vale só o primeiro quadro. O
  formato é reconhecido pelo conteúdo do arquivo; a extensão só precisa ser
  uma das aceitas.
* **Segurança do nome do arquivo:** são recusados (`PARAMETRO_INVALIDO`) nomes
  com caminho, como `../segredo.png`, `..\segredo.png`, `C:\x.png`,
  `/etc/passwd`, `pasta/a.png`, `.`, `..`, nomes vazios, com `:` ou com
  caractere nulo. O arquivo é sempre procurado direto dentro de `IMG_DIR`.
* **Brilho exato:** o brilho é calculado em aritmética inteira (pesos 2126,
  7152 e 722 sobre 10000), o que dá o resultado exato da fórmula. Com `double`,
  67 dos 256 tons de cinza dariam um brilho levemente diferente do valor (o
  cinza 5 daria 4,999999999999999) e um pixel poderia cair do lado errado de
  um limiar.
* **Tolerância de cor:** é a diferença máxima permitida **por canal** (vermelho,
  verde e azul, cada um comparado separadamente, sem somar). Com tolerância 1,
  `#FE0000` conta como `#FF0000`. O alfa **não** entra na comparação de cor; a
  transparência se controla com `ignorar_transparentes`.

> **Tons de cinza (comportamento observado do Java).** Uma imagem em tons de
> cinza de 8 ou 16 bits (`TYPE_BYTE_GRAY`, comum em PNG, BMP e JPG de uma só
> banda) devolve em `BufferedImage.getRGB` valores **mais claros** que a mesma
> imagem salva em RGB, porque o Java trata o cinza como linear e converte para
> sRGB. Medido com o JDK 25 (amostra de cinza gravada, depois lida):
>
> | Amostra gravada | Cinza em RGB (r=g=b) | Cinza de 8 bits (`getRGB`) |
> |---|---|---|
> | 0 | `#000000` | `#000000` |
> | 64 | `#404040` | `#898989` |
> | 128 | `#808080` | `#BCBCBC` |
> | 192 | `#C0C0C0` | `#E1E1E1` |
> | 255 | `#FFFFFF` | `#FFFFFF` |
>
> Como todas as análises seguem a convenção de `getRGB`, uma imagem em tons de
> cinza de 8 bits será medida **mais clara** do que aparece em um visualizador
> de imagens. Para medidas fiéis, salve a imagem como RGB (por exemplo, no
> modo "RGB" do seu editor) antes de copiá-la para a pasta. Um teste fixa esse
> comportamento, para que uma mudança do JDK seja notada.

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

**Mensagem de `IMAGEM_NAO_ENCONTRADA`.** Ela é didática: informa a pasta
configurada e as imagens que existem nela (até 20 nomes; havendo mais, termina
com "e mais N"), para que a IA oriente o usuário em vez de só falhar:

```
IMAGEM_NAO_ENCONTRADA: 'foto.png' não está na pasta 'C:\imagens'. Imagens anexadas à conversa não são visíveis ao servidor: copie o arquivo para essa pasta. Disponíveis: banner.png, icone.png
```

Se a pasta estiver vazia ou não existir, termina com `Disponíveis: nenhuma`. A
existência do arquivo é conferida **antes** do formato: um `foto.webp` que não
está na pasta dá `IMAGEM_NAO_ENCONTRADA`; um `notas.txt` que existe dá
`FORMATO_NAO_SUPORTADO`. Um arquivo que existe mas não é uma imagem de verdade
(texto com extensão `.png`, arquivo vazio ou cortado) dá `IMAGEM_INVALIDA`.

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

### `obter_informacoes`

Informa as medidas exatas de uma imagem: largura, altura, total de pixels,
formato, se tem canal alfa (transparência) e o tamanho do arquivo.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |

Exemplo de chamada: `{"arquivo": "banner.png"}`

```json
{
  "arquivo" : "banner.png",
  "formato" : "PNG",
  "largura" : 40,
  "altura" : 20,
  "totalPixels" : 800,
  "temCanalAlfa" : true,
  "tamanhoBytes" : 125,
  "tamanhoKb" : 1
}
```

`tamanhoKb` é arredondado para cima. `temCanalAlfa` vale `true` para PNG e GIF
gravados com canal alfa, e `false` para JPG, BMP e PNG sem transparência.

Erros específicos:

| Código | Causa |
|---|---|
| `IMAGEM_NAO_ENCONTRADA` | o arquivo não existe na pasta (a mensagem lista as imagens disponíveis) |
| `FORMATO_NAO_SUPORTADO` | o arquivo existe, mas a extensão não é PNG, JPG/JPEG, GIF nem BMP |
| `IMAGEM_INVALIDA` | o arquivo tem extensão de imagem, mas está corrompido, vazio ou não é uma imagem |
| `IMAGEM_MUITO_GRANDE` | o arquivo passa de 20 MB ou a imagem de 25 milhões de pixels |
| `PARAMETRO_INVALIDO` | `arquivo` em branco ou com caminho (`../x.png`, `C:\x.png`, `/etc/passwd`) |
| `PARAMETRO_AUSENTE` | falta `arquivo` |

---

### `contar_pixels_da_cor`

Conta exatamente quantos pixels da imagem têm uma cor e o percentual sobre o
total considerado. A comparação é feita nos canais vermelho, verde e azul (o
alfa não entra), com uma tolerância opcional por canal.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |
| `cor` | string | sim | `#RRGGBB`, `#RGB` ou nome (ex.: `vermelho`) |
| `tolerancia` | inteiro 0 a 255 | não (padrão 0) | diferença máxima permitida em cada canal; 0 = só a cor exata |
| `ignorar_transparentes` | booleano | não (padrão `true`) | se `true`, pixels com alfa 0 não entram na contagem nem no total |

Exemplo de chamada: `{"arquivo": "vermelho-branco.png", "cor": "vermelho"}`

```json
{
  "arquivo" : "vermelho-branco.png",
  "cor" : "#FF0000",
  "tolerancia" : 0,
  "ignorarTransparentes" : true,
  "quantidade" : 30,
  "totalConsiderado" : 100,
  "percentual" : 30.00
}
```

`percentual` tem 2 casas decimais. Se todos os pixels forem transparentes e
`ignorar_transparentes` for `true`, o total considerado é 0 e o percentual
também (sem erro).

Erros específicos:

| Código | Causa |
|---|---|
| `COR_INVALIDA` | `cor` não é hexadecimal válido nem um nome conhecido (a mensagem cita o parâmetro `cor`) |
| `PARAMETRO_INVALIDO` | `tolerancia` fora de 0 a 255 ou não inteira, `ignorar_transparentes` não booleano, ou `arquivo` inválido |
| `PARAMETRO_AUSENTE` | falta `arquivo` ou `cor` |
| `IMAGEM_NAO_ENCONTRADA`, `FORMATO_NAO_SUPORTADO`, `IMAGEM_INVALIDA`, `IMAGEM_MUITO_GRANDE` | iguais aos de `obter_informacoes` |

Erros nos parâmetros são verificados antes de abrir a imagem.

---

### `cores_dominantes`

Lista as cores que mais aparecem na imagem, com a quantidade exata de pixels e
o percentual de cada uma. Cores parecidas são agrupadas: cada canal (vermelho,
verde, azul) é dividido em faixas de `passo` valores, e pixels cujos três
canais caem nas mesmas faixas formam um grupo.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |
| `quantidade` | inteiro 1 a 20 | não (padrão 5) | quantas cores devolver |
| `passo` | inteiro 1 a 128 | não (padrão 32) | tamanho da faixa de cada canal; 1 = cores exatas, maior = grupos mais amplos |
| `ignorar_transparentes` | booleano | não (padrão `true`) | se `true`, pixels com alfa 0 não entram na conta |

Exemplo de chamada: `{"arquivo": "vermelho-branco.png", "quantidade": 2, "passo": 1}`

```json
{
  "arquivo" : "vermelho-branco.png",
  "passo" : 1,
  "ignorarTransparentes" : true,
  "totalConsiderado" : 100,
  "gruposDistintos" : 2,
  "cores" : [ {
    "cor" : "#FFFFFF",
    "quantidade" : 70,
    "percentual" : 70.00
  }, {
    "cor" : "#FF0000",
    "quantidade" : 30,
    "percentual" : 30.00
  } ]
}
```

Como ler o resultado:

- Com `passo` maior que 1, a `cor` de cada grupo é o **centro da faixa** em
  cada canal (no máximo 255), e não uma cor que exista na imagem. Por exemplo,
  com passo 32 o branco aparece como `#F0F0F0`. Use `passo` 1 para obter as
  cores exatas.
- `gruposDistintos` conta todos os grupos da imagem, mesmo os que não couberam
  em `quantidade`.
- Os grupos vêm do mais frequente para o menos frequente; em empate, vem
  primeiro a cor de menor valor hexadecimal.
- Se a imagem não tem pixels considerados (toda transparente), `cores` vem
  vazia e `totalConsiderado` é 0.

Erros específicos:

| Código | Causa |
|---|---|
| `PARAMETRO_INVALIDO` | `quantidade` fora de 1 a 20, `passo` fora de 1 a 128, valor não inteiro, `ignorar_transparentes` não booleano ou `arquivo` inválido |
| `PARAMETRO_AUSENTE` | falta `arquivo` |
| `IMAGEM_NAO_ENCONTRADA`, `FORMATO_NAO_SUPORTADO`, `IMAGEM_INVALIDA`, `IMAGEM_MUITO_GRANDE` | iguais aos de `obter_informacoes` |

---

### `proporcao_claro_escuro`

Conta quantos pixels da imagem são claros e quantos são escuros, com o
percentual de cada lado. Um pixel é **claro** quando o seu brilho (luma, de 0 a
255, calculado como 0,2126 R + 0,7152 G + 0,0722 B) é **maior ou igual** ao
limiar; senão é **escuro**. O brilho usa só as cores: a transparência não
entra na conta.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |
| `limiar` | inteiro 0 a 255 | não (padrão 128) | brilho mínimo para ser considerado claro |
| `ignorar_transparentes` | booleano | não (padrão `true`) | se `true`, pixels com alfa 0 não entram na conta |

Exemplo de chamada: `{"arquivo": "vermelho-branco.png"}`

```json
{
  "arquivo" : "vermelho-branco.png",
  "limiar" : 128,
  "ignorarTransparentes" : true,
  "totalConsiderado" : 100,
  "claros" : {
    "quantidade" : 70,
    "percentual" : 70.00
  },
  "escuros" : {
    "quantidade" : 30,
    "percentual" : 30.00
  }
}
```

O vermelho puro tem brilho 54 (escuro) e o branco 255 (claro). `claros` mais
`escuros` sempre dá `totalConsiderado`. Com `ignorar_transparentes` igual a
`false`, um pixel transparente `#00000000` conta como escuro (brilho 0).

Erros específicos:

| Código | Causa |
|---|---|
| `PARAMETRO_INVALIDO` | `limiar` fora de 0 a 255 ou não inteiro, `ignorar_transparentes` não booleano ou `arquivo` inválido |
| `PARAMETRO_AUSENTE` | falta `arquivo` |
| `IMAGEM_NAO_ENCONTRADA`, `FORMATO_NAO_SUPORTADO`, `IMAGEM_INVALIDA`, `IMAGEM_MUITO_GRANDE` | iguais aos de `obter_informacoes` |

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
