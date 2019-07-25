/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.corrections.proposals;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.nls.changes.CreateFileChange;
import org.eclipse.jdt.ls.core.internal.corrections.CorrectionMessages;
import org.eclipse.jdt.ls.core.internal.corrections.IInvocationContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * This proposal is listed in the corrections list for a "type not found"
 * problem. It offers to create a new type.
 *
 * @see UnresolvedElementsSubProcessor#getTypeProposals(IInvocationContext,
 *      IProblemLocation, Collection)
 */
public class NewCUProposal extends ChangeCorrectionProposal {
	private static String PACKAGEHEADER = "package_header";
	private static String CURSOR = "cursor";

	public static final int K_CLASS = 1;
	public static final int K_INTERFACE = 2;
	public static final int K_ENUM = 3;
	public static final int K_ANNOTATION = 4;

	private Name fNode;
	private ICompilationUnit fCompilationUnit;
	private int fTypeKind;
	private IJavaElement fTypeContainer; // IType or IPackageFragment
	private String fTypeNameWithParameters;
	private IType fCreatedType;

	/**
	 * @param name
	 * @param kind
	 * @param cu
	 * @param change
	 * @param relevance
	 */
	public NewCUProposal(ICompilationUnit cu, Name node, int typeKind, IJavaElement typeContainer, int relevance) {
		super("", CodeActionKind.QuickFix, null, relevance); //$NON-NLS-1$

		fCompilationUnit = cu;
		fNode = node;
		fTypeKind = typeKind;
		fTypeContainer = typeContainer;
		if (fNode != null) {
			fTypeNameWithParameters = getTypeName(typeKind, node);
		}

		setDisplayName();
	}

