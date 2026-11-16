# MCP Imagens: análise de imagens e acessibilidade de cores

Servidor e cliente MCP (Model Context Protocol) em Java para **medir** imagens
e cores com exatidão. Modelos de IA enxergam imagens, mas **estimam**
quantidades e cores: não contam pixels nem calculam contraste com precisão. O
servidor faz essas medições de forma exata e determinística, e a IA usa os
números para responder e recomendar. É voltado a pessoas de design, front-end
e UX que querem checar imagens, paletas e contraste (critérios WCAG).

O servidor **não chama nenhuma IA**: ele mede e calcula. A interpretação ("isso
é um problema de acessibilidade?") fica com a IA.

> **Status:** em construção. Este arquivo documenta as onze ferramentas do
> servidor, os resources, os prompts, o cliente, as imagens de exemplo e a
> configuração do Claude Desktop e do MCP Inspector. Faltam a revisão final e a
> seção de limitações conhecidas.

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
verde, azul) é dividido em faixas de `passo_de_agrupamento` valores, e pixels cujos três
canais caem nas mesmas faixas formam um grupo.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |
| `quantidade` | inteiro 1 a 20 | não (padrão 5) | quantas cores devolver |
| `passo_de_agrupamento` | inteiro 1 a 128 | não (padrão 32) | tamanho da faixa de cada canal; 1 = cores exatas, maior = grupos mais amplos |
| `ignorar_transparentes` | booleano | não (padrão `true`) | se `true`, pixels com alfa 0 não entram na conta |

Exemplo de chamada: `{"arquivo": "vermelho-branco.png", "quantidade": 2, "passo_de_agrupamento": 1}`

```json
{
  "arquivo" : "vermelho-branco.png",
  "passoDeAgrupamento" : 1,
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

- Com `passo_de_agrupamento` maior que 1, a `cor` de cada grupo é o **centro da faixa** em
  cada canal (no máximo 255), e não uma cor que exista na imagem. Por exemplo,
  com passo 32 o branco aparece como `#F0F0F0`. Use `passo_de_agrupamento` 1 para obter as
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
| `PARAMETRO_INVALIDO` | `quantidade` fora de 1 a 20, `passo_de_agrupamento` fora de 1 a 128, valor não inteiro, `ignorar_transparentes` não booleano ou `arquivo` inválido |
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

### `contar_pixels_transparentes`

Conta quantos pixels da imagem são **transparentes** (alfa 0),
**semitransparentes** (alfa de 1 a 254) e **opacos** (alfa 255), com o
percentual de cada tipo sobre todos os pixels da imagem. Imagens sem canal alfa
(JPG, BMP) têm todos os pixels opacos.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |

Exemplo de chamada: `{"arquivo": "alfas.png"}` (imagem de 3 pixels vermelhos,
com alfa 0, 128 e 255)

```json
{
  "arquivo" : "alfas.png",
  "temCanalAlfa" : true,
  "totalPixels" : 3,
  "transparentes" : {
    "quantidade" : 1,
    "percentual" : 33.33
  },
  "semitransparentes" : {
    "quantidade" : 1,
    "percentual" : 33.33
  },
  "opacos" : {
    "quantidade" : 1,
    "percentual" : 33.33
  }
}
```

Os três percentuais são arredondados separadamente, então a soma pode dar
99,99 ou 100,01; as quantidades sempre somam `totalPixels`. Esta ferramenta
não tem o parâmetro `ignorar_transparentes`, pois o objetivo dela é justamente
contar os transparentes.

Erros específicos:

| Código | Causa |
|---|---|
| `PARAMETRO_INVALIDO` | `arquivo` em branco ou com caminho, ou parâmetro desconhecido |
| `PARAMETRO_AUSENTE` | falta `arquivo` |
| `IMAGEM_NAO_ENCONTRADA`, `FORMATO_NAO_SUPORTADO`, `IMAGEM_INVALIDA`, `IMAGEM_MUITO_GRANDE` | iguais aos de `obter_informacoes` |

---

### `histograma_de_brilho`

