# Stencil Template Engine

Stencil is a templating engine to produce Office Open XML (mostly Microsoft
Office's Word `.docx` files) from Java programs. It has a simple syntax and no
programming is needed to write or modify document templates.

The aim of this project is to provide an easy-to-use and freely available tool
for generating office documents.

<p align="center"><img src="https://raw.githubusercontent.com/erdos/stencil/master/docs/graphics.svg?sanitize=true" alt="stencil flow"/></p>

You can use either Microsoft Word or LibreOffice to edit the document templates.
The template expressions are just simple texts, and you can even colour-code
them to make the template more readable.

[![Clojars Project](https://img.shields.io/clojars/v/io.github.erdos/stencil-core.svg)](https://clojars.org/io.github.erdos/stencil-core)
![CI](https://github.com/erdos/stencil/actions/workflows/flow.yml/badge.svg)
[![codecov](https://codecov.io/gh/erdos/stencil/branch/master/graph/badge.svg)](https://codecov.io/gh/erdos/stencil)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/erdos/stencil/issues)
<!-- [![HitCount](http://hits.dwyl.io/erdos/stencil.svg)](http://hits.dwyl.io/erdos/stencil) -->
[![Hits](https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https%3A%2F%2Fgithub.com%2Ferdos%2Fstencil&count_bg=%239F3DC8&title_bg=%23555555&icon=&icon_color=%23E7E7E7&title=hits&edge_flat=false)](https://hits.seeyoufarm.com)
[![EPL 2.0](https://img.shields.io/badge/License-EPL%202.0-red.svg)](https://www.eclipse.org/legal/epl-2.0/)


## Features

- Works with `docx` and `pptx` files
- Simple value substitution
- Substituting HTML text for dynamic text formatting
- Conditional and repeating blocks in the template
- Show/hide rows and columns in tables
- Dynamically replace images in the template

## Getting Started with the Library

- See the [Example templates](examples)
- Read the [Documentation](docs/index.md)
- Read about the [Java API](docs/GettingStarted.md#java-api) and the [Clojure API](docs/GettingStarted.md#clojure-api) or see the [Technical Documentation](https://cljdoc.org/d/io.github.erdos/stencil-core/)


## Getting Started with the Service

The project has a simple [service implementation](https://github.com/erdos/stencil/tree/master/service), which is available on GitHub Packages as a [Container image](https://github.com/users/erdos/packages/container/package/stencil).


## Version

**Latest stable** version is `0.5.2`

**Latest snapshot** version is `0.5.3-SNAPSHOT`

If you are using Maven, add the followings to your `pom.xml`:

The dependency:

``` xml
<dependency>
  <groupId>io.github.erdos</groupId>
  <artifactId>stencil-core</artifactId>
  <version>0.5.2</version>
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
file: `[io.github.erdos/stencil-core "0.5.2"]`

Previous versions are available on the [Stencil Clojars](https://clojars.org/io.github.erdos/stencil-core) page.


## License

Copyright (c) Janos Erdos. All rights reserved. The use and distribution terms
for this software are covered by the Eclipse Public License 2.0
(https://www.eclipse.org/legal/epl-2.0/) which can be found in the file
`LICENSE.txt` at the root of this distribution. By using this software in any
fashion, you are agreeing to be bound by the terms of this license. You must not
remove this notice, or any other, from this software.
