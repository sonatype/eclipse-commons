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
import org.netbeans.validation.api.Problem;
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

    private SwtValidationGroup loadButtonGroup;
    
    private SwtValidationGroup okButtonGroup;
    
    private SwtValidationGroup rootGroup;

    public RemoteResourceLookupPage( String serverUrl )
    {
        super( RemoteResourceLookupPage.class.getName() );
        this.serverUrl = serverUrl;
//        setPageComplete( false );

        loadButtonGroup = SwtValidationGroup.create( new LoadButtonValidationUI() );
        
        rootGroup = SwtValidationGroup.create(SwtValidationUI.createUI(this, SwtValidationUI.MESSAGE));
        rootGroup.addItem(loadButtonGroup, false);
        okButtonGroup = SwtValidationGroup.create(SwtValidationUI.createUI(this, SwtValidationUI.BUTTON));
        rootGroup.addItem(okButtonGroup, false);
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

        setControl( panel );

        rootGroup.performValidation();
        if ( serverUrl != null )
        {
            reload( true );
        }
    }

    protected UrlInputComposite createUrlInputComposite( Composite parent )
    {
        return new UrlInputComposite( parent, null, getLoadButtonValidationGroup(), UrlInputComposite.ALLOW_ANONYMOUS );
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
                reload(false);
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
                    setInput( input = null );
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

    private void reload(boolean initial)
    {
        input = null;
        final String url = urlInputComposite.getUrl();
        updateLoadControls( false, Messages.remoteResourceLookupPage_loading, IMessageProvider.INFORMATION, false );

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

        if ( exception[0] == null )
        {
            updateLoadControls( true, selectMessage, IMessageProvider.NONE, initial );
        }
        else
        {
            String message = exceptionToUIText( exception[0] );

            if ( message == null )
            {
                message = NLS.bind( Messages.remoteResourceLookupPage_error_other, exception[0].getMessage() );
            }
            log.error( message, exception[0] );

            updateLoadControls( true, message, IMessageProvider.ERROR, false );
        }
    }

    protected String exceptionToUIText( Exception e )
    {
        // TODO mkleint: this sort of won't work in governor use cases
        return ErrorHandlingUtils.convertNexusIOExceptionToUIText( e );
    }

    private void updateLoadControls( final boolean enable, final String message, final int messageType, final boolean initial )
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
                if (initial) {
                	setInitialInput( input );
                } else {
                	setInput( input );
                }
                if ( messageType == IMessageProvider.ERROR )
                {
                    expandableComposite.setExpanded( true );
                    updateExpandableState();
                    urlInputComposite.setFocus();
                    setMessage( message, messageType );
//                    setPageComplete( false );
                }
                else
                {
                    resourceComposite.setFocus();
                    if ( rootGroup.performValidation() == null )
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
     * this is the validation group for handling the ok/finish button
     * @return
     */
    protected SwtValidationGroup getFinishButtonValidationGroup()
    {
        return okButtonGroup;
    }    
    
    abstract protected Composite createResourcePanel( Composite parent );

    /**
     * set input to the custom child fields. called from UI thread.
     * @param input
     */
    abstract protected void setInput( Object input );
    
    /**
     * this method is called when there is an initial serverUrl passed in constructor and
     * the loading from that server succeeds. To be used for preselection of values in subclasses.
     * Use by overriding. The default implementation just calls setInput(). if you override, do so as well, or
     * make sure you call setInput() yourself
     * @param input
     */
    protected void setInitialInput( Object input )
    {
    	setInput(input);
    }

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
