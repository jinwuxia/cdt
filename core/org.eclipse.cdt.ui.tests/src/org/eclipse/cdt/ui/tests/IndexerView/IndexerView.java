/**********************************************************************
 * Copyright (c) 2005 IBM Canada and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM Rational Software - Initial API and implementation 
 **********************************************************************/
package org.eclipse.cdt.ui.tests.IndexerView;

import java.io.IOException;

import org.eclipse.cdt.core.index.ICDTIndexer;
import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.core.search.ICSearchConstants;
import org.eclipse.cdt.internal.core.index.IEntryResult;
import org.eclipse.cdt.internal.core.index.IIndex;
import org.eclipse.cdt.internal.core.index.IIndexer;
import org.eclipse.cdt.internal.core.index.sourceindexer.AbstractIndexer;
import org.eclipse.cdt.ui.testplugin.CTestPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.PropertySheet;

/**
 * @author dsteffle
 */
public class IndexerView extends ViewPart {
    private static final String INDEXER_VIEW___ = "Indexer View - "; //$NON-NLS-1$
    private static final String _INDEXER_MENU_MANAGER = "#Indexer_Menu_Manager"; //$NON-NLS-1$
    private static final String SET_FILTERS = "Set Filters"; //$NON-NLS-1$
    private static final String NEXT_PAGE = "Next Page"; //$NON-NLS-1$
    private static final String PREVIOUS_PAGE = "Previous Page"; //$NON-NLS-1$
    public static final String VIEW_ID = "org.eclipse.cdt.ui.tests.IndexerView"; //$NON-NLS-1$
    private static final String PROPERTIES_VIEW = "org.eclipse.ui.views.PropertySheet"; //$NON-NLS-1$
    protected static final String BLANK_STRING = ""; //$NON-NLS-1$
    static TableViewer viewer;
    protected Action previousPageAction;
    protected Action nextPageAction;
    protected Action singleClickAction;
    protected Action setFiltersAction;
    protected IIndexer[] indexers = new IIndexer[CTestPlugin.getWorkspace().getRoot().getProjects().length];
    protected IProject project = null;
        
    protected static ViewContentProvider.StartInitializingIndexerView initializeIndexerViewJob = null;

