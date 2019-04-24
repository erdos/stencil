# Developer Troubleshooting

This document contains a small list of know-how related to dealing with the OOXML format.


## XML namespace aliases

Problem: XML files must not contains xml namespace aliases starting with `xml` prefix.

Example: Bad alias: `xml123`, good alias: `a123`.

Solution: Use different alias names.

- LibreOffice: OK
- Word: will not open file.


## Ignorable attribute

Problem: The [Ignorable](https://docs.microsoft.com/en-us/dotnet/framework/wpf/advanced/mc-ignorable-attribute) attribute must contain valid XML namespace aliases.

It is a problem because many XML processors hide the aliases after parsing the XML document.

- LibreOffice: OK
- Word: will not open file.

Solution: keep track of and reuse the xml ns alias names.

See also: `Requires` attribute of the `Choice` tag.


## XML space attribute

Problem: space symbols disappear from the document.

Solution: Use the `xml:space="preserve"` attribute consequently.


## Wrong relationships file

Problem: LibreOffice will not open an OOXML file if the relationships file contains an XML namespace alias.

Solution: Generate relationships files without XML namespace aliases (enforce the use of default XML namespace alias).

- Word: unknown effect
- LibreOffice: will not open file


## Invalid zip entries

Problem: The zip entry paths mut not contain a `../` part.

- LibreOffice: OK
- Word: will not open file.
