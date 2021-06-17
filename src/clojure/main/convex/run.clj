(ns convex.run

  ""

  {:author "Adam Helinski"}

  (:import (convex.core.lang.impl ErrorValue)
           (java.io File))
  (:refer-clojure :exclude [eval
                            load])
  (:require [clojure.string]
            [convex.code     :as $.code]
            [convex.cvm      :as $.cvm]
            [convex.sync     :as $.sync]
            [convex.watch    :as $.watch]))


(declare eval-form
         eval-trx
         eval-trx+
         out)


;;;;;;;;;; CVM keywords


(def kw-arity

  ""

  ($.code/keyword "arity"))



(def kw-code

  ""

  ($.code/keyword "code"))



(def kw-error

  ""

  ($.code/keyword "cvm.error"))



(def kw-eval-trx

  ""

  ($.code/keyword "eval.trx"))



(def kw-exception

  ""

  ($.code/keyword "exception"))


(def kw-expansion

  ""

  ($.code/keyword "expansion"))



(def kw-inject-value+

  ""

  ($.code/keyword "inject-value+"))



(def kw-main-src

  ""

  ($.code/keyword "main.src"))



(def kw-message

  ""

  ($.code/keyword "message"))



(def kw-read-illegal

  ""

  ($.code/keyword "read.illegal"))



(def kw-read-src

  ""

  ($.code/keyword "read.src"))



(def kw-strx-unknown

  ""

  ($.code/keyword "cvm.unknown"))



(def kw-sync-dep+

  ""

  ($.code/keyword "sync-dep+"))



(def kw-trace

  ""

  ($.code/keyword "trace"))


;;;;;;;;;; CVM symbols


(def sym-catch

  ""

  ($.code/symbol "cvm.catch"))



(def sym-cycle

  ""

  ($.code/symbol "*cvm.cycle*"))



(def sym-error

  ""

  ($.code/symbol "*cvm.error*"))



(def sym-juice-last

  ""

  ($.code/symbol "*cvm.juice.last*"))



(def sym-trx-id

  ""

  ($.code/symbol "*cvm.trx.id*"))


;;;;;;;;;; Miscellaneous


(defn datafy-exception

  ""

  [^ErrorValue exception]

  ($.code/map {kw-code    (.getCode exception)
               kw-message (.getMessage exception)
               kw-trace   ($.code/vector (.getTrace exception))}))



(def d*ctx-base

  ""

  (delay
    ($.cvm/juice-refill ($.cvm/ctx))))



(defn dep+

  ""

  [trx+]

  (let [trx-first (first trx+)]
    (when ($.code/list? trx-first)
      (let [item (first trx-first)]
        (when ($.code/symbol? item)
          (when (= (str item)
                   "cvm.read")
            (not-empty (reduce (fn [hmap x]
                                 (assoc hmap
                                        (str (first x))
                                        (str (second x))))
                               {}
                               (second trx-first)))))))))



(defn update-hook-fn


  ""

  [env kw form]

  (if-some [hook (second form)]
    (let [env-2 (eval-trx env
                          hook)]
      (if (env-2 :convex.run/error)
        env-2
        (assoc-in env-2
                  [:convex.run/hook+
                   kw]
                  (-> env-2
                      :convex.sync/ctx
                      $.cvm/result))))
    (update env
            :convex.run/hook+
            dissoc
            kw)))


;;;;;;;;;; Output


(defn error

  ""

  [env cvm-kw-type cvm-data]

  ((env :convex.run/on-error)
   (assoc env
          :convex.run/error
          ($.code/vector [cvm-kw-type
                          cvm-data]))))



(defn error-default

  ""

  [env]

  (out env
       ($.code/vector [kw-error
                       (env :convex.run/error)])))



(defn out-default

  ""

  [x]

  (-> x
      str
      tap>))