    public class ViewContentProvider implements IStructuredContentProvider,
            ITreeContentProvider {

        private static final String POPULATING_INDEXER_VIEW = "populating indexer view"; //$NON-NLS-1$
        protected IndexerNodeParent invisibleRoot;
        
        protected boolean displayForwards=false;
        protected boolean displayBackwards=false;
        
        private class InitializeView extends Job {
            private static final String ALL_NAME_SEARCH = "*"; //$NON-NLS-1$
            private static final String INDEXER_VIEW = "Indexer View"; //$NON-NLS-1$
            TableViewer theViewer = null;
            
            public InitializeView(String name, TableViewer viewer) {
                super(name);
                this.theViewer = viewer;
            }

            protected IStatus run(IProgressMonitor monitor) {
                
                for(int i=0; i<indexers.length; i++) {
                    if (indexers[i] instanceof ICDTIndexer) {
                        if (project == null) {
                            CTestPlugin.getStandardDisplay().asyncExec(new Runnable() {
                                public void run() {
                                    MessageDialog.openInformation(theViewer.getControl().getShell(), INDEXER_VIEW,
                                    "SourceIndexer points to a null project."); //$NON-NLS-1$ //$NON-NLS-2$        
                                }
                            });
                            
                            return Status.CANCEL_STATUS;
                        }
                        
                        IIndex index = ((ICDTIndexer)indexers[i]).getIndex(project.getFullPath(), true, true);
                        
                        if (index==null) return Status.CANCEL_STATUS;
                        
                        try {
                            char[] prefix = BLANK_STRING.toCharArray();
                            char optionalType = (char)0;
                            char[] name = ALL_NAME_SEARCH.toCharArray();
                            char [][] containingTypes = new char[0][];
                            int matchMode = ICSearchConstants.PATTERN_MATCH;
                            boolean isCaseSensitive = false;
                            
                            char[] queryString = AbstractIndexer.bestPrefix( prefix, optionalType, name, containingTypes, matchMode, isCaseSensitive);
                            
                            IEntryResult[] results = index.queryEntries(queryString);
                            if (results == null) return Status.CANCEL_STATUS;
                            
                            int size = results.length; 
                            IndexerNodeLeaf[] children = new IndexerNodeLeaf[size];
                            for(int j=0; j<size; j++) {
                                children[j] = new IndexerNodeLeaf(results[j], index.getIndexFile());
                                children[j].setParent(invisibleRoot);
                            }
                            
                            invisibleRoot.setChildren(children);
                            
                            invisibleRoot.setIsForward(true); // initial display direction is forward
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                
                return Status.OK_STATUS;
            }
        }
        
        public class InitializeRunnable implements Runnable {
            TableViewer view = null;
            boolean updateView = true;
            
            public InitializeRunnable(TableViewer view, boolean updateView) {
                this.view = view;
                this.updateView = updateView;
            }
            
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                if (!updateView) return; 
                    
                view.refresh();
                
                if (view.getTable().getItems().length > 0) {
                    TableItem[] selection = new TableItem[1];
                    selection[0] = view.getTable().getItems()[0];
                    
                    // select the first item to prevent it from being selected accidentally (and possibly switching editors accidentally)
                    view.getTable().setSelection(selection);
                }
                
                previousPageAction.setEnabled(displayBackwards);
                nextPageAction.setEnabled(displayForwards);
            }
        }
        
        private class StartInitializingIndexerView extends Job {
            private static final String INITIALIZE_INDEXER_VIEW = "initialize Indexer View"; //$NON-NLS-1$
            InitializeView job = null;
            boolean updateView=true;
            
            public StartInitializingIndexerView(InitializeView job, boolean updateView) {
                super(INITIALIZE_INDEXER_VIEW);
                this.job = job;
                this.updateView = updateView;
            }
            
            protected IStatus run(IProgressMonitor monitor) {
                job.schedule();
                
                try {
                    job.join();
                } catch (InterruptedException ie) {
                    return Status.CANCEL_STATUS;
                }
                    
                CTestPlugin.getStandardDisplay().asyncExec(new InitializeRunnable(viewer, updateView)); // update the view from the Display thread
                
                updateView=true;
                
                return job.getResult();
            }
        }
        
        public ViewContentProvider() {
            this(null, false, false);
        }
        
        public void setDisplayForwards(boolean displayForwards) {
            this.displayForwards = displayForwards;
        }

        public void setDisplayBackwards(boolean displayBackwards) {
            this.displayBackwards = displayBackwards;
        }
        
        public ViewContentProvider(IndexerNodeParent parent, boolean displayForwards, boolean displayBackwards) {
            if (parent == null) {
                invisibleRoot = new IndexerNodeParent(null, null, this);
                initializeIndexerViewJob = new StartInitializingIndexerView(new InitializeView(POPULATING_INDEXER_VIEW, viewer), true);
                initializeIndexerViewJob.schedule();    
            } else {
                invisibleRoot = parent;
                initializeIndexerViewJob = new StartInitializingIndexerView(new InitializeView(POPULATING_INDEXER_VIEW, viewer), false);
                initializeIndexerViewJob.schedule();
            }
            
            this.displayForwards=displayForwards;
            this.displayBackwards=displayBackwards;
        }
        
        public Object[] getElements(Object inputElement) {
            if (inputElement.equals(getViewSite())) {
                return getChildren(invisibleRoot);
             }
             return getChildren(inputElement);
        }

        public void dispose() {}

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // TODO Auto-generated method stub
        }

        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof IndexerNodeParent) {
                return ((IndexerNodeParent) parentElement).getChildren();
             }
             return new Object[0];
        }

