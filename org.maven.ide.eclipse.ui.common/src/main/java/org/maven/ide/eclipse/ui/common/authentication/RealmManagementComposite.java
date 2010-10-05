package org.maven.ide.eclipse.ui.common.authentication;

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.IAuthRegistry;
import org.maven.ide.eclipse.authentication.ISecurityRealmURLAssoc;
import org.maven.ide.eclipse.authentication.SecurityRealmURLAssoc;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.Messages;
import org.maven.ide.eclipse.ui.common.composites.ValidatingComposite;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

public class RealmManagementComposite
    extends ValidatingComposite
{
    private static final int URL_COLUMN = 0;

    private static final int ACCESS_COLUMN = 1;

    private static final String EMPTY_URL = "http://"; //$NON-NLS-1$

    private Text idText;

    private String id;

    private Text nameText;

    private String name;

    private Text descriptionText;

    private String description;

    private Combo authenticationCombo;

    private AuthenticationType authenticationType;

    private TableViewer urlViewer;

    private Button addButton;

    private Button removeButton;

    private boolean updating;

    private boolean dirty;

    private boolean newRealm;

    private IAuthRealm realm;

    private List<ISecurityRealmURLAssoc> urlAssocs;

    private Set<ISecurityRealmURLAssoc> toDelete;

    private Set<ISecurityRealmURLAssoc> toUpdate;

    private AuthenticationType[] authenticationOptions = new AuthenticationType[] {
        AuthenticationType.USERNAME_PASSWORD, AuthenticationType.CERTIFICATE,
        AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD };

    private String[] authenticationLabels = new String[] {
        Messages.realmManagementComposite_authenticationType_password,
        Messages.realmManagementComposite_authenticationType_ssl,
        Messages.realmManagementComposite_authenticationType_passwordAndSsl };

    public RealmManagementComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup )
    {
        super( parent, widthGroup, validationGroup );
        GridLayout gridLayout = new GridLayout( 3, false );
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = INPUT_INDENT;
        setLayout( gridLayout );

        createIdControls();
        createNameControls();
        createDescriptionControls();
        createAuthenticationControls();
        createUrlViewer();
        setRealm( null );
    }

    private void createIdControls()
    {
        createLabel( Messages.realmManagementComposite_realmId_label );

        idText = new Text( this, SWT.BORDER );
        idText.setLayoutData( createInputData( 2, 1 ) );
        idText.setData( "name", "idText" ); //$NON-NLS-1$ //$NON-NLS-2$
        idText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                id = idText.getText();
                setDirty();
            }
        } );
        SwtValidationGroup.setComponentName( idText, Messages.realmManagementComposite_realmId_name );
        addToValidationGroup( idText, new Validator<String>()
        {
            public void validate( Problems problems, String componentName, String model )
            {
                if ( model.length() > 0 && AuthFacade.getAuthRegistry().getRealm( model ) != null )
                {
                    problems.add( NLS.bind( Messages.realmManagementComposite_realmId_exists, model ) );
                }
                else
                {
                    StringValidators.REQUIRE_NON_EMPTY_STRING.validate( problems, componentName, model );
                    StringValidators.NO_WHITESPACE.validate( problems, componentName, model );
                }
            }

            public Class<String> modelType()
            {
                return String.class;
            }
        } );
    }

    private void createNameControls()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.realmManagementComposite_realmName_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        nameText = new Text( this, SWT.BORDER );
        nameText.setLayoutData( createInputData( 2, 1 ) );
        nameText.setData( "name", "nameText" ); //$NON-NLS-1$ //$NON-NLS-2$
        nameText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                name = nameText.getText();
                setDirty();
            }
        } );
        SwtValidationGroup.setComponentName( nameText, Messages.realmManagementComposite_realmName_name );
        addToValidationGroup( nameText, StringValidators.REQUIRE_NON_EMPTY_STRING );
    }

    private void createDescriptionControls()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.realmManagementComposite_realmDescription_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        descriptionText = new Text( this, SWT.BORDER );
        descriptionText.setLayoutData( createInputData( 2, 1 ) );
        descriptionText.setData( "name", "descriptionText" ); //$NON-NLS-1$ //$NON-NLS-2$
        descriptionText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                description = descriptionText.getText();
                setDirty();
            }
        } );
    }

    private void createAuthenticationControls()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.realmManagementComposite_realmAuthentication_label );
        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false ) );
        addToWidthGroup( label );

        authenticationCombo = new Combo( this, SWT.READ_ONLY );
        authenticationCombo.setItems( authenticationLabels );
        authenticationCombo.select( 0 );
        authenticationCombo.setData( "name", "authenticationCombo" ); //$NON-NLS-1$ //$NON-NLS-2$
        authenticationCombo.setLayoutData( createInputData( 2, 1 ) );
        authenticationCombo.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                int n = authenticationCombo.getSelectionIndex();
                if ( n >= 0 )
                {
                    authenticationType = authenticationOptions[n];
                    setDirty();
                }
            }
        } );
    }

    public void createUrlViewer()
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( Messages.realmManagementComposite_urlViewer_label );
        GridData labelData = new GridData( SWT.LEFT, SWT.TOP, false, false, 3, 1 );
        labelData.verticalIndent = INPUT_INDENT;
        label.setLayoutData( labelData );

        urlViewer = new TableViewer( this, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.SINGLE | SWT.V_SCROLL );
        GridData viewerData = new GridData( SWT.FILL, SWT.FILL, true, true, 2, 3 );
        viewerData.heightHint = 100;
        viewerData.widthHint = 400;
        urlViewer.getControl().setLayoutData( viewerData );

        TableViewerColumn urlColumn = new TableViewerColumn( urlViewer, SWT.NONE, URL_COLUMN );
        urlColumn.getColumn().setText( Messages.realmManagementComposite_urlViewer_urlColumn );
        urlColumn.getColumn().setWidth( 240 );
        urlColumn.setEditingSupport( new EditingSupport( urlViewer )
        {
            private CellEditor editor;

            @Override
            protected void setValue( Object element, Object value )
            {
                if ( element instanceof ISecurityRealmURLAssoc )
                {
                    ISecurityRealmURLAssoc assoc = (ISecurityRealmURLAssoc) element;
                    assoc.setUrl( String.valueOf( value ) );
                    toUpdate.add( assoc );
                    urlViewer.update( element, null );
                }
            }

            @Override
            protected Object getValue( Object element )
            {
                if ( element instanceof ISecurityRealmURLAssoc )
                {
                    return ( (ISecurityRealmURLAssoc) element ).getUrl();
                }
                return null;
            }

            @Override
            protected CellEditor getCellEditor( Object element )
            {
                if ( editor == null )
                {
                    editor = new TextCellEditor( urlViewer.getTable() );
                }
                return editor;
            }

            @Override
            protected boolean canEdit( Object element )
            {
                return true;
            }
        } );

        TableViewerColumn accessColumn = new TableViewerColumn( urlViewer, SWT.NONE, ACCESS_COLUMN );
        accessColumn.getColumn().setText( Messages.realmManagementComposite_urlViewer_accessColumn );
        accessColumn.getColumn().setWidth( 160 );
        accessColumn.setEditingSupport( new EditingSupport( urlViewer )
        {
            private CellEditor editor;

            @Override
            protected void setValue( Object element, Object value )
            {
                if ( element instanceof ISecurityRealmURLAssoc )
                {
                    ISecurityRealmURLAssoc assoc = (ISecurityRealmURLAssoc) element;
                    assoc.setAnonymousAccess( RealmComposite.ANONYMOUS_OPTIONS[(Integer) value] );
                    toUpdate.add( assoc );
                    urlViewer.update( element, null );
                }
            }

            @Override
            protected Object getValue( Object element )
            {
                if ( element instanceof ISecurityRealmURLAssoc )
                {
                    AnonymousAccessType anonymousAccessType = ( (ISecurityRealmURLAssoc) element ).getAnonymousAccess();
                    for ( int i = RealmComposite.ANONYMOUS_OPTIONS.length - 1; i >= 0; i-- )
                    {
                        if ( RealmComposite.ANONYMOUS_OPTIONS[i].equals( anonymousAccessType ) )
                        {
                            return i;
                        }
                    }
                }
                return null;
            }

            @Override
            protected CellEditor getCellEditor( Object element )
            {
                if ( editor == null )
                {
                    editor =
                        new ComboBoxCellEditor( urlViewer.getTable(), RealmComposite.ANONYMOUS_LABELS, SWT.READ_ONLY );
                }
                return editor;
            }

            @Override
            protected boolean canEdit( Object element )
            {
                return true;
            }
        } );

        urlViewer.getTable().setHeaderVisible( true );
        urlViewer.getTable().setLinesVisible( true );

        urlViewer.setContentProvider( new IStructuredContentProvider()
        {
            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            }

            public void dispose()
            {
            }

            @SuppressWarnings( "rawtypes" )
            public Object[] getElements( Object inputElement )
            {
                if ( inputElement instanceof Collection )
                {
                    return ( (Collection) inputElement ).toArray();
                }
                return null;
            }
        } );
        urlViewer.setLabelProvider( new UrlLabelProvider() );
        urlViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                updateRemoveButton();
            }
        } );

        addButton = new Button( this, getButtonStyle() );
        addButton.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        addButton.setText( Messages.realmManagementComposite_addUrl );
        addButton.setData( "name", "addButton" ); //$NON-NLS-1$ //$NON-NLS-2$
        addButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                ISecurityRealmURLAssoc assoc =
                    new SecurityRealmURLAssoc( null, EMPTY_URL, null, RealmComposite.ANONYMOUS_OPTIONS[0] );
                urlAssocs.add( 0, assoc );
                urlViewer.refresh();
                urlViewer.setSelection( new StructuredSelection( assoc ), true );
                urlViewer.editElement( assoc, URL_COLUMN );
            }
        } );

        removeButton = new Button( this, getButtonStyle() );
        removeButton.setLayoutData( new GridData( SWT.FILL, SWT.TOP, false, false ) );
        removeButton.setText( Messages.realmManagementComposite_removeUrl );
        removeButton.setData( "name", "removeButton" ); //$NON-NLS-1$ //$NON-NLS-2$
        removeButton.setEnabled( false );
        removeButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                IStructuredSelection selection = (IStructuredSelection) urlViewer.getSelection();
                if ( !selection.isEmpty() )
                {
                    ISecurityRealmURLAssoc assoc = (ISecurityRealmURLAssoc) selection.getFirstElement();
                    urlAssocs.remove( assoc );
                    if ( assoc.getId() != null )
                    {
                        toDelete.add( assoc );
                        toUpdate.remove( assoc );
                    }
                    urlViewer.refresh();
                }
            }
        } );
    }

    public void setControlsEnabled( boolean enabled )
    {
        idText.setEnabled( enabled );
        nameText.setEnabled( enabled );
        descriptionText.setEnabled( enabled );
        authenticationCombo.setEnabled( enabled );

        urlViewer.getControl().setEnabled( enabled );
        addButton.setEnabled( enabled );
        updateRemoveButton();
    }

    private void updateRemoveButton()
    {
        removeButton.setEnabled( !urlViewer.getSelection().isEmpty() );
    }

    public void setRealm( IAuthRealm realm )
    {
        setRealm( realm, false );
    }

    public void setRealm( IAuthRealm realm, boolean newRealm )
    {
        this.realm = realm;
        this.newRealm = newRealm;
        dirty = newRealm;
        updating = true;

        urlAssocs = new ArrayList<ISecurityRealmURLAssoc>();
        toDelete = new HashSet<ISecurityRealmURLAssoc>();
        toUpdate = new HashSet<ISecurityRealmURLAssoc>();

        if ( realm == null )
        {
            setControlsEnabled( false );
            id = ""; //$NON-NLS-1$
            name = ""; //$NON-NLS-1$
            description = ""; //$NON-NLS-1$
            authenticationType = authenticationOptions[0];

            idText.setText( "" ); //$NON-NLS-1$
            nameText.setText( "" ); //$NON-NLS-1$
            descriptionText.setText( "" ); //$NON-NLS-1$
            authenticationCombo.select( 0 );
        }
        else
        {
            setControlsEnabled( true );

            idText.setEnabled( newRealm );

            idText.setText( nvl( realm.getId() ) );
            nameText.setText( nvl( realm.getName() ) );
            descriptionText.setText( nvl( realm.getDescription() ) );

            authenticationType = realm.getAuthenticationType();
            int selection = 0;
            for ( int i = authenticationOptions.length - 1; i >= 0; i-- )
            {
                if ( authenticationOptions[i].equals( authenticationType ) )
                {
                    selection = i;
                    break;
                }
            }
            authenticationCombo.select( selection );

            Collection<ISecurityRealmURLAssoc> assocs = AuthFacade.getAuthRegistry().getURLToRealmAssocs();
            for ( ISecurityRealmURLAssoc assoc : assocs )
            {
                if ( id.equals( assoc.getRealmId() ) )
                {
                    urlAssocs.add( assoc );
                }
            }

            if ( newRealm )
            {
                idText.selectAll();
                idText.setFocus();
            }
        }
        urlViewer.setInput( urlAssocs );

        updating = false;
    }

    public void setDirty()
    {
        if ( !updating )
        {
            dirty = true;
        }
    }

    public boolean isDirty()
    {
        return dirty || !toDelete.isEmpty() || !toUpdate.isEmpty();
    }

    public IAuthRealm getRealm()
    {
        return realm;
    }

    public String save( IProgressMonitor monitor )
    {
        IAuthRegistry authRegistry = AuthFacade.getAuthRegistry();

        if ( dirty )
        {
            IAuthRealm authRealm = null;
            if ( !newRealm )
            {
                authRealm = authRegistry.getRealm( id );
            }

            if ( authRealm == null )
            {
                authRegistry.addRealm( id, name, description, authenticationType, monitor );
            }
            else
            {
                authRealm.setName( name );
                authRealm.setDescription( description );
                authRealm.setAuthenticationType( authenticationType );
                authRegistry.updateRealm( authRealm, monitor );
            }

            dirty = false;
        }

        if ( !toDelete.isEmpty() )
        {
            for ( ISecurityRealmURLAssoc assoc : toDelete )
            {
                authRegistry.removeURLToRealmAssoc( assoc.getId(), monitor );
            }
            toDelete.clear();
        }

        if ( !toUpdate.isEmpty() )
        {
            for ( ISecurityRealmURLAssoc assoc : toUpdate )
            {
                if ( assoc.getId() == null )
                {
                    authRegistry.addURLToRealmAssoc( assoc.getUrl(), id, assoc.getAnonymousAccess(), monitor );
                }
                else
                {
                    authRegistry.updateURLToRealmAssoc( assoc, monitor );
                }
            }
            toUpdate.clear();
        }
        return id;
    }

    private class UrlLabelProvider
        extends LabelProvider
        implements ITableLabelProvider
    {
        public Image getColumnImage( Object element, int columnIndex )
        {
            return null;
        }

        public String getColumnText( Object element, int columnIndex )
        {
            if ( element instanceof ISecurityRealmURLAssoc )
            {
                ISecurityRealmURLAssoc assoc = (ISecurityRealmURLAssoc) element;
                switch ( columnIndex )
                {
                    case URL_COLUMN:
                        return assoc.getUrl();
                    case ACCESS_COLUMN:
                        AnonymousAccessType anonymousAccessType = assoc.getAnonymousAccess();
                        for ( int i = RealmComposite.ANONYMOUS_OPTIONS.length - 1; i >= 0; i-- )
                        {
                            if ( RealmComposite.ANONYMOUS_OPTIONS[i].equals( anonymousAccessType ) )
                            {
                                return RealmComposite.ANONYMOUS_LABELS[i];
                            }
                        }
                }
            }
            return super.getText( element );
        }
    }
}
