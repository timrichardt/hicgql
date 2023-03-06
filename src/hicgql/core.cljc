(ns hicgql.core
  "Hiccup for GraphQL."
  (:require [clojure.string]
            #?(:clj [cheshire.core])))


(defn- mapstr
  [f nodes]
  (->> (map f nodes)
       (interpose ",")
       (apply str)))


(defn stringify
  [object]
  ;; TODO: support deep variables (now :$kw only catched at
  ;; first level
  (if (and (keyword? object)
           (clojure.string/starts-with? (name object) "$"))
    (name object)
    #?(:clj (cheshire.core/generate-string object)
       :cljs (js/JSON.stringify (clj->js object)))))


(declare node->graphql)


(defn- field->graphql
  [node]
  (let [node-name (name node)]
    (when-not (= node-name "_")
      node-name)))


(defn- fragment->graphql
  [node]
  (str "..." (name node)))


(defn- vars->graphql
  "```clojure
  {:$var1 \"Type1\"
   :$var2 [\"Type\" \"val2\"]}
  ;; => ($var1: Type1, $var2: Type2 = \"val 2\")
  ```"
  [varmap]
  (if (empty? varmap)
    ""
    (->> varmap
         (map (fn [[k v]]
                (if (vector? v)
                  (str (name k) ":" (first v) "=" (stringify v))
                  ;; TODO: maybe represent type decls also
                  ;; as data structures?
                  (str (name k) ":" v))))
         (interpose ",")
         (apply str)
         (#(str "(" % ")")))))


(defn- alias->graphql
  [[alias node]]
  (str (name alias) ":" (node->graphql node)))


(defn- args->graphql
  "```clojure
  {:arg1 \"val\"
   :arg2 :val
   :arg3 :$var}
  ;; => \"(arg1: ''val'', arg2: val, arg3: $var)\"
  ```"
  [argmap]
  (if (empty? argmap)
    ""
    (->> argmap
         (map (fn [[k v]]
                (str (name k) ":" (stringify v))))
         (interpose ",")
         (apply str)
         (#(str "(" % ")")))))


(defn- directives->graphql
  "```clojure
  [:dir1
   [:dir2 {:arg1 \"val1\"}]]  => @dir1 @dir2(arg1: \"val1\")
  ```"
  [directives]
  (->> directives
       (map (fn [directive]
              (cond
                (keyword? directive)
                (str "@" (name directive))

                (vector? directive)
                (let [[directive args] directive]
                  (str "@" (name directive)
                       (when args
                         (args->graphql args)))))))
       (interpose " ")
       (apply str)))


(declare node->graphql)


(defn- operation->graphql
  ""
  [op vars selection]
  (let [type (:*/type vars)
        vars (dissoc vars :*/type)]
    (str
     (when type
       (str (name type) " "))
     (name op)
     (vars->graphql vars)
     (when selection
       (str "{" (mapstr node->graphql selection) "}")))))


(defn- inline-fragment->graphql
  [[type & subfields]]
  (str "... on " (name type) "{" (mapstr node->graphql subfields) "}"))


(defn- selection->graphql
  [field props subfields]
  (let [args (dissoc props :!)
        dirs (:! props)]
    (str (field->graphql field)
         (args->graphql args)
         (directives->graphql dirs)
         (when (seq subfields)
           (str "{" (mapstr node->graphql subfields) "}")))))


(defn- fragment-def->graphql
  [field {:keys [on]} selection]
  (str "fragment " (name field)
       (when on
         (str " on " (name on)))
       "{" (mapstr node->graphql selection) "}"))


(defn- node->graphql
  ""
  [node]
  (cond
    (keyword? node)
    (if (= (namespace node) "...")
      (fragment->graphql node)
      (field->graphql node))

    (sequential? node)
    (let [[field arg1] node]
      (if (and (= field :+/_) (= (count node) 1))
        "{}"
        (if (sequential? field)
          (mapstr node->graphql node)
          (condp = (namespace field)
            "*" (cond (or (vector? arg1) (keyword? arg1))
                      (operation->graphql field {} (rest node))

                      (map? arg1)
                      (operation->graphql field arg1 (drop 2 node)))

            "§" (if (map? arg1)
                  (fragment-def->graphql field arg1 (drop 2 node))
                  (fragment-def->graphql field {} (rest node)))

            "?" (inline-fragment->graphql node)

            ">" (alias->graphql node)

            "+" (cond (map? arg1)
                      (selection->graphql field arg1 (drop 2 node))

                      :else
                      (selection->graphql field {} (rest node)))

            nil (if (map? arg1)
                  (selection->graphql field arg1 nil)
                  (mapstr node->graphql node))))))))


(defn graphql
  ""
  [& args]
  (->> args
       (filter seq) ;; TODO: why? (prolly convenience)
       (map node->graphql)
       (interpose \newline)
       (apply str)))


(comment
  (graphql
   [:+/_ :id])
  ;; => "{id}"

  (graphql
   [:*/Op :id])

  (graphql
   [:§/Fragment {:on :Type}
    :field])

  (graphql
   [:+/_
    [:+/pizzas {:!     [[:include {:if :$baked}]]
                :limit 1}
     [:+/self
      :id]]])

  (graphql
   [:*/Op {:*/type :query
           :$var   'VAL} :id])
  ;; => "query Op($var:VAL){id}"
  
  (graphql
   [:*/m {:$var "Type"}
    [:+/selection
     :subfield1
     [:subfield2 {:arg "val"}]]])
  ;; => "m($var:Type){selection{subfield1,subfield2(arg:\"val\")}}"

  (graphql
   [:+/_
    [:+/fieldWithSubfields
     :subfield1
     :subfield2
     [:+/anotherSubfieldedField
      :andSoOn]]])

  (graphql
   [:+/_
    [:fieldWithArgs {:stringArg "string"
                     :numberArg 2
                     :objArg    {:size [27.32 "cm"]}}]])

  (graphql
   [:+/_
    [:fieldWithDirs {:! [[:dir2 {:arg1 :$var
                                 :arg2 'VAL}]
                         :dir1]}]])

  (graphql
   [:+/_
    [:?/TypeA :id]])

  (graphql
   [:+/_
    [:>/alias [:someField {:arg "val"}]]])
  "{alias:someField(arg:\"val\")}"

  (graphql
   [:+/_
    (for [m ["M" "N" "P"]]
      (for [a ["A" "B" "C"]
            x ["X" "Y" "Z"]]
        [:+/_ (keyword (str a x m))]))]))
