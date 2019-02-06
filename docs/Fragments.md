# Fragments

It is possible to reuse parts of template documents in other templates. For example, the client
may want to store the header and footer of the templates in a separate file in order to make it
easier to change it.

First of all you need to **define a fragment** using the following syntax:

```
{<i>%fragment "Fragment Name"%</i>}
Fragment body comes here.
{<i>%end%</i>}
```

Then you can include a fragment by writing `{<i>% include "Fragment Name" %</i>}` in the document.
The fragment name must be a string literal meaning it can not come from a variable.


## Specification

<a><b>{%fragment "Elephant"%}fe</b><c>frag{%end%}</c></a>

<a>{%elephant%}</a>
-->
<a><b>fe</b><c>frag</c></a>

Otlet: az eredeti helyen menjunk fel es keressuk meg a paragraph elemet.
Ezutan 
