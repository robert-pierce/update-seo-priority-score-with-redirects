(ns seo-priority-score-redirect-update
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.data :refer [diff]]))

(def update-stats (volatile! {:count 0
                              :urls []})  )

(def current-date (.format 
                   (java.text.SimpleDateFormat. "MM-dd-yy")         
                   (new java.util.Date)))

(def redirect-uri "redirect-indexables.edn")

(def ag-seo-priority-score-urls-uri "ag_priority_score_urls_without_refinements_8-29-18.csv")
(def ag-seo-priority-score-urls-outfile (format "ag_priority_score_urls_without_refinements_%s.csv"
                                                current-date))

(def rent-seo-priority-score-urls-uri "rent_priority_score_urls_without_refinements_8-29-18.csv")
(def rent-seo-priority-score-urls-outfile (format "rent_priority_score_urls_without_refinements_%s.csv"
                                              current-date))

(def redirect-indexables
  (with-open [r (io/reader redirect-uri)]
    (edn/read (java.io.PushbackReader. r))))

(def ag-seo-priority-score-urls
  (with-open [r (io/reader ag-seo-priority-score-urls-uri)] 
    (doall
     (csv/read-csv r))))

(def rent-seo-priority-score-urls
 (with-open [r (io/reader rent-seo-priority-score-urls-uri)] 
   (doall
    (csv/read-csv r))))


(defn sps-data->map
  [sps-data-csv]
  (map zipmap
       (->> (first sps-data-csv)
            (map keyword)
            repeat)
       (rest sps-data-csv)))

(defn sps-map-by-url
  [sps-map]
  (reduce (fn [acc {:keys [url] :as entry}]
            (assoc acc url entry))
          {}
          sps-map))

(defn update-sps-map
  [sps-map-by-url redirect-indexables sps-site]
  (vals 
   (reduce (fn [acc {:keys [url site] :as redirect-entry}]
             (if (= site sps-site)
               (do
                 (if (get acc url)
                     (let [count (:count @update-stats)
                           urls (:urls @update-stats)]
                       (vreset! update-stats {:count (inc count)
                                              :urls (conj urls url)})))
                 (update acc url #(assoc % :Indexable "0")))
               acc))
           sps-map-by-url
           redirect-indexables)))

(defn write-sps-map-to-csv
  [sps-map outfile]
  (with-open [w (io/writer outfile)]
    (let [header (map name (keys (first sps-map)))
          data (map (comp vec vals) sps-map)]
      (csv/write-csv w
                     (cons header data)))))

;;;;;;;;;;;;;;;;;;;;;;; Create new ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-ag-doc
  []
  (let [raw-csv ag-seo-priority-score-urls
        csv->map (sps-data->map raw-csv)
        map-by-url (sps-map-by-url csv->map)
        data-to-write (update-sps-map map-by-url redirect-indexables "ag")
        outfile ag-seo-priority-score-urls-outfile]
    (write-sps-map-to-csv data-to-write outfile)
    (println (format "Finished running update. The count of url is : %s " (:count @update-stats)))
    (println "")
    (println "a sample of the urls is ")
    (take 50 (:urls @update-stats))))

(defn update-rent-doc
  []
  (vreset! update-stats {:count 0
                        :urls []})
  (let [raw-csv rent-seo-priority-score-urls
        csv->map (sps-data->map raw-csv)
        map-by-url (sps-map-by-url csv->map)
        data-to-write (update-sps-map map-by-url redirect-indexables "rent")
        outfile rent-seo-priority-score-urls-outfile]
    (write-sps-map-to-csv data-to-write outfile)
    (println (format "Finished running update. The count of url is : %s " (:count @update-stats)))
    (println "")
    (println "a sample of the urls is ")
    (take 50 (:urls @update-stats))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;Verify all urls exist in new as in old;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn sps-map-by-urlid
  [sps-map]
  (reduce (fn [acc {:keys [URLid] :as entry}]
            (assoc acc URLid entry))
          {}
          sps-map))

(defn verify-doc
  [path-to-old path-to-new]
  (let [old-raw (with-open [r (io/reader path-to-old)] 
                  (doall
                   (csv/read-csv r)))
        old-csv->map (sps-data->map old-raw)
        old-map-by-url (sps-map-by-urlid old-csv->map)
        old-urls (keys old-map-by-url)
        
        new-raw (with-open [r (io/reader path-to-new)] 
                  (doall
                   (csv/read-csv r)))
        new-csv->map (sps-data->map new-raw)
        new-map-by-url (sps-map-by-urlid new-csv->map)
        new-urls (keys new-map-by-url)
        diff (clojure.data/diff (set new-urls) (set old-urls))]
    (if (and (nil? (first diff)) (nil? (second diff)))
      (println "urls math, no difference")
      (do 
        (println "urls do NOT match")
        (println (str "number of urls in new that are not in old " (count (first diff))))
        (spit "in-new-not-in-old.txt" (first diff))
        (println (str "number of urls in old that are not in new " (count (second diff))))
        (spit "in-old-not-in-new.txt" (second diff))
        (println (str "number of urls in both " (count (nth diff 2))))
        (spit "in-both.txt" (nth diff 2))))))