Mostra como o brilho (luma, de 0 a 255) se distribui pelos pixels: a escala é
dividida em faixas iguais, da mais escura à mais clara, e para cada faixa vêm o
intervalo de brilho, a quantidade exata de pixels e o percentual. Serve para
saber se a imagem é escura, clara ou tem pouco contraste. O brilho é truncado
para inteiro antes de escolher a faixa, e as faixas vazias aparecem com zero.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |
| `faixas` | inteiro 2 a 32 | não (padrão 8) | em quantas faixas dividir a escala de brilho |
| `ignorar_transparentes` | booleano | não (padrão `true`) | se `true`, pixels com alfa 0 não entram na conta |

Exemplo de chamada: `{"arquivo": "vermelho-branco.png", "faixas": 4}`

```json
{
  "arquivo" : "vermelho-branco.png",
  "numeroDeFaixas" : 4,
  "ignorarTransparentes" : true,
  "totalConsiderado" : 100,
  "faixas" : [ {
    "indice" : 0,
    "lumaMinima" : 0,
    "lumaMaxima" : 63,
    "quantidade" : 30,
    "percentual" : 30.00
  }, {
    "indice" : 1,
    "lumaMinima" : 64,
    "lumaMaxima" : 127,
    "quantidade" : 0,
    "percentual" : 0.00
  }, {
    "indice" : 2,
    "lumaMinima" : 128,
    "lumaMaxima" : 191,
    "quantidade" : 0,
    "percentual" : 0.00
  }, {
    "indice" : 3,
    "lumaMinima" : 192,
    "lumaMaxima" : 255,
    "quantidade" : 70,
    "percentual" : 70.00
  } ]
}
```

Quando 256 não divide igualmente pelo número de faixas, os limites são
arredondados para cima: com 3 faixas, os intervalos são 0 a 85, 86 a 170 e 171
a 255. Os intervalos sempre cobrem de 0 a 255 sem falhas nem sobreposição.

Erros específicos:

| Código | Causa |
|---|---|
| `PARAMETRO_INVALIDO` | `faixas` fora de 2 a 32 ou não inteiro, `ignorar_transparentes` não booleano ou `arquivo` inválido |
| `PARAMETRO_AUSENTE` | falta `arquivo` |
| `IMAGEM_NAO_ENCONTRADA`, `FORMATO_NAO_SUPORTADO`, `IMAGEM_INVALIDA`, `IMAGEM_MUITO_GRANDE` | iguais aos de `obter_informacoes` |

---

### `comparar_imagens`

Compara duas imagens do **mesmo tamanho** pixel a pixel e diz quantos pixels
são iguais e quantos são diferentes, com o percentual, e o menor retângulo que
contém todas as diferenças. Serve, por exemplo, para conferir se uma alteração
de design mudou só o que deveria.

Dois pixels são diferentes quando **algum dos quatro canais** (alfa, vermelho,
verde ou azul) difere em mais que a tolerância. Um pixel totalmente
transparente é comparado como os outros: as cores dele contam.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo_a` | string | sim | primeira imagem, como mostrada por `listar_imagens` |
| `arquivo_b` | string | sim | segunda imagem, como mostrada por `listar_imagens` |
| `tolerancia` | inteiro 0 a 255 | não (padrão 0) | diferença máxima aceita em cada canal; 0 = só pixels idênticos |

Exemplo de chamada: `{"arquivo_a": "par-a.png", "arquivo_b": "par-b.png"}`
(duas imagens de 6x6, a segunda com um quadrado de 2x2 pixels de outra cor)

```json
{
  "arquivoA" : "par-a.png",
  "arquivoB" : "par-b.png",
  "largura" : 6,
  "altura" : 6,
  "totalPixels" : 36,
  "tolerancia" : 0,
  "identicas" : false,
  "pixelsIguais" : {
    "quantidade" : 32,
    "percentual" : 88.89
  },
  "pixelsDiferentes" : {
    "quantidade" : 4,
    "percentual" : 11.11
  },
  "regiaoDiferente" : {
    "xMin" : 2,
    "yMin" : 3,
    "xMax" : 3,
    "yMax" : 4
  }
}
```

`regiaoDiferente` usa coordenadas a partir de (0, 0) no canto superior
esquerdo, e os quatro extremos fazem parte do retângulo. Se as imagens são
idênticas (`identicas` igual a `true`), `regiaoDiferente` vem `null`.

Erros específicos:

| Código | Causa |
|---|---|
| `DIMENSOES_DIFERENTES` | as imagens têm largura ou altura diferentes; a mensagem traz as duas dimensões (`a primeira tem 6x6 e a segunda tem 7x6`) |
| `PARAMETRO_INVALIDO` | `tolerancia` fora de 0 a 255 ou não inteira, ou algum nome de arquivo inválido |
| `PARAMETRO_AUSENTE` | falta `arquivo_a` ou `arquivo_b` |
| `IMAGEM_NAO_ENCONTRADA`, `FORMATO_NAO_SUPORTADO`, `IMAGEM_INVALIDA`, `IMAGEM_MUITO_GRANDE` | iguais aos de `obter_informacoes`; a mensagem cita o arquivo com o problema |

---

### `verificar_paleta`

Verifica se a imagem usa só as cores de uma **paleta**: devolve quantos pixels
(e que percentual) estão dentro dela e quantos estão fora, e as 5 cores exatas
que mais aparecem fora. Serve para conferir se um design respeita a identidade
visual.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |
| `cores_permitidas` | string | sim | de 1 a 30 cores separadas por vírgula, cada uma em `#RRGGBB`, `#RGB` ou nome (ex.: `#FFFFFF, #000000, azul`) |
| `tolerancia` | inteiro 0 a 255 | não (padrão 0) | diferença máxima por canal para um pixel contar como uma cor da paleta |
| `ignorar_transparentes` | booleano | não (padrão `true`) | se `true`, pixels com alfa 0 não entram na conta |

