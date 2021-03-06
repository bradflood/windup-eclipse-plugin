/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.windup.ui.internal.services;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.resources.IFile;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jboss.tools.windup.ui.WindupUIPlugin;
import org.jboss.tools.windup.ui.internal.Messages;
import org.jboss.tools.windup.ui.internal.rules.NewRuleFromSelectionWizard;
import org.jboss.tools.windup.ui.internal.rules.RulesetEditor;
import org.jboss.tools.windup.ui.internal.rules.RulesetEditorWrapper;
import org.jboss.tools.windup.ui.internal.services.ContextMenuService.WindupAction;
import org.jboss.tools.windup.ui.internal.views.TaskListView;
import org.w3c.dom.Element;

@SuppressWarnings("restriction")
public class CreateMigrationIssueService implements MouseListener, IMenuListener {

	@Inject private EPartService partService;
	@Inject private RulesetSelectionCreationService creationService;
	
	private ITextEditor editor;
	
	private WindupAction CREATE_MIGRATION_TASK = new WindupAction(Messages.createMigrationIssue,
		WindupUIPlugin.getImageDescriptor(WindupUIPlugin.IMG_WINDUP), () -> {
		MPart part = partService.showPart(TaskListView.VIEW_ID, PartState.ACTIVATE);
		TaskListView view = (TaskListView)part.getObject();
		view.createMigrationIssueFromSelection();
	});
	
	@Inject
	private void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part) {
		unregisterListener();
		if (part != null) {
			ITextEditor theEditor = getTextEditor(part);
			if (theEditor != null) {
				MenuManager menuManager = getMenuManager(theEditor);
				if (menuManager != null) {
					registerListener(theEditor, menuManager);
				}
			}
		}
	}
	
	private ITextEditor getTextEditor(MPart part) {
		ITextEditor theEditor = null;
		Object client = part.getObject();
		if (client instanceof CompatibilityPart) {
			IWorkbenchPart workbenchPart = ((CompatibilityPart) client).getPart();
			if (!(workbenchPart instanceof ITextEditor)) {
				workbenchPart = workbenchPart.getAdapter(ITextEditor.class);
			}
			if (workbenchPart instanceof ITextEditor) {
				theEditor = (ITextEditor)workbenchPart;
			}
		}
		return theEditor;
	}
	
	private void unregisterListener() {
		if (editor != null) {
			MenuManager menuManager = getMenuManager(editor);
			if (menuManager != null) {
				menuManager.removeMenuListener(this);
			}
			editor = null;
		}
	}
	
	private MenuManager getMenuManager(ITextEditor editor) {
		Control control = editor.getAdapter(Control.class);
		if (control != null && !control.isDisposed()) {
			Menu menu = control.getMenu();
			if (menu != null && !menu.isDisposed()) {
				return (MenuManager)menu.getData(MenuManager.MANAGER_KEY);
			}
		}
		return null;
	}
	
	private void registerListener(ITextEditor editor, MenuManager menuManager) {
		this.editor = editor;
		menuManager.addMenuListener(this);
	}

	@Override
	public void menuAboutToShow(IMenuManager manager) {
		manager.add(CREATE_MIGRATION_TASK);
		manager.add(createRuleFromSelectionAction(editor));
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent e) {
	}
	@Override
	public void mouseDown(MouseEvent e) {
	}
	@Override
	public void mouseUp(MouseEvent e) {
	}
	
	private WindupAction createRuleFromSelectionAction(ITextEditor theEditor) {
		WindupAction action = new WindupAction(Messages.createRuleFromSelection,
				WindupUIPlugin.getImageDescriptor(WindupUIPlugin.IMG_WINDUP), () -> {
				IEclipseContext context = WindupUIPlugin.getDefault().getContext();
				NewRuleFromSelectionWizard wizard = ContextInjectionFactory.make(NewRuleFromSelectionWizard.class, context);
				WizardDialog wizardDialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard) {
					public Point getInitialSize() {
						return new Point(575, 220);
					}
				};
				if (wizardDialog.open() == Window.OK) {
					IFile ruleset = wizard.getRuleset();
					if (ruleset != null && ruleset.exists()) {
						try {
				            IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new FileEditorInput(ruleset), 
				            		RulesetEditor.ID, true, IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID);
							if (editorPart instanceof RulesetEditorWrapper) {
								RulesetEditorWrapper wrapper = (RulesetEditorWrapper)editorPart;
								List<Element> elements = creationService.createRulesFromEditorSelection(theEditor, wrapper);
								if (elements != null && !elements.isEmpty()) {
									wrapper.selectAndReveal(elements.get(elements.size()-1));
								}
							}
						} catch (PartInitException e) {
							WindupUIPlugin.log(e);
						}
					}
				}
			});
		return action;
	}
}
