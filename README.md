# Algoritmo de recorrência

Calcula uma função a partir de uma entrada menor, até alcançar um caso base. Aceita duas regras, com parâmetros informados pelo terminal:

- **Subtração:** `T(n) = T(n - passo) + a*n + b`.
- **Divisão inteira:** `T(n) = T(n / divisor) + a*n + b`, descartando a parte decimal da divisão.

Quando `n` é menor ou igual ao limiar informado, retorna o valor base. O programa mostra as chamadas e seus retornos. Para subtrações com muitas etapas, usa uma fórmula equivalente que evita uma cadeia longa de chamadas. Os resultados usam inteiros exatos com `BigInteger`.

## Executar a aplicação

Requer Java 17 ou superior. Na pasta do projeto:

```sh
java src/main/java/RecurrenceSolver.java
```

Responda às perguntas do terminal. Digite `q` a qualquer momento para sair.

## Executar os testes

Requer também Maven 3.9 ou superior:

```sh
mvn test
```

Os testes verificam os cálculos, casos base, entradas inválidas, números grandes e a equivalência entre a recursão e o cálculo otimizado.

## Exemplos para enviar ao terminal

Após iniciar a aplicação, cole um dos blocos abaixo. A ordem dos valores é: **tipo, n, limiar, valor base, passo/divisor, a, b, mostrar etapas**. O último `0` encerra o programa após o resultado.

### 1. Subtração

Calcula `T(5)` usando `T(n) = T(n-1) + 2*n + 3`, com valor base `1` para `n <= 1`:

```text
1
5
1
1
1
2
3
1
0
```

**Resultado esperado: `T(5) = 41`.** As chamadas passam por `5 → 4 → 3 → 2 → 1`. Na volta, somamos as parcelas à base: `1 + 7 + 9 + 11 + 13 = 41`.

### 2. Divisão

Calcula `T(16)` usando `T(n) = T(n/2) + 5`, com valor base `1` para `n <= 1`:

```text
2
16
1
1
2
0
5
1
0
```

**Resultado esperado: `T(16) = 21`.** O caminho é `16 → 8 → 4 → 2 → 1`. São quatro reduções, cada uma acrescentando `5` ao valor base: `1 + 4*5 = 21`.
