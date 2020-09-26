(defproject codesmith/logger "0.0.0"
  :description "A logback/logstash appender clojure wrapper"
  :url "https://github.com/codesmith-gmbh/logger"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                 [ch.qos.logback/logback-core "1.2.3"]

                 [net.logstash.logback/logstash-logback-encoder "6.4"]

                 [cheshire "5.10.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.11.2"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.11.2"]]
  :java-source-paths ["java"]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.1"]]}}
  :plugins [[org.clojars.cvillecsteele/lein-git-version "1.2.7"]
            [lein-ancient "0.6.14"]
            [test2junit "1.4.2"]]
  :git-version {:path           "src/codesmith/logger"
                :root-ns        "codesmith.logger"
                :version-cmd    "git describe --match release/*.* --abbrev=4 --dirty=--DIRTY--"
                :tag-to-version ~#(if (> (count %) 8)
                                    (subs % 8)
                                    %)})
