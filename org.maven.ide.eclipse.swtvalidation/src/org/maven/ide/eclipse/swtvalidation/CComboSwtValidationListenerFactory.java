
package org.maven.ide.eclipse.swtvalidation;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Display;
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
public class CComboSwtValidationListenerFactory extends ValidationListenerFactory<CCombo, String> {

    public CComboSwtValidationListenerFactory() {
        super(CCombo.class, String.class);
    }

    @Override
    protected ValidationListener<CCombo> createListener(CCombo ct, ValidationStrategy vs, ValidationUI vui, Validator<String> validators) {
        assert Display.getCurrent() != null : "Must be called on event thread";
        final Validator<String> merged = ValidatorUtils.merge(validators);
        return new CComboSWTValidationListener(ct, vs, vui, merged);
    }

    private static class CComboSWTValidationListener extends ValidationListener<CCombo>
            implements ModifyListener, FocusListener, SelectionListener {

        private Validator<String> validator;
        private boolean hasFatalProblem = false;

        private CComboSWTValidationListener(CCombo component, ValidationStrategy strategy, ValidationUI validationUI, Validator<String> validator) {
            super(CCombo.class, validationUI, component);
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
                    component.addSelectionListener(this);
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
            CCombo component = getTarget();
            if (component.isDisposed()||!component.isEnabled()) {
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

        public void widgetSelected(SelectionEvent se) {
            performValidation();
        }

        public void widgetDefaultSelected(SelectionEvent se) {
        }
    }
}
