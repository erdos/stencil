# Stencil Templating for Programmers

First of all, you need to add Stencil to the list of dependencies in your project.
The Stencil home page shows the current version you can use in your build files.

## Java API

First you need to add the dependency to your `pom.xml` file.
See the `README.md` file for information about the latest stable version.

The public Java API is accessible in the `io.github.erdos.stencil.API` class.

1. First, we have to prepare a template file. Call `API.prepare()` funtion with the template file.
2. Second, we can render the prepared template using the `API.render()` function.
3. When you do not use a prepared template instance any more, call its `cleanup()` method to free allocated resources.

The following example takes a template from the file system, fills it with data
from the arguments and writes the rendered document back to the file system.

``` java
public void renderInvoiceDocument(String userName, Integer totalCost) throws IOException {

    final File template = new File("/home/developer/templates/invoice.docx");

    final PreparedTemplate prepared = API.prepare(template);

    final Map<String, Object> data = new HashMap<>();
    data.put("name", userName);
    data.put("cost", totalCost);

    final EvaluatedDocument rendered = API.render(prepared, TemplateData.fromMap(data));

    rendered.writeToFile(new File("/home/developer/rendered/invoice-" + userName + ".docx"));
    }
```

## Clojure API

Before writing any code, add the latest version of the stencil project to the
`:dependencies` section of your `project.clj` file. See the `README.md` file for
the latest stable version.

You need to import the stencil api namespace: `(require '[stencil.api :refer :all])`

First, we need to compile a template file.

``` clojure
(def template-1 (prepare (clojure.java.io/resource "template1.docx")))
```

Then, we can define a helper function to render the template.

``` clojure
(defn render-template-1 [output-file data]
  (render! template-1 data :output output-file))
```

Call the function to render file.

``` clojure
(render-template-1 "/tmp/output-1.docx" {"customerName" "John Doe"})
```

This renders the template to `/tmp/output-1.docx` with the supplied data.

Call the `(cleanup!)` function on the prepared template instance when not used anymore to clean
up associated resources.

## Converting to other formats

If you need to convert the resulting document to other document types you can
use the amazing [JODConverter](https://github.com/sbraconnier/jodconverter) library.
