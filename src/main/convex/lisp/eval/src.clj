(ns convex.lisp.eval.src

  "Mimicks [[convex.lisp.eval]] but for evaling Convex Lisp source, strings of code."

  {:author "Adam Helinski"}

  (:require [convex.lisp     :as $]
            [convex.lisp.ctx :as $.ctx]))


;;;;;;;;;;


(defn ctx

  "Reads Convex Lisp source, evaluates it, and returns `ctx`."

  [ctx src]

  ($.ctx/eval ($.ctx/fork ctx)
              ($/read src)))



(defn error

  "Like [[ctx]] but returns the error that has occured (or nil)."

  [ctx src]

  (-> (convex.lisp.eval.src/ctx ctx
                                src)
      $.ctx/error
      $/datafy))



(defn error?

  "Like [[ctx]] but returns a boolean indicating if an error occured."

  [ctx src]

  (-> (convex.lisp.eval.src/ctx ctx
                                src)
      $.ctx/error
      some?))



(defn result

  "Like [[ctx]] but returns the result as Clojure data."

  [ctx src]

  (-> (convex.lisp.eval.src/ctx ctx
                                src)
      $.ctx/result
      $/datafy))



(defn value

  "Like [[ctx]] but returns either an [[error]] or a [[result]]."
  
  [ctx src]

  (let [ctx-2 (convex.lisp.eval.src/ctx ctx
                                        src)
        error ($.ctx/error ctx-2)]
    (if (nil? error)
      (-> ctx-2
          $.ctx/result
          $/datafy)
      error)))
