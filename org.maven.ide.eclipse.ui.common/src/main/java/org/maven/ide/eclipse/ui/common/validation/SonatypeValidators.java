package org.maven.ide.eclipse.ui.common.validation;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.osgi.util.NLS;
import org.maven.ide.eclipse.ui.common.Messages;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

/**
 * @author mkleint
 */
public final class SonatypeValidators
{

    public static Validator<String> createArtifactIdValidators()
    {
        return ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING,
                                     // Validators.MAY_NOT_START_WITH_DIGIT,
                                     StringValidators.NO_WHITESPACE,
                                     StringValidators.regexp( "[a-zA-Z0-9_\\-.]*",
                                                              Messages.validation_errors_invalidCharacters, false ) );
    }

    public static Validator<String> createGroupIdValidators()
    {
        return ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING,
                                     // Validators.MAY_NOT_START_WITH_DIGIT,
                                     StringValidators.NO_WHITESPACE,
                                     StringValidators.regexp( "[a-zA-Z0-9_\\-.]*",
                                                              Messages.validation_errors_invalidCharacters, false ) );
    }

    public static Validator<String> createVersionValidators()
    {
        return ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING, StringValidators.NO_WHITESPACE,
                                     StringValidators.regexp( "[a-zA-Z0-9_\\-.]*",
                                                              Messages.validation_errors_invalidCharacters, false ) );
    }

    public static Validator<String> createRemoteHttpUrlValidators()
    {
        return ValidatorUtils.merge( HTTP_URL, URL_MUST_BE_VALID );

    }

    /**
     * 
     */
    public static Validator<String> HTTP_URL = new StringVal()
    {

        public void validate( Problems problems, String compName, String model )
        {
            if ( !model.startsWith( "http://" ) && !model.startsWith( "https://" ) )
            {
                problems.add( MessageFormat.format( Messages.validation_errors_remoteUrlRequired, compName ) );
            }
        }
    };

    /**
     * 
     */
    public static Validator<String> EXISTS_IN_WORKSPACE = new StringVal()
    {

        public void validate( Problems problems, String compName, String model )
        {
            if ( model.trim().length() == 0 )
            {
                return;
            }
            IWorkspace workspace = ResourcesPlugin.getWorkspace();

            IProject project = workspace.getRoot().getProject( model );

            if ( project.exists() )
            {
                problems.add( MessageFormat.format( Messages.validation_errors_projectExists, model ) );
            }
        }
    };

    public static Validator<String> EMPTY_OR_URL = new StringVal()
    {
        public void validate( Problems problems, String compName, String model )
        {
            if ( model.trim().length() == 0 )
            {
                return;
            }

            URL_MUST_BE_VALID.validate( problems, compName, model );
        }
    };
    
    public static Validator<String> URL_MUST_BE_VALID = new  StringVal() 
    {
            public void validate(Problems problems, String compName, String model) {
                StringValidators.REQUIRE_NON_EMPTY_STRING.validate( problems, compName, model );
                try {
                    URL url = new URL (model);
                    String host = url.getHost();
                    if (host.indexOf(" ") > 0 || host.indexOf ("\t") > 0) {
                        problems.add (NLS.bind( Messages.errors_host_may_not_contain_space, host )); //NOI18N
                        return;
                    }
                    //#MECLIPSE-1317
                    try
                    {
                        new URI( model ).normalize();
                    }
                    catch ( URISyntaxException e )
                    {
                        String problem = NLS.bind( Messages.errors_not_valid_url, model); 
                        problems.add(problem);
                        return;
                    }
                    
                    String protocol = url.getProtocol();
                    if ("mailto".equals(protocol)) { //NOI18N
                        String emailAddress = url.toString().substring("mailto:".length()); //NOI18N
                        emailAddress = emailAddress == null ? "" : emailAddress;
                        StringValidators.EMAIL_ADDRESS.validate(problems, compName,
                                emailAddress);
                        return;
                    }
                } catch (MalformedURLException e) {
                    String problem = NLS.bind( Messages.errors_not_valid_url, model); 
                    problems.add(problem);
                }
            }

    };
    
    private static abstract class StringVal implements Validator<String> {
        public Class<String> modelType()
        {
            return String.class;
        }
    }
}
