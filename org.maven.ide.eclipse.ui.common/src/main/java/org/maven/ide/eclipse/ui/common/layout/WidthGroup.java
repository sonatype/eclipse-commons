package org.maven.ide.eclipse.ui.common.layout;

import java.util.HashSet;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * Group of controls with the same width (originally from m2eclipse)
 */
public class WidthGroup
    extends ControlAdapter
{
    private final HashSet<Control> controls = new HashSet<Control>();

    @Override
    public void controlResized( ControlEvent e )
    {
        int maxWidth = 0;
        for ( Control c : this.controls )
        {
            int width = c.getSize().x;
            if ( width > maxWidth )
            {
                maxWidth = width;
            }
        }
        if ( maxWidth > 0 )
        {
            for ( Control c : this.controls )
            {
                GridData gd = (GridData) c.getLayoutData();
                gd.widthHint = maxWidth;
                c.getParent().layout();
            }
        }
    }

    public void addControl( Control control )
    {
        controls.add( control );
        control.getParent().layout();
    }
}
