# Stencil Template Engine

Stencil is an open source templating engine that transforms Office Open XML documents (mostly Microsoft
Office's Word `.docx` files) from the JVM. It has a simple syntax and no programming is needed to write document templates.

<p align="center"><img src="https://raw.githubusercontent.com/erdos/stencil/master/docs/graphics.svg?sanitize=true" alt="stencil flow"/></p>

You can use either Microsoft Word or LibreOffice to edit the document templates.
The template expressions are just simple textual expressions, and you can even colour-code
them to make your template more readable.

[![Clojars Project](https://img.shields.io/clojars/v/io.github.erdos/stencil-core.svg)](https://clojars.org/io.github.erdos/stencil-core)
![CI](https://github.com/erdos/stencil/actions/workflows/flow.yml/badge.svg)
[![codecov](https://codecov.io/gh/erdos/stencil/branch/master/graph/badge.svg)](https://codecov.io/gh/erdos/stencil)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/erdos/stencil/issues)
<!-- [![HitCount](http://hits.dwyl.io/erdos/stencil.svg)](http://hits.dwyl.io/erdos/stencil) -->
[![Hits](https://hits.sh/github.com/erdos/stencil.svg?view=today-total&color=80c521)](https://hits.sh/github.com/erdos/stencil/)
[![EPL 2.0](https://img.shields.io/badge/License-EPL%202.0-red.svg)](https://www.eclipse.org/legal/epl-2.0/)


## Features

- 📄 **Multiple Formats:** Works with `docx` and `pptx` files
- 💻 **Simple syntax:** For value substitution, conditional and repeating blocks
- 🔧 **Extendable:** Dozens of [built-in functions](https://stencil.erdos.dev/Functions.html) callable from the template
- 📰 **Dynamic content:** Substituting HTML text for dynamic text formatting
- 🌄 **Images and links:** Dynamically replace images and links in the template
- 👀 **Tables:** Show/hide rows and columns in tables
- 📐 **Programmable:** Offers API for Java and Clojure. Deployable as a Docker container.

## 📖 Getting Started with the Library

- See the [Example templates](examples)
- Read the [Documentation](https://stencil.erdos.dev)
- Reference the [Java API](docs/GettingStarted.md#java-api) and the [Clojure API](docs/GettingStarted.md#clojure-api).

## 🐳 Getting Started with the Service

The project has a simple [service implementation](https://github.com/erdos/stencil/tree/master/service), which is available on GitHub Packages as a [Container image](https://github.com/users/erdos/packages/container/package/stencil).


## 👉 Version

**Latest stable** version is `0.6.7`

**Latest snapshot** version is `0.6.8-SNAPSHOT`

Previous versions are available on the [Stencil Clojars](https://clojars.org/io.github.erdos/stencil-core) page.

<details>
  <summary><b>For Java with Maven</b></summary>

  If you are using Maven, add the followings to your `pom.xml`:

  1. The dependency:

``` xml
<dependency>
  <groupId>io.github.erdos</groupId>
  <artifactId>stencil-core</artifactId>
  <version>0.6.7</version>
</dependency>
```

2. And the [Clojars](https://clojars.org) repository:

``` xml
<repository>
  <id>clojars.org</id>
  <url>https://repo.clojars.org</url>
</repository>
```  
</details>

<details>
  <summary><b>For Java with Gradle</b></summary>

  Add to the `dependencies` section of your `build.gradle` file: `implementation('io.github.erdos/stencil-core:0.6.7')`
  </details>

<details>
  <summary><b>For Clojure with Leiningen</b></summary>

  If you are using Leiningen, add the following to the `:dependencies` section of your `project.clj` file:
  
  `[io.github.erdos/stencil-core "0.6.7"]`
</details>

<details>
  <summary><b>For Clojure with deps.edn</b></summary>

  Add `io.github.erdos/stencil-core {:mvn/version "0.6.7"}`
</details>



## 😎 License

Copyright (c) Janos Erdos. All rights reserved. The use and distribution terms
for this software are covered by the Eclipse Public License 2.0
(https://www.eclipse.org/legal/epl-2.0/) which can be found in the file
`LICENSE.txt` at the root of this distribution. By using this software in any
fashion, you are agreeing to be bound by the terms of this license. You must not
remove this notice, or any other, from this software.
