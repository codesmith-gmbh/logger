(ns test.versiontest
  (:gen-class)
  (:require [ch.codesmith.logger :as log]))

(log/deflogger)

#_:clj-kondo/ignore
(defn -main []
  (log/set-version-file-path! "version.edn")
  (prn {:version                  (log/version)
        :version-from-file        (log/version-from-file)
        :version-from-ns-as-class (log/version-from-ns-as-class)}))