Exemplo de chamada: `{"arquivo": "vermelho-branco.png", "cores_permitidas": "vermelho, #0000FF"}`

```json
{
  "arquivo" : "vermelho-branco.png",
  "coresPermitidas" : [ "#FF0000", "#0000FF" ],
  "tolerancia" : 0,
  "ignorarTransparentes" : true,
  "totalConsiderado" : 100,
  "dentroDaPaleta" : {
    "quantidade" : 30,
    "percentual" : 30.00
  },
  "foraDaPaleta" : {
    "quantidade" : 70,
    "percentual" : 70.00
  },
  "coresForaDaPaleta" : [ {
    "cor" : "#FFFFFF",
    "quantidade" : 70,
    "percentual" : 100.00
  } ]
}
```

Como ler o resultado:

- `coresPermitidas` mostra a paleta em hexadecimal, sem repetições.
- `coresForaDaPaleta` traz até 5 cores **exatas** (sem agrupamento), da mais
  frequente para a menos frequente; em empate, vem primeiro o hexadecimal
  menor. O `percentual` de cada uma é sobre os pixels **fora da paleta**, e
  não sobre a imagem inteira. Se tudo está dentro da paleta, a lista vem vazia.
- A comparação com a paleta usa só os canais vermelho, verde e azul (o alfa não
  entra), como em `contar_pixels_da_cor`.

Erros específicos:

| Código | Causa |
|---|---|
| `COR_INVALIDA` | uma cor da lista não é válida; a mensagem indica o item (`parâmetro 'cores_permitidas (item 2)': cor desconhecida: 'xablau'; ...`) |
| `PARAMETRO_INVALIDO` | lista com item vazio (`vermelho,,branco`), com mais de 30 cores, `tolerancia` fora de 0 a 255, ou `arquivo` inválido |
| `PARAMETRO_AUSENTE` | falta `arquivo` ou `cores_permitidas` |
| `IMAGEM_NAO_ENCONTRADA`, `FORMATO_NAO_SUPORTADO`, `IMAGEM_INVALIDA`, `IMAGEM_MUITO_GRANDE` | iguais aos de `obter_informacoes` |

---

### `contraste_das_cores_dominantes`

Pega as **duas cores que mais aparecem** na imagem e calcula o contraste entre
elas pelo WCAG 2.x, com o veredito de cada critério (os mesmos de
`calcular_contraste`). A cor mais frequente é tratada como o fundo, e a segunda
como o texto.

> **É uma aproximação.** O servidor não detecta onde está o texto: a
> ferramenta só faz sentido para telas e banners com texto sobre um fundo liso.
> O resultado sempre traz esse aviso no campo `avisos`.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `arquivo` | string | sim | nome do arquivo, como mostrado por `listar_imagens` |
| `passo_de_agrupamento` | inteiro 1 a 128 | não (padrão 32) | tamanho da faixa de cada canal, como em `cores_dominantes`; 1 = cores exatas |

Os pixels totalmente transparentes são sempre ignorados.

Exemplo de chamada: `{"arquivo": "vermelho-branco.png", "passo_de_agrupamento": 1}`

