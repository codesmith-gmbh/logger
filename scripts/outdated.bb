(ns outdated
  (:require [babashka.fs :as fs]
            [babashka.process :as ps]))

(def script-dir (fs/parent (fs/real-path (fs/path *file*))))
(def project-dir (fs/parent script-dir))

(def deps '{:deps {com.github.liquidz/antq {:mvn/version "RELEASE"}}})

(def exclusions
  [])

(defn check-deps-file [file args]
  @(ps/process ["clojure"
                "-Sdeps" (with-out-str (pr deps))
                "-M" "-m" "antq.core" (into args
                                            (map #(str "--exclude=" %)
                                                 exclusions))]
               {:inherit true
                :dir     (fs/file file)}))

(defn check-project []
  (doseq [directory (concat [project-dir (fs/path project-dir "nvd")])]
    (println "Checking for" (str directory))
    (check-deps-file directory *command-line-args*)))



