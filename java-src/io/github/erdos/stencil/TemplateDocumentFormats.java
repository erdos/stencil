package io.github.erdos.stencil;

import java.util.Optional;

/**
 * These types are used when preprocessing a template document.
 */
public enum TemplateDocumentFormats {

    /**
     * Microsoft Word Open XML Format Document file.
     */
    DOCX,

    /**
     * Microsoft PowerPoint Open XML Presentation file.
     */
    PPTX,

    /**
     * OpenDocument Text Document for LibreOffice.
     */
    ODT,

    /**
     * OpenDocument presentation files for LibreOffice.
     */
    ODP,

    /**
     * Raw XML file.
     */
    XML,

    /**
     * Simple text file without formatting. Like XML but without a header.
     */
    TXT;

    public static Optional<TemplateDocumentFormats> ofExtension(String fileName) {
        for (TemplateDocumentFormats format : TemplateDocumentFormats.values()) {
            if (fileName.toUpperCase().endsWith("." + format.name())) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
