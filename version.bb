(def modifier-snapshot "SNAPSHOT")

(defn parse-version [version]
  (when-let [[[_ major minor patch modifier]]
             (seq (re-seq #"(\d+)\.(\d+)\.(\d+)(-SNAPSHOT)?"
                          version))]
    {:major (parse-long major)
     :minor (parse-long minor)
     :patch (parse-long patch)
     :modifier (case modifier
                 "-SNAPSHOT" modifier-snapshot
                 nil nil)}))

(defn bump-version [version]
  (assert (map? version))
  (-> version
      (assoc :modifier modifier-snapshot)
      (update :patch inc)))

(defn prev-version [version]
  (assert (map? version))
  (-> version
      (assoc :modifier modifier-snapshot)
      (update :patch dec)))

(defn next-version [version]
  (assert (map? version))
  (if (= modifier-snapshot (:modifier version))
    (dissoc version :modifier)
    (bump-version version)))

(defn render-version [{:keys [major minor patch modifier] :as version}]
  (cond-> (str major "." minor "." patch)
    modifier (str "-" modifier)))

(defn replace-in-file [file old-str new-str]
  (println "Replacing in" file ":" old-str "->" new-str)
  (-> file
      slurp
      (.replace old-str new-str)
      (->> (spit file)))
  (clojure.java.shell/sh "git" "add" (str file))
  nil)

(def current-version-parsed
  (-> "project.clj" slurp read-string nnext first parse-version))

(->> (for [[flag version] {:current        current-version-parsed
                           :next           (next-version current-version-parsed)
                           :prev           (prev-version current-version-parsed)
                           :bump           (bump-version current-version-parsed)}
           [mod val]      {:stable         (render-version (assoc version :modifier nil))
                           :snapshot       (render-version (assoc version :modifier modifier-snapshot))
                           :raw            (render-version version)
                           :stable?        (not (#{"SNAPSHOT"} (:modifier version)))}]
       [[flag mod] val])
     (reduce (fn [a [path val]] (assoc-in a path val)) {})
     (def versions))

(replace-in-file "project.clj"
                 (-> versions :current :raw)
                 (-> versions :next :raw))

(replace-in-file "service/project.clj"
                 (-> versions :current :raw)
                 (-> versions :next :raw))

(when (-> versions :next :stable?)
  (replace-in-file "README.md"
                   (-> versions :prev :stable)
                   (-> versions :current :stable))
  (replace-in-file "README.md"
                   (-> versions :current :snapshot)
                   (-> versions :bump :snapshot)))

(println :versions versions)
