(defproject sps_redirect_update "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.csv "0.1.4"]]
  :main ^:skip-aot sps-redirect-update.core
  :target-path "target/%s"
  :jvm-opts ["-Xmx8g"]
  :profiles {:uberjar {:aot :all}})
