(ns stencil.ooxml
  "Contains common xml element tags and attributes"
  (:refer-clojure :exclude [val name type]))

;; run and properties
(def r :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/r)
(def rPr :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/rPr)

(def t :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/t)

;; paragraph and properties
(def p :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/p)
(def pPr :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/pPr)

(def val :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/val)

(def b :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/b)
(def i :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/i)
(def strike :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/strike)

(def u :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/u)

(def vertAlign :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/vertAlign)

;; Content Break: http://officeopenxml.com/WPtextSpecialContent-break.php
(def br :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/br)

(def type :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/type)

(def w :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/w)

;; XML space attribute (eg.: preserve)
(def space :xmlns.http%3A%2F%2Fwww.w3.org%2FXML%2F1998%2Fnamespace/space)

;; font name
(def name :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/name)


(def style :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/style)
;; id of style definition
(def style-id :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/styleId)

;; relation id used with images
(def r-embed :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2FofficeDocument%2F2006%2Frelationships/embed)
(def r-id :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2FofficeDocument%2F2006%2Frelationships/id)

(def bookmark-start  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/bookmarkStart)

(def bookmark-end  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/bookmarkEnd)

(def fld-char  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/fldChar)

(def tag-instr-text  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/instrText)

(def num-pr  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/numPr)

(def fld-char-type  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/fldCharType)

(def tag-num  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/num)

(def tag-lvl  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/lvl)

(def xml-abstract-num-id  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/abstractNumId)

(def tag-abstract-num  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/abstractNum)

(def attr-numId  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/numId)

(def attr-ilvl  :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fwordprocessingml%2F2006%2Fmain/ilvl)

(def default-aliases
  {;default namespace aliases from a LibreOffice 6.4 OOXML Text document
   "http://schemas.openxmlformats.org/markup-compatibility/2006" "mc"
   "urn:schemas-microsoft-com:office:office" "o"
   "http://schemas.openxmlformats.org/officeDocument/2006/relationships" "r"
   "urn:schemas-microsoft-com:vml" "v"
   "http://schemas.openxmlformats.org/wordprocessingml/2006/main" "w"
   "urn:schemas-microsoft-com:office:word" "w10"
   "http://schemas.microsoft.com/office/word/2010/wordml" "w14"
   "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" "wp"
   "http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" "wp14"
   "http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" "wpg"
   "http://schemas.microsoft.com/office/word/2010/wordprocessingShape" "wps"
   ;additional aliases from a new MS Word docx file
   "http://schemas.microsoft.com/office/drawing/2016/ink" "aink"
   "http://schemas.microsoft.com/office/drawing/2017/model3d" "am3d"
   "http://schemas.microsoft.com/office/drawing/2014/chartex" "cx"
   "http://schemas.microsoft.com/office/drawing/2015/9/8/chartex" "cx1"
   "http://schemas.microsoft.com/office/drawing/2015/10/21/chartex" "cx2"
   "http://schemas.microsoft.com/office/drawing/2016/5/9/chartex" "cx3"
   "http://schemas.microsoft.com/office/drawing/2016/5/10/chartex" "cx4"
   "http://schemas.microsoft.com/office/drawing/2016/5/11/chartex" "cx5"
   "http://schemas.microsoft.com/office/drawing/2016/5/12/chartex" "cx6"
   "http://schemas.microsoft.com/office/drawing/2016/5/13/chartex" "cx7"
   "http://schemas.microsoft.com/office/drawing/2016/5/14/chartex" "cx8"
   "http://schemas.openxmlformats.org/officeDocument/2006/math" "m"
   "http://schemas.microsoft.com/office/word/2012/wordml" "w15"
   "http://schemas.microsoft.com/office/word/2018/wordml" "w16"
   "http://schemas.microsoft.com/office/word/2018/wordml/cex" "w16cex"
   "http://schemas.microsoft.com/office/word/2018/wordml/cid" "w16cid"
   "http://schemas.microsoft.com/office/word/2020/wordml/sdtdatahash" "w16sdtdh"
   "http://schemas.microsoft.com/office/word/2015/wordml/symex" "w16se"
   "http://schemas.microsoft.com/office/word/2006/wordml" "wne"
   "http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" "wpc"
   "http://schemas.microsoft.com/office/word/2010/wordprocessingInk" "wpi"
   ;additional aliases from a LibreOffice 6.4 OOXL spreadsheet file
   "http://schemas.microsoft.com/office/spreadsheetml/2009/9/main" "x14"
   "http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" "xdr"
   ;additional aliases from a sample MS Excel xlsx file
   "http://schemas.microsoft.com/office/spreadsheetml/2010/11/main" "x15"
   "http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac" "x14ac"
   "http://schemas.microsoft.com/office/spreadsheetml/2014/revision" "xr"
   "http://schemas.microsoft.com/office/spreadsheetml/2015/revision2" "xr2"
   "http://schemas.microsoft.com/office/spreadsheetml/2016/revision3" "xr3"
   "http://schemas.microsoft.com/office/spreadsheetml/2016/revision6" "xr6"
   "http://schemas.microsoft.com/office/spreadsheetml/2016/revision10" "xr10"})

;; drawing, binary large image or picture
(def blip :xmlns.http%3A%2F%2Fschemas.openxmlformats.org%2Fdrawingml%2F2006%2Fmain/blip)