```json
{
  "arquivo" : "vermelho-branco.png",
  "passoDeAgrupamento" : 1,
  "totalConsiderado" : 100,
  "coresDominantes" : [ {
    "cor" : "#FFFFFF",
    "quantidade" : 70,
    "percentual" : 70.00
  }, {
    "cor" : "#FF0000",
    "quantidade" : 30,
    "percentual" : 30.00
  } ],
  "contraste" : {
    "corTexto" : "#FF0000",
    "corFundo" : "#FFFFFF",
    "razao" : 4.00,
    "vereditos" : [ {
      "criterio" : "aa_texto_normal",
      "nome" : "AA texto normal",
      "limiar" : 4.5,
      "passa" : false
    }, ... ]
  },
  "avisos" : [ "Aproximação: o servidor não detecta o texto da imagem. ..." ]
}
```

(`vereditos` resumido aqui: no resultado real vêm os cinco critérios.)

Casos especiais:

| Situação | Resultado |
|---|---|
| imagem com uma única cor dominante | `coresDominantes` tem um item, `razao` é 1.00 (nenhum critério passa) e `avisos` traz "A imagem tem uma única cor dominante..." |
| imagem toda transparente | `coresDominantes` vem vazia, `contraste` é `null` e `avisos` explica que não há pixels para analisar |
| `passo_de_agrupamento` maior que 1 | `avisos` acrescenta que cada cor é o centro de um grupo de cores parecidas, e não uma cor exata; por isso a razão pode diferir da real. Use 1 para as cores exatas |

Erros específicos:

| Código | Causa |
|---|---|
| `PARAMETRO_INVALIDO` | `passo_de_agrupamento` fora de 1 a 128 ou não inteiro, ou `arquivo` inválido |
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

## Resources

Resources são conteúdos que o cliente (ou a IA) pode **ler pelo endereço**,
sem chamar uma ferramenta. Todos devolvem JSON (`application/json`) em um único
conteúdo de texto e não recebem parâmetros.

| Endereço | O que devolve |
|---|---|
| `imagens://cores-nomeadas` | o dicionário de nomes de cores em português para `#RRGGBB`, o mesmo que as ferramentas usam para entender `vermelho`, `azul-marinho` etc. |
| `imagens://criterios-wcag` | a tabela dos limiares de contraste do WCAG (AA e AAA, texto normal e grande, componentes de interface), a mesma que `calcular_contraste` usa |
| `imagens://imagens` | as imagens da pasta de imagens, a mesma lista de `listar_imagens` |

Exemplo de leitura de `imagens://criterios-wcag` (resumido):

```json
{
  "fonte" : "WCAG 2.x: critérios 1.4.3 (Contraste mínimo, nível AA), ...",
  "criterios" : [ {
    "id" : "aa_texto_normal",
    "nome" : "AA texto normal",
    "limiar" : 4.5,
    "descricao" : "Texto comum, abaixo de 18 pt (24 px), ..."
  }, ... ]
}
```

Erros possíveis:

| Situação | Resultado |
|---|---|
| endereço que não existe (por exemplo `imagens://nada`) | erro do protocolo: o servidor responde que o resource não foi encontrado |
| `imagens://imagens` e a pasta de imagens não pode ser lida | erro do protocolo com a mensagem do problema |

Os dois primeiros resources leem arquivos que fazem parte do próprio servidor
(`src/main/resources/imagens/*.json`), por isso não falham.

## Prompts

Prompts são modelos de instrução que o cliente pode pedir ao servidor. Cada um
recebe argumentos e devolve **uma mensagem de usuário** com a instrução pronta,
que orienta a IA a usar as ferramentas certas e a interpretar os números sem
inventar nada. O servidor não chama nenhuma IA: ele só monta o texto.

### `analisar_acessibilidade_de_cores`

Instrui a IA a medir as cores de uma imagem e interpretar o contraste pelos
critérios do WCAG.

| Argumento | Obrigatório | Descrição |
|---|---|---|
| `arquivo` | sim | nome do arquivo da imagem, como mostrado por `listar_imagens` |

