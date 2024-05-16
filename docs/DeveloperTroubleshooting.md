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


### In numbering definition

The root node in the numbering definition must contain an `Ignorable` tag (which can be also empty). Needed for Word.


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



## OOXML Model

```
 +-------------+
 | _rels/.rels | < this is the entry point
 +-------------+
        |
        v
 +-------------------+   +------------------------------+
 | word/document.xml |===| word/_rels/document.xml.rels |
 +-------------------+   +------------------------------+
                           |             |       |
                           v             |       v
                +--------------------+   |   +------------------+   +-----------------------------+
                | word/numbering.xml |   |   | word/header1.xml |===| word/_rels/header1.xml.rels | * references images
                +--------------------+   |   +------------------+   +-----------------------------+
                 > shared across all     v
                                        +-----------------+
                                        | word/styles.xml | > shared across all
                                        +-----------------+
```

## OOXML Presentation Model

- Entry point is the same `.rels` file and main document is usually `ppt/_rels/presentation.xml`
- Main document references both `slide` and `slideMaster` and `theme`
  - SlideMaster references `slideLayout` (and `theme`) pages
  - Slide references `slideLayout` pages
  - Theme page has no references
  - SlideLayout references `slideMaster` pages. Note, there is a circular reference there!

### SlideMaster

> The master slide is the template upon which presentation slides are built. It specifies the shapes and objects as placeholders for content on presentation slides, as well as the formatting of the content within the placeholders. Of course the content and formatting specified on a master slide can be altered by layout slides and the presentation slides themselves, but absent such overrides, the master slide establishes the overall look and feel of the presentation. [Source](http://officeopenxml.com/prSlideMaster.php)

### SlideLayout

> A slide layout is essentially a template design which can be applied to one or more slides, defining the default appearance and positioning of objects on the slide. It "sits" on top of the master slide, acting as an override to alter or supplement information provided on the master slide. When applied to a slide, all corresponding content within objects on the slide is mapped to the slide layout placeholders. [Source](http://officeopenxml.com/prSlideLayout.php)


```                                                                                                         
+-------------+                                                                                             
| _rels/.rels | < this is the entry point                                                                   
+-------------+                                                                                             
       |                                         ┌───────────────► /ppt/presProps.xml                       
       v                                         │                                                          
┌─────────────────────┐                          │                                                          
│/ppt/presentation.xml├─────┬────────┬────────┬──┴───────────────► /ppt/theme/theme1.xml                    
└─────────────────────┘     │        │        │                         ▲                                   
                            │        │        ▼                         │                                   
                            │        │     ┌────────────────────────────┴─────┐                             
                            │        │     │/ppt/slideMasters/slideMaster1.xml├────────┐                    
                            │        │     └──────────────────────────────────┘        │                    
                            │        ▼                                ▲                │                    
                            │   ┌──────────────────────────────────┐  │                ▼                    
                            │   │/ppt/slideMasters/slideMaster2.xml│  │ ┌──────────────────────────────────┐
                            │   └──────────────────────────────────┘  └─┤/ppt/slideLayouts/slideLayout1.xml│
                            ▼                                           └──────────────────────────────────┘
                       ┌──────────────────────┐                                        ▲                    
                       │/ppt/slides/slide1.xml├────────────────────────────────────────┘                    
                       ├──────────────────────┤                                                             
                       │/ppt/slides/slide2.xml│                                                             
                       └──────────────────────┘                                                             
```