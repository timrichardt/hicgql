(defproject io.github.timrichardt/hicgql "0.1.0"
  :description "GraphQL in Clojure data structures."
  :url "https://github.com/timrichardt/hicgql"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/clojurescript "1.10.520" :scope "provided"]
   [cheshire "5.10.2"]]

  :repositories
  {"clojars" {:url "https://clojars.org/repo"
              :sign-releases false}}
  
  :source-paths
  ["src"])
