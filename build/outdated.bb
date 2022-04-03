#!/usr/bin/env bb
(ns outdated
  (:require [babashka.fs :as fs])
  (:import (java.util List)
           (java.nio.file Path)))

(def script-dir (fs/parent (fs/real-path (fs/path *file*))))
(def project-dir (fs/parent script-dir))

(defn sh!
  "Execute the given shell command and redirect the ouput/error to the standard output error; returns nil."
  [^Path directory & args]
  (let [^Process process (.. (ProcessBuilder. ^List args)
                             (directory (fs/file directory))
                             (inheritIO)
                             (start))]
    (.waitFor process)))

(def deps '{:deps {com.github.liquidz/antq {:mvn/version "RELEASE"}}})

(def exclusions
  [])

(defn outdated! [file args]
  (apply sh! file "clojure"
         "-Sdeps" (with-out-str (pr deps))
         "-M" "-m" "antq.core" (into args
                                     (map #(str "--exclude=" %)
                                          exclusions))))


(doseq [directory (concat [project-dir (fs/path project-dir "nvd")])]
  (println "Checking for" (str directory))
  (outdated! directory *command-line-args*))




