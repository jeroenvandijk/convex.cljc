# Changelog

All notable changes for `module/net` between stable releases of this
repository.


---


## 2023-06-01

- Add
    - `$.client/endpoint`
    - `$.key-pair/key-pair?`
    - `$.pfx/alias+`
    - `$.server/belief`
    - `$.server/n-belief-received`
    - `$.server/n-belief-sent`
    - `$.server/data`
    - `$.server/endpoint`
    - `$.server/pubkey`
    - `$.server/state`
    - `$.server/status`
- Change
    - `$.client/peer-status` returns a cell Map instead of a Vector
    - `$.pfx/key-pair-get` returns Nil for inexistent aliases (instead of throwing)
    - `$.server/data` returns more information
    - `$.server/persist`
        - Returns a boolean indicating success
        - Accepts a root key with `:convex.server/root-key`
- Remove
    - `$.key-pair/sign-hash` and `$.key-pair/verify-hash` which where somewhat redundant
    - `$.server/host`
    - `$.server/port`


---


## 2022-10-24

- Add
  - Sign and verify hashes directly


---


## Swich to [calver](https://calver.org)

Initially, the repository used semantic versioning. Below are the remainders of
this epoch.


---


## 0.0.0-alpha3 - 2022-09-22

- Add
    - Signature verification in `$.key-pair` (formerly `$.sign`)
    - Support for local clients
- Change
    - Peer servers now persist their data when stopping by default
    - Prefix `$.client` functions for results with `result->`
    - Rename `$.client/sequence` to `$.client/sequence-id`
    - Rename `$.sign` to `$.key-pair`
    - Rename `$.sign/signed` to `$.key-pair/sign`
    - Update to `:module/cvm` 0.0.0-alpha4 and Convex 0.7.9


---


## 0.0.0-alph2 - 2021-10-03

- Add
    - Merge former `:module/crypto` project, bringing new namespaces:
        - `$.pfx`
        - `$.sign`
- Change
    - Upgrade `:module/cvm` to `0.0.0-alpha3`


---


## 0.0.0-alpha1 - 2021-08-28

- Change
    - Upgrade `:module/crypto` to `0.0.0-alpha1`
    - Upgrade `:module/cvm` to `0.0.0-alpha2`


---


## 0.0.0-alpha0 - 2021-08-21

- Add
    - First API iteration
        - `$.client`
        - `$.server`
    - Depends on
        - `convex-peer/0.7.0`
        - `crypto.clj/0.0.0-alpha0`
        - `cvm.clj/0.0.0-alpha1`
