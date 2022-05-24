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
