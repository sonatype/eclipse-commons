

package org.maven.ide.eclipse.swtvalidation;


import org.eclipse.swt.widgets.Combo;
import org.netbeans.validation.api.ui.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;

/**
 * {@link ValidationGroup} subclass specialized for handling Swing
 * components.  This subclass has {@code add}-methods for adding
 * GUI-components for common Swing cases. There are also a method for
 * getting the {@link SwingComponentDecorationFactory} used by this
 * SwingValidationGroup to create decorations for the separate
 * GUI-components added to the group. A custom {@code SwingComponentDecorationFactory}
 * can be specified when creating the {@code SwingValidationGroup}.
 *
 * <p> For components this library supports out-of-the-box such as
 * <code>JTextField</code>s or <code>JComboBox</code>es, simply call
 * one of the <code>add()</code> methods with your component and
 * validators.  For validating your own components or ones this class
 * doesn't have methods for, you implement {@link ValidationListener}s, and add them
 * to the {@code ValidationGroup} using the the method
 * {@link ValidationGroup#add(org.netbeans.validation.api.ui.ValidationItem)}
 */
public final class SwtValidationGroup extends ValidationGroup {
    private final SwtComponentDecorationFactory decorator;

    private SwtValidationGroup(GroupValidator additionalGroupValidation, SwtComponentDecorationFactory decorator, ValidationUI... ui) {
        super(additionalGroupValidation, ui);
        if (ui == null) {
            throw new NullPointerException();
        }
        this.decorator = ( decorator!=null ? decorator : SwtComponentDecorationFactory.getDefault() );
    }

    public static SwtValidationGroup create(ValidationUI... ui) {
        assert Display.getCurrent() != null : "Must be called on event thread";
        return new SwtValidationGroup(null, null, ui);
    }

    /**
     * Creates a {@code SWTValidationGroup}.
     *
     * Will use a {@code SwtComponentDecorationFactory} returned by {@link SwtComponentDecorationFactory#get()} to modify the appearance of
     * subsequently added components (to show that there is a problem with a
     * component's content). To instead use a custom {@code SwingComponentDecorationFactory}, call
     * {@link #create(org.netbeans.validation.api.ui.GroupValidator, SwtComponentDecorationFactory, org.netbeans.validation.api.ui.ValidationUI[]) }
     *
     * @param ui Zero or more {@code ValidationUI}:s. Will be used by the {@code SWTValidationGroup} to show the leading problem (if any)
     */
    public static SwtValidationGroup create(GroupValidator additionalGroupValidation, ValidationUI... ui) {
        assert Display.getCurrent() != null : "Must be called on event thread";
        return new SwtValidationGroup(additionalGroupValidation, null, ui);
    }

    /**
     * Creates a {@code SWTValidationGroup}.
     * @param additionalGroupValidation may be null
     * @param ui Zero or more {@code ValidationUI}:s. Will all be used by the
     * {@code SWTValidationGroup} to show the leading problem (if any)
     * @param decorator A decorator to be used to modify the appearance of
     * subsequently added components (to show that there is a problem with a
     * component's content).
     */
    public static SwtValidationGroup create(GroupValidator additionalGroupValidation, SwtComponentDecorationFactory decorator, ValidationUI... ui) {
        assert Display.getCurrent() != null : "Must be called on event thread";
        return new SwtValidationGroup(additionalGroupValidation, decorator, ui);
    }

    /**
     * Gets the currently set component decorator used to modify
     * components appearance (to show that there is a problem with a
     * component's content).
     * @return decorator A decorator. May not be null.
     */
    final SwtComponentDecorationFactory getComponentDecorationFactory() {
        return decorator;
    }

    @Override
    protected final <T> ValidationUI decorationFor (T comp) {
        ValidationUI dec = comp instanceof Widget ?
            this.getComponentDecorationFactory().decorationFor((Widget) comp) :
            ValidationUI.NO_OP;
        return dec;
    }

    /**
     * Add a text component to be validated using the passed validators.
     *
     * <p> When a problem occurs, the created ValidationListener will
     * use a {@link ValidationUI} created by this {@code ValidationGroup} to decorate
     * the component.
     *
     * <p> <b>Note:</b> All methods in this class must be called from
     * the AWT Event Dispatch thread, or assertion errors will be
     * thrown.  Manipulating components on other threads is not safe.
     *
     * <p> Swing {@code Document}s (the model used by JTextComponent)
     * are thread-safe, and can be modified from other threads.  In
     * the case that a text component validator receives an event on
     * another thread, validation will be scheduled for later,
     * <i>on</i> the event thread.
     *
     * @param comp A text component such as a <code>JTextField</code>
     * @param validators One or more Validators
     */
    public final void add(Text comp, Validator<String>... validators) {
        assert Display.getCurrent() != null : "Must be called on event thread";
        assert validators.length > 0 : "Empty validator array";
        Validator<String> merged = ValidatorUtils.merge(validators);
        ValidationListener<Text> vl = ValidationListenerFactory.createValidationListener(comp,
                ValidationStrategy.DEFAULT,
                this.getComponentDecorationFactory().decorationFor(comp),
                merged);
        this.addItem (vl, false);
    }


