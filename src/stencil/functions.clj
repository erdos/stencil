(ns stencil.functions
  "Function definitions"
  (:require [clojure.string]
            [stencil.util :refer [fail find-first]]))

(set! *warn-on-reflection* true)

(defmulti call-fn
  "Extend this multimethod to make additional functions available from the template files.
   The first argument is the lowercase function name which is used for dispatching the calls.
   The rest of the arguments are the function call parameters."
  (fn [function-name & _args-seq] function-name))

(defmacro def-stencil-fn [name docs & bodies]
  (assert (string? name))
  (assert (string? docs))
  `(.addMethod ^clojure.lang.MultiFn call-fn ~name
               ~(with-meta `(fn [_# & args#] (apply (fn ~@bodies) args#)) {::docs docs})))

(def-stencil-fn "range"
  "Creates an array of numbers between bounds, for use in iteration forms.
    Parameters are start value (default 0), upper bound, step size (default 1).
   Eg.: range(4) = [0, 1, 2, 3], range(2,4) = [2, 3], range(2, 10, 2) = [2, 4, 8]"
  ([x] (range x))
  ([x y] (range x y))
  ([x y z] (range x y z)))

(def-stencil-fn "integer"
  "Converts parameter to integer number. Returns null for missing value."
  [n] (some-> n biginteger))

(def-stencil-fn "decimal"
  "Converts parameter to decimal number. Returns null for missing value."
  [f] (with-precision 8 (some-> f bigdec)))

;; The format() function calls java.lang.String.format()
;; but it predicts the argument types from the format string and
;; converts the argument values to the correct types to prevent runtime errors.
(let [fs-pattern #"%(?:(\d+)\$)?([-#+ 0,(\<]*)?(\d+)?(\.\d+)?([tT])?([a-zA-Z%])"
      get-types  (fn [pattern-str]
                   (second (reduce (fn [[max-idx types] [_ idx _ _ _ _ type]]
                                     (if idx
                                       [max-idx (assoc types (dec (Long/valueOf ^String idx)) type)]
                                       [(inc max-idx) (assoc types max-idx type)]))
                                   [0 {}]
                                   (re-seq fs-pattern pattern-str))))
      cache (atom ())
      cache-size 32
      get-types (fn [p] (or (some (fn [[k v]] (when (= k p) v)) @cache)
                            (doto (get-types p)
                              (->> (swap! cache (fn [c t] (take cache-size (cons [p t] c))))))))]
  (def-stencil-fn "formatWithLocale"
    "Similar to `format()` but first parameter is an IETF Language Tag.

     **Usage:** `formatWithLocale('hu', '%,.2f', number)`

     **Example:**
     To format the value of price as a price string: {%=format('$ %(,.2f', price) %}. It may output $ (6,217.58)."
    [locale pattern-str & args]
    (when-not (string? pattern-str)
      (fail "Format pattern must be a string!" {:pattern pattern-str}))
    (when (empty? args)
      (fail "Format function expects at least two parameters!" {}))
    (let [types (get-types pattern-str)
          locale (if (string? locale) (java.util.Locale/forLanguageTag ^String locale) locale)]
      (->> args
           (map-indexed (fn [idx value]
                          (case (types idx)
                            ("c" "C")                     (cond (nil? value) nil
                                                                (string? value) (first value)
                                                                :else (char (int value)))
                            ("d" "o" "x" "X")             (some-> value biginteger)
                            ("e" "E" "f" "g" "G" "a" "A") (with-precision 8 (some-> value bigdec))
                            value)))
           (to-array)
           (String/format locale pattern-str)))))

(def-stencil-fn "format"
  "Calls String.format function."
  [pattern-str & args]
  (apply call-fn "formatWithLocale" (java.util.Locale/getDefault) pattern-str args))

;; finds first nonempy argument
(def-stencil-fn "coalesce"
  "Accepts any number of arguments, returns the first not-empty value."
  [& args-seq]
  (find-first (some-fn number? true? false? not-empty) args-seq))

(def-stencil-fn "length"
  "The `length(x)` function returns the length of the value in `x`:
- Returns the number of characters when `x` is a string.
- Returns the number of elements the `x` is a list/array.
- Returns the number of key/value pairs when `x` is an object/map.
- Returns zero when `x` is `null`."
  [items] (count items))

(def-stencil-fn "contains"
  "Expects two arguments: a value and a list. Checks if list contains the value.
   Usage: contains('myValue', myList)"
  [item items] (boolean (some #{(str item)} (map str items))))

(def-stencil-fn "sum"
  "Expects one number argument containing a list with numbers. Sums up the numbers and returns result.
   Usage: sum(myList)"
  [items] (reduce + items))

(def-stencil-fn "list"
  "Creates a list collection from the supplied arguments.
   Intended to be used with other collections functions."
  [& elements] (vec elements))

(defn- lookup [column data]
  (second (or (find data column)
              (find data (keyword column)))))

(def-stencil-fn "map"
  "Selects values under a given key in a sequence of maps. 
   The first parameter is a string which contains what key to select:
   - It can be a single key name
   - It can be a nested key, separated by `.` character. For example: `outerkey.innerkey`
   - It can be used for selecting from multidimensional arrays: `outerkey..innerkey`
   
   Example use cases with data: `{'items': [{'price': 10, 'name': 'Wood'}, {'price': '20', 'name': 'Stone'}]}`

   - `join(map('name', items), ',')`: to create a comma-separated string of item names. Prints `Wood, Stone`.
   - `sum(map('price', items))`: to write the sum of item prices. Prints `30`.
   "
  [^String column data]
  (when-not (string? column)
    (fail "First parameter of map() must be a string!" {}))
  (reduce (fn [elems p]
            (if (empty? p)
              (do (doseq [e elems :when (not (or (nil? e)
                                                 (sequential? e)
                                                 (instance? java.util.List e)))]
                    (fail "Wrong data, expected sequence, got: " {:data e}))
                  (mapcat seq elems))
              (do (doseq [e elems :when (not (or (nil? e) (map? e) (instance? java.util.Map e)))]
                    (fail "Wrong data, expected map, got: " {:data e}))
                  (keep (partial lookup p) elems))))
          data
          (.split column "\\.")))

(def-stencil-fn "joinAnd"
  "Joins a list of items using two separators.
   The first separator is used to join the items except for the last item.
   The second separator is used to join the last item.
   When two items are supplied, then only the second separator is used.

   **Example:** call `joinAnd(xs, ', ', ' and ')` to get `'1, 2, 3 and 4'`."
  [elements ^String separator1 ^String separator2]
  (case (count elements)
    0 ""
    1 (str (first elements))
    (str (clojure.string/join separator1 (butlast elements)) separator2 (last elements))))

(def-stencil-fn "replace"
  "The replace(text, pattern, replacement) function replaces all occurrences
   of pattern in text by replacement."
  [text pattern replacement]
  (clojure.string/replace (str text) (str pattern) (str replacement)))
