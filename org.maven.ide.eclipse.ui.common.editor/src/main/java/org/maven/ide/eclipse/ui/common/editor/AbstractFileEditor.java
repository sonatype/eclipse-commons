package org.maven.ide.eclipse.ui.common.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.maven.ide.eclipse.ui.common.editor.internal.Activator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileEditor
    extends FormEditor
{
    private Logger log = LoggerFactory.getLogger( AbstractFileEditor.class );

    private List<IFormPage> pages;

    private ActivationListener activationListener;

    public AbstractFileEditor()
    {
        pages = new ArrayList<IFormPage>();
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    public void setPartName( String partName )
    {
        super.setPartName( partName );
    }

    protected void updatePages()
    {
        for ( IFormPage page : pages )
        {
            if ( page instanceof ValidatingFormPage )
            {
                ( (ValidatingFormPage) page ).updatePage();
            }
        }
    }

    public boolean hasErrors()
    {
        for ( IFormPage page : pages )
        {
            if ( page instanceof ValidatingFormPage )
            {
                if ( ( (ValidatingFormPage) page ).hasErrors() )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void clearDirty()
    {
        for ( IFormPage page : pages )
        {
            if ( page instanceof ValidatingFormPage )
            {
                ( (ValidatingFormPage) page ).setDirty( false );
            }
        }
        firePropertyChange( PROP_DIRTY );
    }

    @Override
    public int addPage( IFormPage page )
        throws PartInitException
    {
        page.addPropertyListener( new IPropertyListener()
        {
            public void propertyChanged( Object source, int propId )
            {
                if ( propId == PROP_DIRTY )
                {
                    firePropertyChange( PROP_DIRTY );
                }
            }
        } );

        pages.add( page );

        return super.addPage( page );
    }

    @Override
    public void removePage( int pageIndex )
    {
        if ( pages.size() > pageIndex )
        {
            pages.remove( pageIndex );
        }
        super.removePage( pageIndex );
    }

    public static void updateAllEditors( String id )
    {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        IEditorReference[] editors = workbenchPage.findEditors( null, id, IWorkbenchPage.MATCH_ID );

        if ( editors != null )
        {
            for ( IEditorReference editorReference : editors )
            {
                IEditorPart editorPart = editorReference.getEditor( true );
                if ( editorPart instanceof AbstractFileEditor )
                {
                    ( (AbstractFileEditor) editorPart ).updatePages();
                }
            }
        }
    }

    @Override
    public void init( IEditorSite site, IEditorInput input )
        throws PartInitException
    {
        super.init( site, input );

        if ( activationListener != null )
        {
            activationListener.dispose();
        }
        activationListener = new ActivationListener( site.getWorkbenchWindow().getPartService() );
    }
    
    @Override
    public void dispose()
    {
        if ( activationListener != null )
        {
            activationListener.dispose();
        }

        super.dispose();
    }

    class ActivationListener
        implements IPartListener, IWindowListener, IResourceChangeListener
    {
        private IWorkbenchPart activePart;

        private IPartService partService;

        private boolean busy;

        private long lastModified;

        private IDocumentProvider documentProvider;

        private IEditorInput input;

        private IResource resource;

        private ActivationListener( IPartService partService )
            throws PartInitException
        {
            this.partService = partService;
            partService.addPartListener( this );
            PlatformUI.getWorkbench().addWindowListener( this );

            try
            {
                input = getEditorInput();
                documentProvider = DocumentProviderRegistry.getDefault().getDocumentProvider( input );
                if ( documentProvider == null )
                {
                    log.error( "No document provider found for editor input", input );
                }
                else
                {
                    documentProvider.connect( input );
                }

                if ( input instanceof IFileEditorInput )
                {
                    resource = ( (IFileEditorInput) input ).getFile();
                    ResourcesPlugin.getWorkspace().addResourceChangeListener( this );
                }

                updateLastModified();
            }
            catch ( CoreException e )
            {
                throw new PartInitException( NLS.bind( "Error opening editor: {0}", e.getMessage() ), e );
            }
        }

        private void dispose()
        {
            partService.removePartListener( this );
            PlatformUI.getWorkbench().removeWindowListener( this );

            if ( documentProvider != null )
            {
                documentProvider.disconnect( input );
            }

            if ( resource != null )
            {
                ResourcesPlugin.getWorkspace().removeResourceChangeListener( this );
            }
        }

        private void doActivate()
        {
            if ( busy )
            {
                return;
            }
            if ( activePart == AbstractFileEditor.this )
            {
                busy = true;

                long l = lastModified;
                updateLastModified();
                if ( l != lastModified )
                {
                    try
                    {
                        boolean deleted =
                            ( documentProvider != null && documentProvider.isDeleted( input ) )
                                || ( resource != null && !resource.exists() );
                        if ( deleted )
                        {
                            // if the file is deleted and there are unsaved changes, ask to keep the editor open
                            askAndClose();
                        }
                        else
                        {
                            // if the file is changed, reload automatically; if dirty, then ask first
                            if ( !isDirty()
                                || MessageDialog.openQuestion( getSite().getShell(),
                                                               "File Changed",
                                                               NLS.bind( "The file \"{0}\" has been changed on the file system. Do you want to replace the editor contents with these changes?",
                                                                         input.getToolTipText() ) ) )
                            {
                                init( getEditorSite(), input );
                                clearDirty();
                            }
                        }
                    }
                    catch ( PartInitException e )
                    {
                        StatusManager.getManager().handle( e, Activator.PLUGIN_ID );
                    }
                }

                busy = false;
            }
        }

        private void askAndClose()
        {
            if ( !( isDirty() && MessageDialog.openQuestion( getSite().getShell(),
                                                             "File Deleted",
                                                             NLS.bind( "The file \"{0}\" has been deleted on the file system. Do you want to keep the editor open (answering \"No\" will discard your changes)?",
                                                                       input.getToolTipText() ) ) ) )
            {
                close( false );
            }
            else
            {
                updateLastModified();
            }
        }

        private void updateLastModified()
        {
            if ( documentProvider != null )
            {
                lastModified = documentProvider.getModificationStamp( input );
            }
            else if ( resource != null )
            {
                lastModified = resource.getModificationStamp();
            }
        }

        public void partOpened( IWorkbenchPart part )
        {
        }

        public void partDeactivated( IWorkbenchPart part )
        {
            activePart = null;
        }

        public void partClosed( IWorkbenchPart part )
        {
        }

        public void partBroughtToTop( IWorkbenchPart part )
        {
        }

        public void partActivated( IWorkbenchPart part )
        {
            activePart = part;
            doActivate();
        }

        public void windowActivated( IWorkbenchWindow window )
        {
            if ( window == getEditorSite().getWorkbenchWindow() )
            {
                window.getShell().getDisplay().asyncExec( new Runnable()
                {
                    public void run()
                    {
                        doActivate();
                    }
                } );
            }
        }

        public void windowClosed( IWorkbenchWindow window )
        {
        }

        public void windowDeactivated( IWorkbenchWindow window )
        {
        }

        public void windowOpened( IWorkbenchWindow window )
        {
        }

        public void resourceChanged( IResourceChangeEvent event )
        {
            try
            {
                IResourceDelta delta = event.getDelta();
                if ( delta != null )
                {
                    delta.accept( new IResourceDeltaVisitor()
                    {
                        public boolean visit( IResourceDelta delta )
                            throws CoreException
                        {
                            if ( ( delta.getKind() & IResourceDelta.REMOVED ) != 0
                                && resource.equals( delta.getResource() ) )
                            {
                                getSite().getShell().getDisplay().asyncExec( new Runnable()
                                {
                                    public void run()
                                    {
                                        askAndClose();
                                    }
                                } );
                                return false;
                            }
                            return true;
                        }
                    } );
                }
            }
            catch ( CoreException e )
            {
                log.error( e.getMessage(), e );
            }
        }

        private void setBusy( boolean b )
        {
            busy = b;
        }
    }

    public void setBusy( boolean b )
    {
        if ( activationListener != null )
        {
            activationListener.setBusy( b );
            if ( !b )
            {
                activationListener.updateLastModified();
            }
        }
    }
}
