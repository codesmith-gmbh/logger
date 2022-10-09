(ns outdated
  (:require [babashka.fs :as fs]
            [babashka.process :as ps]
            [clojure.java.io :as io]))

(def deps '{:deps {com.github.liquidz/antq {:mvn/version "2.1.932"}
                   org.slf4j/slf4j-simple  {:mvn/version "2.0.3"}}})

(def exclusions
  [])

(defn tmp-dir-for-deps-map [deps-map-var]
  (let [deps-map-sym (symbol deps-map-var)
        deps-file    (io/file "target"
                              (namespace deps-map-sym)
                              (name deps-map-sym)
                              "deps.edn")]
    (io/make-parents deps-file)
    (spit deps-file (prn-str @deps-map-var))
    (.getParent deps-file)))

(defn check-outdated-deps [dir args]
  (println "Checking outdated dependencies for" (str dir))
  @(ps/process ["clojure"
                "-Sdeps" (with-out-str (pr deps))
                "-M" "-m" "antq.core" (into args
                                            (map #(str "--exclude=" %)
                                                 exclusions))]
               {:inherit true
                :dir     (fs/file dir)})
  (println))