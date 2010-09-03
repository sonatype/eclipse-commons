package org.maven.ide.eclipse.ui.common.editor.internal;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.maven.ide.eclipse.ui.common.editor.internal.messages"; //$NON-NLS-1$

    public static String abstractFileEditor_errorOpeningEditor;

    public static String abstractFileEditor_fileChanged_message;

    public static String abstractFileEditor_fileChanged_title;

    public static String abstractFileEditor_fileDeleted_message;

    public static String abstractFileEditor_fileDeleted_title;

    public static String abstractFileEditor_noDocumentProvider;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages( BUNDLE_NAME, Messages.class );
    }

    private Messages()
    {
    }
}
