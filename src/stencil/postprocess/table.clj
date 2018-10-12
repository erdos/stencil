(ns stencil.postprocess.table
  "XML fa utofeldolgozasat vegzo kod."
  (:require [clojure.zip :as zip]
            [stencil.types :refer :all]
            [stencil.util :refer :all]))

(set! *warn-on-reflection* true)

;; az ennel keskenyebb oszlopokat kidobjuk!
(def min-col-width 20)

(def ooxml-val :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/val)
(def ooxml-w :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/w)

(defn- loc-cell?  [loc] (some-> loc zip/node :tag name #{"tc" "td" "th"}))
(defn- loc-row?   [loc] (some-> loc zip/node :tag name #{"tr"}))
(defn- loc-table? [loc] (some-> loc zip/node :tag name #{"tbl" "table"}))

(defn- find-first-in-tree [pred tree]
  (assert (zipper? tree))
  (assert (fn? pred))
  (find-first (comp pred zip/node) (take-while (complement zip/end?) (iterate zip/next tree))))

(defn- find-last-child [pred tree]
  (assert (zipper? tree))
  (assert (fn? pred))
  (last (filter (comp pred zip/node) (take-while some? (iterate zip/right (zip/down tree))))))

(defn- first-right-sibling
  "Finds first right sibling that matches the predicate."
  [pred loc] (find-first pred (iterations zip/right loc)))

(defn- first-parent
  "Finds closest parent element that matches the predicate."
  [pred loc]
  (assert (ifn? pred))
  (assert (zipper? loc))
  (find-first pred (iterations zip/up loc)))

(defn- find-enclosing-cell [loc] (first-parent loc-cell? loc))
(defn- find-enclosing-row [loc] (first-parent loc-row? loc))
(defn- find-enclosing-table [loc] (first-parent loc-table? loc))

(defn- find-closest-row-right [loc] (first-right-sibling loc-row? loc))
(defn- find-closest-cell-right [loc] (first-right-sibling loc-cell? loc))

(defn- goto-nth-sibling-cell [n loc]
  (assert (int? n))
  (assert (zipper? loc))
  (nth (filter loc-cell? (iterations zip/right (zip/leftmost loc))) n))

(defn- find-first-child [pred loc]
  (assert (ifn? pred))
  (assert (zipper? loc))
  (find-first (comp pred zip/node) (take-while some? (iterations zip/right (zip/down loc)))))

(defn- ensure-child [tag-name loc]
  (assert string? tag-name)
  (assert (zipper? loc))
  (or (find-first-child #(and (map? %) (#{tag-name} (name (:tag %)))) loc)
      (zip/next (zip/insert-child loc {:tag (keyword tag-name) :content []}))))

;; finds first child with given tag name
(defn- child-of-tag [tag-name loc]
  (assert (zipper? loc))
  (assert (string? tag-name))
  (find-first-child #(some-> % :tag name (= tag-name)) loc))

(defn- cell-width
  "Az aktualis table cella logikai szelesseget adja vissza. Alapertelmezetten 1."
  [loc]
  (assert (zipper? loc))
  (assert (loc-cell? loc))
  (let [cell-loc      (find-enclosing-cell loc)
        cell          (zip/node cell-loc)]
    (case (name (:tag cell))
      ;; html
      ("td" "th") (-> cell :attrs :colspan ->int (or 1))

      ;; ooxml
      "tc"        (or (some->> loc (child-of-tag "tcPr") (child-of-tag "gridSpan") zip/node :attrs ooxml-val ->int) 1))))

(defn shrink-column
  "Decreases logical width of current TD cell."
  [col-loc shrink-amount]
  (assert (zipper? col-loc))
  (assert (loc-cell? col-loc))
  (assert (pos? shrink-amount))
  (assert (integer? shrink-amount))
  (let [old-width (cell-width col-loc)]
    (assert (< shrink-amount old-width))
    (case (name (:tag (zip/node col-loc)))
      "td"        (zip/edit col-loc update-in [:attrs :colspan] - shrink-amount)
      ("th" "tc") (-> (->> col-loc (child-of-tag "tcPr") (child-of-tag "gridSpan"))
                      (zip/edit update-in [:attrs ooxml-val] #(str (- (->int %) shrink-amount))) (zip/up) (zip/up)))))

(defn- current-column-indices
  "Visszaadja egy halmazban, hogy hanyadik oszlop(ok)ban vagyunk benne eppen."
  [loc]
  (assert (zipper? loc))
  (let [cell         (find-enclosing-cell loc)
        current-cell-width (cell-width cell)
        cells-before (filter loc-cell? (next (iterations zip/left cell)))
        before-sum   (reduce + (map cell-width cells-before))]
    (set (for [i (range current-cell-width)] (+ before-sum i)))))

(defn remove-columns
  "Egy adott row alatt az adott sorszamu oszlopokat eltavolitja.
   Figyelembe veszi a cellak COLSPAN ertekeit, tehat ha egy cella
   tobb oszlopot is elfoglal, akkor elobb csak keskenyiti es csak akkor
   tavolitja el, ha a szelessege nulla lenne.

   Visszater a row lokatorral."
  [row-loc removable-columns column-resize-strategy]
  (assert (zipper? row-loc) "Elso parameter zipper legyen!")
  (assert (seq removable-columns))
  (assert (contains? column-resize-modes column-resize-strategy))
  (let [row-loc           (find-enclosing-row row-loc)
        removable-columns (set removable-columns)]
    (assert (some? row-loc)         "Nem cell-ben vagyunk!")
    (assert (seq removable-columns) "Melyik oszlopokat tavolitsuk el?")

    (loop [current-loc (goto-nth-sibling-cell 0 (zip/down row-loc))
           current-idx (int 0)]
      (let [column-width (cell-width current-loc)
            col-indices  (for [i (range column-width)] (+ current-idx i))
            last-column? (nil? (find-closest-cell-right (zip/right current-loc)))
            shrink-by    (count (filter removable-columns col-indices))]
        (if last-column?
          (if (pos? shrink-by)
            (if (>= shrink-by column-width)
              (find-enclosing-row (zip/remove current-loc))
              (find-enclosing-row (shrink-column current-loc shrink-by)))
            (find-enclosing-row current-loc))
          (if (pos? shrink-by)
            (if (>= shrink-by column-width)
              (recur (find-closest-cell-right (zip/next (zip/remove current-loc)))
                     (int (+ current-idx column-width)))
              (recur (find-closest-cell-right (zip/right (shrink-column current-loc shrink-by)))
                     (int (+ current-idx column-width))))
            (recur (find-closest-cell-right (zip/right current-loc))
                   (int (+ current-idx column-width)))))))))

(defn map-each-rows [f table & colls]
  (assert (fn? f))
  (assert (loc-table? table))
  (if-let [first-row (find-closest-row-right (zip/down table))]
    (loop [current-row first-row
           colls       colls]
      (let [fixed-row (apply f current-row (map first colls))]
        (if-let [next-row (some-> fixed-row zip/right find-closest-row-right)]
          (recur next-row (map next colls))
          (find-enclosing-table fixed-row))))
    table))

(defn remove-children-at-indices [loc indices]
  (assert (zipper? loc))
  (assert (set? indices))
  (zip/edit loc update :content
            (partial keep-indexed (fn [idx child] (when-not (contains? indices idx) child)))))

(defn calc-column-widths [original-widths expected-total strategy]
  (assert (seq original-widths))
  (assert (number? expected-total))
  (case strategy
    :cut
    (do ; (assert (= expected-total (reduce + original-widths)))
      original-widths)

    :rational
    (let [original-total (reduce + original-widths)]
      (for [w original-widths] (* expected-total (/ w original-total))))

    :resize-last
    (concat (butlast original-widths)
            [(reduce - expected-total (butlast original-widths))])))

(defn table-resize-widths
  "Elavolitja a table grid-bol anem haznalatos oszlopokat."
  [table-loc column-resize-strategy removed-column-indices]
  (assert (loc-table? table-loc))
  (assert (keyword? column-resize-strategy))
  (assert (set? removed-column-indices))
  (let [find-grid-loc (fn [loc] (find-first-in-tree (every-pred map? #(some-> % :tag name (#{"tblGrid"}))) loc))
        total-width (fn [table-loc]
                      (assert (zipper? table-loc))
                      (some->> table-loc
                               find-grid-loc
                               (zip/children)
                               (keep (comp ->int ooxml-w :attrs))
                               (reduce +)))

        ;; egy sor cellainak az egyedi szelesseget is beleirja magatol
        fix-row-widths (fn [grid-widths row]
                         (assert (zipper? row))
                         (assert (every? number? grid-widths))
                         (loop [cell (some-> row zip/down find-closest-cell-right)
                                parent row
                                grid-widths grid-widths]
                           (if-not cell
                             parent
                             (-> (->> cell (ensure-child "tcPr") (ensure-child "tcW"))
                                 (zip/edit assoc-in [:attrs ooxml-w] (str (reduce + (take (cell-width cell) grid-widths))))
                                 (zip/up) (zip/up)
                                 (as-> * (recur (some-> * zip/right find-closest-cell-right)
                                                (zip/up *) (drop (cell-width cell) grid-widths)))))))

        fix-table-cells-widths (fn [table-loc grid-widths]
                                 (assert (sequential? grid-widths))
                                 (assert (loc-table? table-loc))
                                 (map-each-rows (partial fix-row-widths grid-widths) table-loc))

        ;; a tablazat teljes szelesseget beleirja a grid szelessegek szummajakent
        fix-table-width (fn [table-loc]
                          (assert (zipper? table-loc))
                          (-> (some->> table-loc (child-of-tag "tblPr") (child-of-tag "tblW"))
                              (some-> (zip/edit assoc-in [:attrs ooxml-w] (str (total-width table-loc)))
                                      (find-enclosing-table))
                              (or table-loc)))]
    (if-let [grid-loc (find-grid-loc table-loc)]
      (let [result-table (find-enclosing-table (remove-children-at-indices grid-loc removed-column-indices))]
        (if-let [widths (->> result-table find-grid-loc zip/children (keep (comp ->int ooxml-w :attrs)) seq)]
          (let [new-widths (calc-column-widths widths (total-width grid-loc) column-resize-strategy)]
            (-> (find-grid-loc result-table)
                (zip/edit update :content (partial map (fn [w cell] (assoc-in cell [:attrs ooxml-w] (str (int w)))) new-widths))
                (find-enclosing-table)
                (fix-table-width)
                (fix-table-cells-widths new-widths)))
          result-table))
      table-loc)))

;; visszaadja soronkent a jobboldali margo objektumot
(defn get-right-borders [original-start-loc]
  (for [row   (zip/children (find-enclosing-table original-start-loc))
        :when (and (map? row) (#{"tr"} (name (:tag row))))
        :let  [last-of-tag (fn [tag xs] (last (filter  #(and (map? %) (some-> % :tag name #{tag})) (:content xs))))]]
    (some->> row (last-of-tag "tc") (last-of-tag "tcPr") (last-of-tag "tcBorders") (last-of-tag "right"))))

(defn- table-set-right-borders
  "Ha egy tablazat utolso oszlopat tavolitottuk el, akkor az utolso elotti oszlop cellaibol a border-right ertekeket
   at kell masolni az utolso oszlop cellaiba"
  [table-loc right-borders]
  (assert (sequential? right-borders))
  (map-each-rows
   (fn [row border]
     (if border
       (if-let [last-col (find-last-child #(and (map? %) (some-> % :tag name #{"tc"})) row)]
         (-> last-col
             (->> (ensure-child "tcPr") (ensure-child "tcBorders") (ensure-child "right"))
             (zip/replace border)
             (find-enclosing-row))
         row)
       row))
   (find-enclosing-table table-loc) right-borders))

(defn- remove-current-column
  "A jelenlegi csomoponthoz tartozo oszlopot eltavolitja a tablazatbol.
   Visszater a gyoker elemmel."
  [start-loc column-resize-strategy]
  (let [column-indices (current-column-indices start-loc)
        table          (find-enclosing-table start-loc)
        right-borders  (get-right-borders table)
        column-last?   (nil? (find-closest-cell-right (zip/right (find-enclosing-cell start-loc))))]
    (-> (map-each-rows #(remove-columns % column-indices column-resize-strategy) table)
        (find-enclosing-table)
        (table-resize-widths column-resize-strategy column-indices)
        (cond-> column-last? (table-set-right-borders right-borders))
        (zip/root))))

;; TODO: handle rowspan property!
(defn- remove-current-row [start]
  (-> start (find-enclosing-row) (zip/remove) (zip/root)))

(defn remove-columns-by-markers-1
  "Megkeresi az elso HideTableColumnMarkert es a tablazatbol a hozza tartozo
   oszlopot kitorli. Visszaadja az XML fat."
  [xml-tree]
  (if-let [marker (find-first-in-tree hide-table-column-marker? (xml-zip xml-tree))]
    (let [resize-strategy (:columns-resize (zip/node marker))]
      (remove-current-column marker resize-strategy))
    xml-tree))

(defn remove-rows-by-markers-1
  "Megkeresi az elso HideTableRowMarkert es a tablazatbol a hozza tartozo
   sort kitorli. Visszaadja az XML fat."
  [xml-tree]
  (if-let [marker (find-first-in-tree hide-table-row-marker? (xml-zip xml-tree))]
    (remove-current-row marker)
    xml-tree))

(defn remove-table-thin-columns-1
  "Ha a tablazatban van olyan oszlop, amely szelessege nagyon kicsi, az egesz oszlopot eltavolitja."
  [xml-tree]
  ;; Ha talalunk olyan gridCol oszlopot, ami nagyon kicsi
  (if-let [loc (find-first-in-tree #(and (map? %)
                                         (some-> % :tag name (#{"gridCol"}))
                                         (some-> % :attrs ooxml-w ->int (< min-col-width))) (xml-zip xml-tree))]
    (let [col-idx (count (filter #(some-> % zip/node :tag) (next (iterations zip/left loc))))
          table-loc (find-enclosing-table (zip/remove loc))]
      (zip/root (map-each-rows #(remove-columns % #{col-idx} :rational) table-loc)))
    xml-tree))

(defn remove-empty-table-rows-1 [xml-tree]
  ;; TODO: implement this
  xml-tree)

(defn remove-empty-tables-1 [xml-tree]
  ;; TODO: implement this
  xml-tree)

(defn fix-tables [xml-tree]
  (->> xml-tree
       (fixpt remove-table-thin-columns-1)
       (fixpt remove-columns-by-markers-1)
       (fixpt remove-rows-by-markers-1)
       (fixpt remove-empty-table-rows-1)
       (fixpt remove-empty-tables-1)))

:ok
