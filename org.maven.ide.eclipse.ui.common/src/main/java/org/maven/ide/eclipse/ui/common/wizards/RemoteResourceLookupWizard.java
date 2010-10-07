package org.maven.ide.eclipse.ui.common.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;

//mkleint: this class is a case of premature abstraction.
//it doesn't appear to be used anywhere and there is actually no way to use this class without getting a NPE.
//candidate for deletion I suppose.
abstract public class RemoteResourceLookupWizard
    extends Wizard
{
    private RemoteResourceLookupPage page;

    private String serverUrl;

    public RemoteResourceLookupWizard( String serverUrl )
    {
        this.serverUrl = serverUrl;
    }

    @Override
    public boolean performFinish()
    {
        return true;
    }

    @Override
    public void addPages()
    {
        page = new RemoteResourceLookupPage( serverUrl )
        {

            @Override
            protected void setInput( Object input )
            {
                RemoteResourceLookupWizard.this.setInput( input );
            }

            @Override
            protected Object loadResources( String url, IProgressMonitor monitor )
                throws Exception
            {
                return RemoteResourceLookupWizard.this.loadResources( url, monitor );
            }

            @Override
            protected Composite createResourcePanel( Composite parent )
            {
                return RemoteResourceLookupWizard.this.createResourcePanel( parent );
            }
        };
        setWindowTitle( page.getTitle() );
        addPage( page );
    }

    abstract protected Composite createResourcePanel( Composite parent );

    abstract protected void setInput( Object input );

    abstract protected Object loadResources( String url, IProgressMonitor monitor )
        throws Exception;
}
