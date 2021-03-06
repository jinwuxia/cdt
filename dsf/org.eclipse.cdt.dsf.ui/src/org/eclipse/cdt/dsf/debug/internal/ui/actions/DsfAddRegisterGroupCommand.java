/*******************************************************************************
 * Copyright (c) 2014 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc Khouzam (Ericsson) - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.dsf.debug.internal.ui.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command performing adding a register group.
 * @since 2.4
 */
public class DsfAddRegisterGroupCommand extends AbstractDsfRegisterGroupActions {
	@Override
	public void setEnabled(Object evaluationContext) {
		boolean state = false;
	    if (evaluationContext instanceof IEvaluationContext) {
	        Object s = ((IEvaluationContext) evaluationContext).getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
	        Object p = ((IEvaluationContext) evaluationContext).getVariable(ISources.ACTIVE_PART_NAME);
	        if (s instanceof IStructuredSelection && p instanceof IWorkbenchPart) {
	        	state = canAddRegisterGroup((IWorkbenchPart)p, (IStructuredSelection)s);
	        }
	    }
		setBaseEnabled(state);
	}
	
	@Override
	public Object execute(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (selection instanceof IStructuredSelection) {
			addRegisterGroup(part, (IStructuredSelection)selection);
		}
    	return null;
    }
}
