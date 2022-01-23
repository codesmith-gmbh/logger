(ns build
  (:require [ch.codesmith.anvil.shell :as sh]
            [ch.codesmith.anvil.libs :as libs]
            [ch.codesmith.anvil.release :as rel]
            [clojure.tools.build.api :as b]))

(def lib 'ch.codesmith/logger)
(def version (str "0.6." (b/git-count-revs {})))
(def release-branch-name "master")

(def description-data
  {:license        :epl
   :inception-year 2020
   :description    "Thin macro layer on top of SLF4J"
   :organization   {:name "Codesmith GmbH"
                    :url  "https://codesmith.ch"}
   :authors        [{:name  "Stanislas Nanchen"
                     :email "stan@codesmith.ch"}]
   :scm            {:type         :github
                    :organization "codesmith-gmbh"
                    :project      "logger"}})


(defn verify []
  (sh/sh! "./build/verify"))

(defn jar [_]
  (libs/jar {:lib              lib
             :version          version
             :target-dir       "target"
             :with-pom?        true
             :description-data description-data
             :clean?           true}))

(defn release [_]
  (rel/check-released-allowed release-branch-name)
  (verify)
  (let [jar-file (jar {})]
    (libs/deploy {:jar-file jar-file
                  :lib      lib
                  :pom-file "target/classes/META-INF/maven/ch.codesmith/blocks/pom.xml"})
    (rel/git-release! {:deps/coord          lib
                       :version             version
                       :release-branch-name release-branch-name
                       :artifact-type       :mvn})))
