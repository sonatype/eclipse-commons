
package org.maven.ide.eclipse.swtvalidation;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationListenerFactory;
import org.netbeans.validation.api.ui.ValidationStrategy;
import org.netbeans.validation.api.ui.ValidationUI;


/**
 *
 * @author mkleint
 */
public class TextSwtValidationListenerFactory extends ValidationListenerFactory<Text, String> {

    public TextSwtValidationListenerFactory() {
        super(Text.class, String.class);
    }

    @Override
    protected ValidationListener<Text> createListener(Text ct, ValidationStrategy vs, ValidationUI vui, Validator<String> validators) {
        assert Display.getCurrent() != null : "Must be called on event thread";
        final Validator<String> merged = ValidatorUtils.merge(validators);
        return new TextSWTValidationListener(ct, vs, vui, merged);
    }

    private static class TextSWTValidationListener extends ValidationListener<Text>
            implements ModifyListener, FocusListener {

        private Validator<String> validator;
        private boolean hasFatalProblem = false;

        private TextSWTValidationListener(Text component, ValidationStrategy strategy, ValidationUI validationUI, Validator<String> validator) {
            super(Text.class, validationUI, component);
            this.validator = validator;
            if (strategy == null) {
                throw new NullPointerException("strategy null");
            }
//TODO        component.addPropertyChangeListener("enabled", new PropertyChangeListener() {
//            public void propertyChange(PropertyChangeEvent evt) {
//                performValidation();
//            }
//        });
            switch (strategy) {
                case DEFAULT:
                case ON_CHANGE_OR_ACTION:
                    component.addModifyListener(this);
                    break;
                case INPUT_VERIFIER:
                    component.addVerifyListener(new VerifyListener() {
                        public void verifyText(VerifyEvent ve) {
                            performValidation();
                            ve.doit = !hasFatalProblem;
                        }
                    });
                    break;
                case ON_FOCUS_LOSS:
                    component.addFocusListener(this);
                    break;
            }
            performValidation(); // Make sure any initial errors are discovered immediately.
        }

        @Override
        protected void performValidation(Problems ps) {
            Text component = getTarget();
            if (!component.isEnabled()) {
                return;
            }
            validator.validate(ps, SwtValidationGroup.nameForComponent(component), component.getText());
            hasFatalProblem = ps.hasFatal();

        }

        public void modifyText(ModifyEvent me) {
            performValidation();
        }

        public void focusGained(FocusEvent fe) {
        }

        public void focusLost(FocusEvent fe) {
            performValidation();
        }
    }
}
