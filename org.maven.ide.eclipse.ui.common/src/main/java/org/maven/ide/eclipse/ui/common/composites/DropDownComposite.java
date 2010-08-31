package org.maven.ide.eclipse.ui.common.composites;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class DropDownComposite
    extends Composite
{
    private Text text;

    private Button button;

    private Shell popup;

    private Composite popupComposite;

    public DropDownComposite( Composite parent, FormToolkit toolkit )
    {
        super( parent, SWT.BORDER );

        GridLayout gridLayout = new GridLayout( 2, false );
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        setLayout( gridLayout );

        int flatStyle = SWT.NONE;
        if ( toolkit != null )
        {
            flatStyle |= SWT.FLAT;
            toolkit.adapt( this );
            toolkit.paintBordersFor( this );
        }

        text = new Text( this, SWT.READ_ONLY | flatStyle );
        text.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        text.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyPressed( KeyEvent e )
            {
                if ( e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN )
                {
                    e.doit = false;
                    if ( ( e.stateMask & SWT.ALT ) != 0 )
                    {
                        togglePopup();
                    }
                }
            }
        } );
        text.addMouseListener( new MouseAdapter()
        {
            @Override
            public void mouseDown( MouseEvent e )
            {
                togglePopup();
            }
        } );

        button = new Button( this, SWT.ARROW | SWT.DOWN | flatStyle );
        button.setLayoutData( new GridData( SWT.RIGHT, SWT.FILL, false, true ) );
        button.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                togglePopup();
            }
        } );

        createPopupShell();
    }

    private void createPopupShell()
    {
        popup = new Shell( getShell(), SWT.NO_TRIM | SWT.ON_TOP );
        GridLayout gridLayout = new GridLayout();
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        popup.setLayout( gridLayout );

        Listener listener = new Listener()
        {
            public void handleEvent( Event event )
            {
                if ( event.widget == popup && popup.isVisible() )
                {
                    switch ( event.type )
                    {
                        case SWT.Close:
                            event.doit = false;
                            showPopup( false );
                            break;
                        case SWT.Deactivate:
                            Point point = button.toControl( getDisplay().getCursorLocation() );
                            Point size = button.getSize();
                            Rectangle rect = new Rectangle( 0, 0, size.x, size.y );
                            if ( !rect.contains( point ) )
                            {
                                showPopup( false );
                            }
                            break;
                    }
                }
            }
        };
        popup.addListener( SWT.Close, listener );
        popup.addListener( SWT.Deactivate, listener );

        popupComposite = createPopupContent( popup );
    }

    abstract protected Composite createPopupContent( Shell popup );

    public void showPopup( boolean show )
    {
        if ( show )
        {
            if ( isVisible() )
            {
                Display display = getDisplay();
                Point popupSize = popup.computeSize( SWT.DEFAULT, SWT.DEFAULT );
                Rectangle parentRect = display.map( getParent(), null, getBounds() );
                Point comboSize = getSize();
                Rectangle displayRect = getMonitor().getClientArea();
                int width = Math.max( comboSize.x, popupSize.x );
                int height = popupSize.y;
                int x = parentRect.x;
                int y = parentRect.y + comboSize.y;
                if ( y + height > displayRect.y + displayRect.height )
                {
                    y = parentRect.y - height;
                }
                if ( x + width > displayRect.x + displayRect.width )
                {
                    x = displayRect.x + displayRect.width - popupSize.x;
                }
                popup.setBounds( x, y, width, height );
                popup.setVisible( true );
                popup.setActive();
                if ( isFocusControl() )
                {
                    popupComposite.setFocus();
                }
            }
        }
        else
        {
            if ( popup.isVisible() )
            {
                popup.setVisible( false );
                if ( !popup.isDisposed() && isFocusControl() )
                {
                    text.setFocus();
                }
            }
        }
    }

    @Override
    public boolean isFocusControl()
    {
        if ( text.isFocusControl() || button.isFocusControl() || popup.isFocusControl() )
        {
            return true;
        }
        return super.isFocusControl();
    }

    @Override
    public void dispose()
    {
        super.dispose();
        popup.dispose();
    }

    public String getText()
    {
        return text.getText();
    }

    public void setText( String text )
    {
        this.text.setText( text );
    }

    public Text getTextControl()
    {
        return text;
    }

    public void addModifyListener( ModifyListener listener )
    {
        text.addModifyListener( listener );
    }

    public void removeModifyListener( ModifyListener listener )
    {
        text.removeModifyListener( listener );
    }

    @Override
    public void setEnabled( boolean enabled )
    {
        super.setEnabled( enabled );
        text.setEnabled( enabled );
        button.setEnabled( enabled );
        showPopup( false );
    }

    public void togglePopup()
    {
        showPopup( !popup.isVisible() );
    }
}
