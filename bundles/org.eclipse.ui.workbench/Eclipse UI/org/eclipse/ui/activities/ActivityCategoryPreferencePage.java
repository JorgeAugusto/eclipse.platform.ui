/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.activities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.OverlayIcon;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.activities.ws.ActivityEnabler;
import org.eclipse.ui.internal.activities.ws.ActivityMessages;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Activities preference page that primarily shows categories and can optionally
 * show an advanced dialog that allows fine-tune adjustmenet of activities. This
 * page may be used by product developers to provide basic ability to tweak the
 * enabled activity set. You may provide certain strings to this class via
 * method #2 of {@link org.eclipse.core.runtime.IExecutableExtension}.
 * 
 * @see #ACTIVITY_NAME
 * @see #ALLOW_ADVANCED
 * @see #CAPTION_MESSAGE
 * @see #CATEGORY_NAME
 * @see #ACTIVITY_PROMPT_BUTTON
 * @see #ACTIVITY_PROMPT_BUTTON_TOOLTIP
 * 
 * @since 3.1
 */
public final class ActivityCategoryPreferencePage extends PreferencePage implements
        IWorkbenchPreferencePage, IExecutableExtension {

    /**
     * The name to use for the activities.  Ie: "Capabilities".
     */
    public static final String ACTIVITY_NAME = "activityName"; //$NON-NLS-1$

    /**
     * The parameter to use if you want the page to show the allow button. Must
     * be true or false.
     */
    public static final String ALLOW_ADVANCED = "allowAdvanced"; //$NON-NLS-1$
    
    /**
     * The string to use for the message at the top of the preference page.
     */
    public static final String CAPTION_MESSAGE = "captionMessage"; //$NON-NLS-1$
    
    /**
     * The name to use for the activity categories.  Ie: "Roles".
     */
    public static final String CATEGORY_NAME = "categoryName"; //$NON-NLS-1$
    
    /**
     * The label to be used for the prompt button. Ie: "&Prompt when enabling capabilities".
     */    
    public static final String ACTIVITY_PROMPT_BUTTON = "activityPromptButton"; //$NON-NLS-1$

    /**
     * The tooltip to be used for the prompt button. Ie: "Prompt when a feature is first used that requires enablement of capabilities".
     */    
    public static final String ACTIVITY_PROMPT_BUTTON_TOOLTIP = "activityPromptButtonTooltip"; //$NON-NLS-1$
    
    private class AdvancedDialog extends Dialog {

        ActivityEnabler enabler;
        /**
         * @param parentShell
         */
        protected AdvancedDialog(Shell parentShell) {
            super(parentShell);
            setShellStyle(getShellStyle() | SWT.RESIZE);
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
         */
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(ActivityMessages.ActivitiesPreferencePage_advancedDialogTitle);
            newShell.setSize(new Point(400, 500));
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
         */
        protected Control createDialogArea(Composite parent) {
            Composite composite = (Composite) super.createDialogArea(parent);
            enabler = new ActivityEnabler(workingCopy, strings);
            Control enablerControl = enabler.createControl(composite);
            enablerControl.setLayoutData(new GridData(GridData.FILL_BOTH));
            return composite;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.dialogs.Dialog#okPressed()
         */
        protected void okPressed() {
            enabler.updateActivityStates();            
            super.okPressed();
        }
    }
    private class CategoryLabelProvider extends LabelProvider implements
            ITableLabelProvider, IActivityManagerListener {

        private LocalResourceManager manager = new LocalResourceManager(
                JFaceResources.getResources());

        private ImageDescriptor lockDescriptor;

        private boolean decorate;

        /**
         * @param decorate
         */
        public CategoryLabelProvider(boolean decorate) {
            this.decorate = decorate;
            lockDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
                    PlatformUI.PLUGIN_ID, "icons/full/ovr16/lock_ovr.gif"); //$NON-NLS-1$
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object,
         *      int)
         */
        public Image getColumnImage(Object element, int columnIndex) {
            ICategory category = (ICategory) element;
            ImageDescriptor descriptor = PlatformUI.getWorkbench()
                    .getActivitySupport().getImageDescriptor(category);
            if (descriptor != null) {
                try {
                    if (decorate) {
                        if (isLocked(category)) {
                            ImageData originalImageData = descriptor
                                    .getImageData();
                            OverlayIcon overlay = new OverlayIcon(
                                    descriptor, lockDescriptor, new Point(
                                            originalImageData.width,
                                            originalImageData.height));
                            return manager.createImage(overlay);
                        }
                    }

                    return manager.createImage(descriptor);
                } catch (DeviceResourceException e) {
                    e.printStackTrace();
                    // ignore
                }
            }  
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object,
         *      int)
         */
        public String getColumnText(Object element, int columnIndex) {
            String name = null;
            ICategory category = (ICategory) element;
            try {
                name = category.getName();
            } catch (NotDefinedException e) {
                name = category.getId();
            }
            if (decorate && isLocked(category)) {
                name = NLS.bind(ActivityMessages.ActivitiesPreferencePage_lockedMessage, name);
            }
            return name;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
         */
        public void dispose() {
            super.dispose();
            manager.dispose();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.ui.activities.IActivityManagerListener#activityManagerChanged(org.eclipse.ui.activities.ActivityManagerEvent)
         */
        public void activityManagerChanged(
                ActivityManagerEvent activityManagerEvent) {
            if (activityManagerEvent.haveEnabledActivityIdsChanged()) {
                categoryViewer.setCheckedElements(getEnabledCategories());
                fireLabelProviderChanged(new LabelProviderChangedEvent(this));
            }
        }
    }

    private class CategoryContentProvider implements IStructuredContentProvider {

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        public Object[] getElements(Object inputElement) {
            // convert to category objects
            return WorkbenchActivityHelper.resolveCategories(workingCopy,
                    (Set) inputElement);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.IContentProvider#dispose()
         */
        public void dispose() {

        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
         *      java.lang.Object, java.lang.Object)
         */
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        }
    }

    private class EmptyCategoryFilter extends ViewerFilter {

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
         *      java.lang.Object, java.lang.Object)
         */
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {
            ICategory category = (ICategory) element;
            if (WorkbenchActivityHelper.getActivityIdsForCategory(category)
                    .isEmpty())
                return false;
            return true;
        }
    }

    protected IWorkbench workbench;

    private CheckboxTableViewer categoryViewer;

    private TableViewer dependantViewer;

    private Text descriptionText;

    private IMutableActivityManager workingCopy;

    private Button activityPromptButton;

    private boolean allowAdvanced = false;

    private Button advancedButton;
    
    private Properties strings = new Properties();

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);  
        composite.setFont(parent.getFont());
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = layout.marginWidth = 0;
        composite.setLayout(layout);
        Label label = new Label(composite, SWT.WRAP);
        label
                .setText(strings.getProperty(CAPTION_MESSAGE, ActivityMessages.ActivitiesPreferencePage_captionMessage));
        label.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = 400;
        data.horizontalSpan = 2;        
        label.setLayoutData(data);
        label = new Label(composite, SWT.NONE); //spacer
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        data.horizontalSpan = 2;
        label.setLayoutData(data);
        createPromptButton(composite);
        createCategoryArea(composite);
        createDetailsArea(composite);
        createButtons(composite);
        return composite;
    }

    /**
     * @param composite
     */
    private void createPromptButton(Composite composite) {
        activityPromptButton = new Button(composite, SWT.CHECK);
        activityPromptButton.setFont(composite.getFont());
        activityPromptButton.setText(strings.getProperty(ACTIVITY_PROMPT_BUTTON, ActivityMessages.activityPromptButton));
        activityPromptButton.setToolTipText(strings.getProperty(ACTIVITY_PROMPT_BUTTON_TOOLTIP, ActivityMessages.activityPromptToolTip));
        GridData data = new GridData();
        data.horizontalSpan = 2;
        activityPromptButton.setLayoutData(data);
        activityPromptButton.setSelection(getPreferenceStore()
                .getBoolean(
                        IPreferenceConstants.SHOULD_PROMPT_FOR_ENABLEMENT));
    }

    private void createButtons(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(4, false);
        layout.marginHeight = layout.marginWidth = 0;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        composite.setLayoutData(data);

        Button enableAll = new Button(composite, SWT.PUSH);
        enableAll.setFont(parent.getFont());
        enableAll.addSelectionListener(new SelectionAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                workingCopy.setEnabledActivityIds(workingCopy
                        .getDefinedActivityIds());
            }
        });
        enableAll.setText("&Enable All"); //$NON-NLS-1$
        setButtonLayoutData(enableAll);

        Button disableAll = new Button(composite, SWT.PUSH);
        disableAll.setFont(parent.getFont());
        disableAll.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                workingCopy.setEnabledActivityIds(Collections.EMPTY_SET);
            }
        });
        disableAll.setText("D&isable All"); //$NON-NLS-1$
        setButtonLayoutData(disableAll);
        
        if (allowAdvanced) {
        		Label spacer = new Label(composite, SWT.NONE);
        		data = new GridData(GridData.GRAB_HORIZONTAL);
        		spacer.setLayoutData(data);
            advancedButton = new Button(composite, SWT.PUSH);
            advancedButton.setFont(parent.getFont());
            advancedButton.addSelectionListener(new SelectionAdapter() {

                /*
                 * (non-Javadoc)
                 * 
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                public void widgetSelected(SelectionEvent e) {
                    Shell parent = e.display.getActiveShell();
                    AdvancedDialog dialog = new AdvancedDialog(parent);
                    dialog.open(); // logic for updating the working copy is in the dialog class.                    
                }
            });
            advancedButton.setText(ActivityMessages.ActivitiesPreferencePage_advancedButton); //$NON-NLS-1$
            int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
            Point minSize = advancedButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
            data.widthHint = Math.max(widthHint, minSize.x);
            data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                    | GridData.VERTICAL_ALIGN_CENTER);
            advancedButton.setLayoutData(data);
        }
    }

    /**
     * @param parent
     */
    private void createDetailsArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = layout.marginWidth = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Label label = new Label(composite, SWT.NONE);
            label.setFont(parent.getFont());
            label.setText(ActivityMessages.ActivityEnabler_description);
            descriptionText = new Text(composite, SWT.WRAP | SWT.READ_ONLY | SWT.BORDER);
            descriptionText.setFont(parent.getFont());
            GridData data = new GridData(GridData.FILL_BOTH);
            data.heightHint = 100;
            data.widthHint = 200;
            descriptionText.setLayoutData(data);
        }
        {
            Label label = new Label(composite, SWT.NONE);
            label.setFont(parent.getFont());
            label.setText(ActivityMessages.ActivitiesPreferencePage_requirements);            
            dependantViewer = new TableViewer(composite, SWT.BORDER);
            dependantViewer.getControl().setFont(parent.getFont());
            dependantViewer.getControl().setLayoutData(
                    new GridData(GridData.FILL_BOTH));
            dependantViewer.setContentProvider(new CategoryContentProvider());
            dependantViewer.addFilter(new EmptyCategoryFilter());
            dependantViewer.setLabelProvider(new CategoryLabelProvider(false));
            dependantViewer.setInput(Collections.EMPTY_SET);
        }
    }

    /**
     * @param parent
     */
    private void createCategoryArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = layout.marginWidth = 0;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.widthHint = 200;
        composite.setLayoutData(data);
        Label label = new Label(composite, SWT.NONE);
        label.setFont(parent.getFont());
        label.setText(strings.getProperty(CATEGORY_NAME, ActivityMessages.ActivityEnabler_categories) + ':'); //$NON-NLS-1$
        Table table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.SINGLE);
        table.setFont(parent.getFont());
        table.addSelectionListener(new SelectionAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                if (e.detail == SWT.CHECK) {
                    TableItem tableItem = (TableItem) e.item;

                    ICategory category = (ICategory) tableItem.getData();
                    if (isLocked(category)) {
                        tableItem.setChecked(true);
                        e.doit = false; // veto the check
                        return;
                    }
                    Set activitySet = WorkbenchActivityHelper
                            .getActivityIdsForCategory(category);
                    if (tableItem.getChecked())
                        activitySet.addAll(workingCopy.getEnabledActivityIds());
                    else {
                        HashSet newSet = new HashSet(workingCopy
                                .getEnabledActivityIds());
                        newSet.removeAll(activitySet);
                        activitySet = newSet;
                    }

                    workingCopy.setEnabledActivityIds(activitySet);
                }
            }
        });
        categoryViewer = new CheckboxTableViewer(table);
        categoryViewer.getControl().setFont(parent.getFont());
        categoryViewer.getControl().setLayoutData(
                new GridData(GridData.FILL_BOTH));
        categoryViewer.setContentProvider(new CategoryContentProvider());
        CategoryLabelProvider categoryLabelProvider = new CategoryLabelProvider(
                true);
        workingCopy.addActivityManagerListener(categoryLabelProvider);
        categoryViewer.setLabelProvider(categoryLabelProvider);
        categoryViewer.setSorter(new ViewerSorter());
        categoryViewer.addFilter(new EmptyCategoryFilter());

        categoryViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {

                    /*
                     * (non-Javadoc)
                     * 
                     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
                     */
                    public void selectionChanged(SelectionChangedEvent event) {
                        ICategory element = (ICategory) ((IStructuredSelection) event
                                .getSelection()).getFirstElement();
                        setDetails(element);
                    }
                });
        categoryViewer.setInput(workingCopy.getDefinedCategoryIds());

        categoryViewer.setCheckedElements(getEnabledCategories());
    }

    private ICategory[] getEnabledCategories() {
        return WorkbenchActivityHelper.resolveCategories(workingCopy,
                WorkbenchActivityHelper.getEnabledCategories(workingCopy));
    }

    protected void setDetails(ICategory category) {
        if (category == null) {
            clearDetails();
            return;
        }
        Set categories = null;
        if (WorkbenchActivityHelper.isEnabled(workingCopy, category.getId())) {
            categories = WorkbenchActivityHelper.getDisabledCategories(
                    workingCopy, category.getId());

        } else {
            categories = WorkbenchActivityHelper.getEnabledCategories(
                    workingCopy, category.getId());
        }

        categories = WorkbenchActivityHelper.getContainedCategories(
                workingCopy, category.getId());
        dependantViewer.setInput(categories);
        try {
            descriptionText.setText(category.getDescription());
        } catch (NotDefinedException e) {
            descriptionText.setText(""); //$NON-NLS-1$
        }
    }

    /**
     * Clear the details area.
     */
    protected void clearDetails() {
        dependantViewer.setInput(Collections.EMPTY_SET);
        descriptionText.setText(""); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
        this.workbench = workbench;
        workingCopy = workbench.getActivitySupport().createWorkingCopy();
        setPreferenceStore(WorkbenchPlugin.getDefault().getPreferenceStore());
    }

    /**
     * Return whether the category is locked.
     * 
     * @param category
     *            the category to test
     * @return whether the category is locked
     */
    protected boolean isLocked(ICategory category) {
        return !WorkbenchActivityHelper.getDisabledCategories(workingCopy,
                category.getId()).isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    public boolean performOk() {
        workbench.getActivitySupport().setEnabledActivityIds(
                workingCopy.getEnabledActivityIds());
        getPreferenceStore().setValue(
                IPreferenceConstants.SHOULD_PROMPT_FOR_ENABLEMENT,
                activityPromptButton.getSelection());
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        super.performDefaults();
        activityPromptButton.setSelection(getPreferenceStore()
                .getDefaultBoolean(
                        IPreferenceConstants.SHOULD_PROMPT_FOR_ENABLEMENT));
        
        Set defaultEnabled = new HashSet();
        Set activityIds = workingCopy.getDefinedActivityIds();
        for (Iterator i = activityIds.iterator(); i.hasNext();) {
            String activityId = (String) i.next();
            IActivity activity = workingCopy.getActivity(activityId);
            try {
                if (activity.isDefaultEnabled()) {
                    defaultEnabled.add(activityId);
                }
            } catch (NotDefinedException e) {
                // this can't happen - we're iterating over defined activities.
            }
        }
        
        workingCopy.setEnabledActivityIds(defaultEnabled);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement,
     *      java.lang.String, java.lang.Object)
     */
    public void setInitializationData(IConfigurationElement config,
            String propertyName, Object data) {
        if (data instanceof Hashtable) {
            Hashtable table = (Hashtable)data;
            allowAdvanced = Boolean.valueOf((String) table.remove(ALLOW_ADVANCED)).booleanValue();
            strings.putAll(table);
        }
    }
}
