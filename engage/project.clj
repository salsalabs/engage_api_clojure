(defproject engage "0.2.0"
  :description "Clojure for Engage API"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[cli-matic "0.4.3"]
                 [clj-commons/clj-yaml "0.7.106"]
                 [clj-http "3.12.0"]
                 [clj-time "0.5.1"]
                 [clojure.java-time "0.3.0"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/data.json "2.0.2"]
                 [org.clojure/tools.cli "1.0.194"]]
  :main ^:skip-aot engage.demo.core
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
