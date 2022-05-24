# hicgql
GraphQL in Clojure data structures, [Hiccup](https://github.com/weavejester/hiccup) style.

1. [Installation](#installation)
2. [Examples](#examples)
   1. [Operations](#operations)
   2. [Fields, Fragments, and Selections](#fields-fragments-and-selections)
      1. [Arguments and Values](#arguments-and-values)
      2. [Directives](#directives)
   3. [Aliases](#aliases)
   4. [Inline Fragments](#inline-fragments)
   5. [Fragment definitions](#fragment-definitions)
   6. [Clojure sequences](#clojure-sequences)
3. [Usage with `re-graph`](#usage-with-re-graph)
4. [References](#references)
5. [License](#license)

___

## Installation
[![Clojars Project](https://img.shields.io/clojars/v/io.github.timrichardt/hicgql.svg)](https://clojars.org/io.github.timrichardt/hicgql)

`hicgql` is in Clojars. Just add it.
```clojure
[io.github.timrichardt/hicgql "0.2.0"]
````
```clojure
io.github.timrichardt/hicgql {:mvn/version "0.2.0"}
```

```clojure
(:require
 [hicgql.core :refer [graphql]])
```

## Examples
GraphQL documents are described with nested vectors, where the first element of each vector is a keyword describing the type of the document element.

```clojure
[:<ELEMENT-TYPE> :<PROPS?> :<CHILD? OR CHILDREN?>]
```

`hicgql.core/graphql` accepts an arbitrary number of operations and renders them concatenated with a`\n` inbetween.

### Operations
Operations are described with `:*/` namespaced keywords, e.g. `:*/OperationName`. Valid types are `:query`, `:mutation`, `subscription`, and can be set via `:*/type`. Variables are supplied as `:$var "Type"` pairs.

```clojure
(graphql
 [:*/Op {:*/type :query
         :$var   "Type"
         :$var2  ["Type2" "default-value"]} :id])
```

```graphql
query Op($var: Type, $var2: Type2 = "default-value") {
  id
}
```

### Fields, Fragments, and Selections
Fields with subfields are prefixed by `:+/` namespace. Data fields without subfields do not have a namespace.

```clojure
[:+/fieldWithSubfields
 :subfield1
 :subfield2
 :.../fragment
 [:+/anotherSubfieldedField
  :andSoOn]]
```

```graphql
{
  fieldWithSubfields {
    subfield1
    subfield2
    ...fragment
    anotherSubfieldedField {
      andSoOn
    }
  }
}
```

#### Arguments and Values
Arguments to fields can be set with a map which is the second element of the selection vector. Argument names are the keys, the values can be clojure data structures that can be meaningfully translated with
`js/JSON.stringify` or `cheshire.core/generate-string`.
```clojure
[:+/_
 [:fieldWithArgs {:stringArg "string"
                  :numberArg 2
                  :objArg    {:size [27.32 "cm"]}}]]
```

```graphql
{
  fieldWithArgs(stringArg: "string", numberArg: 2, objArg: {size: [27.32 "cm"]})
}
```

To define a GraphQL document, that does not start with an operation, there is `:+/_`: `[:+/_ field]` → `{ field }`.

#### Directives
Directives can be set with the `:!` key in the property map. `!:`'s value has to be a list of directives. A directive is either a key `:directive` → `@directive`, or can be supplied with arguments, like a field.
```clojure
[:+/_
 [:fieldWithDirs {:! [[:dir2 {:arg1 :$var
                              :arg2 'VAL}]
                      :dir1]}]]
```

```graphql
{
  fieldWithDirs @dir2(arg1: $var, arg2: VAL) @dir1
}
```

Directives are applied from bottom to top.

### Aliases
Aliases can be set with the `:>/` namespace. The `name` of the keyword is the name of the aliased field.
```clojure
[:+/_
 [:>/alias [:someField {:arg "val"}]]]
```

```graphql
{
  alias: someField(arg: "val")
}
```

### Inline Fragments
Inline fragments are defined with `:?/` prefixed keywords. The `name` of the keyword has to be the type the fragment is of.
```clojure
[:+/_
 [:?/TypeA :id]]
```

```graphql
{
  ... on TypeA {
    id
  }
}
```

### Fragment definitions
Fragments are defined as operations, but with `:§/` namespaced keywords. The fragment type has to be set via the `:on` property.
```clojure
[:§/Fragment {:on :Type}
 :field]
```

```graphql
fragment Fragment on Type {
  field
}
```
### Clojure sequences
It is possible, to use for example `for`, to generate a list of fields.
```clojure
[:+/_
 (for [m ["M" "N"]]
   (for [a ["A" "B" "C"]
         x ["X" "Y" "Z"]]
     (keyword (str a x m))))]
```

```graphql
{AXM,AYM,AZM,BXM,BYM,BZM,CXM,CYM,CZM,AXN,AYN,AZN,BXN,
BYN,BZN,CXN,CYN,CZN,AXP,AYP,AZP,BXP,BYP,BZP,CXP,CYP,CZP}
```

## Usage with `re-graph`
`re-graph` adds the operation type on it's own, so you must not supply the `*:/type` keyword to the property map.
```clojure
(re-graph/query
 :query-id
 (graphql
  [:*/MyQuery {:$var "String"}
   [:+/selection {:arg :$var}
    :subfield]])
 {:var "value"}
 callback)
```

## References
1. GraphQL Spec October 2021, http://spec.graphql.org/October2021/
2. Hiccup by James Reeves, https://github.com/weavejester/hiccup
3. re-graph by Oliver Hine, https://github.com/oliyh/re-graph

## License
Eclipse Public License
https://www.eclipse.org/legal/epl-v10.html
