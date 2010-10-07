package org.maven.ide.eclipse.ui.common;

import org.eclipse.swt.widgets.Text;

public class FormUtils
{
    public static String toNull( String s )
    {
        return s == null || s.length() == 0 ? null : s;
    }

    public static String toNull( Text text )
    {
        return toNull( text.getText().trim() );
    }

    public static String nvl( String s )
    {
        return s == null ? "" : s;
    }
}
