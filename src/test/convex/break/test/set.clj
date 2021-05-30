(ns convex.break.test.set

  "Testing set operations."

  {:author "Adam Helinski"}

  (:require [clojure.test.check.generators :as TC.gen]
            [clojure.test.check.properties :as TC.prop]
            [convex.break.eval             :as $.break.eval]
            [convex.break.gen              :as $.break.gen]
            [convex.break.prop             :as $.break.prop]
            [convex.lisp.gen               :as $.lisp.gen]))


;;;;;;;;;;


($.break.prop/deftest mono

  ;; Using only one set.

  (TC.prop/for-all [s $.lisp.gen/set]
    (let [ctx ($.break.eval/ctx* (def s
                                      ~s))]
      ($.break.prop/mult*

        "There is no difference between a set and itself"
        ($.break.eval/result ctx
                             '(= #{}
                                 (difference s
                                             s)))

        "The intersection of a set with itself is the set itself"
        ($.break.eval/result ctx
                             '(= s
                                 (intersection s
                                               s)))

        "A set is a subset of itself"
        ($.break.eval/result ctx
                             '(subset? s
                                       s))

        "The union of a set with itself is the set itself"
        ($.break.eval/result ctx
                             '(= s
                                 (union s
                                        s)))

        "`empty`"
        ($.break.eval/result ctx
                             '(= #{}
                                 (empty s)))

        "Rebuilding set using `into` on empty set"
        ($.break.eval/result ctx
                             '(= s
                                 (into (empty s)
                                       s)))

        "Rebuilding set using `into` on the set itself"
        ($.break.eval/result ctx
                             '(= s
                                 (into s
                                       s)))

        "Rebuilding set from list using `into`"
        ($.break.eval/result ctx
                             '(= s
                                 (into (empty s)
                                       (into (list)
                                             s))))

        "Rebuilding set by applying it to `conj`"
        ($.break.eval/result ctx
                             '(= s
                                 (apply conj
                                        #{}
                                        (vec s))))

        "Adding all values of set into that same set does not change anything"
        ($.break.eval/result ctx
                             '(= s
                                 (reduce conj
                                         s
                                         s)))

        "A set contains each of its values"
        ($.break.eval/result ctx
                             '($/every? (fn [v]
                                          (contains-key? s
                                                         v))
                                        s))

        "`disj` consistent with `contains-key?`"
        ($.break.eval/result ctx
                             '($/every? (fn [v]
                                          (not (contains-key? (disj s
                                                                    v)
                                                              v)))
                                        s))

        "`disj` all values returns an empty set"
        ($.break.eval/result ctx
                             '(= #{}
                                 (reduce disj
                                         s
                                         s)))))))



