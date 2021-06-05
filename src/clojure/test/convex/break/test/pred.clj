(ns convex.break.test.pred

  "Tests Convex core type predicate. 
  
   Specialized predicates such as `contains-key?` or `fn?` are located in relevant namespace."

  {:author "Adam Helinski"}

  (:require [clojure.test                  :as t]
            [clojure.test.check.generators :as TC.gen]
            [clojure.test.check.properties :as TC.prop]
            [convex.break.eval             :as $.break.eval]
            [convex.lisp                   :as $.lisp]
            [convex.lisp.gen               :as $.lisp.gen]
            [helins.mprop                  :as mprop]))


;;;;;;;;;;


(defn- -prop

  ;; Used by [[pred-data-false]] and [[pred-data-true]].

  [form result? f gen]

  (TC.prop/for-all [x gen]
    (let [result ($.break.eval/result* (~form ~x))]

      (mprop/mult

        "Returns right boolean value"

        (result? result)
        

        "Consistent with Clojure"

        (if f
          (= result
             (f x))
          true)))))



(defn prop-false

  "Like [[pred-data-true]] but tests for negative results."


  ([form gen]

   (prop-false form
               nil
               gen))


  ([form f gen]

   (-prop form
          false?
          f
          gen)))



(defn prop-true

  "Tests if a value generated by `gen` passes a data predicate on the CVM.
  
   If `f-clojure` is given, also ensures that the very same value produces the exact same result
   in Clojure."


  ([form gen]

   (prop-true form
              nil
              gen))


  ([form f gen]

   (-prop form
          true?
          f
          gen)))


;;;;;;;;;;


(mprop/deftest account?--false

  {:ratio-num 10}

  (prop-false 'account?
              (TC.gen/such-that (comp not
                                      $.lisp/address?)
                                $.lisp.gen/any)))



(mprop/deftest address?--false

  {:ratio-num 10}

  (prop-false 'address?
              (TC.gen/such-that (comp not
                                      $.lisp/address?)
                                $.lisp.gen/any)))



(mprop/deftest address?--true

  {:ratio-num 50}

  (TC.prop/for-all* [$.lisp.gen/address]
                    #($.break.eval/result (list 'address?
                                                %))))



(mprop/deftest blob?--false

  {:ratio-num 10}

  (prop-false 'blob?
              (TC.gen/such-that (comp not
                                      $.lisp/blob?)
                                $.lisp.gen/any)))



(mprop/deftest blob?--true

  {:ratio-num 50}

  (prop-true 'blob?
             $.lisp.gen/blob))



(mprop/deftest boolean?--false

  {:ratio-num 10}

  (prop-false 'boolean?
              boolean?
              (TC.gen/such-that (comp not
                                      boolean?)
                                $.lisp.gen/any)))



(t/deftest boolean?--true

  (t/is (true? ($.break.eval/result true))
        "True")

  (t/is (false? ($.break.eval/result false))
        "False"))



#_(mprop/deftest coll?--false

  {:ratio-num 10}

  ;; TODO. Returns true on blob-like items.

  (prop-false 'coll?
              coll?
              $.lisp.gen/scalar))



(mprop/deftest coll?--true

  {:ratio-num 10}

  (prop-true 'coll?
             coll?
             $.lisp.gen/collection))



(mprop/deftest keyword?--false

  {:ratio-num 10}

  (prop-false 'keyword?
              keyword?
              (TC.gen/such-that (comp not
                                      keyword?)
                                $.lisp.gen/any)))



(mprop/deftest keyword?--true

  {:ratio-num 50}

  (prop-true 'keyword?
             keyword?
             $.lisp.gen/keyword))



(mprop/deftest list?--false

  {:ratio-num 15}

  (prop-false 'list?
              $.lisp/list?
              (TC.gen/such-that (comp not
                                      $.lisp/list?)
                                $.lisp.gen/any)))



(mprop/deftest list?--true

  {:ratio-num 10}

  (prop-true 'list?
             $.lisp/list?
             $.lisp.gen/list))



(mprop/deftest long?--false

  {:ratio-num 10}

  (prop-false 'long?
              int?
              (TC.gen/such-that (comp not
                                      int?)
                                $.lisp.gen/any)))



(mprop/deftest long?--true

  {:ratio-num 50}

  (prop-true 'long?
             int?
             $.lisp.gen/long))



(mprop/deftest map?--false

  {:ratio-num 15}

  (prop-false 'map?
              map?
              (TC.gen/such-that #(not (map? %))
                                $.lisp.gen/any)))



(mprop/deftest map?--true

  {:ratio-num 10}

  (prop-true 'map?
             map?
             $.lisp.gen/map))



(mprop/deftest nil?--false

  {:ratio-num 10}

  (prop-false 'nil?
              nil?
              (TC.gen/such-that some?
                                $.lisp.gen/any)))



(t/deftest nil?--true

  (t/is (true? (nil? ($.break.eval/result nil))))

  (t/is (true? (nil? ($.break.eval/result '(do nil))))))



(mprop/deftest number?--false

  {:ratio-num 10}

  (prop-false 'number?
              number?
              (TC.gen/such-that (comp not
                                      number?)
                                $.lisp.gen/any)))



(mprop/deftest number?--true

  {:ratio-num 50}

  (prop-true 'number?
             number?
             $.lisp.gen/number))



(mprop/deftest set?--false

  {:ratio-num 10}

  (prop-false 'set?
              set?
              (TC.gen/such-that (comp not
                                      set?)
                                $.lisp.gen/any)))



(mprop/deftest set?--true

  {:ratio-num 10}

  (prop-true 'set?
             set?
             $.lisp.gen/set))



(mprop/deftest str?--false

  {:ratio-num 10}

  (prop-false 'str?
              string?
              (TC.gen/such-that (comp not
                                      string?)
                                $.lisp.gen/any)))



(mprop/deftest str?--true

  {:ratio-num 10}

  (prop-true 'str?
             string?
             $.lisp.gen/string))



(mprop/deftest symbol?--false

  {:ratio-num 10}

  (prop-false 'symbol?
              (TC.gen/such-that (comp not
                                      $.lisp/quoted?)
                                $.lisp.gen/any)))



(mprop/deftest symbol?--true

  {:ratio-num 50}

  (prop-true 'symbol?
             $.lisp.gen/symbol-quoted))



(mprop/deftest vector?--false

  {:ratio-num 10}

  (prop-false 'vector?
              vector?
              (TC.gen/such-that (comp not
                                      vector?)
                                $.lisp.gen/any)))



(mprop/deftest vector?--true

  {:ratio-num 10}

  (prop-true 'vector?
             vector?
             $.lisp.gen/vector))
