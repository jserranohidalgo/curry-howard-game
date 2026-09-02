# Third-party notices

The game is one JavaScript file. That file contains the libraries below, compiled
in, so their licences travel with it — which is what this document is for.

Every licence here was read from the artifact's own `pom.xml` in the build's
dependency cache on 2026-09-03, and every copyright line from the project's own
licence file. Nothing is quoted from memory.

## What is bundled

| Library | Version | Licence | Project |
|---|---|---|---|
| cats-core | 2.13.0 | MIT | <https://typelevel.org/cats> |
| cats-free | 2.13.0 | MIT | <https://typelevel.org/cats> |
| cats-kernel | 2.13.0 | MIT | <https://typelevel.org/cats> |
| Laminar | 17.2.1 | MIT | <https://laminar.dev> |
| Airstream | 17.2.1 | MIT | <https://github.com/raquo/Airstream> |
| ew | 0.2.0 | MIT | <https://github.com/raquo/ew> |
| tuplez-full-light | 0.4.0 | MIT | <https://github.com/tulz-app/tuplez> |
| scalajs-dom | 2.8.0 | MIT | <https://scala-js.org> |
| scalajs-library | 1.22.0 | Apache License 2.0 | <https://www.scala-js.org/> |
| scalajs-javalib | 1.22.0 | Apache License 2.0 | <https://www.scala-js.org/> |
| scala-library | 2.13.18 | Apache License 2.0 | <https://www.scala-lang.org/> |
| scala3-library | 3.3.8 | Apache License 2.0 | <https://www.scala-lang.org/> |

The game's own code is MIT — see [LICENSE](LICENSE).

The interface asks for four typefaces and ships none of them: they are named in
the stylesheet and fall back to whatever the reader's system provides, so no font
is distributed here and none is listed above.

The tutor's server is Python 3 and uses only the standard library.

## The MIT works

Each carries its own copyright, and this is the notice that travels with it:

- **cats, cats-free, cats-kernel** — Cats Copyright (c) 2015 Cats Contributors.
- **Laminar** — Copyright 2017 Nikita Gazarov
- **Airstream** — Copyright 2018 Nikita Gazarov
- **ew** — Copyright 2022 Nikita Gazarov
- **tuplez** — Copyright 2020 Iurii Malchenko
- **scalajs-dom** — Copyright (c) 2013 Li Haoyi

The permission notice they share is in [licences/MIT.txt](licences/MIT.txt).

## The Apache works

Scala and Scala.js are licensed under the Apache License, Version 2.0, whose full
text is in [licences/Apache-2.0.txt](licences/Apache-2.0.txt).

- **Scala** — Copyright (c) 2002–2026 EPFL, Copyright (c) 2011–2026 Lightbend, Inc.
- **Scala.js** — Copyright (c) 2013–2026 EPFL

None of the four Apache-licensed artifacts carries a `NOTICE` file inside its
jar, so there is no notice text to propagate beyond this.

## The images

The photographs and the logos the game shows are a separate matter, with their
own sources and conditions, and they are credited where they are used and listed
in [docs/assets/credits.md](docs/assets/credits.md).
