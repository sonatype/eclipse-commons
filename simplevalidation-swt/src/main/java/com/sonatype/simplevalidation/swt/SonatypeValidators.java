
package com.sonatype.simplevalidation.swt;

import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.openide.util.NbBundle;

/**
 *
 * @author mkleint
 */
public final class SonatypeValidators {

    public static Validator<String> createArtifactIdValidators() {
        return ValidatorUtils.merge(
//                    StringValidators.REQUIRE_NON_EMPTY_STRING,
//                        Validators.MAY_NOT_START_WITH_DIGIT,
                    StringValidators.NO_WHITESPACE,
                    StringValidators.regexp("[a-zA-Z0-9_\\-.]+", NbBundle.getMessage(SonatypeValidators.class, "ERR_Coordinate_Invalid"), false)
               );
    }

    public static Validator<String> createGroupIdValidators() {
        return ValidatorUtils.merge(
//                    StringValidators.REQUIRE_NON_EMPTY_STRING,
//                        Validators.MAY_NOT_START_WITH_DIGIT,
                    StringValidators.NO_WHITESPACE,
                    StringValidators.regexp("[a-zA-Z0-9_\\-.]+", NbBundle.getMessage(SonatypeValidators.class, "ERR_Coordinate_Invalid"), false)
               );
    }

    public static Validator<String> createVersionValidators() {
        return ValidatorUtils.merge(
//                    StringValidators.REQUIRE_NON_EMPTY_STRING,
//                        Validators.MAY_NOT_START_WITH_DIGIT,
                    StringValidators.NO_WHITESPACE,
                    StringValidators.regexp("[a-zA-Z0-9_\\-.]+", NbBundle.getMessage(SonatypeValidators.class, "ERR_Coordinate_Invalid"),  false)
               );
    }
}