A instrução pede, em ordem: chamar `contraste_das_cores_dominantes` (que
devolve o contraste entre as duas cores mais frequentes, com os vereditos),
chamar `cores_dominantes` para ver as demais cores e `calcular_contraste` para
outros pares relevantes, interpretar os vereditos do WCAG (consultando
`imagens://criterios-wcag`) e recomendar ajustes, confirmando-os com
`calcular_contraste`. A IA é orientada a afirmar só o que os números sustentam,
a avisar que o contraste das cores dominantes é uma aproximação e a tratar o
resultado como contraste entre **pares de cores**, já que o servidor não sabe
qual cor é texto.

### `descrever_composicao_de_cores`

Instrui a IA a descrever a composição de cores de uma imagem.

| Argumento | Obrigatório | Descrição |
|---|---|---|
| `arquivo` | sim | nome do arquivo da imagem, como mostrado por `listar_imagens` |
| `quantidade` | não (padrão 5) | quantas cores dominantes incluir, de 1 a 20 |

A instrução pede para chamar `obter_informacoes`, `cores_dominantes` (com a
`quantidade` pedida) e `proporcao_claro_escuro`, e responder com um parágrafo
sobre a composição mais uma tabela de cor, quantidade de pixels e percentual.

Erros possíveis (nos dois prompts), devolvidos como erro do protocolo com o
código e a mensagem:

| Código | Causa |
|---|---|
| `PARAMETRO_AUSENTE` | falta `arquivo` |
| `PARAMETRO_INVALIDO` | `arquivo` em branco, `quantidade` fora de 1 a 20 ou não inteira, ou argumento que o prompt não declara |

O nome do arquivo não é conferido na pasta ao montar o prompt: se ele não
existir, o erro aparece quando a IA chamar as ferramentas.

## Cliente MCP

O cliente é um programa de console que inicia o servidor como subprocesso (STDIO),
lista as ferramentas e executa **qualquer uma** delas: ele lê o schema JSON da
ferramenta escolhida e pergunta o valor de cada parâmetro, sem saber nada das
ferramentas de antemão. Suporta os tipos `string`, `integer`, `number` e
`boolean`, os únicos usados pelo servidor.

Como executar (depois de `mvn clean package -Pserver -DskipTests` e
`mvn clean package -Pclient -DskipTests`):

```bash
cd target/dist-client
java -jar mcp_imagens_client.jar
```

Os caminhos são relativos à pasta `target/dist-client`, de onde o cliente deve
ser executado: o JAR do servidor é `../../server/mcp_imagens_server.jar` e, se a
variável de ambiente `IMG_DIR` não estiver definida, a pasta de imagens é
`../../imagens`. Com `IMG_DIR` definida, o cliente repassa o valor ao servidor.

Uso:

| Digite | O que acontece |
|---|---|
| o número ou o nome de uma ferramenta | o cliente pede os parâmetros e mostra o resultado |
| `listar` | pede de novo a lista de ferramentas ao servidor |
| `recursos` | lista os resources do servidor, numerados |
| `ler <número ou endereço>` | mostra o conteúdo de um resource (por exemplo `ler imagens://criterios-wcag`) |
| `prompts` | lista os prompts do servidor, numerados |
| `prompt <número ou nome>` | pede os argumentos do prompt e mostra a instrução montada |
| `sair` | encerra o cliente (o fim da entrada também encerra) |

Ao pedir um parâmetro, o cliente mostra o tipo, se é obrigatório, o valor padrão
e a faixa permitida (quando o schema informa). Parâmetros opcionais deixados em
branco não são enviados, e o servidor usa o padrão. Valores que não servem ao
tipo (por exemplo `abc` para um inteiro) são recusados na hora, e booleanos
aceitam `true`/`false` ou `sim`/`não`.

O código do cliente está dividido em classes pequenas, no pacote `client`:

| Classe | Responsabilidade |
|---|---|
| `McpImagensClient` (pacote `main`) | coordena o ciclo menu, escolha, parâmetros, execução, resultado |
| `FabricaDeClienteMcp` | localiza o JAR do servidor, inicia o subprocesso e repassa `IMG_DIR` |
| `MenuConsole` | mostra o menu e interpreta a escolha (número ou nome) |
| `ArgumentPrompter` | pergunta os parâmetros a partir do schema da ferramenta |
| `ConversorDeValores` | converte o texto digitado para o tipo do parâmetro |
| `ResultPrinter` | mostra o resultado da ferramenta |
| `ExploradorDeRecursosEPrompts` | comandos de resources e prompts |

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

### No Eclipse

