# Algoritmo de recorrência

Calcula `T(n)` a partir de uma expressão informada pelo usuário e mostra como as chamadas chegam à base e retornam seus resultados. A expressão pode combinar várias chamadas, coeficientes, divisão, subtração, potências e condições.

O código da aplicação está em uma única classe, `src/main/java/RecurrenceSolver.java`. As expressões são interpretadas uma vez, e os resultados de cada tamanho são guardados para evitar cálculos repetidos. Cadeias profundas usam uma pilha explícita para continuar o cálculo sem esgotar a pilha de chamadas do Java.

## Executar a aplicação

Requer Java 17 ou superior. Na pasta do projeto:

```sh
java src/main/java/RecurrenceSolver.java
```

O terminal pergunta, nesta ordem: expressão, tamanho `n`, limiar do caso base, expressão da base e exibição das etapas. Depois do resultado, aceita outra expressão. Digite `q` para sair.

## Executar os testes

Requer também Maven 3.9 ou superior:

```sh
mvn test
```

Para conferir testes, compilação, formatação e gerar o JAR, use JDK 21 ou superior:

```sh
mvn clean verify
java -jar target/recurrence-solver-1.0.0.jar
```

Os testes cobrem exemplos com resultados conhecidos, contagens de algoritmos, fórmulas fechadas, expressões geradas comparadas com um cálculo independente, bases, condições, arredondamento, números grandes, entradas inválidas e sessões completas no terminal.

## Como montar a expressão

Digite o lado direito, como `2*T(n/2)+n`, ou a igualdade completa, `T(n)=2*T(n/2)+n`. Use `*` para multiplicar: `2*T(...)` e `3*n`.

| Escrita | Significado | Exemplo |
| --- | --- | --- |
| `n` | Tamanho da chamada atual. | Em `T(8)`, `n` vale `8`. |
| `T(...)` | Resultado de um subproblema. | `T(n-1)` ou `T((n-1)/2)`. |
| `+`, `-`, `*`, `/` | Operações aritméticas. | `2*T(n/2)+3*n+1`. |
| `^` | Potência com expoente inteiro. | `3*T(n/3)+n^2`. |
| `%` | Resto entre inteiros. | `n%2` identifica a paridade. |
| `floor(x)`, `ceil(x)` | Arredondar para baixo ou para cima. | `T(ceil(n/2))`. |
| `abs(x)`, `min(a,b)`, `max(a,b)` | Módulo, menor e maior valor. | `max(T(n-1),T(n/2))+1`. |
| `if(condicao,a,b)` | Avaliar `a` se a condição for verdadeira; caso contrário, `b`. | `if(n%2==0,T(n/2)+1,T(n-1)+1)`. |

As condições aceitam `==`, `!=`, `<`, `<=`, `>` e `>=`. Parênteses organizam o cálculo. Use ponto em decimais, como `0.5`. Potências têm precedência sobre o sinal: `-2^2` vale `-4`; `(-2)^2` vale `4`.

A divisão conserva a fração durante o cálculo. Ao entrar em `T`, um argumento fracionário é arredondado para baixo: `T(7/2)` chama `T(3)`. Para chamar `T(4)`, escreva `T(ceil(7/2))`. Cada chamada usa um tamanho não negativo e menor que o atual.

Se o resultado for uma dízima, apenas sua apresentação decimal é arredondada para 34 algarismos significativos, com aviso no terminal. Os cálculos intermediários e os resultados inteiros permanecem exatos.

O limiar define quando parar. Com limiar `1` e base `n`, temos `T(0)=0` e `T(1)=1`; com base `1`, ambos valem `1`. Para bases diferentes dentro do limiar, use uma expressão como `if(n==0,0,1)`.

Por exemplo, para `T(n)=2*T(n/2)+8*T(n/4)+2`, digite `2*T(n/2)+8*T(n/4)+2`. Escolhendo `n=8`, limiar `2` e base `1`, o resultado é `34`: primeiro `T(4)=2*1+8*1+2=12`; depois `T(8)=2*12+8*1+2=34`.

