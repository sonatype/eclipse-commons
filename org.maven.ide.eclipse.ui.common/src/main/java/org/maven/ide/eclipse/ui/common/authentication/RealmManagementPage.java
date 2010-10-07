package org.maven.ide.eclipse.ui.common.authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.internal.AuthRealm;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.maven.ide.eclipse.ui.common.Activator;
import org.maven.ide.eclipse.ui.common.Images;
import org.maven.ide.eclipse.ui.common.Messages;

public class RealmManagementPage
    extends WizardPage
{
    private IAction addRealmAction;

    private IAction removeRealmAction;

    private IAction reloadRegistryAction;

    private TableViewer realmViewer;

    private RealmManagementComposite realmManagementComposite;

    private SwtValidationGroup validationGroup;

    private Collection<IAuthRealm> realms;

    // private Set<IAuthRealm> toRemove;

    public RealmManagementPage()
    {
        super( RealmManagementPage.class.getName() );
        setTitle( Messages.realmManagementPage_title );
        setDescription( Messages.realmManagementPage_description );

        // toRemove = new HashSet<IAuthRealm>();
        validationGroup = SwtValidationGroup.create( SwtValidationUI.createUI( this ) );
    }

    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout() );

        SashForm sash = new SashForm( composite, SWT.SMOOTH );
        sash.setOrientation( SWT.HORIZONTAL );
        sash.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        createRealmList( sash );
        createRealmDetails( sash );
        sash.setWeights( new int[] { 2, 5 } );

        setControl( composite );

        loadRealms();
        updateSelection( null );
    }

    private void createRealmList( Composite parent )
    {
        ViewForm viewForm = new ViewForm( parent, SWT.FLAT | SWT.BORDER );

        createRealmToolbar( viewForm );
        createRealmViewer( viewForm );
    }

    private void createRealmToolbar( ViewForm viewForm )
    {
        ToolBarManager toolBarManager = new ToolBarManager( SWT.FLAT | SWT.RIGHT );

        addRealmAction = new Action( Messages.realmManagementPage_add_action )
        {
            @Override
            public void run()
            {
                if ( checkUnsavedChanges() )
                {
                    return;
                }

                realmManagementComposite.setRealm( null );
                for ( int i = 1; i > 0; i++ )
                {
                    String realmId = "realm" + i; //$NON-NLS-1$
                    boolean found = false;
                    for ( IAuthRealm realm : realms )
                    {
                        if ( realmId.equals( realm.getId() ) )
                        {
                            found = true;
                            break;
                        }
                    }
                    if ( !found )
                    {
                        String name = NLS.bind( Messages.realmManagementPage_newRealmNameTemplate, i );
                        IAuthRealm newRealm = new NewAuthRealm( realmId, NLS.bind( "<{0}>", name ) ); //$NON-NLS-1$
                        realms.add( newRealm );
                        realmViewer.refresh();
                        realmViewer.setSelection( new StructuredSelection( newRealm ) );
                        newRealm.setName( name );
                        realmManagementComposite.setRealm( newRealm, true );
                        return;
                    }
                }
            }
        };
        addRealmAction.setToolTipText( Messages.realmManagementPage_add_tooltip );
        addRealmAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor( ISharedImages.IMG_OBJ_ADD ) );
        addToolbarAction( toolBarManager, addRealmAction );

        removeRealmAction = new Action( Messages.realmManagementPage_remove_action )
        {
            @Override
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) realmViewer.getSelection();
                if ( !selection.isEmpty() )
                {
                    final IAuthRealm authRealm = (IAuthRealm) selection.getFirstElement();
                    boolean newUnsaved = authRealm instanceof NewAuthRealm;
                    if ( newUnsaved
                        || MessageDialog.openConfirm( getShell(),
                                                      Messages.realmManagementPage_remove_title,
                                                      NLS.bind( Messages.realmManagementPage_remove_message,
                                                                authRealm.getName() ) ) )
                    {
                        if ( newUnsaved )
                        {
                            realms.remove( authRealm );
                        }
                        else
                        {
                            Throwable t = null;
                            try
                            {
                                getContainer().run( true, true, new IRunnableWithProgress()
                                {
                                    public void run( IProgressMonitor monitor )
                                        throws InvocationTargetException, InterruptedException
                                    {
                                        AuthFacade.getAuthRegistry().removeRealm( authRealm.getId(), monitor );
                                    }
                                } );
                            }
                            catch ( InvocationTargetException e )
                            {
                                t = e.getTargetException();
                            }
                            catch ( InterruptedException e )
                            {
                                t = e;
                            }

                            if ( t == null )
                            {
                                loadRealms();
                            }
                            else
                            {
                                String message = NLS.bind( Messages.realmManagementPage_remove_error, t.getMessage() );
                                setMessage( message, IMessageProvider.ERROR );
                                StatusManager.getManager().handle( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                                               message, t ) );
                            }
                            // toRemove.add( authRealm );
                        }

                        realmViewer.setSelection( StructuredSelection.EMPTY );
                        realmViewer.refresh();
                        updateSelection( null );
                    }
                }
            }
        };
        removeRealmAction.setToolTipText( Messages.realmManagementPage_remove_tooltip );
        removeRealmAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor( ISharedImages.IMG_ELCL_REMOVE ) );
        addToolbarAction( toolBarManager, removeRealmAction );

        toolBarManager.add( new Separator() );

        reloadRegistryAction = new Action()
        {
            @Override
            public void run()
            {
                if ( checkUnsavedChanges() )
                {
                    return;
                }

                Throwable t = null;
                try
                {
                    getContainer().run( true, true, new IRunnableWithProgress()
                    {
                        public void run( IProgressMonitor monitor )
                            throws InvocationTargetException, InterruptedException
                        {
                            AuthFacade.getAuthRegistry().reload( monitor );
                            monitor.done();
                        }
                    } );
                }
                catch ( InvocationTargetException e )
                {
                    t = e.getTargetException();
                }
                catch ( InterruptedException e )
                {
                    t = e;
                }

                if ( t == null )
                {
                    loadRealms();
                    updateSelection( null );
                }
                else
                {
                    StatusManager.getManager().handle( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                                   NLS.bind( Messages.realmManagementPage_reload_error,
                                                                             t.getMessage() ), t ) );
                }
            }
        };
        reloadRegistryAction.setToolTipText( Messages.realmManagementPage_reload_tooltip );
        reloadRegistryAction.setImageDescriptor( Images.REFRESH_DESCRIPTOR );
        toolBarManager.add( reloadRegistryAction );

        ToolBar toolBar = toolBarManager.createControl( viewForm );
        toolBar.setBackground( viewForm.getParent().getBackground() );
        viewForm.setTopLeft( toolBar );
    }

    private void addToolbarAction( ToolBarManager toolBarManager, IAction action )
    {
        ActionContributionItem item = new ActionContributionItem( action );
        if ( action.getText() != null && action.getText().length() > 0 && action.getImageDescriptor() != null )
        {
            item.setMode( ActionContributionItem.MODE_FORCE_TEXT );
        }
        toolBarManager.add( item );
    }

    private void createRealmViewer( ViewForm viewForm )
    {
        realmViewer = new TableViewer( viewForm, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.SINGLE | SWT.V_SCROLL );
        realmViewer.setContentProvider( new IStructuredContentProvider()
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
        realmViewer.setLabelProvider( new LabelProvider()
        {
            @Override
            public Image getImage( Object element )
            {
                return Images.AUTH_REALM;
            }

            @Override
            public String getText( Object element )
            {
                if ( element instanceof IAuthRealm )
                {
                    return ( (IAuthRealm) element ).getName();
                }
                return super.getText( element );
            };
        } );
        realmViewer.setComparator( new ViewerComparator() );
        realmViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                IStructuredSelection selection = (IStructuredSelection) realmViewer.getSelection();
                if ( selection.isEmpty() )
                {
                    realmManagementComposite.setRealm( null );
                    removeRealmAction.setEnabled( false );
                }
                else
                {
                    IAuthRealm realm = (IAuthRealm) selection.getFirstElement();
                    IAuthRealm previousRealm = realmManagementComposite.getRealm();
                    if ( realm != previousRealm )
                    {
                        if ( checkUnsavedChanges() )
                        {
                            realmViewer.setSelection( new StructuredSelection( previousRealm ) );
                            return;
                        }
                        if ( previousRealm instanceof NewAuthRealm )
                        {
                            realms.remove( previousRealm );
                            realmViewer.refresh();
                        }
                        realmManagementComposite.setRealm( realm );
                        removeRealmAction.setEnabled( true );
                        addRealmAction.setEnabled( !( realm instanceof NewAuthRealm ) );
                    }
                }
            }
        } );

        viewForm.setContent( realmViewer.getControl() );
    }

    private void createRealmDetails( Composite parent )
    {
        realmManagementComposite = new RealmManagementComposite( parent, null, validationGroup );
    }

    private void loadRealms()
    {
        realms = new ArrayList<IAuthRealm>( AuthFacade.getAuthRegistry().getRealms() );
        realmViewer.setInput( realms );
    }

    public void refreshSelection()
    {
        realmViewer.setSelection( realmViewer.getSelection() );
    }

    private void updateSelection( String realmId )
    {
        if ( realms.isEmpty() )
        {
            realmManagementComposite.setControlsEnabled( false );
            validationGroup.performValidation();
        }
        else
        {
            Object selectedRealm = null;
            if ( realmId != null )
            {
                selectedRealm = AuthFacade.getAuthRegistry().getRealm( realmId );
            }
            if ( selectedRealm == null )
            {
                selectedRealm = realmViewer.getTable().getItem( 0 ).getData();
            }
            realmViewer.setSelection( selectedRealm == null ? StructuredSelection.EMPTY
                            : new StructuredSelection( selectedRealm ), true );
        }
    }

    public void save( IProgressMonitor monitor )
    {
        // if ( !toRemove.isEmpty() )
        // {
        // IAuthRegistry authRegistry = AuthFacade.getAuthRegistry();
        // for ( IAuthRealm realm : toRemove )
        // {
        // authRegistry.removeRealm( realm.getId(), monitor );
        // }
        // toRemove.clear();
        // }
        if ( realmManagementComposite.isDirty() )
        {
            final String realmId = realmManagementComposite.save( monitor );
            Display.getDefault().asyncExec( new Runnable()
            {
                public void run()
                {
                    loadRealms();
                    updateSelection( realmId );
                }
            } );
        }
    }

    private boolean checkUnsavedChanges()
    {
        if ( realmManagementComposite.isDirty() )
        {
            MessageDialog md =
                new MessageDialog( getShell(), Messages.realmManagementPage_realmChanged_title, null,
                                   NLS.bind( Messages.realmManagementPage_realmChanged_message,
                                             realmManagementComposite.getRealm().getName() ), WARNING, new String[] {
                                       Messages.realmManagementPage_realmChanged_discard,
                                       Messages.realmManagementPage_realmChanged_cancel }, 1 );
            if ( md.open() == 1 )
            {
                return true;
            }
        }
        return false;
    }

    protected class NewAuthRealm
        extends AuthRealm
    {
        public NewAuthRealm( String id, String name )
        {
            super( id, name, "", AuthenticationType.USERNAME_PASSWORD ); //$NON-NLS-1$
        }
    };
}
