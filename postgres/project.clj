(defproject io.jepsen/postgres "0.1.3-SNAPSHOT"
  :description "Jepsen tests for PostgreSQL."
  :url "https://github.com/jepsen-io/postgres"
  :scm     {:name "git"
            :url "https://github.com/jepsen-io/jepsen/"
            :dir ".."}
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.6"]
                 [jepsen "0.3.14"]
                 [io.jepsen/sql "0.1.2sb-SNAPSHOT"]
                 ; Jepsen pulls in jackson 2.16, but the Antithesis SDK
                 ; expects a different version. It doesn't matter as
                 ; we are only using the sdk for randomness
                 [com.antithesis/sdk "1.5.1"
                  :exclusions [com.fasterxml.jackson.core/jackson-databind
                               com.fasterxml.jackson.core/jackson-annotations
                               com.fasterxml.jackson.core/jackson-core]]
                 [org.postgresql/postgresql "42.7.11"]
                 [cheshire "6.2.0"]
                 [clj-wallhack "1.0.1"]]
  :java-source-paths ["src"]
  :main jepsen.postgres.cli
  :aot [jepsen.postgres.cli]
  :jvm-opts ["-Djava.awt.headless=true"
             "-server"]
  :repl-options {:init-ns jepsen.postgres}
  :profiles {:uberjar {:aot :all
                       :manifest {"Implementation-Title" "Antithesis FFI for Java"
                                  "Implementation-Version" "1.5.1"
                                  "Specification-Title" "Antithesis SDK Protocol"
                                  "Specification-Version" "1.1.0"}}})
