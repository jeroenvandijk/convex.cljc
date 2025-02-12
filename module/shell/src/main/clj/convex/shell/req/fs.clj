(ns convex.shell.req.fs

  "Requests relating to filesystem utilities."

  {:author "Adam Helinski"}

  (:import (java.io File)
           (java.nio.file DirectoryNotEmptyException
                          Files
                          NoSuchFileException
                          Path
                          StandardCopyOption)
           (java.nio.file.attribute FileAttribute))
  (:refer-clojure :exclude [resolve])
  (:require [babashka.fs :as bb.fs]
            [convex.cell :as $.cell]
            [convex.cvm  :as $.cvm]
            [convex.std  :as $.std]))


;;;;;;;;;;


(defn copy

  "Request for copying files and directories like Unix's `cp`."

  [ctx [source destination]]

  (or (when-not ($.std/string? source)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Source to copy must be a string")))
      (when-not ($.std/string? destination)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Destination copy must be a string")))
      (try
        (let [^String source-2         (str source)
              ^Path   source-path      (.toPath (File. source-2))
                      copy             (fn [^File destination-file]
                                         (Files/copy ^Path source-path
                                                     (.toPath destination-file)
                                                     ^"[Ljava.nio.file.StandardCopyOption;"
                                                     (into-array StandardCopyOption
                                                                 [StandardCopyOption/REPLACE_EXISTING
                                                                  StandardCopyOption/COPY_ATTRIBUTES]))
                                         ($.cvm/result-set ctx
                                                           nil))
              ^String destination-2    (str destination)
              ^File   destination-file (File. destination-2)]
          (if (.isDirectory destination-file)
            (let [^String destination-3      (format "%s/%s"
                                                     destination-2
                                                     (.getFileName source-path))
                  ^File   destination-file-2 (File. destination-3)]
              (if (.isDirectory destination-file-2)
                ($.cvm/exception-set ctx
                                     ($.cell/* :FS)
                                     ($.cell/string (format "Cannot overwrite directory '%s' with non directory '%s'"
                                                            destination-3
                                                            source)))
                (copy destination-file-2)))
            (copy destination-file)))
        ;;
        (catch Throwable ex
          ($.cvm/exception-set ctx
                               ($.cell/* :FS)
                               ($.cell/string (.getMessage ex)))))))



(defn delete 

  "Request for deleting a file or an empty directory."

  ;; TODO. Prints absolute path in errors.
  ;;       Make recursive over populated directories? Or too dangerous?

  [ctx [path]]

  (or (when-not ($.std/string? path)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Path to delete must be a string")))
      (let [^String path (str path)]
        (try
          ($.cvm/result-set ctx
                            (-> path
                                (File.)
                                (.toPath)
                                (Files/deleteIfExists)
                                ($.cell/boolean)))
          ;;
          (catch DirectoryNotEmptyException _ex
            ($.cvm/exception-set ctx
                                 ($.cell/* :FS)
                                 ($.cell/string (format "Cannot delete non-empty directory: %s"
                                                        path))))
          ;;
          (catch Throwable ex
            ($.cvm/exception-set ctx
                                 ($.cell/* :FS)
                                 ($.cell/string (.getMessage ex))))))))



(defn dir?

  "Request returning `true` if `path` is an actual directory."

  [ctx [path]]

  (or (when-not ($.std/string? path)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Path to test for directory must be a string")))
      ($.cvm/result-set ctx
                        ($.cell/boolean (bb.fs/directory? (str path))))))



(defn exists?

  "Request returning `true` if `path` exists."

  [ctx [path]]

  (or (when-not ($.std/string? path)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Path to test must be a string")))
      (try
        ($.cvm/result-set ctx
                          (-> path
                              ^String (str)
                              (File.)
                              (.exists)
                              ($.cell/boolean)))
        (catch Throwable ex
          ($.cvm/exception-set ctx
                               ($.cell/* :FS)
                               ($.cell/string (.getMessage ex)))))))



(defn file?

  "Request returning `true` if `file` is an actual, regular file."

  [ctx [path]]

  (or (when-not ($.std/string? path)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Path to test for file must be a string")))
      ($.cvm/result-set ctx
                        ($.cell/boolean (bb.fs/regular-file? (str path))))))



(defn resolve

  "Request for resolving a filename to a canonical form."

  [ctx [path]]

  (or (when-not ($.std/string? path)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Path to resolve must be a string")))
      ($.cvm/result-set ctx
                        (-> path
                            (str)
                            (bb.fs/expand-home)
                            (bb.fs/canonicalize)
                            (str)
                            ($.cell/string)))))



(defn size

  "Request for returning a filesize in bytes."

  [ctx [path]]

  (or (when-not ($.std/string? path)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Path must be a string")))
      (try
        ;;
        ($.cvm/result-set ctx
                          (-> path
                              (str)
                              (bb.fs/size)
                              ($.cell/long)))
        ;;
        (catch NoSuchFileException _ex
          ($.cvm/result-set ctx
                            nil))
        ;;
        (catch Throwable _ex
          ($.cvm/exception-set ctx
                               ($.cell/* :FS)
                               ($.cell/* "Unable to get file size"))))))



(defn tmp

  "Request for creating a temporary file."

  [ctx [prefix suffix]]

  (or (when-not ($.std/string? prefix)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Prefix for temporary file must be a string")))
      (when-not ($.std/string? suffix)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Suffix for temporary file must be a string")))
      (try
        ($.cvm/result-set ctx
                          (-> (Files/createTempFile (str prefix)
                                                    (str suffix)
                                                    (make-array FileAttribute
                                                                0))
                              (str)
                              ($.cell/string)))
        (catch Throwable ex
          ($.cvm/exception-set ctx
                               ($.cell/* :FS)
                               ($.cell/string (.getMessage ex)))))))



(defn tmp-dir

  "Request for creating a temporary directory."

  [ctx [prefix]]

  (or (when-not ($.std/string? prefix)
        ($.cvm/exception-set ctx
                             ($.cell/code-std* :ARGUMENT)
                             ($.cell/* "Prefix for temporary directory must be a string")))
      (try
        ($.cvm/result-set ctx
                          (-> (Files/createTempDirectory (str prefix)
                                                         (make-array FileAttribute
                                                                     0))
                              (str)
                              ($.cell/string)))
        (catch Throwable ex
          ($.cvm/exception-set ctx
                               ($.cell/* :FS)
                               ($.cell/string (.getMessage ex)))))))
