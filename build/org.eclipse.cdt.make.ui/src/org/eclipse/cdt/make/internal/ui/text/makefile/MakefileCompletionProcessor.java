/*******************************************************************************
 * Copyright (c) 2000, 2010 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.make.internal.ui.text.makefile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.cdt.make.core.makefile.IDirective;
import org.eclipse.cdt.make.core.makefile.IMacroDefinition;
import org.eclipse.cdt.make.core.makefile.IMakefile;
import org.eclipse.cdt.make.core.makefile.IRule;
import org.eclipse.cdt.make.internal.ui.MakeUIImages;
import org.eclipse.cdt.make.internal.ui.MakeUIPlugin;
import org.eclipse.cdt.make.internal.ui.text.CompletionProposalComparator;
import org.eclipse.cdt.make.internal.ui.text.WordPartDetector;
import org.eclipse.cdt.make.ui.IWorkingCopyManager;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;

/**
 * MakefileCompletionProcessor
 */
public class MakefileCompletionProcessor implements IContentAssistProcessor {
	/**
	 * Simple content assist tip closer. The tip is valid in a range
	 * of 5 characters around its pop-up location.
	 */
	protected static class Validator implements IContextInformationValidator, IContextInformationPresenter {
		protected int fInstallOffset;
		@Override
		public boolean isContextInformationValid(int offset) {
			return Math.abs(fInstallOffset - offset) < 5;
		}
		@Override
		public void install(IContextInformation info, ITextViewer viewer, int offset) {
			fInstallOffset = offset;
		}
		@Override
		public boolean updatePresentation(int documentPosition, TextPresentation presentation) {
			return false;
		}
	}

	public class DirectiveComparator implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			String name1;
			String name2;

			if (o1 instanceof IMacroDefinition) {
				name1 = ((IMacroDefinition)o1).getName();
			} else if (o1 instanceof IRule) {
				name1 = ((IRule)o1).getTarget().toString();
			} else {
				name1 =""; //$NON-NLS-1$
			}

			if (o2 instanceof IMacroDefinition) {
				name2 = ((IMacroDefinition)o1).getName();
			} else if (o2 instanceof IRule) {
				name2 = ((IRule)o1).getTarget().toString();
			} else {
				name2 =""; //$NON-NLS-1$
			}

			//return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
			return name1.compareToIgnoreCase(name2);
		}

	}
	protected IContextInformationValidator fValidator = new Validator();
	protected Image imageMacro = MakeUIImages.getImage(MakeUIImages.IMG_OBJS_MACRO);
	protected Image imageTarget = MakeUIImages.getImage(MakeUIImages.IMG_OBJS_TARGET_RULE);

	protected CompletionProposalComparator comparator = new CompletionProposalComparator();
	protected IEditorPart fEditor;
	protected IWorkingCopyManager fManager;

	public MakefileCompletionProcessor(IEditorPart editor) {
		fEditor = editor;
		fManager =  MakeUIPlugin.getDefault().getWorkingCopyManager();
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		WordPartDetector wordPart = new WordPartDetector(viewer.getDocument(), documentOffset);
		boolean macro = wordPart.isMacro();
		IMakefile makefile = fManager.getWorkingCopy(fEditor.getEditorInput());
		IDirective[] statements = null;
		if (macro) {
			IDirective[] m1 = makefile.getMacroDefinitions();
			IDirective[] m2 = makefile.getBuiltinMacroDefinitions();
			statements = new IDirective[m1.length + m2.length];
			System.arraycopy(m1, 0, statements, 0, m1.length);
			System.arraycopy(m2, 0, statements, m1.length, m2.length);
		} else {
			statements = makefile.getTargetRules();
		}

		ArrayList<ICompletionProposal> proposalList = new ArrayList<ICompletionProposal>(statements.length);

		// iterate over all the different categories
		for (IDirective statement : statements) {
			String name = null;
			Image image = null;
			String infoString = "";//getContentInfoString(name); //$NON-NLS-1$
			if (statement instanceof IMacroDefinition) {
				name = ((IMacroDefinition) statement).getName();
				image = imageMacro;
				infoString = ((IMacroDefinition)statement).getValue().toString();
			} else if (statement instanceof IRule) {
				name = ((IRule) statement).getTarget().toString();
				image = imageTarget;
				infoString = name;
			}
			if (name != null && name.startsWith(wordPart.getName())) {
				IContextInformation info = new ContextInformation(name, infoString);
				String displayString = (name.equals(infoString) ? name : name + " - " + infoString); //$NON-NLS-1$
				ICompletionProposal result =
						new CompletionProposal(
								name,
								wordPart.getOffset(),
								wordPart.getName().length(),
								name.length(),
								image,
								displayString,
								info,
								infoString);
				proposalList.add(result);
			}
		}
		ICompletionProposal[] proposals = proposalList.toArray(new ICompletionProposal[0]);
		Arrays.sort(proposals, comparator);
		return proposals;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		WordPartDetector wordPart = new WordPartDetector(viewer.getDocument(), documentOffset);
		boolean macro = wordPart.isMacro();
		IMakefile makefile = fManager.getWorkingCopy(fEditor.getEditorInput());
		ArrayList<String> contextList = new ArrayList<String>();
		if (macro) {
			IDirective[] statements = makefile.getMacroDefinitions();
			for (IDirective statement : statements) {
				if (statement instanceof IMacroDefinition) {
					String name = ((IMacroDefinition) statement).getName();
					if (name != null && name.equals(wordPart.getName())) {
						String value = ((IMacroDefinition) statement).getValue().toString();
						if (value != null && value.length() > 0) {
							contextList.add(value);
						}
					}
				}
			}
			statements = makefile.getBuiltinMacroDefinitions();
			for (IDirective statement : statements) {
				if (statement instanceof IMacroDefinition) {
					String name = ((IMacroDefinition) statement).getName();
					if (name != null && name.equals(wordPart.getName())) {
						String value = ((IMacroDefinition) statement).getValue().toString();
						if (value != null && value.length() > 0) {
							contextList.add(value);
						}
					}
				}
			}
		}

		IContextInformation[] result = new IContextInformation[contextList.size()];
		for (int i = 0; i < result.length; i++) {
			String context = contextList.get(i);
			result[i] = new ContextInformation(imageMacro, wordPart.getName(), context);
		}
		return result;

	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return fValidator;
	}

}
