(ns engage.core
    (:require [reaver :refer [parse extract-from text attr]]))

(defn -main
  [& args]
(def url "https://www.thefarside.com/?referrer=https://www.gocomics.com&utm_source=gocomics&utm_medium=gc-footer&utm_campaign=gc-thefarside-promo")
(def parsed (reaver/parse (slurp url)))
(def r (reaver/extract-from parsed selector [:images] "img[src$=svg]" (reaver/attr :src)))
(def a (first r))
(def image-urls (filter (fn [x] (re-find #"^http" x)) (:images a)))
(map println image-urls))

