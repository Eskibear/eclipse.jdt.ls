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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContext;
import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.ls.core.internal.corrections.CorrectionMessages;
import org.eclipse.jdt.ls.core.internal.corrections.IInvocationContext;
import org.eclipse.jdt.ls.core.internal.preferences.CodeGenerationTemplate;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;

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

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.manipulation.ChangeCorrectionProposalCore#createChange()
	 */
	@Override
	protected Change createChange() throws CoreException {
		IType targetType = createType(fNode.getFullyQualifiedName(), fCompilationUnit);

		CompositeChange createTypeChange = new CompositeChange("Create new type");
		createTypeChange.add(new CreateTextFileChange(targetType.getResource().getRawLocation(), "", "", ""));
		TextFileChange textFileChange = new TextFileChange("Fill in content", (IFile) targetType.getResource()) {
			@Override
			public Object getModifiedElement() {
				return targetType;
			}
		};
		String content = "hehe";
		try {
			String lineDelimiter = StubUtility.getLineDelimiterUsed(fCompilationUnit.getJavaProject());
			String simpleTypeStub = constructTypeStub(fCompilationUnit, fNode.getFullyQualifiedName(), fTypeKind, Flags.AccPublic, lineDelimiter);
			content = constructCUContent(fCompilationUnit, simpleTypeStub, lineDelimiter);

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		textFileChange.setEdit(new InsertEdit(0, content));

		createTypeChange.add(textFileChange);

		return createTypeChange;
	}

	// TODO: enable fileComment, typeComment
	private static String constructCUContent(ICompilationUnit cu, String typeContent, String lineDelimiter) throws CoreException {
		//		String fileComment= getFileComment(cu, lineDelimiter);
		//		String typeComment= getTypeComment(cu, lineDelimiter);
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
	private static String constructTypeStub(ICompilationUnit parentCU, String name, int typeKind, int modifiers, String lineDelimiter) throws CoreException {
		StringBuffer buf = new StringBuffer();

		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}
		String type = ""; //$NON-NLS-1$
		String templateID = ""; //$NON-NLS-1$
		switch (typeKind) {
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
		//		writeSuperClass(buf, imports);
		//		writeSuperInterfaces(buf, imports);

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

	private static IType createType(String typeName, ICompilationUnit cu) {
		IPackageFragment pack = getPackageFragment(cu);
		ICompilationUnit createdCU = pack.getCompilationUnit(getCompilationUnitName(typeName));
		return createdCU.getType(typeName);
	}

	private static IPackageFragment getPackageFragment(ICompilationUnit cu) {
		IJavaElement curr = cu;
		while (!(curr instanceof IPackageFragment)) {
			curr = curr.getParent();
		}
		return (IPackageFragment) curr;
	}

	private static String getCompilationUnitName(String typeName) {
		return typeName + JavaModelUtil.DEFAULT_CU_SUFFIX;
	}

	private ITypeBinding getPossibleSuperTypeBinding(ASTNode node) {
		if (node == null) {
			return null;
		}

		if (fTypeKind == K_ANNOTATION) {
			return null;
		}

		AST ast = node.getAST();
		node = ASTNodes.getNormalizedNode(node);
		ASTNode parent = node.getParent();
		switch (parent.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
				if (node.getLocationInParent() == MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY) {
					return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
				}
				break;
			case ASTNode.THROW_STATEMENT:
				return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				if (parent.getLocationInParent() == CatchClause.EXCEPTION_PROPERTY) {
					return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
				}
				break;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
			case ASTNode.FIELD_DECLARATION:
				return null; // no guessing for LHS types, cannot be a supertype of a known type
			case ASTNode.PARAMETERIZED_TYPE:
				return null; // Inheritance doesn't help: A<X> z= new A<String>(); ->
			case ASTNode.PROVIDES_DIRECTIVE:
				if (node.getLocationInParent() == ProvidesDirective.IMPLEMENTATIONS_PROPERTY) {
					Name serviceName = ((ProvidesDirective) parent).getName();
					IBinding binding = serviceName.resolveBinding();
					if (binding instanceof ITypeBinding) {
						return (ITypeBinding) binding;
					}
				}
				break;
		}
		ITypeBinding binding = ASTResolving.guessBindingForTypeReference(node);
		if (binding != null && !binding.isRecovered()) {
			return binding;
		}
		return null;
	}

	private static String getSnippetContent(ICompilationUnit cu, CodeGenerationTemplate templateSetting, String lineDelimiter, boolean snippetStringSupport) throws CoreException {
		Template template = templateSetting.createTemplate(cu.getJavaProject());
		if (template == null) {
			return null;
		}
		CodeTemplateContext context = new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);

		IPackageDeclaration[] packageDeclarations = cu.getPackageDeclarations();
		String packageName = cu.getParent().getElementName();
		String packageHeader = ((packageName != null && !packageName.isEmpty()) && (packageDeclarations == null || packageDeclarations.length == 0)) ? "package " + packageName + ";\n\n" : "";
		context.setVariable(PACKAGEHEADER, packageHeader);
		String typeName = JavaCore.removeJavaLikeExtension(cu.getElementName());
		List<IType> types = Arrays.asList(cu.getAllTypes());
		int postfix = 0;
		while (types != null && !types.isEmpty() && types.stream().filter(isTypeExists(typeName)).findFirst().isPresent()) {
			typeName = "Inner" + JavaCore.removeJavaLikeExtension(cu.getElementName()) + (postfix == 0 ? "" : "_" + postfix);
			postfix++;
		}
		if (postfix > 0 && snippetStringSupport) {
			context.setVariable(CodeTemplateContextType.TYPENAME, "${1:" + typeName + "}");
		} else {
			context.setVariable(CodeTemplateContextType.TYPENAME, typeName);
		}
		context.setVariable(CURSOR, snippetStringSupport ? "${0}" : "");

		// TODO Consider making evaluateTemplate public in StubUtility
		TemplateBuffer buffer;
		try {
			buffer = context.evaluate(template);
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null) {
			return null;
		}
		String str = buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		return str;
	}

	private static Predicate<IType> isTypeExists(String typeName) {
		return type -> type.getElementName().equals(typeName);
	}

	public IType getCreatedType() {
		return fCreatedType;
	}

	public int getTypeKind() {
		return fTypeKind;
	}

}
