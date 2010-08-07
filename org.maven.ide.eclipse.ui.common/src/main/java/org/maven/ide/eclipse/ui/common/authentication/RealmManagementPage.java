package org.maven.ide.eclipse.ui.common.authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
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
import org.maven.ide.eclipse.authentication.IAuthRealm;
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

    private TableViewer realmViewer;

    private RealmManagementComposite realmManagementComposite;

    private SwtValidationGroup validationGroup;

    private Collection<IAuthRealm> realms;

    public RealmManagementPage()
    {
        super( RealmManagementPage.class.getName() );
        setTitle( Messages.realmManagementPage_title );
        setDescription( Messages.realmManagementPage_description );

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
        sash.setWeights( new int[] { 1, 3 } );

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
                realmViewer.setSelection( StructuredSelection.EMPTY );
                realmManagementComposite.setControlsEnabled( true );
                realmManagementComposite.setRealm( null );
            }
        };
        addRealmAction.setToolTipText( Messages.realmManagementPage_add_tooltip );
        addRealmAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                                                                                                           ISharedImages.IMG_OBJ_ADD ) );
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
                    if ( MessageDialog.openConfirm(
                                                    getShell(),
                                                    Messages.realmManagementPage_remove_title,
                                                    NLS.bind(
                                                              Messages.realmManagementPage_remove_message,
                                                              authRealm.getName() ) ) )
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
                            updateSelection( null );
                        }
                        else
                        {
                            String message = NLS.bind( Messages.realmManagementPage_remove_error, t.getMessage() );
                            setMessage( message, IMessageProvider.ERROR );
                            StatusManager.getManager().handle(
                                                               new Status( IStatus.ERROR, Activator.PLUGIN_ID, message,
                                                                           t ) );
                        }
                    }
                }
            }
        };
        removeRealmAction.setToolTipText( Messages.realmManagementPage_remove_tooltip );
        removeRealmAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                                                                                                              ISharedImages.IMG_ELCL_REMOVE ) );
        addToolbarAction( toolBarManager, removeRealmAction );

        // toolBarManager.update( true );

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

            @SuppressWarnings( "unchecked" )
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
                    realmManagementComposite.setControlsEnabled( false );
                    realmManagementComposite.setRealm( null );
                    removeRealmAction.setEnabled( false );
                }
                else
                {
                    realmManagementComposite.setRealm( (IAuthRealm) selection.getFirstElement() );
                    removeRealmAction.setEnabled( true );
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
        realms = AuthFacade.getAuthRegistry().getRealms();
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
            realmViewer.setSelection( new StructuredSelection( selectedRealm ), true );
        }
    }

    public void save( IProgressMonitor monitor )
    {
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
}
