# Stencil Templating for Programmers

First of all, you need to add Stencil to the list of dependencies in your project.
The Stencil home page shows the current version you can use in your build files.

## Java API

The public Java API is accessible in the `io.github.erdos.stencil.API` class. Check out the [Stencil Javadoc](https://stencil.erdos.dev/javadoc/io/github/erdos/stencil/package-summary.html) page for details.

1. First, we have to prepare a template file. Call `API.prepare()` funtion with the template file.
2. Second, we can render the prepared template using the `API.render()` function.
3. When you do not use a prepared template instance any more, call its `close()` method to free allocated resources.

The following example takes a template from the file system, fills it with data
from the arguments and writes the rendered document back to the file system.

``` java
public void renderInvoiceDocument(String userName, Integer totalCost) throws IOException {

    final File template = new File("/path/to/template.docx");

    final PreparedTemplate prepared = API.prepare(template);

    final Map<String, Object> data = new HashMap<>();
    data.put("name", userName);
    data.put("cost", totalCost);

    final EvaluatedDocument rendered = API.render(prepared, TemplateData.fromMap(data));

    rendered.writeToFile(new File("target/invoice-" + userName + ".docx"));
    }
```

Both the `PreparedTemplate` and `PreparedFragment` types implement `AutoClosable`, which makes resource handling simpler with the `try-with-resources` syntax.

``` java
try (PreparedTemplate template = API.prepare("template.docx")) {
    EvaluatedDocument rendered = API.render(template, TemplateData.fromMap());
    rendered.writeToFile(new File("target/rendered.docx"));
}
```

## Clojure API

We recomment checking the [Stencil Clojure documentation](https://cljdoc.org/d/io.github.erdos/stencil-core) for details and API usage.

You need to import the stencil api namespace: `(require '[stencil.api :as api])`

First, we need to compile a template file.

``` clojure
(def template-1 (api/prepare (clojure.java.io/resource "template1.docx")))
```

Then, we can define a helper function to render the template.

``` clojure
(defn render-template-1 [output-file data]
  (api/render! template-1 data :output output-file))
```

Call the function to render file.

``` clojure
(render-template-1 "/tmp/output-1.docx" {"customerName" "John Doe"})
```

This renders the template to `/tmp/output-1.docx` with the supplied data.

Call the `(api/cleanup!)` function on the prepared template instance when not used anymore to clean
up associated resources. Alternatively, use a [with-open](https://clojuredocs.org/clojure.core/with-open) form
to automate resource deallocation:

``` clojure
(with-open [tempate (api/prepare "/path/to/template.docx")]
  (api/render! template data :output "/path/to/output.docx"))
```

## Closing templates or fragments

Calling the `close` method on a `PreparedTemplate` or a `PreparedFragment` can be used to free up some of the cache filed allocated on creation.
Please note, an already closed template or fragment cannot be used for rendering a template, and an `IllegalStateException` will be thrown in such cases.

## Configuration

- Use the `stencil.tmpdir` environment variable to change the default location used
to store caches for prepared template and fragment files. When not set, the value of `java.io.tmpdir` is used (eg.: `/tmp` in Linux).


## Converting to other formats

If you need to convert the resulting document to other document types you can
use the amazing [JODConverter](https://github.com/sbraconnier/jodconverter) library.
