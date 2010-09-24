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

    public static String errors_not_valid_url; 
    
    public static String errors_host_may_not_contain_space;

    public static String gavComposite_artifactId_label;

    public static String gavComposite_artifactId_name;

    public static String gavComposite_groupId_label;

    public static String gavComposite_groupId_name;

    public static String gavComposite_invalidOsgiVersion;

    public static String gavComposite_version_label;

    public static String gavComposite_version_name;

    public static String listEditorComposite_add;

    public static String listEditorComposite_edit;

    public static String listEditorComposite_find;

    public static String listEditorComposite_remove;

    public static String realmComposite_access_label;

    public static String realmComposite_anonymousAllowed;

    public static String realmComposite_anonymousOnly;

    public static String realmComposite_manageRealms;

	public static String realmComposite_noSelection;

    public static String realmComposite_passwordRequired;

    public static String realmComposite_selectRealmFor;

    public static String realmManagementComposite_addUrl;

    public static String realmManagementComposite_authenticationType_password;

    public static String realmManagementComposite_authenticationType_passwordAndSsl;

    public static String realmManagementComposite_authenticationType_ssl;

    public static String realmManagementComposite_realmAuthentication_label;

    public static String realmManagementComposite_realmDescription_label;

    public static String realmManagementComposite_realmId_exists;

    public static String realmManagementComposite_realmId_label;

    public static String realmManagementComposite_realmId_name;

    public static String realmManagementComposite_realmName_label;

    public static String realmManagementComposite_realmName_name;

    public static String realmManagementComposite_removeUrl;

    public static String realmManagementComposite_urlViewer_accessColumn;

    public static String realmManagementComposite_urlViewer_label;

    public static String realmManagementComposite_urlViewer_urlColumn;

    public static String realmManagementDialog_apply;

    public static String realmManagementDialog_errorSavingRealm;

    public static String realmManagementDialog_title;

    public static String realmManagementPage_add_action;

    public static String realmManagementPage_add_tooltip;

    public static String realmManagementPage_description;

    public static String realmManagementPage_newRealmNameTemplate;

    public static String realmManagementPage_realmChanged_message;

    public static String realmManagementPage_realmChanged_title;

    public static String realmManagementPage_reload_error;

    public static String realmManagementPage_reload_tooltip;

    public static String realmManagementPage_remove_action;

    public static String realmManagementPage_remove_error;

    public static String realmManagementPage_remove_message;

    public static String realmManagementPage_remove_title;

    public static String realmManagementPage_remove_tooltip;

    public static String realmManagementPage_title;

    public static String remoteResourceLookupDialog_error_other;

    public static String remoteResourceLookupDialog_loading;

    public static String remoteResourceLookupDialog_server_collapsed;

    public static String remoteResourceLookupDialog_server_expanded;

    public static String remoteResourceLookupDialog_server_label;

    public static String remoteResourceLookupPage_error_other;

    public static String remoteResourceLookupPage_loading;

    public static String remoteResourceLookupPage_server_collapsed;

    public static String remoteResourceLookupPage_server_expanded;

    public static String remoteResourceLookupPage_server_label;

    public static String secureStorageDialog_errors_noMatch;

    public static String secureStorageDialog_errors_emptyPassword;

    public static String secureStorageDialog_label_confirm;

    public static String secureStorageDialog_label_password;

    public static String secureStorageDialog_messageLogin;

    public static String secureStorageDialog_messageLoginChange;

    public static String secureStorageDialog_title;

    public static String urlInput_anonymousIfEmpty;

    public static String urlInput_browse;

    public static String urlInput_certificateFile_label;

    public static String urlInput_certificateFile_name;

    public static String urlInput_fileSelect_empty;

    public static String urlInput_fileSelect_filter1;

    public static String urlInput_fileSelect_filter2;

    public static String urlInput_fileSelect_notFound;

    public static String urlInput_fileSelect_title;

    public static String urlInput_passphrase_label;

    public static String urlInput_passphrase_name;

    public static String urlInput_password_label;

    public static String urlInput_password_name;

    public static String urlInput_url_label;

    public static String urlInput_url_name;

    public static String urlInput_username_label;

    public static String urlInput_username_name;

    public static String urlInput_useCertificate;

    public static String urlInput_validation_enterUrl;

    public static String urlInput_validation_enterUserCredentials;

    public static String urlInput_validation_invalidUrl;

    public static String validation_errors_invalidCharacters;

    public static String validation_errors_projectExists;

    public static String validation_errors_remoteUrlRequired;
}
