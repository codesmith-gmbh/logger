(ns ch.codesmith.logger.version-test
  (:require [babashka.fs :as fs]
            [babashka.process :as ps]
            [ch.codesmith.anvil.basis :as ab]
            [ch.codesmith.anvil.libs :as libs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [clojure.tools.build.api :as b]))

(def root-path (fs/path "test" "versiontest"))
(def lib 'logger/test)
(def version "test1")

(defn build-test-jar []
  (libs/jar {:root      root-path
             :lib       lib
             :version   version
             :with-pom? false
             :aot       {}
             :clean?    true}))

(deftest version-correctness
  (build-test-jar)
  (binding [b/*project-root* (str root-path)]
    (let [basis     (ab/create-basis {})
          classpath (str/join
                      (System/getProperty "path.separator")
                      (cons
                        (str (fs/absolutize (fs/path
                                              root-path
                                              "target"
                                              (str (name lib) "-" version ".jar"))))
                        (->> (:classpath basis)
                             (filter (fn [[_ {:keys [lib-name]}]] lib-name))
                             (map first))))
          out       (-> (ps/sh ["java"
                                "-cp" classpath
                                "test.versiontest"]
                               {:dir (fs/file root-path)})
                        (ps/check)
                        :out)
          {:keys [version version-from-file version-from-ns-as-class]}
          (-> out
              (str/split-lines)
              last
              edn/read-string)]
      (is version-from-ns-as-class)
      (is version-from-file)
      (is (= version version-from-ns-as-class))
      (is (not= version version-from-file)))))