(defn out

  ""

  [env x]

  (let [out' (env :convex.run/out)
        hook (get-in env
                     [:convex.run/hook+
                      :out])]
    (if hook
      (let [on-error (env :convex.run/on-error)
            env-2    (-> env
                         (assoc :convex.run/on-error
                                identity)
                         (eval-form ($.code/list [hook
                                                  ($.code/quote x)]))
                         (assoc :convex.run/on-error
                                on-error))
            error    (env-2 :convex.run/error)]
        (if error
          (do
            (out' ($.code/string "Fatal error: output hook"))
            env-2)
          (do
            (out' (-> env-2
                      :convex.sync/ctx
                      $.cvm/result))
            env-2)))
      (do
        (out' x)
        env))))


;;;;;;;;;; Special transactions


(defn cvm-do

  ""

  [env form]

  (eval-trx+ env
             (rest form)))



(defn cvm-hook-end

  ""

  [env form]

  (let [form+ (next form)]
    (if (and form+
             (not= form+
                   [nil]))
      (assoc-in env
                [:convex.run/hook+
                 :end]
                form+)
      (update env
              :convex.run/hook+
              dissoc
              :end))))



(defn cvm-hook-out

  ""

  [env form]

  (update-hook-fn env
                  :out
                  form))



(defn cvm-hook-trx

  ""

  [env form]

  (update-hook-fn env
                  :trx
                  form))



(defn cvm-out

  ""

  [env form]

  (let [env-2 (eval-trx env
                        (second form))]
    (if (env-2 :convex.run/error)
      env-2
      (out env-2
           (-> env-2
               :convex.sync/ctx
               $.cvm/result)))))



(defn cvm-out-clear

  ""

  ;; https://www.delftstack.com/howto/java/java-clear-console/

  [env _form]

  (print "\033[H\033[2J")
  env)



(defn cvm-log

  ""

  [env form]

  (let [cvm-sym (second form)]
    (eval-form env
               ($.code/def cvm-sym
                           ($.cvm/log (env :convex.sync/ctx))))))



(defn cvm-read

  ""

  [env _form]

  (error env
         kw-read-illegal
         ($.code/string "CVM special command 'cvm.read' can only be used as first transaction")))



(defn cvm-try

  ""

  [env form]

  (let [form-last (last form)
        catch?    ($.code/call? form-last
                                sym-catch)
        on-error  (env :convex.run/on-error)]
    (-> env
        (assoc :convex.run/on-error
               (fn [env-2]
                 (-> env-2
                     (dissoc :convex.run/error)
                     (cond->
                       catch?
                       (-> (assoc :convex.run/on-error
                                  on-error)
                           (eval-form ($.code/def sym-error
                                                  (env-2 :convex.run/error)))
                           (eval-trx+ (rest form-last))
                           (eval-form ($.code/undef sym-error))))
                     (assoc :convex.run/error
                            :try))))
        (eval-trx+ (-> form
                       rest
                       (cond->
                         catch?
                         butlast)))
        (assoc :convex.run/on-error
               on-error)
        (dissoc :convex.run/error))))


;;;;;


(defn strx

  "Special transaction"

  [trx]

  (when ($.code/list? trx)
    (let [sym-string (str (first trx))]
      (when (clojure.string/starts-with? sym-string
                                         "cvm.")
        (case sym-string
          "cvm.do"        cvm-do
          "cvm.hook.end"  cvm-hook-end
          "cvm.hook.out"  cvm-hook-out
          "cvm.hook.trx"  cvm-hook-trx
          "cvm.log"       cvm-log
          "cvm.out"       cvm-out
          "cvm.out.clear" cvm-out-clear
          "cvm.read"      cvm-read
          "cvm.try"       cvm-try
          (fn [env _trx]
            (error env
                   kw-strx-unknown
                   ($.code/string sym-string))))))))


;;;;;;;;;; Preparing transactions


(defn expand

  ""

  [env form]

  (let [ctx-2     ($.cvm/expand (env :convex.sync/ctx)
                                form)
        exception ($.cvm/exception ctx-2)]
    (if exception
      (error env
             kw-expansion
             ($.code/vector [(datafy-exception exception)
                             ($.code/quote form)]))
      (assoc env
             :convex.sync/ctx
             ctx-2))))



(defn inject-value+

  ""

  [env]

  (let [ctx       ($.cvm/eval (env :convex.sync/ctx)
                              ($.code/do [($.code/def sym-juice-last
                                                      ($.code/long (env :convex.run/juice-last)))
                                          ($.code/def sym-trx-id
                                                      ($.code/long (env :convex.run/i-trx)))]))
        exception ($.cvm/exception ctx)]
    (if exception
      (error env
             kw-inject-value+
             (datafy-exception exception))
      (assoc env
             :convex.sync/ctx
             ctx))))


;;;;;;;;;; Evaluation


(defn eval-form

  ""

  [env form]

  (let [ctx       ($.cvm/juice-refill (env :convex.sync/ctx))
        juice     ($.cvm/juice ctx)
        ctx-2     ($.cvm/eval ctx
                              form)
        exception ($.cvm/exception ctx-2)]
    (cond->
      (-> env
          (assoc :convex.run/juice-last (- juice
                                           ($.cvm/juice ctx-2))
                 :convex.sync/ctx       ctx-2)
          (update :convex.run/i-trx
                  inc))
      exception
      (error kw-exception
             ($.code/vector [(datafy-exception exception)
                             ($.code/quote form)])))))



(defn eval-trx

  ""

  [env trx]

  (if-some [f-strx (strx trx)]
    (f-strx env
            trx)
    (let [env-2 (inject-value+ env)]
      (if (env-2 :convex.run/error)
        env-2
        (let [env-3 (expand env-2
                            trx)]
          (if (env-3 :convex.run/error)
            env-3
            (let [trx-2 (-> env-3
                            :convex.sync/ctx
                            $.cvm/result)]
              (if-some [hook-trx (get-in env
                                         [:convex.run/hook+
                                          :trx])]
                (let [env-4 (eval-form env-3
                                       ($.code/list [hook-trx
                                                     ($.code/quote trx-2)]))]
                  (if (env-4 :convex.run/error)
                    env-4
                    (-> env-4
                        (update :convex.run/hook+
                                dissoc
                                :trx)
                        (eval-trx (-> env-4
                                      :convex.sync/ctx
                                      $.cvm/result))
                        (assoc-in [:convex.run/hook+
                                   :trx]
                                  hook-trx))))
                (eval-form env-3
                           trx-2)))))))))



(defn eval-trx+

  ""

  
  ([env]

   (eval-trx+ env
              (env :convex.run/trx+)))


  ([env trx+]

   (reduce (fn [env-2 trx]
             (let [env-3 (eval-trx env-2
                                   trx)]
               (if (env-3 :convex.run/error)
                 (reduced env-3)
                 env-3)))
           env
           trx+)))



(defn exec-trx+

  ""

  [env]

  (let [env-2    (-> env
                     (assoc :convex.run/i-trx      0
                            :convex.run/juice-last 0)
                     (update :convex.sync/ctx
                             (fn [ctx]
                               ($.cvm/eval ctx
                                           ($.code/def sym-cycle
                                                       ($.code/long (or (env :convex.watch/cycle)
                                                                        0))))))
                     eval-trx+)
        hook-end (get-in env-2 
                         [:convex.run/hook+
                          :end])]
    (-> (if hook-end
          (let [env-3 (eval-trx+ (dissoc env-2
                                         :convex.run/error)
                                 hook-end)]
            (if (env-3 :convex.run/error)
              (out env-3
                   ($.code/string "Fatal error: end hook"))
              env-3))
          env-2)
        (dissoc :convex.run/hook+))))


;;;;;;;;;; 


(defn init

  ""

  [env]

  (-> env
      (update :convex.run/on-error
              #(or %
                   error-default))
      (update :convex.run/out
              #(or %
                   out-default))))



