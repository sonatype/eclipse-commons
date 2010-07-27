package org.maven.ide.eclipse.ui.common;

import java.beans.Beans;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Combo;

public class InputHistory
{
    /** the history limit */
    protected static final int MAX_HISTORY = 10;

    /** dialog settings to store input history */
    protected IDialogSettings dialogSettings;

    /** the Map of field ids to List of comboboxes that share the same history */
    private Map<String, List<Combo>> comboMap;

    private List<String> privileged;

    public InputHistory( String sectionName )
    {
        this( sectionName, new String[0] );
    }

    public InputHistory( String sectionName, String[] privileged )
    {
        comboMap = new HashMap<String, List<Combo>>();

        Activator activator = Activator.getDefault();
        if ( activator != null )
        {
            IDialogSettings pluginSettings = activator.getDialogSettings();
            dialogSettings = pluginSettings.getSection( sectionName );
            if ( dialogSettings == null )
            {
                dialogSettings = pluginSettings.addNewSection( sectionName );
                pluginSettings.addSection( dialogSettings );
            }
        }
        assert privileged != null;
        this.privileged = Arrays.asList( privileged );
    }

    /** Loads the input history from the dialog settings. */
    public void load()
    {
        if ( Beans.isDesignTime() )
        {
            return;
        }

        for ( Map.Entry<String, List<Combo>> e : comboMap.entrySet() )
        {
            String id = e.getKey();
            Set<String> items = new LinkedHashSet<String>();
            String[] itemsArr = dialogSettings.getArray( id );
            items.addAll( privileged );
            if ( itemsArr != null )
            {
                items.addAll( Arrays.asList( itemsArr ) );
            }
            for ( Combo combo : e.getValue() )
            {
                if ( !combo.isDisposed() )
                {
                    String text = combo.getText();
                    combo.setItems( items.toArray( new String[0] ) );
                    if ( text.length() > 0 )
                    {
                        // setItems() clears the text input, so we need to restore it
                        combo.setText( text );
                    }
                    else if ( items.size() > 0 )
                    {
                        combo.setText( items.iterator().next() );
                    }
                }
            }
        }
    }

    /** Saves the input history into the dialog settings. */
    public void save()
    {
        for ( Map.Entry<String, List<Combo>> e : comboMap.entrySet() )
        {
            String id = e.getKey();

            Set<String> history = new LinkedHashSet<String>( MAX_HISTORY );

            for ( Combo combo : e.getValue() )
            {
                String lastValue = combo.getText();
                if ( lastValue != null && lastValue.trim().length() > 0 )
                {
                    history.add( lastValue );
                }
            }

            Combo combo = e.getValue().iterator().next();
            String[] items = combo.getItems();
            for ( int j = 0; j < items.length && history.size() < MAX_HISTORY; j++ )
            {
                // do not store the privileged items if they are not selected.
                // we eventually inject the same or different set next time
                if ( !privileged.contains( items[j] ) )
                {
                    history.add( items[j] );
                }
            }

            dialogSettings.put( id, history.toArray( new String[history.size()] ) );
        }
    }

    /** Adds an input control to the list of fields to save. */
    public void add( String id, Combo combo )
    {
        if ( combo != null )
        {
            List<Combo> combos = comboMap.get( id );
            if ( combos == null )
            {
                combos = new ArrayList<Combo>();
                comboMap.put( id, combos );
            }
            combos.add( combo );
        }
    }
}
