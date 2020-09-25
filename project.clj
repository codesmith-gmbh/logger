(defproject org.clojars.stanhbb/smbh-log "0.0.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                 [ch.qos.logback/logback-core "1.2.3"]
                 [cheshire "5.8.0"]
                 [net.logstash.logback/logstash-logback-encoder "4.11"]
                 [org.clojure/clojure "1.9.0" :scope "provided"]]
  :java-source-paths ["java"]
  :plugins [[org.clojars.cvillecsteele/lein-git-version "1.2.7"]
            [lein-ancient "0.6.10"]
            [lein-marginalia "0.9.0"]
            [test2junit "1.2.5"]]
  :git-version {:path           "src/smbh/log"
                :root-ns        "smbh.log"
                :version-cmd    "git describe --match release/*.* --abbrev=4 --dirty=--DIRTY--"
                :tag-to-version ~#(if (> (count %) 8)
                                    (subs % 8)
                                    %)})
