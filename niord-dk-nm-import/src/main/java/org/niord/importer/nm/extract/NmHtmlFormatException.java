package org.niord.importer.nm.extract;

/**
 * Exception thrown if the HTML format is invalid
 */
public class NmHtmlFormatException extends Exception {

    /** Constructor **/
    public NmHtmlFormatException(String message) {
        super(message);
    }

    /** Constructor **/
    public NmHtmlFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