1. **File, Import, Maven, Existing Maven Projects** e escolha a pasta do projeto.
2. Clique com o botão direito no projeto, **Run As, Maven build...**, e informe:
   * servidor com testes: **Goals** `clean verify`, **Profiles** `server`;
   * servidor sem testes: **Goals** `clean package -DskipTests`, **Profiles** `server`;
   * cliente: **Goals** `clean package -DskipTests`, **Profiles** `client`.
3. Para só rodar os testes unitários: botão direito no projeto, **Run As, JUnit Test**
   (ou **Maven test**).

O Eclipse precisa estar configurado com um JDK 25 (**Window, Preferences, Java,
Installed JREs**).

## Imagens de exemplo

A pasta `imagens/` do repositório traz quatro imagens pequenas e originais
para testar o servidor sem precisar de arquivos próprios. Elas são **geradas**
pela classe `GeradorDeImagensDeExemplo` (em `src/test/java`, pacote
`testutil`), desenhadas com Java 2D e sem suavização de bordas, então cada
pixel é uma das poucas cores da paleta e os números das ferramentas são fáceis
de conferir.

| Arquivo | Tamanho | O que é |
|---|---|---|
| `banner.png` | 400x120 | texto "Promoção de Verão" claro (`#F5F1E8`) sobre fundo escuro (`#1B2A41`) e uma barra laranja (`#E4572E`) |
| `icone.png` | 64x64 | ícone de "certo" em um círculo azul, com fundo **transparente** e um anel **semitransparente** |
| `tela-v1.png` | 320x200 | tela de um aplicativo: cabeçalho, texto, um texto de apoio em cinza claro (`#AAAAAA`, contraste baixo) e um botão azul |
| `tela-v2.png` | 320x200 | a mesma tela com o botão verde e o texto de apoio em cinza mais escuro (`#767676`), para testar `comparar_imagens` |

Para gerar de novo (por exemplo, depois de mudar o desenho):

```bash
mvn test-compile
java -cp target/test-classes br.edu.ifpr.mcp_imagens.testutil.GeradorDeImagensDeExemplo imagens
```

No Eclipse, basta executar a classe (**Run As, Java Application**); o
argumento é a pasta de saída e o padrão é `imagens`.

Para analisar uma imagem sua, **copie o arquivo para a pasta de imagens** (a
pasta `imagens/` do projeto ou a que você configurar em `IMG_DIR`) e cite o
nome dele.

## Claude Desktop

