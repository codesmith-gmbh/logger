(ns sole
  (:require [babashka.fs :as fs]
            [lint]
            [outdated]))

(def project-dir (fs/path "."))

(defn lint-project []
  (lint/lint project-dir))

(defn check-outdated-deps []
  (doseq [directory (concat [project-dir (fs/file project-dir "nvd")
                             (outdated/tmp-dir-for-deps-map #'outdated/deps)
                             (outdated/tmp-dir-for-deps-map #'lint/deps)])]
    (outdated/check-outdated-deps directory *command-line-args*)))