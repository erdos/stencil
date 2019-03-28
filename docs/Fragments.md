# Fragments

It is possible to reuse parts of template documents in other templates. For example, the client
may want to store the header and footer of the templates in a separate file in order to make it
easier to change it.

First of all you need to **define a fragment** by creating a docx document. The content of the 
document will be used to include in the main document.

Then you can include a fragment by writing `{<i>% include "Fragment Name" %</i>}` in the document.
The fragment name must be a string literal meaning it can not come from a variable.


