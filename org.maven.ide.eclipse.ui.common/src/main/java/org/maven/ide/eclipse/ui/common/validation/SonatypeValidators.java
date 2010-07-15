package org.maven.ide.eclipse.ui.common.validation;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
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
        return ValidatorUtils.merge( HTTP_URL, StringValidators.URL_MUST_BE_VALID );

    }

    /**
     * 
     */
    public static Validator<String> HTTP_URL = new Validator<String>()
    {

        public void validate( Problems problems, String compName, String model )
        {
            if ( !model.startsWith( "http://" ) && !model.startsWith( "https://" ) )
            {
                problems.add( MessageFormat.format( Messages.validation_errors_remoteUrlRequired, compName ) );
            }
        }

        public Class<String> modelType()
        {
            return String.class;
        }

    };

    /**
     * 
     */
    public static Validator<String> EXISTS_IN_WORKSPACE = new Validator<String>()
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

        public Class<String> modelType()
        {
            return String.class;
        }
    };

    public static Validator<String> EMPTY_OR_URL = new Validator<String>() {
        public void validate( Problems problems, String compName, String model )
        {
            if ( model.trim().length() == 0 )
            {
                return;
            }
            
            StringValidators.URL_MUST_BE_VALID.validate( problems, compName, model );
        }

        public Class<String> modelType()
        {
            return String.class;
        }
    };
}
