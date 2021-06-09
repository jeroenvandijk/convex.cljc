(ns convex.lib.dev.trust

  "Dev environment for the Trust library."

  {:author           "Adam Helinski"
   :clj-kondo/config '{:linters {:unused-namespace {:level :off}}}}

  (:require [convex.code     :as $.code]
            [convex.cvm      :as $.cvm]
            [convex.cvm.eval :as $.cvm.eval]
            [convex.disk     :as $.disk]
            [convex.lisp     :as $.lisp]))


;;;;;;;;;;


(comment


  (def w*ctx
       ($.disk/watch [["src/convex/lib/trust.cvx"
                      {:wrap (partial $.code/deploy
                                      '$)}]]))

  ($.cvm/exception @w*ctx)

  (.close w*ctx)



  ($.cvm.eval/result @w*ctx
                     '(do
                        (let [addr (deploy ($/build-whitelist {:whitelist [42]}))]
                          [($/trusted? addr
                                       42)
                           ($/trusted? addr
                                       100)])))


  ($.cvm.eval/result @w*ctx
                     '(do
                        (let [addr (deploy ($/add-trusted-upgrade nil))]
                          (call addr
                                (upgrade '(def foo 42)))
                          (lookup addr
                                  'foo))))

  )