	private void setDisplayName() {
		String containerName;
		if (fNode != null) {
			containerName = ASTNodes.getQualifier(fNode);
		} else {
			if (fTypeContainer instanceof IPackageFragment) {
				containerName = ((IPackageFragment) fTypeContainer).getElementName();
			} else {
				containerName = ""; //$NON-NLS-1$
			}
		}
		String typeName = fTypeNameWithParameters;
		String containerLabel = BasicElementLabels.getJavaElementName(containerName);
		String typeLabel = BasicElementLabels.getJavaElementName(typeName);
		boolean isInnerType = fTypeContainer instanceof IType;
		switch (fTypeKind) {
			case K_CLASS:
				if (fNode != null) {
					if (isInnerType) {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerclass_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerclass_intype_description, new String[] { typeLabel, containerLabel }));
						}
					} else {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_inpackage_description, new String[] { typeLabel, containerLabel }));
						}
					}
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewclass_inpackage_description, containerLabel));
				}
				break;
			case K_INTERFACE:
				if (fNode != null) {
					if (isInnerType) {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerinterface_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerinterface_intype_description, new String[] { typeLabel, containerLabel }));
						}
					} else {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_inpackage_description, new String[] { typeLabel, containerLabel }));
						}
					}
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewinterface_inpackage_description, containerLabel));
				}
				break;
			case K_ENUM:
				if (fNode != null) {
					if (isInnerType) {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerenum_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerenum_intype_description, new String[] { typeLabel, containerLabel }));
						}
					} else {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_inpackage_description, new String[] { typeLabel, containerLabel }));
						}
					}
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewenum_inpackage_description, containerLabel));
				}
				break;
			case K_ANNOTATION:
				if (fNode != null) {
					if (isInnerType) {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerannotation_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerannotation_intype_description, new String[] { typeLabel, containerLabel }));
						}
					} else {
						if (containerName.length() == 0) {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_description, typeLabel));
						} else {
							setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_inpackage_description, new String[] { typeLabel, containerLabel }));
						}
					}
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewannotation_inpackage_description, containerLabel));
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown type kind"); //$NON-NLS-1$
		}
	}

	private static String getTypeName(int typeKind, Name node) {
		String name = ASTNodes.getSimpleNameIdentifier(node);

		if (typeKind == K_CLASS || typeKind == K_INTERFACE) {
			ASTNode parent = node.getParent();
			if (parent.getLocationInParent() == ParameterizedType.TYPE_PROPERTY) {
				String typeArgBaseName = name.startsWith(String.valueOf('T')) ? String.valueOf('S') : String.valueOf('T'); // use 'S' or 'T'

				int nTypeArgs = ((ParameterizedType) parent.getParent()).typeArguments().size();
				StringBuilder buf = new StringBuilder(name);
				buf.append('<');
				if (nTypeArgs == 1) {
					buf.append(typeArgBaseName);
				} else {
					for (int i = 0; i < nTypeArgs; i++) {
						if (i != 0) {
							buf.append(", "); //$NON-NLS-1$
						}
						buf.append(typeArgBaseName).append(i + 1);
					}
				}
				buf.append('>');
				return buf.toString();
			}
		}
		return name;
	}

	private TypeDeclaration findEnclosingTypeDeclaration(ASTNode node, String typeName) {
		Iterator<ASTNode> iter;
		if (node instanceof CompilationUnit) {
			iter = ((CompilationUnit) node).types().iterator();
		} else if (node instanceof TypeDeclaration) {
			if (typeName.equals(((TypeDeclaration) node).getName().toString())) {
				return (TypeDeclaration) node;
			}
			iter = ((TypeDeclaration) node).bodyDeclarations().iterator();
		} else {
			return null;
		}

		while (iter.hasNext()) {
			TypeDeclaration decl = findEnclosingTypeDeclaration(iter.next(), typeName);
			if (decl != null) {
				return decl;
			}
		}
		return null;
	}

	@Override
	protected Change createChange() throws CoreException {
		IType targetType;
		if (fTypeContainer instanceof IType) {
			// e.g. OldClass.NewClass
			IType enclosingType = (IType) fTypeContainer;
			ICompilationUnit parentCU = enclosingType.getCompilationUnit();

			ASTParser astParser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			astParser.setSource(parentCU);
			CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
			TypeDeclaration enclosingDecl = findEnclosingTypeDeclaration(cu, fTypeContainer.getElementName());

			ASTRewrite rewrite = ASTRewrite.create(cu.getAST());
			final AbstractTypeDeclaration newDeclaration;
			switch (fTypeKind) {
				case K_CLASS:
					newDeclaration = cu.getAST().newTypeDeclaration();
					((TypeDeclaration) newDeclaration).setInterface(false);
					break;
				case K_INTERFACE:
					newDeclaration = cu.getAST().newTypeDeclaration();
					((TypeDeclaration) newDeclaration).setInterface(true);
					break;
				case K_ENUM:
					newDeclaration = cu.getAST().newEnumDeclaration();
					break;
				case K_ANNOTATION:
					newDeclaration = cu.getAST().newAnnotationTypeDeclaration();
					break;
				default:
					newDeclaration = null;
			}

			newDeclaration.setJavadoc(null);
			newDeclaration.setName(cu.getAST().newSimpleName(fTypeNameWithParameters));

			ListRewrite lrw = rewrite.getListRewrite(enclosingDecl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			lrw.insertLast(newDeclaration, null);

			TextEdit res = rewrite.rewriteAST();

			String label = "TODO";
			CompilationUnitChange cuChange = new CompilationUnitChange(label, parentCU);
			cuChange.setEdit(res);
			return cuChange;

			// TextFileChange: fillContent after last sibling
		} else if (fTypeContainer instanceof IPackageFragment) {
			String name = ASTNodes.getSimpleNameIdentifier(fNode);
			ICompilationUnit parentCU = ((IPackageFragment) fTypeContainer).getCompilationUnit(getCompilationUnitName(name));
			targetType = parentCU.getType(name);

			// CreateFileChange: create class in /foo/bar/NewClass.java
			CompositeChange change = new CompositeChange("");
			change.add(new CreateFileChange(targetType.getResource().getRawLocation(), "", ""));

			// Construct AST
			ASTParser astParser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			astParser.setSource(parentCU);
			CompilationUnit cu = (CompilationUnit) astParser.createAST(null);

			String lineDelimiter = StubUtility.getLineDelimiterUsed(fCompilationUnit.getJavaProject());
			String typeStub = constructTypeStub(parentCU, name, Flags.AccPublic, lineDelimiter);
			String cuContent = constructCUContent(parentCU, typeStub, lineDelimiter);

			String label = "TODO";
			CompilationUnitChange cuChange = new CompilationUnitChange(label, parentCU);
			cuChange.setEdit(new InsertEdit(0, cuContent));
			change.add(cuChange);

			return change;
		} else {
			return null;
		}
	}

	// TODO: enable fileComment, typeComment
	private String constructCUContent(ICompilationUnit cu, String typeContent, String lineDelimiter) throws CoreException {
		String fileComment = CodeGeneration.getFileComment(cu, lineDelimiter);
		//		String typeComment= CodeGeneration.getTypeComment(cu, lineDelimiter, typeComment);
		IPackageFragment pack = (IPackageFragment) cu.getParent();
		String content = CodeGeneration.getCompilationUnitContent(cu, null/*fileComment*/, null/*typeComment*/, typeContent, lineDelimiter);
		if (content != null) {

			ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setProject(cu.getJavaProject());
			parser.setSource(content.toCharArray());
			CompilationUnit unit = (CompilationUnit) parser.createAST(null);
			if ((pack.isDefaultPackage() || unit.getPackage() != null) && !unit.types().isEmpty()) {
				return content;
			}
		}
		StringBuilder buf = new StringBuilder();
		if (!pack.isDefaultPackage()) {
			buf.append("package ").append(pack.getElementName()).append(';'); //$NON-NLS-1$
		}
		buf.append(lineDelimiter).append(lineDelimiter);
		//		if (typeComment != null) {
		//			buf.append(typeComment).append(lineDelimiter);
		//		}
		buf.append(typeContent);
		return buf.toString();
	}

	/*
	 * Called from createType to construct the source for this type
	 */
	private String constructTypeStub(ICompilationUnit parentCU, String name, int modifiers, String lineDelimiter) throws CoreException {
		StringBuffer buf = new StringBuffer();

		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}
		String type = ""; //$NON-NLS-1$
		String templateID = ""; //$NON-NLS-1$
		switch (fTypeKind) {
			case K_CLASS:
				type = "class "; //$NON-NLS-1$
				templateID = CodeGeneration.CLASS_BODY_TEMPLATE_ID;
				break;
			case K_INTERFACE:
				type = "interface "; //$NON-NLS-1$
				templateID = CodeGeneration.INTERFACE_BODY_TEMPLATE_ID;
				break;
			case K_ENUM:
				type = "enum "; //$NON-NLS-1$
				templateID = CodeGeneration.ENUM_BODY_TEMPLATE_ID;
				break;
			case K_ANNOTATION:
				type = "@interface "; //$NON-NLS-1$
				templateID = CodeGeneration.ANNOTATION_BODY_TEMPLATE_ID;
				break;
		}
		buf.append(type);
		buf.append(name);

		buf.append(" {").append(lineDelimiter); //$NON-NLS-1$
		String typeBody = CodeGeneration.getTypeBody(templateID, parentCU, name, lineDelimiter);
		if (typeBody != null) {
			buf.append(typeBody);
		} else {
			buf.append(lineDelimiter);
		}
		buf.append('}').append(lineDelimiter);
		return buf.toString();
	}

	private static String getCompilationUnitName(String typeName) {
		return typeName + JavaModelUtil.DEFAULT_CU_SUFFIX;
	}

	public IType getCreatedType() {
		return fCreatedType;
	}

	public int getTypeKind() {
		return fTypeKind;
	}

}
