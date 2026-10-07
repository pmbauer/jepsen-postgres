;; 2026-07-30: vendored from https://github.com/jepsen-io/jepsen/blob/58b4c48629fb31a333d7101ad7554c6d59c9ad61/antithesis/src/jepsen/antithesis.clj
;;   Eclipse Public License 2.0
;;   Only with-rng and test macros, does not include antithesis sdk bits which are causing
;;   issues in the simulator
(ns jepsen.antithesis
  "Provides support for running Jepsen tests in Antithesis. Provides an RNG,
  lifecycle hooks, and assertions.

  ## Randomness

  You should wrap your entire program in `(with-rng ...)`. This does nothing in
  ordinary environments, but in Antithesis, it replaces the jepsen.random RNG
  with one powered by Antithesis.

  ## Wrappers

  Wrap your test map in `(a/test test-map)`."
  (:refer-clojure :exclude [test])
  (:require
   [jepsen
    [db :as db]
    [os :as os]
    [random :as rand]])
  (:import (jepsen.antithesis Random)))

(let [d (delay (System/getenv "ANTITHESIS_OUTPUT_DIR"))]
  (defn dir
    "The Antithesis SDK directory, if present, or nil."
    []
    @d))

(defn antithesis?
  "Are we running in an Antithesis environment?"
  []
  (boolean (dir)))

;; Randomness

(defn replacement-double-weighted-index
  "Antithesis replacement for jepsen.random/double-weighted-index. Takes a
  double array, and picks a random index into it."
  (^long [^doubles weights]
   (replacement-double-weighted-index 0.0 weights))
  (^long [^double total-weight ^doubles weights]
   (.randomChoice ^Random rand/rng (range (alength weights)))))

(def choice-cardinality
  "When selecting long values, we consider something an Antithesis \"choice\"
  if it asks for at most this many elements."
  16)

(defn replacement-long
  "Antithesis replacement for `jepsen.random/long`. Mostly equivalent to the
  original, but when there are less than `choice-cardinality` options, hints to
  Antithesis that we're making a specific choice."
  (^long [] (.nextLong rand/rng))
  (^long [^long upper]
   (if (< 0 upper choice-cardinality)
     (.randomChoice ^Random rand/rng (range upper))
     (.nextLong rand/rng upper)))
  (^long [^long lower, ^long upper]
   (if (< 0 (- upper lower) choice-cardinality)
     (.randomChoice ^Random rand/rng (range lower upper))
     (.nextLong rand/rng lower upper))))

(defn replacement-bool
  "Antithesis replacement for `jepsen.random/bool`. Uses Antithesis choices
  when probabilities are closer to chance than choice-cardinality."
  ([] (.nextBoolean ^Random rand/rng))
  ([^double p]
   (if (< (/ choice-cardinality)
          p
          (- 1 (/ choice-cardinality)))
     (.nextBoolean ^Random rand/rng)
     (< (rand/double) p))))

(defmacro with-rng
  "When running in an Antithesis environment, replaces Jepsen's random source
  with an Antithesis-controlled source. You should wrap your top-level program
  in this."
  [& body]
  `(let [antithesis?# (antithesis?)
         rng#         (if antithesis?# (Random.) rand/rng)]
     (rand/with-rng rng#
       (with-redefs [ ; As of jepsen 0.3.14 jepsen.core/run! and jepsen.cli's test-all
                      ; wrap themselves in rand/with-seed, which calls thread-local-random
                      ; and would replace our RNG.
                      ; Seeds are not effective or used under Antithesis, override with our rng#
                     jepsen.random/thread-local-random
                     (if antithesis?#
                       (fn ([] rng#) ([_seed#] rng#))
                       rand/thread-local-random)

                     jepsen.random/double-weighted-index
                     (if antithesis?#
                       replacement-double-weighted-index
                       rand/double-weighted-index)

                     jepsen.random/long
                     (if antithesis?#
                       replacement-long
                       rand/long)

                     jepsen.random/bool
                     (if antithesis?#
                       replacement-bool
                       rand/bool)]
         ~@body))))

(defn test
  "Prepares a Jepsen test for running in Antithesis. When running inside
  Antithesis, this:

  1. Replaces the OS with a no-op
  2. Repaces the DB with a no-op
  3. Replaces the SSH system with a stub."
  [test]
  (if (antithesis?)
    (assoc test
           :os os/noop
           :db db/noop
           :ssh {:dummy? true})
    test))