        public Object getParent(Object element) {
            if (element instanceof IndexerNodeLeaf) {
                return ((IndexerNodeLeaf) element).getParent();
             }
             return null;
        }

        public boolean hasChildren(Object element) {
            if (element instanceof IndexerNodeParent)
                return ((IndexerNodeParent) element).hasChildren();
             return false;
        }
        
        public IndexerNodeParent getInvisibleRoot() {
            return invisibleRoot;
        }

        public boolean isDisplayForwards() {
            return displayForwards;
        }

        public boolean isDisplayBackwards() {
            return displayBackwards;
        }
        
        public String getProjectName() {
            if (project == null) return BLANK_STRING;
            
            return project.getName();
        }
    }
    
    class ViewLabelProvider extends LabelProvider {

        public String getText(Object obj) {
          if (obj == null) return BLANK_STRING;
           return obj.toString();
        }

        public Image getImage(Object obj) {
           String imageKey = IndexerViewPluginImages.IMG_WARNING;

           if (obj instanceof IndexerNodeLeaf) {
               String word = String.valueOf(((IndexerNodeLeaf)obj).getResult().getWord());
               
               if (word.startsWith(FilterIndexerViewDialog.ENTRY_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_FUNCTION_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_FUNCTION_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_FUNCTION_DECL_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_FUNCTION_DECL;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_CONSTRUCTOR_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_CONSTRUCTOR_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_CONSTRUCTOR_DECL_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_CONSTRUCTOR_DECL;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_NAMESPACE_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_NAMESPACE_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_NAMESPACE_DECL_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_NAMESPACE_DECL;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_FIELD_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_FIELD_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_FIELD_DECL_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_FIELD_DECL;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_ENUMTOR_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_ENUMTOR_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_ENUMTOR_DECL_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_ENUMTOR_DECL;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_METHOD_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_METHOD_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_METHOD_DECL_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_METHOD_DECL;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_MACRO_DECL_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_MACRO_DECL;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_INCLUDE_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_INCLUDE_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_SUPER_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_SUPER_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_T_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_TYPEDEF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_C_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_CLASS;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_V_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_VARIABLE;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_S_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_STRUCT;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_E_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_ENUM;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_U_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_UNION;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_REF_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_TYPE_REF;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_D_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_DERIVED;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_F_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_FRIEND;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_G_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_FWD_CLASS;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_H_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_FWD_STRUCT;
               } else if (word.startsWith(FilterIndexerViewDialog.ENTRY_TYPE_DECL_I_STRING)) {
                   imageKey = IndexerViewPluginImages.IMG_FWD_UNION;
               }
           }
           
           return IndexerViewPluginImages.get(imageKey);
        }
     }

    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

        viewer.setContentProvider(new ViewContentProvider());

        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setInput(getViewSite());
        
        makeActions();
        hookContextMenu();
        hookSingleClickAction();

        contributeToActionBars();
    }

    private void makeActions() {
        previousPageAction = new Action() {
            public void run() {
                if (viewer.getContentProvider() instanceof ViewContentProvider) {
                    IndexerNodeParent root = ((ViewContentProvider)viewer.getContentProvider()).getInvisibleRoot();
                    root.setIsForward(false);
                }
                viewer.refresh();
                
                setEnabled(((ViewContentProvider)viewer.getContentProvider()).isDisplayBackwards());
                nextPageAction.setEnabled(((ViewContentProvider)viewer.getContentProvider()).isDisplayForwards());
            }
        };
        previousPageAction.setText(PREVIOUS_PAGE);
        previousPageAction.setToolTipText(PREVIOUS_PAGE);
        previousPageAction.setImageDescriptor(IndexerViewPluginImages.DESC_BACK);
        
        nextPageAction = new Action() {
            public void run() {
                if (viewer.getContentProvider() instanceof ViewContentProvider) {
                    IndexerNodeParent root = ((ViewContentProvider)viewer.getContentProvider()).getInvisibleRoot();
                    root.setIsForward(true);
                }
                viewer.refresh();
                
                previousPageAction.setEnabled(((ViewContentProvider)viewer.getContentProvider()).isDisplayBackwards());
                setEnabled(((ViewContentProvider)viewer.getContentProvider()).isDisplayForwards());
            }
        };
        nextPageAction.setText(NEXT_PAGE);
        nextPageAction.setToolTipText(NEXT_PAGE);
        nextPageAction.setImageDescriptor(IndexerViewPluginImages.DESC_NEXT);
        
        setFiltersAction = new Action() {
            public void run() {
                if (!(viewer.getContentProvider() instanceof ViewContentProvider)) return;

                FilterIndexerViewDialog dialog = new FilterIndexerViewDialog(getSite().getShell(), ((ViewContentProvider)viewer.getContentProvider()).getInvisibleRoot(), project.getName());
                int result = dialog.open();
                
                if (result == IDialogConstants.OK_ID) {
                    viewer.setContentProvider(new ViewContentProvider(((ViewContentProvider)viewer.getContentProvider()).getInvisibleRoot(), true, true));
                    ((ViewContentProvider)viewer.getContentProvider()).getInvisibleRoot().setView((ViewContentProvider)viewer.getContentProvider()); // update the root's content provider
                    
                    previousPageAction.setEnabled(((ViewContentProvider)viewer.getContentProvider()).isDisplayBackwards());
                    nextPageAction.setEnabled(((ViewContentProvider)viewer.getContentProvider()).isDisplayForwards());
                }
            }
        };
        setFiltersAction.setText(SET_FILTERS);
        setFiltersAction.setToolTipText(SET_FILTERS);
        setFiltersAction.setImageDescriptor(IndexerViewPluginImages.DESC_FILTER_BUTTON);
        
        singleClickAction = new IndexerHighlighterAction();
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager(_INDEXER_MENU_MANAGER);
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            private void hideMenuItems(IMenuManager manager) {
            }

            public void menuAboutToShow(IMenuManager manager) {
                IndexerView.this.fillContextMenu(manager);
                hideMenuItems(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    void fillContextMenu(IMenuManager manager) {
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private class IndexerHighlighterAction extends Action {
        public void run() {
            ISelection selection = viewer.getSelection();
            
            IViewPart part = getSite().getPage().findView(PROPERTIES_VIEW);
            if (part instanceof PropertySheet) {
                ((PropertySheet)part).selectionChanged(getSite().getPart(), selection); // TODO Devin need to instead get the part that this action belongs to... 
            }
        }
    }
    
    private void hookSingleClickAction() {
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                singleClickAction.run();
            }
        });
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(previousPageAction);
        manager.add(nextPageAction);
        manager.add(new Separator());
        manager.add(setFiltersAction);
        manager.add(new Separator());
    }

    public void setFocus() {
        IViewPart part = getSite().getPage().findView(PROPERTIES_VIEW);
        if (part instanceof PropertySheet) {
            ((PropertySheet)part).selectionChanged(getSite().getPart(), viewer.getSelection());
        }
    }

    public void appendIndexer(IIndexer indexer) {
        indexers = (IIndexer[])ArrayUtil.append(IIndexer.class, indexers, indexer);
    }
    
    public void clearIndexers() {
        indexers = new IIndexer[CTestPlugin.getWorkspace().getRoot().getProjects().length];
    }
    
    public void setContentProvider(ViewContentProvider provider) {
        viewer.setContentProvider(provider);
    }
    
    public void setProject(IProject project) {
        this.setPartName(INDEXER_VIEW___ + project.getName());
        this.project=project;
    }
    
    public static ViewerFilter[] getViewerFilters() {
        return viewer.getFilters();
    }

    public String getProjectName() {
        if (project == null) return BLANK_STRING;
        
        return project.getName();
    }
}
