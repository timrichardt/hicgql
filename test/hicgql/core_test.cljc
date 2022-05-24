(ns hicgql.core-test
  (:require [hicgql.core :refer [graphql]]
            #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is] :include-macros true])))

(deftest operation
  (is (= (graphql
          [:*/Op {:*/type :query
                  :$var   "Type"
                  :$var2  ["Type2" "default-value"]} :id])
         "query Op($var:Type,$var2:Type2=[\"Type2\",\"default-value\"]){id}")))


(deftest unnamed-operation
  (is (= (graphql [:+/_])
         "{}")))


(deftest field-with-subfields
  (is (= (graphql
          [:+/_
           [:+/fieldWithSubfields
            :subfield1
            :subfield2
            :.../fragment
            [:+/anotherSubfieldedField
             :andSoOn]]])
         "{fieldWithSubfields{subfield1,subfield2,...fragment,anotherSubfieldedField{andSoOn}}}")))


(deftest field-with-args
  (is (= (graphql
          [:+/_
           [:fieldWithArgs {:stringArg "string"
                            :numberArg 2
                            :objArg    {:size [27.32 "cm"]}}]])
         "{fieldWithArgs(stringArg:\"string\",numberArg:2,objArg:{\"size\":[27.32,\"cm\"]})}")))


(deftest field-with-dirs
  (is (= (graphql
          [:+/_
           [:fieldWithDirs {:! [[:dir2 {:arg1 :$var
                                        :arg2 'VAL}]
                                :dir1]}]])
         "{fieldWithDirs@dir2(arg1:$var,arg2:\"VAL\") @dir1}")))


(deftest aliases
  (is (= (graphql
          [:+/_
           [:>/alias [:someField {:arg "val"}]]])
         "{alias:someField(arg:\"val\")}")))


(deftest inline-fragment
  (is (= (graphql
          [:+/_
           [:?/TypeA :id]])
         "{... on TypeA{id}}")))


(deftest fragment-definition
  (is (= (graphql [:§/Fragment {:on :Type}
                   :field])
         "fragment Fragment on Type{field}")))


(deftest clojure-sequences
  (is (= (graphql
          [:+/_
           (for [m ["M" "N" "P"]]
             (for [a ["A" "B" "C"]
                   x ["X" "Y" "Z"]]
               (keyword (str a x m))))])
         "{AXM,AYM,AZM,BXM,BYM,BZM,CXM,CYM,CZM,AXN,AYN,AZN,BXN,BYN,BZN,CXN,CYN,CZN,AXP,AYP,AZP,BXP,BYP,BZP,CXP,CYP,CZP}")))