(defn read-src

  ""

  [env path]

  (let [[err
         src] (try
                [nil
                 (slurp path)]
                (catch Throwable err
                  [err
                   nil]))]
    (if err
      (error env
             kw-main-src
             ($.code/vector [($.code/string path)
                             ;; TODO. Datafy JVM exception.
                             err]))
      (assoc env
             :convex.run/src
             src))))



(defn process-src

  ""

  [env]

  (let [[err
         trx+] (try
                 [nil
                  ($.cvm/read-many (env :convex.run/src))]
                 (catch Throwable err
                   [err
                    nil]))]
    (if err
      (error env
             kw-read-src
             ;; TODO. Datafy JVM exception.
             err)
      (let [dep+' (dep+ trx+)]
        (-> env
            (assoc :convex.run/dep+ dep+'
                   :convex.run/trx+ (cond->
                                      trx+
                                      (seq dep+')
                                      rest))
            (dissoc :convex.run/src))))))



(defn main-file

  ""

  [env path]

  (let [env-2 (-> env
                  init
                  (read-src path))]
    (if (env-2 :convex.run/error)
      env-2
      (process-src env-2))))



(defn once

  ""

  [env]

  (if (env :convex.run/error)
    env
    (if-some [dep+' (env :convex.run/dep+)]
      (let [env-2    (merge env
                            ($.sync/disk ($.cvm/fork @d*ctx-base)
                                         dep+'))
            err-sync (env-2 :convex.sync/error)]
        (if err-sync
          (error env-2
                 kw-sync-dep+
                 ;; TODO. Datafy JVM exception.
                 err-sync)
          (exec-trx+ env-2)))
      (-> env
          (assoc :convex.sync/ctx
                 ($.cvm/fork @d*ctx-base))
          exec-trx+))))


;;;;;;;;;; Evaluating a given source string


(defn eval

  ""


  ([src]

   (eval nil
         src))


  ([env src]

   (-> env
       init
       (assoc :convex.run/src
              src)
       process-src
       once)))


;;;;;;;;;; Load files


(defn load

  ""


  ([path]

   (load nil
         path))


  ([env path]

   (-> (main-file env
                  path)
       once)))


;;;;;;;;;; Watch files


(defn watch

  ""


  ([path]

   (watch nil
          path))


  ([env ^String path]

   (let [a*env ($.watch/init (assoc env
                                    :convex.watch/extra+
                                    #{(.getCanonicalPath (File. path))}))]
     (send a*env
           (fn [env]
             (assoc env
                    :convex.watch/on-change
                    (fn on-change [{:as      env-2
                                    dep-old+ :convex.run/dep+
                                    err-sync :convex.sync/error}]
                      (let [env-3 (dissoc env-2
                                          :convex.run/error)]
                        (if err-sync
                          (error env-3
                                 kw-sync-dep+
                                 ;; TODO. Datafy JVM exception.
                                 err-sync)
                          (if (or (nil? dep-old+)
                                  (seq (env-3 :convex.watch/extra->change)))
                            (let [env-4 (main-file env-3
                                                   path)]
                              (if (env-4 :convex.error/error)
                                env-4
                                (let [dep-new+ (env-4 :convex.run/dep+)]
                                  (if (= (not-empty dep-new+)
                                         dep-old+)
                                    (-> env-4
                                        (dissoc :convex.watch/extra->change)
                                        $.sync/patch
                                        $.sync/eval
                                        exec-trx+)
                                    (do
                                      ($.watch/-stop env-4)
                                      ($.watch/-start a*env
                                                      (-> (select-keys env-4
                                                                       [:convex.run/dep+
                                                                        :convex.run/on-error
                                                                        :convex.run/out
                                                                        :convex.run/trx+
                                                                        :convex.sync/ctx-base
                                                                        :convex.watch/cycle
                                                                        :convex.watch/extra+
                                                                        :convex.watch/ms-debounce
                                                                        :convex.watch/on-change])
                                                          (assoc :convex.watch/sym->dep
                                                                 dep-new+))))))))
                            (exec-trx+ env-3))))))))
     ($.watch/start a*env)
     a*env)))


;;;;;;;;;;


(comment


  (eval "(cvm.out (+ 2 2))")



  (load "src/convex/dev/app/run.cvx")



  (def a*env
       (watch "src/convex/dev/app/run.cvx"))

  (clojure.pprint/pprint (dissoc @a*env
                                 :input->code))

  ($.watch/stop a*env)


  (agent-error a*env)


  )