(ns lint
  (:require [babashka.fs :as fs]
            [babashka.process :as ps]
            [clojure.string :as str]))

(def kondo-config (str (fs/real-path (fs/path ".clj-kondo" "config.edn"))))

(def deps '{:deps      {clj-kondo/clj-kondo {:mvn/version "2022.05.31"}}
            :main-opts ["-m" "clj-kondo.main"]})

(def condo-command-prefix
  (let [installed-status (:exit @(ps/process ["bash" "-c" "command -v clj-kondo"]))]
    (if (= 0 installed-status)
      ["clj-kondo"]
      ["clojure" "-Sdeps" (with-out-str (pr deps))
       "-M" "-m" "clj-kondo.main"])))

(defn deps-path [directory]
  (-> (ps/process ["clojure" "-A:dev:test" "-Spath"] {:out :string :dir (fs/file directory)})
      ps/check
      :out))

(defn lint
  ([directory]
   (println "Linting " (str directory))
   (lint directory (concat condo-command-prefix
                           ["--config" kondo-config "--lint"
                            (deps-path directory)
                            "--parallel" "--dependencies" "--copy-configs"]))
   (lint directory (concat condo-command-prefix
                           ["--config" kondo-config "--lint"
                            (str/join ":"
                                      (filter #(->> % (fs/path directory) fs/exists?)
                                              ["src" "test"]))]))
   (println))
  ([directory command]
   (-> (ps/process command {:dir     (fs/file directory)
                            :inherit true})
       ps/check)))