#_($.break.prop/deftest poly

  ;; Using at least 2 sets.

  ;; TODO. Fails because of: https://github.com/Convex-Dev/convex/issues/155

  (TC.prop/for-all [s+ (TC.gen/vector $.break.gen/maybe-set
                                      2
                                      8)]
    (let [ctx ($.break.eval/ctx* (do
                                   (def s+
                                        ~s+)
                                   (def -difference
                                        (apply difference
                                               s+))
                                   (def -intersection
                                        (apply intersection
                                               s+))
                                   (def -union
                                        (apply union
                                               s+))
                                   (def n-difference
                                        (count -difference))
                                   (def n-intersection
                                        (count -intersection))
                                   (def n-s+
                                        (mapv count
                                              s+))
                                   (def n-union
                                        (count -union))))]
      ($.break.prop/mult*

        "Difference is a set"
        ($.break.eval/result ctx
                             '(set? -difference))

        "Intersection is a set"
        ($.break.eval/result ctx
                             '(set? -intersection))

        "Union is a set"
        ($.break.eval/result ctx
                             '(set? -union))

        "Difference cannot be bigger than first set"
        ($.break.eval/result ctx
                             '(<= n-difference
                                  (first n-s+)))

        "Difference cannot be bigger than union"
        ($.break.eval/result ctx
                             '(<= n-difference
                                  n-union))

        "Intersection cannot be bigger th first set"
        ($.break.eval/result ctx
                             '(<= n-intersection
                                  (first n-s+)))

        "Intersection cannot be bigger than union"
        ($.break.eval/result ctx
                             '(<= n-intersection
                                  n-union))

        "All sets are <= than union"
        ($.break.eval/result ctx
                             '($/every? (fn [s]
                                          (<= (count s)
                                              n-union))
                                        s+))

        "Difference is a subset of first set"
        ($.break.eval/result ctx
                             '(let [s (first s+)]
                                (and (subset? -difference
                                              s)
                                     ($/every? (fn [v]
                                                 (contains-key? s
                                                                v))
                                               -difference))))

        "Non-empty difference is not a subset of sets other than first one"
        ($.break.eval/result ctx
                             '(if (empty? -difference)
                                true
                                ($/every? (fn [s]
                                            (and (not (subset? -difference
                                                               s))
                                                 ($/every? (fn [v]
                                                             (not (contains-key? s
                                                                                 v)))
                                                           -difference)))
                                          (next s+))))

        "Intersection is a subset of all sets"
        ($.break.eval/result ctx
                             '($/every? (fn [s]
                                          (and (subset? -intersection
                                                        s)
                                               ($/every? (fn [v]
                                                           (contains-key? s
                                                                          v))
                                                         -intersection)))
                                        s+))

        "All sets are subsets of union"
        ($.break.eval/result ctx
                             '($/every? (fn [s]
                                          (and (subset? s
                                                        -union)
                                               ($/every? (fn [v]
                                                           (contains-key? -union
                                                                          v))
                                                         s)))
                                        s+))

        "Emulating union with `into`"
        ($.break.eval/result ctx
                             '(= -union
                                 (reduce into
                                         #{}
                                         s+)))

        "Removing difference items from first set removes the difference"
        ($.break.eval/result ctx
                             '(= #{}
                                 (apply difference
                                        (cons (reduce disj
                                                      (first s+)
                                                      -difference)
                                              (next s+)))))

        "Removing intersection items from first set removes the intersection"
        ($.break.eval/result ctx
                             '(= #{}
                                 (apply intersection
                                        (cons (reduce disj
                                                      (first s+)
                                                      -intersection)
                                              (next s+)))))

        "Difference and intersection have nothing in common"
        ($.break.eval/result ctx
                             '(= #{}
                                 (intersection -difference
                                               -intersection)
                                 (intersection -intersection
                                               -difference)))

        "Difference between difference and intersection is difference"
        ($.break.eval/result ctx
                             '(= (difference -difference
                                             -intersection)
                                 -difference))

        "Difference between intersection and difference is intersection"
        ($.break.eval/result ctx
                             '(= (difference -intersection
                                             -difference)
                                 -intersection))

        ;; TODO. Fails because of: https://github.com/Convex-Dev/convex/issues/155
        ;;
        ;; "No difference between difference and union"
        ;; ($.break.eval/result ctx
        ;;                     '(= #{}
        ;;                         (difference -difference
        ;;                                     -union)))

        "No difference between intersection and union"
        ($.break.eval/result ctx
                             '(= #{}
                                 (difference -intersection
                                             -union)))

        "Intersection between difference and intersection is a subset of first set"
        ($.break.eval/result ctx
                             '(subset? (intersection -difference
                                                     -intersection)
                                       (first s+)))

        ;; TODO. Fails because of: https://github.com/Convex-Dev/convex/issues/155
        ;;
        ;; "Intersection between difference and union is difference"
        ;; ($.break.eval/result ctx
        ;;                     '(= -difference
        ;;                         (intersection -difference
        ;;                                       -union)))

        "Intersection between intersection and union is intersection"
        ($.break.eval/result ctx
                             '(= -intersection
                                 (intersection -intersection
                                               -union)))

        "Union between difference and intersection is a subset of first set"
        ($.break.eval/result ctx
                            '(subset? (union -difference
                                             -intersection)
                                      (first s+)))

        ;; "Union between difference and union is union"
        ;; ($.break.eval/result ctx
        ;;                     '(= -union
        ;;                         (union -difference
        ;;                                -union)))

        "Union between intersection and union is union"
        ($.break.eval/result ctx
                             '(= -union
                                 (union -intersection
                                        -union)))

        ;; TODO. Fails because of: https://github.com/Convex-Dev/convex/issues/153 
        ;;
        ;; "Order of arguments does not matter in `union`"
        ;; ($.break.eval/result ctx
        ;;                     '(= -union
        ;;                         (apply union
        ;;                                (into (list)
        ;;                                      s+))))
        ))))
