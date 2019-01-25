# Stencil Template Engine

Stencil is a templating engine to produce Office Open XML (mostly Microsoft
Office's Word `.docx` files) from Java programs. It has a simple syntax and no
programming is needed to write or modify document templates.

The aim of this project is to provide an easy-to-use and freely available tool
for generating office documents.

You can use either Microsoft Word or LibreOffice to edit the document templates.
The template expressions are just simple texts, and you can event colour-code
them to make the template more readable.

[![Clojars Project](https://img.shields.io/clojars/v/io.github.erdos/stencil-core.svg)](https://clojars.org/io.github.erdos/stencil-core)
[![CircleCI](https://circleci.com/gh/erdos/stencil/tree/master.svg?style=svg)](https://circleci.com/gh/erdos/stencil/tree/master)
[![EPL 2.0](https://img.shields.io/badge/License-EPL%202.0-red.svg)](https://www.eclipse.org/legal/epl-2.0/)


## Getting Started

- Read the [Documentation](docs/index.md)
- Read the [Technical Documentation](https://cljdoc.org/d/io.github.erdos/stencil-core/)
- See the [Example templates](examples)


## Version

**Latest stable** version is `0.2.7`

**Latest snapshot** version is `0.2.8-SNAPSHOT`

If you are using Maven, add the followings to your `pom.xml`:

The dependency:

``` xml
<dependency>
  <groupId>io.github.erdos</groupId>
  <artifactId>stencil-core</artifactId>
  <version>0.2.7</version>
</dependency>
```

And the [Clojars](https://clojars.org) repository:

``` xml
<repository>
  <id>clojars.org</id>
  <url>https://repo.clojars.org</url>
</repository>
```

Alternatively, if you are using Leiningen, add the following to
the `:dependencies` section of your `project.clj`
file: `[io.github.erdos/stencil-core "0.2.7"]`

Previous versions are available on the [Stencil Clojars](https://clojars.org/io.github.erdos/stencil-core) page.

## License

Copyright (c) Janos Erdos. All rights reserved. The use and distribution terms
for this software are covered by the Eclipse Public License 2.0
(https://www.eclipse.org/legal/epl-2.0/) which can be found in the file
`LICENSE.txt` at the root of this distribution. By using this software in any
fashion, you are agreeing to be bound by the terms of this license. You must not
remove this notice, or any other, from this software.