    /**
     * Add a combo box to be validated using the passed validators
     *
     * <p> When a problem occurs, the created {@link ValidationListener} will
     * use a {@link ValidationUI} created by this {@code ValidationGroup} to decorate
     * the component.
     *
     * <p> <b>Note:</b> All methods in this class must be called from
     * the AWT Event Dispatch thread, or assertion errors will be
     * thrown.  Manipulating components on other threads is not safe.
     *
     * @param box A combo box component
     * @param validators One or more Validators
     */
    @SuppressWarnings("unchecked")
    public final void add(Combo box, Validator<String>... validators) {
        assert Display.getCurrent() != null : "Must be called on event thread";
        ValidationListener<Combo> vl = ValidationListenerFactory.createValidationListener(box,
                ValidationStrategy.DEFAULT, 
                this.getComponentDecorationFactory().decorationFor(box),
                ValidatorUtils.<String>merge(validators));
        this.addItem(vl, false);
    }

//TODO
//    /**
//     * Add a JList to be validated using the passed validators
//     *
//     * <p> When a problem occurs, the created {@link ValidationListener} will
//     * use a {@link ValidationUI} created by this {@code ValidationGroup} to decorate
//     * the component.
//     *
//     * <p> <b>Note:</b> All methods in this class must be called from
//     * the AWT Event Dispatch thread, or assertion errors will be
//     * thrown.  Manipulating components on other threads is not safe.
//     *
//     * @param list A JList component
//     * @param validators One or more Validators
//     */
//    @SuppressWarnings("unchecked")
//    public final void add(List list, Validator<Integer[]>... validators) {
//        assert Display.getCurrent() != null : "Must be called on event thread";
//        this.add (ValidationListenerFactory.createValidationListener(list, ValidationStrategy.DEFAULT, this.getComponentDecorationFactory().decorationFor(list), ValidatorUtils.merge(validators)));
//    }


//TODO
//    /**
//     * Add a validator of button models - typically to see if any are selected.
//     *
//     * <p> <b>Note:</b> All methods in this class must be called from
//     * the AWT Event Dispatch thread, or assertion errors will be
//     * thrown.  Manipulating components on other threads is not safe.
//     *
//     * @param buttons The buttons
//     * @param validators A number of Validators
//     */
//    @SuppressWarnings("unchecked")
//    public final void add(final Button[] buttons, Validator<Integer[]>... validators) {
//        assert Display.getCurrent() != null : "Must be called on event thread";
//        this.add (ValidationListenerFactory.createValidationListener(buttons, ValidationStrategy.DEFAULT, ValidationUI.NO_OP, ValidatorUtils.merge(validators)));
//    }


//TODO
//    /**
//     * Create a label which will show the current problem if any, which
//     * can be added to a panel that uses validation
//     *
//     * @return A JLabel
//     */
//    public final Label createProblemLabel() {
//        assert Display.getCurrent() != null : "Must be called on event thread";
//        final MultilineLabel result = new MultilineLabel();
//        addUI(result.createUI());
//        return result;
//    }
//
//    /**
//     * Create a Popup which can be shown over a component to display what the
//     * problem is.  The resulting popup will be word-wrapped and effort will be
//     * made to ensure it fits on-screen in the case of lengthy error messages.
//     *
//     * @param problem The problem to show
//     * @param target The target component
//     * @param relativeLocation The coordinates where the popup should appear,
//     * <i>in the coordinate space of the target component, not the screen</i>.
//     * @return A popup.  Generally, use the returned popup once and get a new
//     * one if you want to show a message again.  The returned popup will take
//     * care of hiding itself on component hierarchy changes.
//     */
//    static Popup createProblemPopup (Problem problem, Component target, Point relativeLocation) {
//        return MultilineLabelUI.showPopup(problem, target, relativeLocation.x, relativeLocation.y);
//    }

    /**
     * Client property which can be set to provide a component's name
     * for use in validation messages.  If not set, the component's
     * <code>getName()</code> method is used.
     */
    private static final String CLIENT_PROP_NAME = "_name";

    /**
     * Get a string name for a component using the following strategy:
     * <ol>
     * <li>Check <code>jc.getClientProperty(CLIENT_PROP_NAME)</code></li>
     * <li>If that returned null, call <code>jc.getName()</code>
     * </ol>
     * @param jc The component
     * @return its name, if any, or null
     */
    public static String nameForComponent(Widget jc) {
        String result = (String) jc.getData(CLIENT_PROP_NAME);
        if (result == null) {
            result = jc.toString();
        }
        return result;
    }

    public static void setComponentName(Widget comp, String localizedName) {
        comp.setData(CLIENT_PROP_NAME, localizedName);
    }
}
