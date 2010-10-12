package org.maven.ide.eclipse.ui.common.wizards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.ui.common.Images;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;

abstract public class RemoteArtifactLookupPage<T>
    extends RemoteResourceLookupPage
{
    private TreeViewer treeViewer;

    private Version selection;

    public RemoteArtifactLookupPage( String serverUrl )
    {
        super( serverUrl );
    }

    @SuppressWarnings( "unchecked" )
    protected Composite createResourcePanel( Composite parent )
    {
        treeViewer =
            new TreeViewer( parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE );
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true );
        gd.heightHint = 200;
        gd.widthHint = 400;
        treeViewer.getTree().setLayoutData( gd );

        treeViewer.setComparator( new ViewerComparator() );
        treeViewer.setContentProvider( new ITreeContentProvider()
        {
            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            	if (newInput == null) {
                	//just disable finish when no tree content, no error message
            		getRootValidationGroup().remove(getFinishButtonValidationGroup());
            		setPageComplete(false);
            	} else {
            		getRootValidationGroup().addItem(getFinishButtonValidationGroup(), false);
            		setPageComplete(true);
            		getFinishButtonValidationGroup().performValidation();
            	}
            }

            public void dispose()
            {
            }

            public Object[] getElements( Object inputElement )
            {
                if ( inputElement instanceof Collection )
                {
                    return ( (Collection) inputElement ).toArray();
                }
                return null;
            }

            public boolean hasChildren( Object element )
            {
                return element instanceof RemoteArtifactLookupPage<?>.Group
                    || element instanceof RemoteArtifactLookupPage<?>.Artifact;
            }

            public Object getParent( Object element )
            {
                return null;
            }

            public Object[] getChildren( Object parentElement )
            {
                if ( parentElement instanceof RemoteArtifactLookupPage<?>.Group )
                {
                    return ( (Group) parentElement ).getArtifacts();
                }
                else if ( parentElement instanceof RemoteArtifactLookupPage<?>.Artifact )
                {
                    return ( (Artifact) parentElement ).getVersions();
                }
                return null;
            }
        } );
        treeViewer.setLabelProvider( new LabelProvider()
        {
            @Override
            public Image getImage( Object element )
            {
                if ( element instanceof RemoteArtifactLookupPage<?>.Group )
                {
                    return Images.GROUP;
                }
                else if ( element instanceof RemoteArtifactLookupPage<?>.Artifact )
                {
                    return Images.ARTIFACT;
                }
                else if ( element instanceof RemoteArtifactLookupPage<?>.Version )
                {
                    return Images.VERSION;
                }
                return super.getImage( element );
            }
        } );
        treeViewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                if ( !selection.isEmpty() )
                {
                    Object o = selection.getFirstElement();
                    if ( o instanceof RemoteArtifactLookupPage<?>.Group
                        || o instanceof RemoteArtifactLookupPage<?>.Artifact )
                    {
                        if ( treeViewer.getExpandedState( o ) )
                        {
                            treeViewer.collapseToLevel( o, -1 );
                        }
                        else
                        {
                            treeViewer.expandToLevel( o, 1 );
                        }
                    }
                    else if ( o instanceof RemoteArtifactLookupPage<?>.Version )
                    {
                        getContainer().showPage( getNextPage() );
                    }
                }
            }
        } );
        treeViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                selection = null;

                IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                if ( !selection.isEmpty() )
                {
                    Object o = selection.getFirstElement();
                    if ( o instanceof RemoteArtifactLookupPage<?>.Version )
                    {
                        saveSelection( (Version) o );
                    }
                }
            }
        } );

        getFinishButtonValidationGroup().add( treeViewer, new Validator<ISelection>()
        {
            public Class<ISelection> modelType()
            {
                return ISelection.class;
            }

            public void validate( Problems problems, String name, ISelection selection )
            {
                IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                if ( selection.isEmpty()
                    || !( structuredSelection.getFirstElement() instanceof RemoteArtifactLookupPage<?>.Version ) )
                {
                    problems.add( getDescription(), Severity.FATAL );
                }
            }
        } );

        return treeViewer.getTree();
    }

    protected void saveSelection( Version version )
    {
        selection = version;
    }

    protected Version getSelection()
    {
        return selection;
    }

    @Override
    protected void setInput( Object input )
    {
        treeViewer.setInput( input );
    }
    
    protected TreeViewer getTreeViewer() {
    	return treeViewer;
    }

    abstract protected String getGroupId( T entry );

    abstract protected String getArtifactId( T entry );

    abstract protected String getVersion( T entry );

    protected Collection<Group> createGroups( Collection<T> entries )
    {
        Map<String, Group> groups = new HashMap<String, Group>();
        if ( entries != null )
        {
            for ( T entry : entries )
            {
                String groupId = getGroupId( entry );
                Group group = groups.get( groupId );
                if ( group == null )
                {
                    group = new Group( groupId );
                    groups.put( groupId, group );
                }
                group.addArtifact( entry );
            }
        }
        return groups.values();
    }

    protected class Group
    {
        private String groupId;

        private Map<String, Artifact> artifacts;

        private Artifact[] artifactArray;

        private Group( String groupId )
        {
            this.groupId = groupId;
            artifacts = new HashMap<String, Artifact>();
        }

        private void addArtifact( T entry )
        {
            String artifactId = getArtifactId( entry );
            Artifact artifact = artifacts.get( artifactId );
            if ( artifact == null )
            {
                artifact = new Artifact( artifactId );
                artifacts.put( artifactId, artifact );
            }
            artifact.addVersion( entry );
        }

        public Artifact[] getArtifacts()
        {
            if ( artifactArray == null )
            {
                artifactArray = artifacts.values().toArray(new RemoteArtifactLookupPage.Artifact[0]);
            }
            return artifactArray;
        }

        @Override
        public String toString()
        {
            return groupId;
        }
    }

    protected class Artifact
    {
        private String artifactId;

        private List<Version> versions;

        private Version[] versionArray;

        private Artifact( String artifactId )
        {
            this.artifactId = artifactId;
            versions = new ArrayList<Version>();
        }

        private void addVersion( T entry )
        {
            versions.add( new Version( entry ) );
        }

        public Version[] getVersions()
        {
            if ( versionArray == null )
            {
                versionArray = versions.toArray(new RemoteArtifactLookupPage.Version[0]);
            }
            return versionArray;
        }

        @Override
        public String toString()
        {
            return artifactId;
        }
    }

    protected class Version
    {
        private String groupId;

        private String artifactId;

        private String version;

        private T entry;

        private Version( T entry )
        {
            this.groupId = RemoteArtifactLookupPage.this.getGroupId( entry );
            this.artifactId = RemoteArtifactLookupPage.this.getArtifactId( entry );
            this.version = RemoteArtifactLookupPage.this.getVersion( entry );
            this.entry = entry;
        }

        @Override
        public String toString()
        {
            return version;
        }

        public String getGroupId()
        {
            return groupId;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public String getVersion()
        {
            return version;
        }

        public T getEntry()
        {
            return entry;
        }
    }
}
