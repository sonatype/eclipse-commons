package org.maven.ide.eclipse.ui.common;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE = "org.maven.ide.eclipse.ui.common.messages";

    static
    {
        NLS.initializeMessages( BUNDLE, Messages.class );
    }

    public static String errors_authFailed;

    public static String errors_forbidden;

    public static String errors_resourceNotFound;

    public static String errors_unresolvedAddress;

    public static String urlInput_anonymousIfEmpty;

    public static String urlInput_browse;

    public static String urlInput_certificateFile;

    public static String urlInput_fileSelect_empty;

    public static String urlInput_fileSelect_filter1;

    public static String urlInput_fileSelect_filter2;

    public static String urlInput_fileSelect_notFound;

    public static String urlInput_fileSelect_title;

    public static String urlInput_passphrase;

    public static String urlInput_password;

    public static String urlInput_username;

    public static String urlInput_useCertificate;

    public static String urlInput_validation_enterUrl;

    public static String urlInput_validation_enterUserCredentials;

    public static String urlInput_validation_invalidUrl;

    public static String validation_errors_invalidCharacters;

    public static String validation_errors_projectExists;

    public static String validation_errors_remoteUrlRequired;
}
