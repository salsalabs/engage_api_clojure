(defproject engage_segment_xref "0.1.0-SNAPSHOT"
  :description "Create a CSV of supporters in a group and the other groups to which they belong."
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[clojure.java-time "0.3.0"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/data.json "2.0.2"]
                 [clj-commons/clj-yaml "0.7.106"]
                 [org.clojure/tools.cli "1.0.194"]
                 [clj-http "3.12.0"]
                 [clj-time "0.5.1"]
                 [cli-matic "0.4.3"]
                 [engage "0.2.0"]]
  :resource-paths ["resources" ]
  :main ^:skip-aot segment_xref.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
