package org.maven.ide.eclipse.ui.common.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.ErrorHandlingUtils;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class RemoteResourceLookupPage
    extends WizardPage
{
    private Logger log = LoggerFactory.getLogger( RemoteResourceLookupPage.class );

    private String serverUrl;

    private String selectMessage;

    private String readyToLoadMessage;

    private String serverName;

    private String resourceLabelText;

    private String loadButtonText;

    private Object input;

    private ExpandableComposite expandableComposite;

    private UrlInputComposite urlInputComposite;

    private Composite resourceComposite;

    private Button loadButton;

    private SwtValidationGroup validationGroup;

    public RemoteResourceLookupPage( String serverUrl )
    {
        super( RemoteResourceLookupPage.class.getName() );
        this.serverUrl = serverUrl;
        setPageComplete( false );

        validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
    }

    public void setServerName( String name )
    {
        serverName = name;
    }

    public void setSelectMessage( String message )
    {
        selectMessage = message;
        setDescription( selectMessage );
    }

    public void setReadyToLoadMessage( String message )
    {
        readyToLoadMessage = message;
    }

    public void setResourceLabelText( String text )
    {
        resourceLabelText = text;
    }

    public void setLoadButtonText( String text )
    {
        loadButtonText = text;
    }

    public void createControl( Composite parent )
    {
        Composite panel = new Composite( parent, SWT.NONE );
        GridLayout gl = new GridLayout( 1, false );
        gl.marginLeft = 10;
        gl.marginRight = 10;
        panel.setLayout( gl );
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true );
        gd.heightHint = 400;
        panel.setLayoutData( gd );

        createExpandableComposite( panel );
        resourceComposite = createResourcePanel( panel );

        validationGroup.performValidation();
        if ( serverUrl != null )
        {
            reload();
        }

        setControl( panel );
        // updateExpandableState();
    }

    protected UrlInputComposite createUrlInputComposite( Composite parent )
    {
        return new UrlInputComposite( parent, null, getValidationGroup(), UrlInputComposite.ALLOW_ANONYMOUS );
    }

    private void createExpandableComposite( final Composite parent )
    {
        expandableComposite =
            new ExpandableComposite( parent, ExpandableComposite.COMPACT | ExpandableComposite.TWISTIE );
        expandableComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );

        urlInputComposite = createUrlInputComposite( expandableComposite );
        urlInputComposite.setUrlLabelText( NLS.bind( Messages.remoteResourceLookupPage_server_label, serverName ) );
        urlInputComposite.setUrl( serverUrl );

        Composite reloadPanel = new Composite( parent, SWT.NONE );
        GridLayout gridLayout = new GridLayout( 2, false );
        gridLayout.marginWidth = 0;
        reloadPanel.setLayout( gridLayout );
        reloadPanel.setLayoutData( new GridData( SWT.FILL, SWT.BOTTOM, false, false ) );

        Label label = new Label( reloadPanel, SWT.NONE );
        label.setText( resourceLabelText );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.BOTTOM, true, false ) );

        loadButton = new Button( reloadPanel, SWT.PUSH );
        loadButton.setText( loadButtonText );
        loadButton.setLayoutData( new GridData( SWT.RIGHT, SWT.BOTTOM, false, false ) );
        loadButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                reload();
            }
        } );

        validationGroup.addItem( new UrlValidationListener( urlInputComposite ), false );

        String url = urlInputComposite.getUrlText();
        if ( url.length() > 0 )
        {
            serverUrl = url;
        }
        expandableComposite.setExpanded( serverUrl == null );
        expandableComposite.setClient( urlInputComposite );
        expandableComposite.addExpansionListener( new ExpansionAdapter()
        {
            public void expansionStateChanged( ExpansionEvent e )
            {
                updateExpandableState();
            }
        } );

        updateExpandableTitle();
    }

    private class UrlValidationListener
        extends ValidationListener<UrlInputComposite>
        implements ModifyListener
    {
        protected UrlValidationListener( UrlInputComposite urlInputComposite )
        {
            super( UrlInputComposite.class, ValidationUI.NO_OP, urlInputComposite );
            urlInputComposite.addModifyListener( this );
        }

        @Override
        protected void performValidation( Problems problems )
        {
            String url = urlInputComposite.getUrlText();
            if ( url.length() == 0 )
            {
                loadButton.setEnabled( false );
            }
            else
            {
                if ( !url.equals( serverUrl ) )
                {
                    setInput( input = null );
                    serverUrl = url;
                    problems.add( readyToLoadMessage, Severity.FATAL );
                }
                loadButton.setEnabled( true );
            }
        }

        public void modifyText( ModifyEvent e )
        {
            performValidation();
        }
    }

    private void updateExpandableState()
    {
        updateExpandableTitle();

        // Shell shell = getShell();
        // Point minSize = shell.getMinimumSize();
        // shell.setMinimumSize( shell.getSize().x, minSize.y );
        // shell.pack();
        expandableComposite.getParent().layout();
        // shell.setMinimumSize( minSize );
    }

    private void updateExpandableTitle()
    {
        expandableComposite.setText( expandableComposite.isExpanded() ? NLS.bind(
                                                                                  Messages.remoteResourceLookupPage_server_expanded,
                                                                                  serverName )
                        : NLS.bind( Messages.remoteResourceLookupPage_server_collapsed, serverName,
                                    urlInputComposite.getUrl() ) );
    }

    private void reload()
    {
        input = null;
        final String url = urlInputComposite.getUrl();
        updateLoadControls( false, Messages.remoteResourceLookupPage_loading, IMessageProvider.INFORMATION );

        final Exception[] exception = new Exception[1];
        try
        {
            getContainer().run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor )
                    throws InvocationTargetException, InterruptedException
                {
                    monitor.beginTask( Messages.remoteResourceLookupPage_loading, 2 );

                    try
                    {
                        input = loadResources( url, monitor );
                        updateLoadControls( true, selectMessage, IMessageProvider.NONE );
                    }
                    catch ( Exception e )
                    {
                        exception[0] = e;
                    }
                    finally
                    {
                        monitor.done();
                    }
                }
            } );
        }
        catch ( InvocationTargetException e )
        {
            exception[0] = e;
        }
        catch ( InterruptedException e )
        {
            exception[0] = e;
        }

        if ( exception[0] != null )
        {
            String message = exceptionToUIText( exception[0] );

            if ( message == null )
            {
                message = NLS.bind( Messages.remoteResourceLookupPage_error_other, exception[0].getMessage() );
            }
            log.error( message, exception[0] );

            updateLoadControls( true, message, IMessageProvider.ERROR );
        }
    }

    protected String exceptionToUIText( Exception e )
    {
        // TODO mkleint: this sort of won't work in governor use cases
        return ErrorHandlingUtils.convertNexusIOExceptionToUIText( e );
    }

    private void updateLoadControls( final boolean enable, final String message, final int messageType )
    {
        if ( resourceComposite.isDisposed() )
        {
            return;
        }

        getShell().getDisplay().syncExec( new Runnable()
        {
            public void run()
            {
                loadButton.setEnabled( enable );
                setInput( input );
                if ( messageType == IMessageProvider.ERROR )
                {
                    expandableComposite.setExpanded( true );
                    updateExpandableState();
                    urlInputComposite.setFocus();
                    setMessage( message, messageType );
                    setPageComplete( false );
                }
                else
                {
                    resourceComposite.setFocus();
                    if ( validationGroup.performValidation() == null )
                    {
                        setMessage( message, messageType );
                    }
                }
            }
        } );
    }

    protected String getServerUrl()
    {
        return urlInputComposite.getUrlText();
    }

    @SuppressWarnings( "unchecked" )
    protected void addToValidationGroup( Control control, Validator<String> validator )
    {
        validationGroup.add( control, validator );
    }

    protected SwtValidationGroup getValidationGroup()
    {
        return validationGroup;
    }

    abstract protected Composite createResourcePanel( Composite parent );

    abstract protected void setInput( Object input );

    abstract protected Object loadResources( String url, IProgressMonitor monitor )
        throws Exception;
}