> **Aviso importante:** o servidor lê as imagens **da pasta configurada**, e
> não os anexos da conversa. Para o Claude analisar uma imagem, copie o arquivo
> para a pasta `IMG_DIR` e cite o nome dele (por exemplo, "analise o
> `banner.png`"). Se a imagem não estiver lá, o servidor devolve
> `IMAGEM_NAO_ENCONTRADA` com a lista do que existe na pasta.

1. Gere o servidor: `mvn clean package -Pserver -DskipTests` (cria
   `server/mcp_imagens_server.jar` e `server/libs`).
2. No Claude Desktop, abra **Settings, Developer, Edit Config**. O arquivo é o
   `claude_desktop_config.json`:
   * Windows: `%APPDATA%\Claude\claude_desktop_config.json`
   * macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
3. Acrescente o servidor em `mcpServers` (ajuste os caminhos para o seu
   computador).

   Windows (as barras invertidas são duplicadas no JSON):

   ```json
   {
     "mcpServers": {
       "mcp-imagens": {
         "command": "java",
         "args": [
           "-jar",
           "C:\projetos\mcp_imagens\server\mcp_imagens_server.jar"
         ],
         "env": {
           "IMG_DIR": "C:\projetos\mcp_imagens\imagens"
         }
       }
     }
   }
   ```

   macOS:

   ```json
   {
     "mcpServers": {
       "mcp-imagens": {
         "command": "java",
         "args": [
           "-jar",
           "/Users/voce/projetos/mcp_imagens/server/mcp_imagens_server.jar"
         ],
         "env": {
           "IMG_DIR": "/Users/voce/projetos/mcp_imagens/imagens"
         }
       }
     }
   }
   ```

4. **Encerre completamente o Claude Desktop** antes de abrir de novo (no
   Windows, feche também pelo ícone da bandeja; no macOS, `Cmd+Q`). Fechar só
   a janela não recarrega a configuração.
5. Ao abrir, o servidor `mcp-imagens` deve aparecer na lista de ferramentas da
   conversa, com as 11 ferramentas. Os resources e os prompts aparecem no menu
   de anexos (**+**) da conversa.

O comando `java` precisa estar no `PATH` e ser o JDK 25. Se não estiver, use o
caminho completo do executável em `command` (por exemplo,
`C:\Program Files\Java\jdk-25\bin\java.exe`). Para ver os erros do
servidor, abra os logs do Claude Desktop (**Settings, Developer**; o servidor
escreve o log no stderr, nunca no canal do protocolo).

### Perguntas para experimentar

Com as imagens de exemplo na pasta `IMG_DIR`:

| Pergunta | O que esperar |
|---|---|
| "Quais imagens estão disponíveis?" | `listar_imagens`: as quatro imagens de exemplo |
| "Qual o contraste entre #777777 e branco? Passa no WCAG AA?" | `calcular_contraste`: razão 4,48; falha no AA de texto normal e passa no AA de texto grande |
| "Quantos pixels do `icone.png` são transparentes?" | `contar_pixels_transparentes`: 963 transparentes (23,51%), 1343 semitransparentes e 1790 opacos |
| "As telas `tela-v1.png` e `tela-v2.png` mudaram? Onde?" | `comparar_imagens`: 4418 pixels diferentes (6,9%), na região de (16, 79) a (135, 165) |
| "Quais são as cores dominantes do `banner.png`? O texto passa no contraste?" | `cores_dominantes` e `contraste_das_cores_dominantes`: `#1B2A41` (91,66%), `#F5F1E8` (6,84%) e `#E4572E` (1,5%); razão 12,81, passa em todos os critérios |
| "A `tela-v1.png` segue a paleta #FFFFFF, #1F3A5F, #333333, #2E86AB?" | `verificar_paleta`: 99,54% dentro da paleta; fora dela só o `#AAAAAA` (292 pixels), o texto de apoio |
| "Use o prompt `analisar_acessibilidade_de_cores` para a `tela-v1.png`" | o Claude segue o roteiro do prompt, medindo antes de concluir |

## MCP Inspector

O [MCP Inspector](https://modelcontextprotocol.io/docs/tools/inspector) é uma
interface web para testar servidores MCP. O projeto traz um `Dockerfile` (Node
mais Java e o Inspector) e um `compose.yaml` que sobem o Inspector já
executando o servidor, com a pasta `imagens/` montada e `IMG_DIR` configurada.

Pré-requisito: Docker Desktop.

```bash
mvn clean package -Pserver -DskipTests
docker compose up -d
```

Depois abra <http://127.0.0.1:6274>. O Inspector inicia o servidor
(`java -jar /opt/mcp/server/mcp_imagens_server.jar`) por STDIO. Se ele pedir um
token de acesso, ele aparece no log: `docker compose logs mcp-inspector`. Nas
abas **Tools**, **Resources** e **Prompts** dá para listar, chamar e ler tudo o
que o servidor oferece. Para encerrar: `docker compose down`.

O `compose.yaml` monta `./server` e `./imagens` em modo somente leitura
(`:ro`); para testar com outras imagens, coloque-as na pasta `imagens/`.

## Outros assistentes de IA

Qualquer cliente MCP com transporte STDIO funciona com a mesma configuração do
Claude Desktop: o comando `java -jar .../mcp_imagens_server.jar` e a variável
de ambiente `IMG_DIR`. O cliente de console deste projeto (ver "Cliente MCP")
é um exemplo.

## Testes manuais

Além dos testes automatizados (`mvn clean verify -Pserver`), o servidor foi
exercitado nestes clientes:

| Cliente | Resultado |
|---|---|
| Cliente de console deste projeto (`mcp_imagens_client.jar`) | funciona: lista as 11 ferramentas, pede os parâmetros pelo schema e executa; os comandos `recursos`, `ler`, `prompts` e `prompt` funcionam |
| Chamadas JSON-RPC diretas pela entrada padrão do servidor (STDIO) | funciona: as 11 ferramentas respondem com as imagens de `imagens/`, também com várias chamadas enviadas em sequência rápida |
| Claude Desktop | a registrar após o teste (veja o passo a passo acima) |
| MCP Inspector (Docker) | a registrar após o teste (veja o passo a passo acima) |
