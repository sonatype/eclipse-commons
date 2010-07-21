package org.maven.ide.eclipse.ui.common.composites;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.ui.common.Messages;

public class ListEditorComposite<T>
    extends Composite
{
    public static final int ADD = 1;

    public static final int EDIT = 2;

    public static final int REMOVE = 4;

    public static final int FIND = 8;

    public static final int FORM = 16;

    private TableViewer viewer;

    private Button addButton;

    private Button editButton;

    private Button removeButton;

    private Button selectButton;

    private boolean readOnly = false;

    public ListEditorComposite( Composite parent, int style, boolean includeSearch )
    {
        this( parent, style, ADD | REMOVE | FORM | ( includeSearch ? FIND : 0 ) );
    }

    public ListEditorComposite( Composite parent, int style, int editorStyle )
    {
        super( parent, style );

        GridLayout gridLayout = new GridLayout( 2, false );
        gridLayout.marginWidth = 1;
        gridLayout.marginHeight = 1;
        gridLayout.verticalSpacing = 1;
        setLayout( gridLayout );

        boolean formMode = ( editorStyle & FORM ) == FORM;
        int buttonStyle = formMode ? SWT.FLAT : SWT.PUSH;

        final Table table = new Table( this, ( formMode ? SWT.FLAT : 0 ) | SWT.MULTI | SWT.BORDER | style );
        table.setData( "name", "list-editor-composite-table" );
        final TableColumn column = new TableColumn( table, SWT.NONE );
        table.addControlListener( new ControlAdapter()
        {
            public void controlResized( ControlEvent e )
            {
                column.setWidth( table.getClientArea().width );
            }
        } );

        viewer = new TableViewer( table );

        boolean includeSearch = ( editorStyle & FIND ) == FIND;
        GridData viewerData = new GridData( SWT.FILL, SWT.FILL, true, true, 1, 0 );
        viewerData.widthHint = 100;
        viewerData.heightHint = includeSearch ? 125 : 50;
        viewerData.minimumHeight = includeSearch ? 125 : 50;
        table.setLayoutData( viewerData );
        viewer.setData( FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE );

        if ( includeSearch )
        {
            selectButton = new Button( this, buttonStyle );
            selectButton.setText( Messages.listEditorComposite_find );
            GridData gd = new GridData( SWT.FILL, SWT.TOP, false, false );
            gd.verticalIndent = 0;
            selectButton.setLayoutData( gd );
            selectButton.setEnabled( false );
            viewerData.verticalSpan++;
        }

        if ( ( editorStyle & ADD ) == ADD )
        {
            addButton = new Button( this, buttonStyle );
            addButton.setText( Messages.listEditorComposite_add );
            GridData gd = new GridData( SWT.FILL, SWT.TOP, false, false );
            gd.verticalIndent = 0;
            addButton.setLayoutData( gd );
            addButton.setEnabled( false );
            viewerData.verticalSpan++;
        }

        if ( ( editorStyle & EDIT ) == EDIT )
        {
            editButton = new Button( this, buttonStyle );
            editButton.setText( Messages.listEditorComposite_find );
            GridData gd = new GridData( SWT.FILL, SWT.TOP, false, false );
            gd.verticalIndent = 0;
            editButton.setLayoutData( gd );
            editButton.setEnabled( false );
            viewerData.verticalSpan++;
        }

        if ( ( editorStyle & REMOVE ) == REMOVE )
        {
            removeButton = new Button( this, buttonStyle );
            removeButton.setText( Messages.listEditorComposite_remove );
            GridData gd = new GridData( SWT.FILL, SWT.TOP, false, false );
            gd.verticalIndent = 0;
            removeButton.setLayoutData( gd );
            removeButton.setEnabled( false );
            viewerData.verticalSpan++;
        }

        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                updateButtons();
            }
        } );
    }

    public ListEditorComposite( Composite parent, int style )
    {
        this( parent, style, false );
    }

    public void setLabelProvider( ILabelProvider labelProvider )
    {
        viewer.setLabelProvider( labelProvider );
    }

    public void setContentProvider( ListEditorContentProvider<T> contentProvider )
    {
        viewer.setContentProvider( contentProvider );
    }

    public void setInput( List<T> input )
    {
        viewer.setInput( input );
        viewer.setSelection( new StructuredSelection() );
    }

    public void setOpenListener( IOpenListener listener )
    {
        viewer.addOpenListener( listener );
    }

    public void addSelectionListener( ISelectionChangedListener listener )
    {
        viewer.addSelectionChangedListener( listener );
    }

    public void setSelectListener( SelectionListener listener )
    {
        if ( selectButton != null )
        {
            selectButton.addSelectionListener( listener );
            selectButton.setEnabled( true );
        }
    }

    public void setAddListener( SelectionListener listener )
    {
        if ( addButton != null )
        {
            addButton.addSelectionListener( listener );
            addButton.setEnabled( true );
        }
    }

    public void setEditListener( SelectionListener listener )
    {
        if ( editButton != null )
        {
            editButton.addSelectionListener( listener );
        }
    }

    public void setRemoveListener( SelectionListener listener )
    {
        if ( removeButton != null )
        {
            removeButton.addSelectionListener( listener );
        }
    }

    public TableViewer getViewer()
    {
        return viewer;
    }

    public int getSelectionIndex()
    {
        return viewer.getTable().getSelectionIndex();
    }

    public void setSelectionIndex( int n )
    {
        viewer.getTable().setSelection( n );
    }

    @SuppressWarnings( "unchecked" )
    public List<T> getSelection()
    {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        return selection == null ? Collections.emptyList() : selection.toList();
    }

    public void setSelection( List<T> selection )
    {
        viewer.setSelection( new StructuredSelection( selection ), true );
    }

    public void setReadOnly( boolean readOnly )
    {
        this.readOnly = readOnly;
        if ( addButton != null )
        {
            addButton.setEnabled( !readOnly );
        }
        if ( selectButton != null )
        {
            selectButton.setEnabled( !readOnly );
        }
        updateButtons();
    }

    public void refresh()
    {
        if ( !viewer.getTable().isDisposed() )
        {
            viewer.refresh( true );
        }
    }

    public void setCellModifier( ICellModifier cellModifier )
    {
        viewer.setColumnProperties( new String[] { "?" } );

        TextCellEditor editor = new TextCellEditor( viewer.getTable() );
        viewer.setCellEditors( new CellEditor[] { editor } );
        viewer.setCellModifier( cellModifier );
    }

    void updateButtons()
    {
        boolean b = !readOnly && !viewer.getSelection().isEmpty();
        if ( editButton != null )
        {
            editButton.setEnabled( b );
        }
        if ( removeButton != null )
        {
            removeButton.setEnabled( b );
        }
    }

    public void setDoubleClickListener( IDoubleClickListener listener )
    {
        viewer.addDoubleClickListener( listener );
    }
}
