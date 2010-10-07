package org.maven.ide.eclipse.ui.common.dialogs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.ErrorHandlingUtils;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated
 */
public abstract class RemoteResourceLookupDialog
    extends TitleAreaDialog
{
    private Logger log = LoggerFactory.getLogger( RemoteResourceLookupDialog.class );

    private String serverUrl;

    private String title;

    private String selectMessage;

    private String readyToLoadMessage;

    private String serverName;

    private String resourceLabelText;

    private String loadButtonText;

    private volatile Job loadJob;

    private Object input;

    private ExpandableComposite expandableComposite;

    private UrlInputComposite urlInputComposite;

    private Composite resourceComposite;

    private Button loadButton;

    private SwtValidationGroup loadButtonGroup;
    
    private SwtValidationGroup rootGroup;

    public RemoteResourceLookupDialog( Shell parentShell, String serverUrl )
    {
        super( parentShell );
        this.serverUrl = serverUrl;
        setShellStyle( SWT.CLOSE | SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL );

        loadButtonGroup = SwtValidationGroup.create( new LoadButtonValidationUI() );
        
        rootGroup = SwtValidationGroup.create(SwtValidationUI.createUI(this, SwtValidationUI.MESSAGE));
        rootGroup.addItem(loadButtonGroup, false);
    }

    @Override
    protected void configureShell( Shell newShell )
    {
        super.configureShell( newShell );
        newShell.setText( title );
    }

    @Override
    public void setTitle( String newTitle )
    {
        super.setTitle( newTitle );
        this.title = newTitle;
    }

    public void setServerName( String name )
    {
        serverName = name;
    }

    public void setSelectMessage( String message )
    {
        selectMessage = message;
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

    @Override
    protected Control createButtonBar( Composite parent )
    {
        Control control = super.createButtonBar( parent );
        updateOkState( false );
        return control;
    }

    @Override
    protected Control createDialogArea( Composite parent )
    {
        Composite dialogArea = (Composite) super.createDialogArea( parent );
        super.setTitle( title );

        Composite panel = new Composite( dialogArea, SWT.NONE );
        GridLayout gl = new GridLayout( 1, false );
        gl.marginLeft = 10;
        gl.marginRight = 10;
        panel.setLayout( gl );
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true );
        gd.heightHint = 400;
        panel.setLayoutData( gd );

        createExpandableComposite( panel );
        resourceComposite = createResourcePanel( panel );

        applyDialogFont( dialogArea );
        rootGroup.performValidation();
        if ( serverUrl != null )
        {
            reload();
        }

        return dialogArea;
    }

    private void createExpandableComposite( final Composite parent )
    {
        expandableComposite =
            new ExpandableComposite( parent, ExpandableComposite.COMPACT | ExpandableComposite.TWISTIE );
        expandableComposite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );

        urlInputComposite =
            new UrlInputComposite( expandableComposite, null, loadButtonGroup, UrlInputComposite.ALLOW_ANONYMOUS );
        urlInputComposite.setUrlLabelText( NLS.bind( Messages.remoteResourceLookupDialog_server_label, serverName ) );
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

        rootGroup.addItem( new UrlValidationListener( urlInputComposite ), false );

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
            if ( url.length() != 0 ) 
            {
                if ( !url.equals( serverUrl ) )
                {
                    setInput( null );
                    serverUrl = url;
                    problems.add( readyToLoadMessage, Severity.INFO );
                }
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

        Shell shell = getShell();
        Point minSize = shell.getMinimumSize();
        shell.setMinimumSize( shell.getSize().x, minSize.y );
        shell.pack();
        expandableComposite.getParent().layout();
        shell.setMinimumSize( minSize );
    }

    private void updateExpandableTitle()
    {
        expandableComposite.setText( expandableComposite.isExpanded() ? NLS.bind(
                                                                                  Messages.remoteResourceLookupDialog_server_expanded,
                                                                                  serverName )
                        : NLS.bind( Messages.remoteResourceLookupDialog_server_collapsed, serverName,
                                    urlInputComposite.getUrl() ) );
    }

    private void reload()
    {
        if ( loadJob != null )
        {
            loadJob.cancel();
        }

        input = null;
        final String url = urlInputComposite.getUrl();
        updateLoadControls( false, Messages.remoteResourceLookupDialog_loading, IMessageProvider.INFORMATION );
        loadJob = new Job( Messages.remoteResourceLookupDialog_loading )
        {
            private volatile boolean cancel = false;

            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                if (cancel) return Status.OK_STATUS;
                try
                {
                    input = loadResources( url, monitor );
                }
                catch ( Exception e )
                {
                    String message = exceptionToUIText( e );

                    if ( message == null )
                    {
                        message = NLS.bind( Messages.remoteResourceLookupDialog_error_other, e.getMessage() );
                    }
                    log.error( message, e );

                    if (cancel) return Status.OK_STATUS;
                    updateLoadControls( true, message, IMessageProvider.ERROR );
                    return Status.OK_STATUS;
                }
                if (cancel) return Status.OK_STATUS;

                updateLoadControls( true, selectMessage, IMessageProvider.NONE );
                return Status.OK_STATUS;
            }

            @Override
            protected void canceling()
            {
                cancel  = true;
            }
        };
        loadJob.schedule();
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
                }
                else
                {
                    resourceComposite.setFocus();
                    if ( rootGroup.performValidation() == null )
                    {
                        //clear any previous error messages or the error icon keeps sticked in..
                        setMessage( null, IMessageProvider.ERROR );
                        setMessage( message, messageType );
                    }
                }
            }
        } );
    }

    protected void updateOkState( boolean enable )
    {
        Button ok = getButton( IDialogConstants.OK_ID );
        if ( ok != null )
        {
            ok.setEnabled( enable );
        }
    }

    protected String getServerUrl()
    {
        return urlInputComposite.getUrlText();
    }

    @SuppressWarnings( "unchecked" )
    protected void addToValidationGroup( Control control, Validator<String> validator )
    {
        loadButtonGroup.add( control, validator );
    }

    /**
     * this is the validation group root validation group, contains the load label group as well..
     * and handles error messages 
     * @return
     */
    protected SwtValidationGroup getRootValidationGroup()
    {
        return rootGroup;
    }
    
    /**
     * this is the validation group associated with the url input composite and handles load button enablement
     * @return
     */
    protected SwtValidationGroup getLoadButtonValidationGroup()
    {
        return loadButtonGroup;
    }
    
    /**
     * this is the validation group associated with the url input composite.
     * @return
     * @deprecated
     */
    protected SwtValidationGroup getValidationGroup()
    {
        return getLoadButtonValidationGroup();
    }
    

    abstract protected Composite createResourcePanel( Composite parent );

    abstract protected void setInput( Object input );

    abstract protected Object loadResources( String url, IProgressMonitor monitor )
        throws Exception;
    
    
    private class LoadButtonValidationUI implements ValidationUI {

        public void showProblem(Problem problem) {
        	if (loadButton == null) return;
            loadButton.setEnabled( !problem.isFatal() );
        }

        public void clearProblem() {
        	if (loadButton == null) return;
            loadButton.setEnabled( true );
        }
    };
}
