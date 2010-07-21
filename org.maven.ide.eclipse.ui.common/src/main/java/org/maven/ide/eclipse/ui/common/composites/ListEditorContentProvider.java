package org.maven.ide.eclipse.ui.common.composites;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ListEditorContentProvider<T>
    implements IStructuredContentProvider
{

    public static final Object[] EMPTY = new Object[0];

    @SuppressWarnings( "unchecked" )
    public Object[] getElements( Object input )
    {
        if ( input instanceof List )
        {
            List<T> list = (List<T>) input;
            return list.toArray();
        }
        return EMPTY;
    }

    public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
    {
    }

    public void dispose()
    {
    }
}
