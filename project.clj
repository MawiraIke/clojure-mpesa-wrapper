(defproject clojure-mpesa-wrapper "1.0.5-SNAPSHOT"
  :description "A clojure wrapper around mpesa daraja api"
  :url "https://github.com/MawiraIke/clojure-mpesa-wrapper"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :scm {:name "git" :url "https://github.com/MawiraIke/clojure-mpesa-wrapper"}
  :dependencies [[org.clojure/data.json "1.0.0"]
                 [org.clojure/clojure "1.10.0"]
                 [clj-http "3.10.0"]
                 [pjstadig/utf8 "0.1.0"]]
  :repl-options {:init-ns clojure-mpesa-wrapper.core})
