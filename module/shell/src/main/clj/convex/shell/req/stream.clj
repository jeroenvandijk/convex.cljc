(ns convex.shell.req.stream

  "Requests relating to IO streams."

  {:author "Adam Helinski"}

  (:import (convex.core.exceptions ParseException)
           (java.io BufferedReader)
           (java.lang AutoCloseable)
           (java.util.stream Collectors))
  (:refer-clojure :exclude [flush])
  (:require [convex.cell        :as $.cell]
            [convex.cvm         :as $.cvm]
            [convex.read        :as $.read]
            [convex.shell.io    :as $.shell.io]
            [convex.shell.resrc :as $.shell.resrc]
            [convex.std         :as $.std]
            [convex.write       :as $.write]))


(declare close)


;;;;;;;;;;


(let [handle ($.shell.resrc/create $.shell.io/stderr-txt)]

  (defn stderr

    "Request for returning STDERR."

    [ctx _arg+]

    ($.cvm/result-set ctx
                      handle)))



(let [handle ($.shell.resrc/create $.shell.io/stdin-txt)]

  (defn stdin
  
    "Request for returning STDIN."
  
    [ctx _arg+]

    ($.cvm/result-set ctx
                      handle)))



(let [handle ($.shell.resrc/create $.shell.io/stdout-txt)]

  (defn stdout

    "Request for returning STDOUT."

    [ctx _arg+]

    ($.cvm/result-set ctx
                      handle)))


;;;;;;;;;


(defn- -close-when-no-result

  ;; Sometimes it is best closing the stream if an operation returns no result.

  [ctx handle]

  (cond->
    ctx
    (and (not ($.cvm/exception? ctx))
         (nil? ($.cvm/result ctx)))
    (close [handle])))



(defn- -fail

  ;; Used in case of a stream failure.

  [ctx message]

  ($.cvm/exception-set ctx
                       ($.cell/* :STREAM)
                       message))



(defn- -str-cvx

  ;; Stringifies the given `cell` to be readable.

  [cell]

  (str ($.write/string Long/MAX_VALUE
                       cell)))




(defn- -str-txt

  ;; Stringifies the given `cell` but does not double quote if it is a string.

  [cell]

  (if ($.std/string? cell)
    (str cell)
    (-str-cvx cell)))


;;;;;;;;;;


(defn operation

  "Generic function for carrying out an operation.

   Handles failure."

  [ctx handle op+ f]

  (let [[ok?
         x]  ($.shell.resrc/unwrap ctx
                                   handle)]
    (if ok?
      (let [stream x]
        (try
          ;;
          (f ctx
             stream)
          ;;
          (catch ClassCastException _ex
            (-fail ctx
                   ($.cell/string (format "Stream is missing capability: %s"
                                          op+))))
          ;;
          (catch ParseException ex
            ($.cvm/exception-set ctx
                                 ($.cell/* :READER)
                                 ($.cell/string (.getMessage ex))))
          ;;
          (catch Throwable _ex
            (-fail ctx
                   ($.cell/string (format "Stream failed while performing: %s"
                                          op+))))))
      (let [ctx-2 x]
        ctx-2))))


;;;;;;;;;;


(defn close

  "Request for closing the given stream.

   A result to propagate may be provided."


  ([ctx arg+]

   (close ctx
          arg+
          nil))


  ([ctx [handle] result]

   (operation ctx
              handle
              #{:close}
              (fn [ctx-2 ^AutoCloseable stream]
                (.close stream)
                ($.cvm/result-set ctx-2
                                  result)))))



(defn flush

  "Request for flushing the requested stream."

  [ctx [handle]]

  (operation ctx
             handle
             #{:flush}
             (fn [ctx-2 stream]
               ($.shell.io/flush stream)
               ($.cvm/result-set ctx-2
                                 handle))))



(defn in+

  "Request for reading all available cells from the given stream and closing it."

  [ctx [handle]]

  (operation ctx
             handle
             #{:read}
             (fn [ctx-2 stream]
               ($.cvm/result-set ctx-2
                                 ($.read/stream stream)))))



(defn line

  "Request for reading a line from the given stream and parsing it into a list of cells."

  [ctx [handle]]

  (-> ctx
      (operation handle
                 #{:read}
                 (fn [ctx-2 stream]
                   ($.cvm/result-set ctx-2
                                     ($.read/line stream))))
      (-close-when-no-result handle)))



(defn- -out

  ;; See [[out]].

  [ctx handle cell stringify]

  (operation ctx
             handle
             #{:write}
             (fn [ctx-2 stream]
               ($.write/stream stream
                               stringify
                               cell)
               ($.cvm/result-set ctx-2
                                 cell))))



(defn out

  "Request for writing a `cell` to the given stream."

  [ctx [handle cell]]

  (-out ctx
        handle
        cell
        -str-cvx))



(defn- -outln

  ;; See [[outln]].

  [env handle cell stringify]

  (operation env
             handle
             #{:flush
               :write}
             (fn [ctx stream]
               ($.write/stream stream
                               stringify
                               cell)
               ($.shell.io/newline stream)
               ($.shell.io/flush stream)
               ($.cvm/result-set ctx
                                 cell))))



(defn outln

  "Like [[out]] but appends a new line and flushes the stream."

  [env [handle cell]]

  (-outln env
          handle
          cell
          -str-cvx))



(defn txt-in

  "Request for reading everything from the given stream as text."

  [ctx [handle]]

  (operation ctx
             handle
             #{:read}
             (fn [ctx-2 ^BufferedReader stream]
               (let [result (-> stream
                                (.lines)
                                (.collect (Collectors/joining (System/lineSeparator)))
                                ($.cell/string))]
                 (close ctx-2
                        [handle]
                        result)))))



(defn txt-line

  "Request for reading a line from the given stream as text."

  [ctx [handle]]

  (-> ctx
      (operation handle
                 #{:read}
                 (fn [ctx-2 ^BufferedReader stream]
                   ($.cvm/result-set ctx-2
                                     (some-> (.readLine stream)
                                             ($.cell/string)))))
      (-close-when-no-result handle)))



(defn txt-out

  "Like [[out]] but if `cell` is a string, then it is not double-quoted."

  [ctx [handle cell]]

  (-out ctx
        handle
        cell
        -str-txt))



(defn txt-outln

  "Is to [[outln]] what [[out-txt]] is to [[out]]."

  [ctx [handle cell]]

  (-outln ctx
          handle
          cell
          -str-txt))
