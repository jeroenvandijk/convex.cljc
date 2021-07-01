(ns convex.dev

  "Requires all useful namespaces from all projects for development."

  {:author           "Adam Helinski"
   :clj-kondo/config '{:linters {:unused-import    {:level :off}
                                 :unused-namespace {:level :off}}}}

  (:require [clojure.data]
            [clojure.pprint]
            [convex.app.fuzz]
            [convex.app.run]
            [convex.app.run.help]
            [convex.break]
            [convex.break.gen]
            [convex.break.test.account]
            [convex.break.test.actor]
            [convex.break.test.code]
            [convex.break.test.coerce]
            [convex.break.test.coll]
            [convex.break.test.ctrl]
            [convex.break.test.def]
            [convex.break.test.fn]
            [convex.break.test.fuzz]
            [convex.break.test.literal]
            [convex.break.test.loop]
            [convex.break.test.math]
            [convex.break.test.pred]
            [convex.break.test.set]
            [convex.break.test.symbolic]
            [convex.break.test.syntax]
            [convex.clj                    :as $.clj]
            [convex.clj.gen                :as $.clj.gen]
            [convex.clj.test]
            [convex.code                   :as $.code]
            [convex.cvm                    :as $.cvm]
            [convex.clj.eval               :as $.clj.eval]
            [convex.cvm.test]
            [convex.example.disk]
            [convex.example.exec]
            [convex.example.templ]
            [convex.deploy.lib.trust]
            [convex.deploy.lib.trust.test]
			[convex.deploy.lib.xform]
            [convex.run                    :as $.run]
            [convex.run.ctx                :as $.run.ctx]
            [convex.run.err                :as $.run.err]
            [convex.run.exec               :as $.run.exec]
            [convex.run.kw                 :as $.run.kw]
            [convex.run.sreq               :as $.run.sreq]
            [convex.run.sym                :as $.run.sym]
            [convex.sync                   :as $.sync]
            [convex.watch                  :as $.watch]
            [clojure.test.check.generators :as TC.gen]))


(set! *warn-on-reflection*
      true)


;;;;;;;;;;


(def ppr
     clojure.pprint/pprint)


(defn tap

  ""

  [x]

  (println x)
  (flush))


(add-tap tap)


;;;;;;;;;;


(comment

  
  )