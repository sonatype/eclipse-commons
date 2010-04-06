/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package com.sonatype.simplevalidation.swt;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.ui.*;
import org.openide.util.Lookup;

/**
 * Factory class for creating {@link ValidationUI} instances that can decorate
 * a Swing GUI-component when it has a Problem.
 *
 * <p> By default, one instance of a class implementing this interface
 * is used to create a
 * ValidationUI for all components handled by the simplevalidation
 * framework. This instance can be replaced with a custom one, as
 * described below.  
 * <p>
 * For custom decoration, simply pass a different
 * decorator factory in ValidationGroup.add() (and if necessary, proxy the
 * default decorator factory for all but some specific kind of component).
 * 
 * A rudimentary example of writing a component decorator:  The code and
 * description below show how to replace the default
 * SwingComponentDecorationFactory with one that will create {@code ValidationUI}
 * instances that draws a thick colored border around the component
 * when there is an error.
 * <p>
 *<blockquote><pre>{@code
 * package com.foo.myapp;
 * public class MySwingComponentDecorationFactory extends SwingComponentDecorationFactory {
 *      public ValidationUI decorationFor(final JComponent c) {
 *         return new ValidationUI() {
 *             private javax.swing.border.Border origBorder = c.getBorder();
 *             public void showProblem(Problem problem) {
 *                if( problem == null ) {
 *                    c.setBorder(origBorder);
 *                } else {
 *                    c.setBorder(javax.swing.BorderFactory.createLineBorder(problem.severity().color(), 3));
 *                }
 *             }
 *        };
 *     }
 *  };
 * }</pre></blockquote>
 * 
 * Our <code>MySwingComponentDecorationFactory</code> is then registered so that
 * it can be found using JDK 6's <code>ServiceLoader</code> (or NetBeans'
 * <code>Lookup</code>):  Create a file named <code>org.netbeans.validation.api.ui.swing.SwingComponentDecorationFactory</code>
 * in the folder <code>META-INF/services</code> in your source root (so that
 * it will be included in the JAR file).  Add one line of text to this file -
 * the fully qualified name of your class, e.g.
 * <pre>
 * com.foo.myapp.MySwingComponentDecorationFactory
 * </pre>
 *
 * @author Tim Boudreau
 * @author Hugo Heden
 */
public abstract class SwtComponentDecorationFactory {

    private static final SwtComponentDecorationFactory noOpDecorationFactory = new SwtComponentDecorationFactory() {
        @Override
        public ValidationUI decorationFor(Widget c) {
            return ValidationUI.NO_OP;
        }
    };
    //TODO create a decorator that does something..
    private static SwtComponentDecorationFactory componentDecorator =
            new DefaultSwtComponentDecorationFactory();

    /**
     *
     * Special decorator that does not decorate at all -- even if
     * there is a problem -- a "null" decorator. This is useful if no
     * component decorations are desired. For example application
     * wide:
     *
     * <blockquote><pre>{@code
     * SwingComponentDecorationFactory.set(SwingComponentDecorationFactory.getNoOpDecorationFactory());
     * }</pre></blockquote>
     * Or just for one specific group of components, here a SwingValidationGroup:
     * <blockquote><pre>{@code
     * SwingValidationGroup group = SwingValidationGroup.create(SwingComponentDecorationFactory.getNoOpDecorationFactory());
     * }</pre></blockquote>
     *
     *
     */

    public static final SwtComponentDecorationFactory getNoOpDecorationFactory() {
        return noOpDecorationFactory;
    }


    /**
     * Factory method that creates a {@code ValidationUI} visually attached to
     * the Swing GUI-component when there is a {@code Problem}.  When a
     * {@code Problem} occurs in a component, this {@code ValidationUI} needs to be
     * updated using {@link ValidationUI#showProblem} (this is
     * typically done from within a {@code ValidationListener}) and
     * will then apply some visual mark to the component. When the
     * problem disappears, {@code ValidationListener} will pass {@code
     * null} to {@code ValidationUI#showProblem},
     * which makes sure that the visual cue is removed, so that the
     * components is restored to its original visual state.
     * 
     * @param c The component
     * @return A ValidationUI, visually attached to the component. 
     */
    public abstract ValidationUI decorationFor(Widget c);
    
    /**
     * 
     * Get the current application wide component decorator
     */
    public static final SwtComponentDecorationFactory getDefault() {
        SwtComponentDecorationFactory result = Lookup.getDefault().lookup(SwtComponentDecorationFactory.class);
        if (result == null) {
            result = componentDecorator;
        }
        return result;
    }


    private static class DefaultSwtComponentDecorationFactory extends SwtComponentDecorationFactory {

        @Override
        public ValidationUI decorationFor(final Widget c) {
            assert Display.getCurrent() != null;
            if (c instanceof Control) {
                return new ControlValidationUI((Control)c);
            }
            return ValidationUI.NO_OP;
        }

    }

    private static class ControlValidationUI implements ValidationUI {

        private ControlDecoration dec;
        private Image errorImage;
        private Image warningImage;
        private Image infoImage;
        private ControlValidationUI(Control combo) {
            dec = new ControlDecoration(combo, SWT.TOP | SWT.LEFT);
            dec.setMarginWidth(1);
            dec.setShowHover(true);
            //TODO is this realy the best way to load images?
            errorImage = new Image(Display.getCurrent(), getClass().getResourceAsStream("/org/netbeans/validation/api/error-badge.png"));
            infoImage = new Image(Display.getCurrent(), getClass().getResourceAsStream("/org/netbeans/validation/api/info-badge.png"));
            warningImage = new Image(Display.getCurrent(), getClass().getResourceAsStream("/org/netbeans/validation/api/warning-badge.png"));

            combo.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent de) {
                    dec.dispose();
                    errorImage.dispose();
                    warningImage.dispose();
                    infoImage.dispose();
                }
            });

        }

        @Override
        public void showProblem(Problem prblm) {
            dec.setDescriptionText(prblm.getMessage());
            if (prblm.isFatal()) {
                dec.setImage(errorImage);
            } else {
                if (Severity.WARNING.equals(prblm.severity())) {
                    dec.setImage(warningImage);
                } else if (Severity.INFO.equals(prblm.severity())) {
                    dec.setImage(infoImage);
                } else {
                    dec.setImage(null);
                }
            }
            dec.show();
        }

        @Override
        public void clearProblem() {
            dec.setImage(null);
            dec.hide();
        }

    }

}
