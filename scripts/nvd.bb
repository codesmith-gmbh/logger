(ns nvd
  (:require [babashka.fs :as fs]
            [babashka.process :as ps]))

(defn dev-classpath []
  (-> (ps/process ["clojure" "-A:dev:libs-dev" "-Spath"] {:out :string})
      ps/check
      :out))

(defn nvd [classpath]
  (-> (ps/process ["clojure" "-J-Dclojure.main.report=stderr" "-M" "-m" "nvd.task.check" "config.json" classpath]
                  {:inherit true
                   :dir     (fs/file "nvd")})
      ps/check))

(defn check-vulnerabilities []
  (nvd (dev-classpath)))