## Exemplos para enviar ao terminal

Cada linha explica uma resposta, na ordem das perguntas. **Digite apenas o conteúdo à esquerda de `->`**, uma linha por vez.

### 1. Subtração com custo variável

```text
T(n-1)+2*n+3 -> Expressão: resolver o tamanho anterior e somar 2*n+3.
5 -> Entrada: calcular T(5).
1 -> Limiar: usar a base quando n <= 1.
1 -> Base: devolver 1 nesses tamanhos.
1 -> Etapas: mostrar as chamadas, as substituições e os retornos.
q -> Sair depois de receber o resultado.
```

**Resultado esperado: `T(5)=41`.** A descida passa por `5, 4, 3, 2, 1`. Na volta, cada chamada soma seu custo: `T(2)=1+7=8`, `T(3)=8+9=17`, `T(4)=17+11=28` e `T(5)=28+13=41`.

### 2. Modelo de custo do Merge Sort

```text
2*T(n/2)+n -> Expressão: duas metades, mais custo n para reuni-las.
8 -> Entrada: calcular o custo do modelo para 8 elementos.
1 -> Limiar: parar em tamanhos de no máximo 1 elemento.
1 -> Base: atribuir custo 1 a esses tamanhos.
1 -> Etapas: acompanhar a expansão e a substituição dos resultados.
q -> Sair depois de receber o resultado.
```

**Resultado esperado: `T(8)=32`.** Temos `T(1)=1`, `T(2)=2*1+2=4`, `T(4)=2*4+4=12` e `T(8)=2*12+8=32`. O programa calcula o valor de cada tamanho uma vez e aplica o coeficiente `2`; isso preserva as duas parcelas da equação.

## Algoritmos reais convertidos em expressões

Para estudar custo, precisamos escolher o que contar. Nos modelos abaixo, as parcelas constantes valem uma unidade. Isso não representa tempo em segundos nem uma contagem universal de instruções Java. As linhas de fatorial e Fibonacci calculam o valor da sequência.

| Algoritmo e quantidade calculada | Expressão | Limiar; base | Exemplo |
| --- | --- | --- | --- |
| Busca binária: comparações em um modelo de pior caso. | `T(n/2)+1` | `0`; `0` | `T(8)=4` |
| Merge Sort: duas partes e custo linear para intercalar, incluindo tamanhos ímpares. | `T(floor(n/2))+T(ceil(n/2))+n` | `1`; `1` | `T(5)=17` |
| Selection Sort: comparações entre elementos. | `T(n-1)+n-1` | `1`; `0` | `T(5)=10` |
| Torres de Hanói: quantidade de movimentos. | `2*T(n-1)+1` | `0`; `0` | `T(3)=7` |
| Fatorial: produto de `1` até `n`. | `n*T(n-1)` | `1`; `1` | `T(5)=120` |
| Fibonacci: soma dos dois valores anteriores. | `T(n-1)+T(n-2)` | `1`; `n` | `T(10)=55` |
| Construção de árvore balanceada: quantidade de nós criados a partir de um vetor ordenado. | `T(floor((n-1)/2))+T(ceil((n-1)/2))+1` | `0`; `0` | `T(8)=8` |
| Exponenciação rápida: multiplicações na versão que divide expoentes pares e subtrai 1 dos ímpares. | `if(n%2==0,T(n/2)+1,T(n-1)+1)` | `0`; `0` | `T(13)=6` |

No Merge Sort, as duas partes são processadas, por isso somamos os dois termos. Na exponenciação rápida, apenas um ramo do `if` é executado. Na construção da árvore, subtraímos o elemento escolhido como raiz e dividimos os restantes entre os filhos.

As definições de recursão, fatorial, Fibonacci e Hanói podem ser consultadas em [Recursion, da Universidade de Princeton](https://introcs.cs.princeton.edu/java/23recursion/). Para a relação entre divisão do problema e análise do custo, veja [Recurrences, do MIT](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/1869dbf640ded6b31f1bd369d2001ef5_MIT6_006S20_r03.pdf).
