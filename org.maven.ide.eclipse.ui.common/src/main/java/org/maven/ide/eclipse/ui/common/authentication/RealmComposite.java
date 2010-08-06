package org.maven.ide.eclipse.ui.common.authentication;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthRegistryException;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.ISecurityRealmURLAssoc;
import org.maven.ide.eclipse.authentication.SecurityRealmURLAssoc;
import org.maven.ide.eclipse.authentication.internal.URIHelper;
import org.maven.ide.eclipse.swtvalidation.SwtComponentDecorationFactory;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.composites.DropDownComposite;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ui.ValidationListenerFactory;
import org.netbeans.validation.api.ui.ValidationStrategy;

public class RealmComposite
    extends DropDownComposite
{
    private Text urlText;

    private List list;

    private Combo combo;

    private static final AnonymousAccessType[] anonymousOptions =
        new AnonymousAccessType[] { AnonymousAccessType.NOT_ALLOWED, AnonymousAccessType.ALLOWED,
            AnonymousAccessType.REQUIRED };

    private static final String[] anonymousLabels =
        new String[] { Messages.realmComposite_passwordRequired, Messages.realmComposite_anonymousAllowed,
            Messages.realmComposite_anonymousOnly };

    private boolean dirty;

    private boolean updating;

    private String url;

    private IAuthRealm[] realms;

    private IAuthRealm lastSelectedRealm;

    private AnonymousAccessType anonymousAccessType = AnonymousAccessType.NOT_ALLOWED;

    public RealmComposite( Composite parent, Text urlText, SwtValidationGroup validationGroup, boolean formMode )
    {
        super( parent, formMode ? SWT.FLAT : 0 );
        setClient( urlText );
        setValidationGroup( validationGroup );
    }

    private void setClient( final Text text )
    {
        urlText = text;
        text.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                if ( updating )
                {
                    return;
                }

                url = text.getText().trim();
                if ( url.length() > 0 )
                {
                    try
                    {
                        new URL( url );
                        URIHelper.normalize( url );
                    }
                    catch ( MalformedURLException malformedURLException )
                    {
                        // bad URL
                        url = null;
                    }
                    catch ( AuthRegistryException authRegistryException )
                    {
                        // filter out normalizing errors from realm lookup
                        url = null;
                    }
                }
                else
                {
                    // empty URL
                    url = null;
                }

                if ( url == null )
                {
                    setEnabled( false );
                    setText( "" ); //$NON-NLS-1$
                    dirty = false;
                }
                else
                {
                    IAuthRealm realm = AuthFacade.getAuthRegistry().getRealmForURI( url );

                    if ( realm == null )
                    {
                        setEnabled( true );
                        updateText();
                    }
                    else
                    {
                        setEnabled( false );
                        setText( realm.getName() );
                        dirty = false;
                    }
                }
            }
        } );
    }

    private void setValidationGroup( SwtValidationGroup validationGroup )
    {
        Validator<String> validator = new Validator<String>()
        {

            public Class<String> modelType()
            {
                return String.class;
            }

            public void validate( Problems problems, String componentName, String model )
            {
                if ( url != null && url.length() > 0 && model.length() == 0 )
                {
                    problems.add( NLS.bind( Messages.realmComposite_selectRealmFor, urlText.getData( "_name" ) ), Severity.INFO ); //$NON-NLS-2$
                }
            }
        };

        validationGroup.addItem(
                                 ValidationListenerFactory.createValidationListener(
                                                                                     getTextControl(),
                                                                                     ValidationStrategy.DEFAULT,
                                                                                     SwtComponentDecorationFactory.getDefault().decorationFor(
                                                                                                                                               this ),
                                                                                     validator ), false );
    }

    @Override
    protected Composite createPopupContent( Shell parent )
    {
        parent.addShellListener( new ShellAdapter()
        {
            @Override
            public void shellActivated( ShellEvent e )
            {
                realms = AuthFacade.getAuthRegistry().getRealms().toArray( new IAuthRealm[0] );
                String[] names = new String[realms.length];
                int selection = -1;
                for ( int i = realms.length - 1; i >= 0; i-- )
                {
                    String name = realms[i].getName();
                    names[i] = name;
                    if ( lastSelectedRealm != null && name.equals( lastSelectedRealm.getName() ) )
                    {
                        selection = i;
                    }
                }
                list.setItems( names );
                list.setSelection( selection );
            }
        } );

        Composite composite = new Composite( parent, SWT.BORDER );
        composite.setLayout( new GridLayout( 2, false ) );
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        list = new List( composite, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE );
        GridData gd = new GridData( SWT.FILL, SWT.TOP, true, false, 2, 1 );
        gd.heightHint = 80;
        gd.minimumHeight = 80;
        list.setLayoutData( gd );

        Listener listener = new Listener()
        {
            public void handleEvent( Event event )
            {
                if ( event.widget == list )
                {
                    switch ( event.type )
                    {
                        case SWT.Selection:
                            int n = list.getSelectionIndex();
                            if ( n >= 0 )
                            {
                                lastSelectedRealm = realms[n];
                                updateText();
                                dirty = true;
                            }
                            break;
                        case SWT.MouseDoubleClick:
                            showPopup( false );
                            break;
                    }
                }
            }
        };
        list.addListener( SWT.MouseDoubleClick, listener );
        list.addListener( SWT.Selection, listener );

        Label label = new Label( composite, SWT.NONE );
        label.setText( Messages.realmComposite_access_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );

        combo = new Combo( composite, SWT.NONE );
        combo.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false ) );
        combo.setItems( anonymousLabels );
        combo.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                int n = combo.getSelectionIndex();
                if ( n >= 0 )
                {
                    anonymousAccessType = anonymousOptions[n];
                }
                updateText();
                dirty = true;
            }
        } );
        combo.select( 0 );

        return composite;
    }

    private void updateText()
    {
        setText( lastSelectedRealm == null ? "" : lastSelectedRealm.getName() ); //$NON-NLS-1$
    }

    @Override
    public void setText( String text )
    {
        updating = true;
        super.setText( text );
        updating = false;
    }

    @Override
    public boolean isFocusControl()
    {
        if ( list.isFocusControl() || combo.isFocusControl() )
        {
            return true;
        }
        return super.isFocusControl();
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public void clearDirty()
    {
        dirty = false;
    }

    public ISecurityRealmURLAssoc getSecurityRealmURLAssoc()
    {
        return dirty ? new SecurityRealmURLAssoc( null, url, lastSelectedRealm.getId(), anonymousAccessType ) : null;
    }

    public void save( IProgressMonitor monitor )
    {
        if ( dirty && lastSelectedRealm != null && url != null && url.length() > 0 )
        {
            AuthFacade.getAuthRegistry().addURLToRealmAssoc( url, lastSelectedRealm.getId(), anonymousAccessType,
                                                             monitor );
        }
        dirty = false;
    }
}
