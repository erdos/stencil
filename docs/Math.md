# Math and Logics

It is possible to embed complex mathematical equations and logical formulae in the document logic.

## Simple math

- Use of integers and floating point decimals are supported.
- Use of parentheses in expressions is supported. For example: `{ %=(x.price * (1 + x.tax_pct))% }`
- The following mathematical algebraic operators are supported: `+, -, *, /, %`.

| operator | symbol | example | meaning |
|-----|----|-------------|---|
| addition | `+` | `a + b` | sum of `a` and `b` |
| subtraction | `-` | `a - b`, `x-1` | value of `a` minus `b` |
| multiplication | `*` | `a*b` | value of `a` multiplied by `b` |
| division | `/` | `a/b` | value of `a` divided by `b` |
| modulo   | `%` | `a%b` | remainder of `a` after divided by `b` |

## Logics in conditions

You can use logical operators in the expressions. These are useful inside conditional expressions.

| operator | symbol | example | meaning |
|-----|----|-------------|---|
| conjuction (and) | `&` | `a & b` | both `a` and `b` are not null and not false values |
| disjunction (or) | &#x7c; | a&#x7c;b | either `a` or `b` or both are not null or false |
| negation (not) | `!` | `!a` | value `a` is false or null |

## Function calls

There are simple functions you can call from within the template documents.
Read more on the [Functions documentation](Functions.md)
