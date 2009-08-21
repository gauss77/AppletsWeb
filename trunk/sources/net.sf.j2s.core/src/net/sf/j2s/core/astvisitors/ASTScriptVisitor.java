/*******************************************************************************
 * Copyright (c) 2007 java2script.org and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Zhou Renjian - initial API and implementation
 *******************************************************************************/
package net.sf.j2s.core.astvisitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;

/**
 * ASTScriptVisitor has to solve the following compiling problems:
 * 1. Field and method names:
 * 1.1 Java and JavaScript keywords;
 * 1.2 super; 
 * 1.3 private; 
 * 1.4 .j2smap;
 * 1.5 ...
 * 2. Final variables
 * 3. @-j2s* tags
 * 4. 
 * 
 * @author zhou renjian
 *
 * 2006-12-3
 */
public class ASTScriptVisitor extends ASTJ2SDocVisitor {

	private StringBuffer laterBuffer = new StringBuffer();

	/* for anonymous classes */
	private StringBuffer methodBuffer = new StringBuffer();
	
	//private boolean isInnerClass = false;

	protected AbstractTypeDeclaration rootTypeNode;

	public boolean isMethodRegistered(String methodName) {
		return ((ASTMethodVisitor) getAdaptable(ASTMethodVisitor.class)).isMethodRegistered(methodName);
	}
	
	public String translate(String className, String methodName) {
		return ((ASTMethodVisitor) getAdaptable(ASTMethodVisitor.class)).translate(className, methodName);
	}
	
	public String getPackageName() {
		return ((ASTPackageVisitor) getAdaptable(ASTPackageVisitor.class)).getPackageName();
	}
	
	public String discardGenericType(String name) {
		return ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).discardGenericType(name);
	}
	
	protected String listFinalVariables(List list, String seperator, String scope) {
		return ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).listFinalVariables(list, seperator, scope);
	}
	
	protected String getFullClassName() {
		return ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).getFullClassName();
	}
	
	public String getTypeStringName(Type type) {
		return ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).getTypeStringName(type);
	}
	
	protected String getFieldName(ITypeBinding binding, String name) {
		return ((ASTJ2SMapVisitor) getAdaptable(ASTJ2SMapVisitor.class)).getFieldName(binding, name);
	}
	
	protected String getJ2SName(SimpleName node) {
		return ((ASTJ2SMapVisitor) getAdaptable(ASTJ2SMapVisitor.class)).getJ2SName(node);
	}

	protected String getJ2SName(IVariableBinding binding) {
		return ((ASTJ2SMapVisitor) getAdaptable(ASTJ2SMapVisitor.class)).getJ2SName(binding);
	}
	
	protected boolean isInheritedFieldName(ITypeBinding binding, String name) {
		return ((ASTJ2SMapVisitor) getAdaptable(ASTJ2SMapVisitor.class)).isInheritedFieldName(binding, name);
	}
	
	protected boolean checkKeyworkViolation(String name) {
		return ((ASTFieldVisitor) getAdaptable(ASTFieldVisitor.class)).checkKeyworkViolation(name);
	}
	
	protected boolean checkSameName(ITypeBinding binding, String name) {
		return ((ASTJ2SMapVisitor) getAdaptable(ASTJ2SMapVisitor.class)).checkSameName(binding, name);
	}
	
	public boolean isIntegerType(String type) {
		return ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).isIntegerType(type);
	}
	
	public String getClassName() {
		return ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).getClassName();
	}
	
	protected String getVariableName(String name) {
		return ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).getVariableName(name);
	}
	
	protected boolean canAutoOverride(MethodDeclaration node) {
		return ((ASTMethodVisitor) getAdaptable(ASTMethodVisitor.class)).canAutoOverride(node);
	}
	
	public boolean visit(AnonymousClassDeclaration node) {
		ITypeBinding binding = node.resolveBinding();
		ASTTypeVisitor typeVisitor = ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class));
		
		String anonClassName = null;
		if (binding.isAnonymous() || binding.isLocal()) {
			anonClassName = assureQualifiedName(shortenQualifiedName(binding.getBinaryName()));
		} else {
			anonClassName = assureQualifiedName(shortenQualifiedName(binding.getQualifiedName()));
		}
		String shortClassName = null;
		int idx = anonClassName.lastIndexOf('.');
		if (idx == -1) {
			shortClassName = anonClassName;
		} else {
			shortClassName = anonClassName.substring(idx + 1);
		}

//		typeVisitor.increaseAnonymousClassCount();
//		//ClassInstanceCreation parent = (ClassInstanceCreation) node.getParent();
		String className = typeVisitor.getClassName();
//		String anonymousName = className + "$" + typeVisitor.getAnonymousCount();
//
		String fullClassName = anonClassName;
//		String fullClassName = null;
		String packageName = ((ASTPackageVisitor) getAdaptable(ASTPackageVisitor.class)).getPackageName();
//		if (packageName != null && packageName.length() != 0) {
//			fullClassName = packageName + '.' + anonymousName;
//		} else {
//			fullClassName = anonymousName;
//		}
		
//		if (thisPackageName != null && thisPackageName.length() != 0) {
//			fullClassName = thisPackageName + '.' + anonymousName;
//		} else {
//			fullClassName = anonymousName;
//		}
		
		buffer.append("(Clazz.isClassDefined (\"");
		buffer.append(fullClassName);
		buffer.append("\") ? 0 : ");
		
		StringBuffer tmpBuffer = buffer;
		buffer = methodBuffer;
		methodBuffer = new StringBuffer();
		
		buffer.append("cla$$.$");
		buffer.append(shortClassName);
		buffer.append("$ = function () {\r\n");
		
		buffer.append("Clazz.pu$h ();\r\n");
		buffer.append("cla$$ = ");
		//buffer.append("Clazz.decorateAsType (");
		buffer.append("Clazz.decorateAsClass (");
//		buffer.append(JavaLangUtil.ripJavaLang(fullClassName));
		String oldClassName = className;
		typeVisitor.setClassName(shortClassName);
//		buffer.append(" = function () {\r\n");
		buffer.append("function () {\r\n");
		if (!(node.getParent() instanceof EnumConstantDeclaration)) {
			buffer.append("Clazz.prepareCallback (this, arguments);\r\n");
		}
		StringBuffer oldLaterBuffer = laterBuffer;
		laterBuffer = new StringBuffer();
		List bodyDeclarations = node.bodyDeclarations();

		boolean needPreparation = false;
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) element;
				needPreparation = isFieldNeedPreparation(field);
				if (needPreparation) {
					break;
				}
			} else if (element instanceof Initializer) {
				Initializer init = (Initializer) element;
				if ((init.getModifiers() & Modifier.STATIC) == 0) {
					needPreparation = true;
					break;
				}
			}
		}
		
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof MethodDeclaration) {
				//MethodDeclaration method = (MethodDeclaration) element;
				//if ((method.getModifiers() & Modifier.STATIC) != 0) {
					continue;
				//}
				// there are no static members/methods in the inner type 
				// but there are static final members
//			} else if (element instanceof Initializer) {
//				continue;
			} else if (element instanceof FieldDeclaration
					/*&& isFieldNeedPreparation((FieldDeclaration) element)*/) {
//				continue;
				FieldDeclaration fieldDeclaration = (FieldDeclaration) element;
				if (isFieldNeedPreparation(fieldDeclaration)) {
					visitWith(fieldDeclaration, true);
					continue;
				}
			}
			element.accept(this);
//			element.accept(this);
		}
		
//		ASTScriptVisitor scriptVisitor = new ASTScriptVisitor();
//		scriptVisitor.isInnerClass = true;
//		scriptVisitor.thisClassName = anonymousName;
//		node.accept(scriptVisitor);
//		buffer.append(scriptVisitor.getBuffer());
		
		buffer.append("Clazz.instantialize (this, arguments);\r\n");
		
		buffer.append("}, ");

		
		String emptyFun = "Clazz.decorateAsClass (function () {\r\n" +
				"Clazz.instantialize (this, arguments);\r\n" +
				"}, ";
		idx = buffer.lastIndexOf(emptyFun);
		
		if (idx != -1 && idx == buffer.length() - emptyFun.length()) {
			buffer.replace(idx, buffer.length(), "Clazz.declareType (");
		} else {
			emptyFun = "Clazz.decorateAsClass (function () {\r\n" +
					"Clazz.prepareCallback (this, arguments);\r\n" +
					"Clazz.instantialize (this, arguments);\r\n" +
					"}, ";
			idx = buffer.lastIndexOf(emptyFun);
			
			if (idx != -1 && idx == buffer.length() - emptyFun.length()) {
				buffer.replace(idx, buffer.length(), "Clazz.declareAnonymous (");
			}
		}
		
		int lastIndexOf = fullClassName.lastIndexOf ('.');
		if (lastIndexOf != -1) {
			buffer.append(assureQualifiedName(shortenPackageName(fullClassName)));
			buffer.append(", \"" + fullClassName.substring(lastIndexOf + 1) + "\"");
		} else {
			buffer.append("null, \"" + fullClassName + "\"");
		}

		if (binding != null) {
			ITypeBinding superclass = binding.getSuperclass();
			if (superclass != null) {
				String clazzName = superclass.getQualifiedName();
				clazzName = assureQualifiedName(shortenQualifiedName(clazzName));
				if (clazzName != null && clazzName.length() != 0
						&& !"Object".equals(clazzName)) {
					buffer.append(", ");
					buffer.append(clazzName);
				} else {
					ITypeBinding[] declaredTypes = binding.getInterfaces();
					if (declaredTypes != null && declaredTypes.length > 0) {
						clazzName = declaredTypes[0].getQualifiedName();
						if (clazzName != null && clazzName.length() != 0) {
							clazzName = assureQualifiedName(shortenQualifiedName(clazzName));
							buffer.append(", null, ");
							buffer.append(clazzName);
						}
					}
				}
			}
		}
		buffer.append(");\r\n");
		
		bodyDeclarations = node.bodyDeclarations();
		
		if (needPreparation) {
			buffer.append("Clazz.prepareFields (cla$$, function () {\r\n");
			for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
				ASTNode element = (ASTNode) iter.next();
				if (element instanceof FieldDeclaration) {
					FieldDeclaration field = (FieldDeclaration) element;
					if (!isFieldNeedPreparation(field)) {
						continue;
					}
					element.accept(this);
				} else if (element instanceof Initializer) {
					Initializer init = (Initializer) element;
					if ((init.getModifiers() & Modifier.STATIC) == 0) {
						element.accept(this);
					}
				}
			}
			buffer.append("});\r\n");
		}
		
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof MethodDeclaration) {
				element.accept(this);
			}
		}
		
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof FieldDeclaration) {
				FieldDeclaration fields = (FieldDeclaration) element;
				if ((fields.getModifiers() & Modifier.STATIC) != 0) {
					List fragments = fields.fragments();
					for (int j = 0; j < fragments.size(); j++) {
					//if (fragments.size () == 1) {
						/* replace full class name with short variable name */
						buffer.append("cla$$");
						//buffer.append(fullClassName);
						buffer.append(".");
						VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragments.get(j);
						//buffer.append(vdf.getName());
						vdf.getName().accept(this);
						buffer.append(" = ");
						Expression initializer = vdf.getInitializer();
						if (initializer != null) { 
							initializer.accept(this);
						} else {
							Type type = fields.getType();
							if (type.isPrimitiveType()){
								PrimitiveType pType = (PrimitiveType) type;
								if (pType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
									buffer.append("false");
								} else {
									buffer.append("0");
								}
							} else {
								buffer.append("null");
							}
						}
						buffer.append(";\r\n");
					}
				}
			}
		}
		
		buffer.append("cla$$ = Clazz.p0p ();\r\n");
		
		typeVisitor.setClassName(oldClassName);
		
		buffer.append(laterBuffer);
		laterBuffer = oldLaterBuffer;
		
		buffer.append("};\r\n");

		String methods = methodBuffer.toString();
		methodBuffer = buffer;
		methodBuffer.append(methods);
		buffer = tmpBuffer;
		
		buffer.append(packageName);
		buffer.append(".");
		idx = className.indexOf('$');
		if (idx != -1) {
			buffer.append(className.substring(0, idx));
		} else {
			buffer.append(className);
		}
		buffer.append(".$");
		buffer.append(shortClassName);
		buffer.append("$ ())");
		/*
		 * Anonymouse class won't have static members and methods and initializers
		 */
		return false;
	}

	public boolean visit(CastExpression node) {
		Type type = node.getType();
		/*
		 * TODO: some casting should have its meaning!
		 * int to byte, int to short, long to int will lost values
		 */
		if (type.isPrimitiveType()) {
			ITypeBinding resolveTypeBinding = node.getExpression().resolveTypeBinding();
			if(resolveTypeBinding != null){
				String name = resolveTypeBinding.getName();
				PrimitiveType pType = (PrimitiveType) type;
				if (pType.getPrimitiveTypeCode() == PrimitiveType.INT
						|| pType.getPrimitiveTypeCode() == PrimitiveType.BYTE
						|| pType.getPrimitiveTypeCode() == PrimitiveType.SHORT
						|| pType.getPrimitiveTypeCode() == PrimitiveType.LONG) {
					if ("char".equals(name)) {
						buffer.append("(");
						node.getExpression().accept(this);
						buffer.append (").charCodeAt (0)");
						return false;
					} else if ("float".equals(name) || "double".equals(name)) {
						buffer.append("Math.round (");
						node.getExpression().accept(this);
						buffer.append (")");
						return false;
					}
				}
				if (pType.getPrimitiveTypeCode() == PrimitiveType.CHAR) {
					if ("char".equals(name)) {
//						buffer.append("(");
//						node.getExpression().accept(this);
//						buffer.append (").charCodeAt (0)");
//						return false;
					} else if ("float".equals(name) || "double".equals(name)) {
						// TODO:
						buffer.append("String.fromCharCode (");
						buffer.append("Math.round (");
						node.getExpression().accept(this);
						buffer.append (")");
						buffer.append (")");
						return false;
					} else if ("int".equals(name) || "byte".equals(name)
							|| "double".equals(name) || "float".equals(name)
							|| "short".equals(name) || "long".equals(name)) {
						buffer.append("String.fromCharCode (");
						node.getExpression().accept(this);
						buffer.append (")");
						return false;
					}
				}
			}
		}
		node.getExpression().accept(this);
		return false;
	}

	public boolean visit(ClassInstanceCreation node) {
		AnonymousClassDeclaration anonDeclare = node.getAnonymousClassDeclaration();
		Expression expression = node.getExpression();
		if (anonDeclare == null) {
			if (expression != null) {
				/*
				 * TODO: make sure the expression is not effected
				 */
				expression.accept(this);
			}
			ITypeBinding binding = node.resolveTypeBinding();
			if (binding != null) {
				if (!binding.isTopLevel()) {
					if ((binding.getModifiers() & Modifier.STATIC) == 0) {
						buffer.append("Clazz.innerTypeInstance (");
						if (binding.isAnonymous() || binding.isLocal()) {
							buffer.append(assureQualifiedName(shortenQualifiedName(binding.getBinaryName())));
						} else {
							buffer.append(assureQualifiedName(shortenQualifiedName(binding.getQualifiedName())));
						}
						buffer.append(", this, ");
						buffer.append("null"); // No final variables for non-anonymous class
						List arguments = node.arguments();
						if (arguments.size() > 0) {
							buffer.append(", ");
							visitList(arguments, ", ");
						}
						buffer.append(")");
						return false;
					}
				}
			}
			String fqName = getTypeStringName(node.getType());
			if ("String".equals(fqName) || "java.lang.String".equals(fqName)) {
				buffer.append(" String.instantialize");
			} else if ("Object".equals(fqName) || "java.lang.Object".equals(fqName)) {
				// For discussion, please visit http://groups.google.com/group/java2script/browse_thread/thread/3d6deb9c3c0a0cda
				buffer.append(" new JavaObject");
			} else {
				buffer.append(" new ");
				if (fqName != null) {
					fqName = assureQualifiedName(shortenQualifiedName(fqName));
					buffer.append(fqName);
				}
			}
			buffer.append(" (");
			IMethodBinding methodDeclaration = null;
			IMethodBinding constructorBinding = node.resolveConstructorBinding();
			if (constructorBinding != null) {
				methodDeclaration = constructorBinding.getMethodDeclaration();
			}
			visitMethodParameterList(node.arguments(), methodDeclaration);
			//visitList(node.arguments(), ", ");
			buffer.append(")");
		} else {
			ITypeBinding binding = node.resolveTypeBinding();
			String anonClassName = null;
			if (binding.isAnonymous() || binding.isLocal()) {
				if (binding.getBinaryName() == null) {
					System.out.println("what?" + binding.getQualifiedName());
				}
				anonClassName = assureQualifiedName(shortenQualifiedName(binding.getBinaryName()));
			} else {
				anonClassName = assureQualifiedName(shortenQualifiedName(binding.getQualifiedName()));
			}

//			String baseClassName = assureQualifiedName(shortenQualifiedName(getFullClassName()));
//			String shortClassName = null;
//			int idx = anonClassName.lastIndexOf('.');
//			if (idx == -1) {
//				shortClassName = anonClassName;
//			} else {
//				shortClassName = anonClassName.substring(idx + 1);
//			}
			
//			StringBuffer tmpBuffer = buffer;
//			buffer = methodBuffer;
			
			buffer.append("(");
//			buffer.append(baseClassName);
//			buffer.append(".$");
//			buffer.append(shortClassName);
//			buffer.append("$ (), ");

//			buffer.append(baseClassName);
//			buffer.append(".$");
//			buffer.append(shortClassName);
//			buffer.append("$ = function () {\r\n");
			
			List arguments = node.arguments();
			int argSize = arguments.size();
			ASTVariableVisitor variableVisitor = ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class));
			variableVisitor.isFinalSensible = true;
			
			int lastCurrentBlock = currentBlockForVisit;
			List finalVars = variableVisitor.finalVars;
			List visitedVars = variableVisitor.visitedVars;
			List normalVars = variableVisitor.normalVars;
			List lastVisitedVars = visitedVars;
			List lastNormalVars = normalVars;
			currentBlockForVisit = blockLevel;
			visitedVars = variableVisitor.visitedVars = new ArrayList();
			variableVisitor.normalVars = new ArrayList();
			anonDeclare.accept(this);
			buffer.append(", ");

			buffer.append("Clazz.innerTypeInstance (");
			buffer.append(anonClassName);
			buffer.append(", this, ");
			String scope = null;
			if (methodDeclareStack.size() != 0) {
				scope = (String) methodDeclareStack.peek();
			}
			variableVisitor.normalVars = lastNormalVars;
			buffer.append(listFinalVariables(visitedVars, ", ", scope));
			if (argSize > 0) {
				buffer.append(", ");
			}
			IMethodBinding methodDeclaration = null;
			IMethodBinding constructorBinding = node.resolveConstructorBinding();
			if (constructorBinding != null) {
				methodDeclaration = constructorBinding.getMethodDeclaration();
			}
			visitMethodParameterList(node.arguments(), methodDeclaration);

			//visitList(arguments, ", ");
			
			if (lastCurrentBlock != -1) {
				/* add the visited variables into last visited variables */
				for (int j = 0; j < visitedVars.size(); j++) {
					ASTFinalVariable fv = (ASTFinalVariable) visitedVars.get(j);
					int size = finalVars.size();
					for (int i = 0; i < size; i++) {
						ASTFinalVariable vv = (ASTFinalVariable) finalVars.get(size - i - 1);
						if (vv.variableName.equals(fv.variableName)
								&& vv.blockLevel <= lastCurrentBlock
								&& !lastVisitedVars.contains(vv)) {
							lastVisitedVars.add(vv);
						}
					}
				}
			}
			variableVisitor.visitedVars = lastVisitedVars;
			currentBlockForVisit = lastCurrentBlock;
			//buffer.append("};\r\n");
			buffer.append(")"); // Clazz.innerTypeInstance
			buffer.append(")"); // end of line (..., ...)

//			methodBuffer = buffer;
//			buffer = tmpBuffer;
		}
		return false;
	}

	protected void visitMethodParameterList(List arguments,
			IMethodBinding methodDeclaration, int begin, int end) {
		ITypeBinding[] parameterTypes = null;
		String clazzName = null;
		String methodName = null;
		/*
		Object[] ignores = new Object[] {
				"java.lang.String", "indexOf", new int[] { 0 },
				"java.lang.String", "lastIndexOf", new int[] { 0 },
				"java.lang.StringBuffer", "append", new int[] { 0 },
				"java.lang.StringBuilder", "append", new int[] { 0 }
		};
		*/
		if (methodDeclaration != null) {
			parameterTypes = methodDeclaration.getParameterTypes();
			ITypeBinding declaringClass = methodDeclaration.getDeclaringClass();
			if (declaringClass != null) {
				clazzName = declaringClass.getQualifiedName();
			}
			methodName = methodDeclaration.getName();
		}
		for (int i = begin; i < end; i++) {
			ASTNode element = (ASTNode) arguments.get(i);
			String typeStr = null;
			if (element instanceof CastExpression) {
				CastExpression castExp = (CastExpression) element;
				Expression exp = castExp.getExpression();
				if (exp instanceof NullLiteral) {
					ITypeBinding nullTypeBinding = castExp.resolveTypeBinding();
					if (nullTypeBinding != null) {
						if (nullTypeBinding.isArray()) {
							typeStr = "Array";
						} else if (nullTypeBinding.isPrimitive()) {
							Code code = PrimitiveType.toCode(nullTypeBinding.getName());
							if (code == PrimitiveType.BOOLEAN) {
								typeStr = "Boolean";
							} else{
								typeStr = "Number";
							}
						} else if (!nullTypeBinding.isTypeVariable()) {
							typeStr = assureQualifiedName(shortenQualifiedName(nullTypeBinding.getQualifiedName()));
						}
					}
				}
			}
			if (typeStr != null) {
				buffer.append("Clazz.castNullAs (\"");
				buffer.append(typeStr.replaceFirst("^\\$wt.", "org.eclipse.swt."));
				buffer.append("\")");
			} else {
				boxingNode(element);
				Expression exp = (Expression) element;
				ITypeBinding typeBinding = exp.resolveTypeBinding();
				String typeName = null;
				if (typeBinding != null) {
					typeName = typeBinding.getName();
				}
				String parameterTypeName = null;
				if (parameterTypes != null && parameterTypes.length > i) { // parameterTypes.length <= i, incorrect method invocation
					parameterTypeName = parameterTypes[i].getName();
				}
				if ("char".equals(typeName) && "int".equals(parameterTypeName)) {
					boolean ignored = false;
					/*
					for (int j = 0; j < ignores.length / 3; j++) {
						int[] indexes = (int[]) ignores[i + i + i + 2];
						boolean existed = false;
						for (int k = 0; k < indexes.length; k++) {
							if (indexes[k] == i) {
								existed = true;
								break;
							}
						}
						if (existed) {
							if (ignores[0].equals(Bindings.removeBrackets(clazzName)) && ignores[1].equals(methodName)) {
								ignored = true;
								break;
							}
						}
					}
					*/
					// Keep String#indexOf(int) and String#lastIndexOf(int)'s first char argument
					ignored = (i == 0 
							&& (/*"append".equals(methodName) || */"indexOf".equals(methodName) || "lastIndexOf".equals(methodName))
							&& ("java.lang.String".equals(Bindings.removeBrackets(clazzName))));
					if (!ignored) {
						buffer.append(".charCodeAt (0)");
					}
				}
			}
			if (i < end - 1) {
				buffer.append(", ");
			}
		}
	}

	protected void visitMethodParameterList(List arguments,
			IMethodBinding methodDeclaration) {
		visitMethodParameterList(arguments, methodDeclaration, 0, arguments.size());
	}

	public boolean visit(ConstructorInvocation node) {
		buffer.append("this.construct (");
		IMethodBinding methodDeclaration = null;
		IMethodBinding constructorBinding = node.resolveConstructorBinding();
		if (constructorBinding != null) {
			methodDeclaration = constructorBinding.getMethodDeclaration();
		}
		visitMethodParameterList(node.arguments(), methodDeclaration);
		//visitList(node.arguments(), ", ");
		buffer.append(");\r\n");
		return false;
	}
	public boolean visit(EnumConstantDeclaration node) {
		buffer.append("this.");
		node.getName().accept(this);
		buffer.append(" = ");
		node.getName().accept(this);
		buffer.append(";\r\n");
		return super.visit(node);
	}

	public void endVisit(EnumDeclaration node) {
		if (node != rootTypeNode && node.getParent() != null && node.getParent() instanceof AbstractTypeDeclaration) {
			return ;
		}
//		if (!node.isInterface()) {
			buffer.append("Clazz.instantialize (this, arguments);\r\n");
//		}

		buffer.append("}, ");
		
		
		String emptyFun = "Clazz.decorateAsClass (function () {\r\n" +
				"Clazz.instantialize (this, arguments);\r\n" +
				"}, ";
		int idx = buffer.lastIndexOf(emptyFun);
		
		if (idx != -1 && idx == buffer.length() - emptyFun.length()) {
			buffer.replace(idx, buffer.length(), "Clazz.declareType (");
		}

		String fullClassName = null;//getFullClassName();
		String packageName = ((ASTPackageVisitor) getAdaptable(ASTPackageVisitor.class)).getPackageName();
		String className = ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).getClassName();
		if (packageName != null && packageName.length() != 0) {
			fullClassName = packageName + '.' + className;
		} else {
			fullClassName = className;
		}
//		if (thisPackageName != null && thisPackageName.length() != 0) {
//			fullClassName = thisPackageName + '.' + thisClassName;
//		} else {
//			fullClassName = thisClassName;
//		}
		
		int lastIndexOf = fullClassName.lastIndexOf ('.');
		if (lastIndexOf != -1) {
			buffer.append(assureQualifiedName(shortenPackageName(fullClassName)));
			buffer.append(", \"" + fullClassName.substring(lastIndexOf + 1) + "\"");
		} else {
			buffer.append("null, \"" + fullClassName + "\"");
		}
		buffer.append(", Enum");

		List superInterfaces = node.superInterfaceTypes();
		int size = superInterfaces.size();
		if (size > 0) {
			buffer.append(", ");
		}
		if (size > 1) {
			buffer.append("[");
		}
		for (Iterator iter = superInterfaces.iterator(); iter
				.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			ITypeBinding binding = ((Type) element).resolveBinding();
			if (binding != null) {
				String clazzName = binding.getQualifiedName();
				clazzName = assureQualifiedName(shortenQualifiedName(clazzName));
				buffer.append(clazzName);
			} else {
				buffer.append(element);
			}
			if (iter.hasNext()) {
				buffer.append(", ");
			}
		}
		if (size > 1) {
			buffer.append("]");
		}
		buffer.append(");\r\n");

		buffer.append(laterBuffer);
		
//		EnumTypeWrapper enumWrapper = new EnumTypeWrapper(node);
//		
//		MethodDeclaration[] methods = enumWrapper.getMethods();
		List bd = node.bodyDeclarations();
		int methodCount = 0;
		for (Iterator it = bd.listIterator(); it.hasNext(); ) {
			if (it.next() instanceof MethodDeclaration) {
				methodCount++;
			}
		}
		MethodDeclaration[] methods = new MethodDeclaration[methodCount];
		int next = 0;
		for (Iterator it = bd.listIterator(); it.hasNext(); ) {
			Object decl = it.next();
			if (decl instanceof MethodDeclaration) {
				methods[next++] = (MethodDeclaration) decl;
			}
		}
		for (int i = 0; i < methods.length; i++) {
			methods[i].accept(this);
		}

		List bodyDeclarations = node.bodyDeclarations();
		
		boolean needPreparation = false;
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) element;
				needPreparation = isFieldNeedPreparation(field);
				if (needPreparation) {
					break;
				}
			} else if (element instanceof Initializer) {
				Initializer init = (Initializer) element;
				if ((init.getModifiers() & Modifier.STATIC) == 0) {
					needPreparation = true;
					break;
				}
			}
		}
		if (needPreparation) {
			buffer.append("Clazz.prepareFields (cla$$, function () {\r\n");
			for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
				ASTNode element = (ASTNode) iter.next();
				if (element instanceof FieldDeclaration) {
					FieldDeclaration field = (FieldDeclaration) element;
					if (!isFieldNeedPreparation(field)) {
						continue;
					}
					element.accept(this);
				} else if (element instanceof Initializer) {
					Initializer init = (Initializer) element;
					if ((init.getModifiers() & Modifier.STATIC) == 0) {
						element.accept(this);
					}
				}
			}
			buffer.append("};\r\n");
		}
		
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof Initializer) {
				element.accept(this);
				
			} else if (element instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) element;
			if ((field.getModifiers() & Modifier.STATIC) != 0) {
				List fragments = field.fragments();
				for (int j = 0; j < fragments.size(); j++) {
				//if (fragments.size () == 1) {
					/* replace full class name with short variable name */
					buffer.append("cla$$");
					//buffer.append(fullClassName);
					buffer.append(".");
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragments.get(j);
					//buffer.append(vdf.getName());
					vdf.getName().accept(this);
					buffer.append(" = ");
					Expression initializer = vdf.getInitializer();
					if (initializer != null) { 
						initializer.accept(this);
					} else {
						Type type = field.getType();
						if (type.isPrimitiveType()){
							PrimitiveType pType = (PrimitiveType) type;
							if (pType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
								buffer.append("false");
							} else {
								buffer.append("0");
							}
						} else {
							buffer.append("null");
						}
					}
					buffer.append(";\r\n");
				}
			}
		}
		}

		List constants = node.enumConstants();
		for (int i = 0; i < constants.size(); i++) {
			EnumConstantDeclaration enumConst = (EnumConstantDeclaration) constants.get(i);
			AnonymousClassDeclaration anonDeclare = enumConst.getAnonymousClassDeclaration();
			if (anonDeclare == null) {
				buffer.append("Clazz.defineEnumConstant (");
				/* replace full class name with short variable name */
				buffer.append("cla$$");
				//buffer.append(fullClassName);
				buffer.append(", \"");
				enumConst.getName().accept(this);
				buffer.append("\", " + i + ", [");
				visitList(enumConst.arguments(), ", ");
				buffer.append("]);\r\n");
				
			} else {
				ITypeBinding binding = node.resolveBinding();
				String anonClassName = null;
				if (binding.isAnonymous() || binding.isLocal()) {
					anonClassName = assureQualifiedName(shortenQualifiedName(binding.getBinaryName()));
				} else {
					anonClassName = assureQualifiedName(shortenQualifiedName(binding.getQualifiedName()));
				}
				//int anonCount = ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).getAnonymousCount() + 1;
				anonDeclare.accept(this);

				buffer.append("Clazz.defineEnumConstant (");
				/* replace full class name with short variable name */
				buffer.append("cla$$");
				//buffer.append(fullClassName);
				buffer.append(", \"");
				enumConst.getName().accept(this);
				buffer.append("\", " + i + ", [");
				visitList(enumConst.arguments(), ", ");
				buffer.append("], ");
//				buffer.append(getFullClassName());
//				buffer.append("$" + anonCount + ");\r\n");
				buffer.append(anonClassName);
				buffer.append(");\r\n");

			}
		}

		super.endVisit(node);
	}

	public boolean visit(EnumDeclaration node) {
		ITypeBinding binding = node.resolveBinding();
		ASTTypeVisitor typeVisitor = ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class));
		if (binding != null) {
			if (binding.isTopLevel()) {
				typeVisitor.setClassName(binding.getName());
			} else {
			}
		}
		if ((node != rootTypeNode) && node.getParent() != null && node.getParent() instanceof AbstractTypeDeclaration) {
			/* inner static class */
			ASTScriptVisitor visitor = null;
			try {
				visitor = (ASTScriptVisitor) this.getClass().newInstance();
			} catch (Exception e) {
				visitor = new ASTScriptVisitor(); // Default visitor
			}
			visitor.rootTypeNode = node;
//			visitor.thisClassName = thisClassName + "." + node.getName();
//			visitor.thisPackageName = thisPackageName;
			((ASTTypeVisitor) visitor.getAdaptable(ASTTypeVisitor.class)).setClassName(((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).getClassName());
			((ASTPackageVisitor) visitor.getAdaptable(ASTPackageVisitor.class)).setPackageName(((ASTPackageVisitor) getAdaptable(ASTPackageVisitor.class)).getPackageName());

			node.accept(visitor);
			if ((node.getModifiers() & Modifier.STATIC) != 0) {
				String str = visitor.getBuffer().toString();
				if (!str.startsWith("cla$$")) {
					laterBuffer.append(str);
				} else {
					laterBuffer.append("Clazz.pu$h ();\r\n");
					laterBuffer.append(str);
					laterBuffer.append("cla$$ = Clazz.p0p ();\r\n");
				}
			} else {
				/*
				 * Never reach here!
				 * March 17, 2006
				 */
				buffer.append("if (!Clazz.isClassDefined (\"");
				buffer.append(visitor.getFullClassName());
				buffer.append("\")) {\r\n");
				
				//String className = typeVisitor.getClassName();
				methodBuffer.append("cla$$.$");
				String targetClassName = visitor.getClassName();
				//String prefixKey = className + ".";
				//if (targetClassName.startsWith(prefixKey)) {
				//	targetClassName = targetClassName.substring(prefixKey.length());
				//}
				targetClassName = targetClassName.replace('.', '$');
				methodBuffer.append(targetClassName);
				methodBuffer.append("$ = function () {\r\n");
				methodBuffer.append("Clazz.pu$h ();\r\n");
				methodBuffer.append(visitor.getBuffer().toString());
				methodBuffer.append("cla$$ = Clazz.p0p ();\r\n");
				methodBuffer.append("};\r\n");

				buffer.append(visitor.getPackageName());
				buffer.append(".");
				buffer.append(targetClassName);
				buffer.append(".$");
				buffer.append(visitor.getClassName());
				buffer.append("$ ();\r\n");
				
				buffer.append("}\r\n");
			}
			return false;
		}
		buffer.append("cla$$ = ");
		
		buffer.append("Clazz.decorateAsClass (");
		
		buffer.append("function () {\r\n");
		
		List bodyDeclarations = node.bodyDeclarations();

		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof MethodDeclaration) {
				//MethodDeclaration method = (MethodDeclaration) element;
				//if ((method.getModifiers() & Modifier.STATIC) != 0) {
					continue;
				//}
			} else if (element instanceof Initializer) {
				continue;
			} else if (element instanceof FieldDeclaration 
					/*&& isFieldNeedPreparation((FieldDeclaration) element)*/) {
				//if (node.isInterface()) {
					/*
					 * As members of interface should be treated
					 * as final and for javascript interface won't
					 * get instantiated, so the member will be
					 * treated specially. 
					 */
					//continue;
				//}
				FieldDeclaration fieldDeclaration = (FieldDeclaration) element;
				if (isFieldNeedPreparation(fieldDeclaration)) {
					visitWith(fieldDeclaration, true);
					continue;
				}
			}
			element.accept(this);
		}
		return false;
	}


	public boolean visit(FieldAccess node) {
		/*
		 * TODO: more complicated rules should be considered.
		 * read the JavaDoc
		 */
		node.getExpression().accept(this);
		buffer.append(".");
		node.getName().accept(this);
		return false;
	}

	public boolean visit(FieldDeclaration node) {
		return visitWith(node, false);
	}
	public boolean visitWith(FieldDeclaration node, boolean ignoreInitializer) {
		if ((node.getModifiers() & Modifier.STATIC) != 0) {
			return false;
		}
		ASTNode xparent = node.getParent();
		while (xparent != null 
				&& !(xparent instanceof AbstractTypeDeclaration)
				&& !(xparent instanceof AnonymousClassDeclaration)) {
			xparent = xparent.getParent();
		}
		ITypeBinding typeBinding = null;
		//ITypeBinding anonBinding = null;
		if (xparent != null) {
			if (xparent instanceof AbstractTypeDeclaration) {
				AbstractTypeDeclaration type = (AbstractTypeDeclaration) xparent;
				typeBinding = type.resolveBinding();
			} else if (xparent instanceof AnonymousClassDeclaration) {
				AnonymousClassDeclaration type = (AnonymousClassDeclaration) xparent;
				typeBinding = type.resolveBinding();//.getSuperclass();
			}
		}

		List fragments = node.fragments();
		for (Iterator iter = fragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment element = (VariableDeclarationFragment) iter.next();
			String fieldName = getJ2SName(element.getName());
//			String fieldName = element.getName().getIdentifier();
			String ext = "";
			if (checkKeyworkViolation(fieldName)) {
				ext += "$";
			}
			if (typeBinding != null 
					&& checkSameName(typeBinding, fieldName)) {
				ext += "$";
			}
			//fieldName = ext + fieldName;
			//buffer.append(fieldName);
			buffer.append("this.");
			if (isInheritedFieldName(typeBinding, fieldName)) {
				fieldName = getFieldName(typeBinding, fieldName);
				buffer.append(ext + fieldName);
			} else {
				buffer.append(ext + fieldName);
			}
			//buffer.append(element.getName());
			buffer.append(" = ");
				if (!ignoreInitializer && element.getInitializer() != null) {
					element.getInitializer().accept(this);
				} else {
					boolean isArray = false;
					List frags = node.fragments();
					if (frags.size() > 0) {
						VariableDeclarationFragment varFrag = (VariableDeclarationFragment) frags.get(0);
						IVariableBinding resolveBinding = varFrag.resolveBinding();
						if (resolveBinding != null) {
							isArray = resolveBinding.getType().isArray();
							if (isArray) {
								buffer.append("null");
							}
						}
					}
					if (!isArray) {
						if (node.getType().isPrimitiveType()){
							PrimitiveType pType = (PrimitiveType) node.getType();
							if (pType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
								buffer.append("false");
							} else {
								buffer.append("0");
							}
						} else {
							buffer.append("null");
						}
					}
				}
			buffer.append(";\r\n");
		}
		return false;
	}


	private boolean checkSimpleBooleanOperator(String op) {
		if (op.equals("^")
				|| op.equals("|")
				|| op.equals("&")) {
			return true;
		}
		return false;
	}
	
	private boolean checkInfixOperator(InfixExpression node) {
		if (checkSimpleBooleanOperator(node.getOperator().toString())) {
			return true;
		}
		Expression left = node.getLeftOperand();
		if (left instanceof InfixExpression) {
			if (checkInfixOperator((InfixExpression) left)) {
				return true;
			}
		}
		Expression right = node.getRightOperand();
		if (right instanceof InfixExpression) {
			if (checkInfixOperator((InfixExpression) right)) {
				return true;
			}
		}
		return false;
	}
	
	private void charVisit(ASTNode node, boolean beCare) {
		if (!beCare || !(node instanceof Expression)) {
			boxingNode(node);
			return ;
		}
		ITypeBinding binding = ((Expression) node).resolveTypeBinding();
		if (binding.isPrimitive() && "char".equals(binding.getName())) {
			buffer.append("(");
			boxingNode(node);
			buffer.append(").charCodeAt (0)");
		} else {
			boxingNode(node);
		}
	}
	public boolean visit(InfixExpression node) {
		String constValue = checkConstantValue(node);
		if (constValue != null) {
			buffer.append(constValue);
			return false;
		}
		ITypeBinding expTypeBinding = node.resolveTypeBinding();
		boolean beCare = false;
		if (expTypeBinding != null 
				&& expTypeBinding.getName().indexOf("String") == -1) {
			beCare = true;
		}
		String operator = node.getOperator().toString();
		Expression left = node.getLeftOperand();
		ITypeBinding typeBinding = left.resolveTypeBinding();
		if ("/".equals(operator)) {
			if (typeBinding != null && typeBinding.isPrimitive()) {
				if (isIntegerType(typeBinding.getName())) {
					Expression right = node.getRightOperand();
					ITypeBinding rightTypeBinding = right.resolveTypeBinding();
					if (isIntegerType(rightTypeBinding.getName())) {
						StringBuffer tmpBuffer = buffer;
						buffer = new StringBuffer();
						
						buffer.append("Math.floor (");
						charVisit(left, beCare);
						buffer.append(' ');
						buffer.append(operator);
						buffer.append(' ');
						charVisit(right, beCare);
						buffer.append(')');
						List extendedOperands = node.extendedOperands();
						if (extendedOperands.size() > 0) {
							for (Iterator iter = extendedOperands.iterator(); iter.hasNext();) {
								ASTNode element = (ASTNode) iter.next();
								boolean is2Floor = false;
								if (element instanceof Expression) {
									Expression exp = (Expression) element;
									ITypeBinding expBinding = exp.resolveTypeBinding();
									if (isIntegerType(expBinding.getName())) {
										buffer.insert(0, "Math.floor (");
										is2Floor = true;
									}
								}
								buffer.append(' ');
								buffer.append(operator);
								buffer.append(' ');
								charVisit(element, beCare);
								if (is2Floor) {
									buffer.append(')');
								}
							}
						}

						tmpBuffer.append(buffer);
						buffer = tmpBuffer;
						tmpBuffer = null;
						
						return false;
					}
				}
			}
		}
		boolean simple = false; 
		if (typeBinding != null && typeBinding.isPrimitive()) {
			if ("boolean".equals(typeBinding.getName())) {
				if (checkInfixOperator(node)) {
					buffer.append(" new Boolean (");
					simple = true;
				}
			}
		}

		charVisit(node.getLeftOperand(), beCare);
		buffer.append(' ');
		buffer.append(operator);
		if ("==".equals(operator) || "!=".equals(operator)) {
			if (typeBinding != null && !typeBinding.isPrimitive()
					&& !(node.getLeftOperand() instanceof NullLiteral)
					&& !(node.getRightOperand() instanceof NullLiteral)
					/*&& !(node.getLeftOperand() instanceof StringLiteral) // "abc" == ...
					&& !(node.getRightOperand() instanceof StringLiteral)*/) {
				buffer.append('=');
			}
		}
		buffer.append(' ');
		charVisit(node.getRightOperand(), beCare);
		List extendedOperands = node.extendedOperands();
		if (extendedOperands.size() > 0) {
			for (Iterator iter = extendedOperands.iterator(); iter.hasNext();) {
				buffer.append(' ');
				buffer.append(operator);
				buffer.append(' ');
				ASTNode element = (ASTNode) iter.next();
				charVisit(element, beCare);
			}
		}
		if (simple) {
			buffer.append(").valueOf ()");
		}
		return false;
	}

	public boolean visit(Initializer node) {
		if (getJ2STag(node, "@j2sIgnore") != null) {
			return false;
		}
		//visitList(node.getBody().statements(), "\r\n");
		node.getBody().accept(this);
		return false;
	}

	public void endVisit(MethodDeclaration node) {
		if (getJ2STag(node, "@j2sIgnore") != null) {
			addAnonymousClassDeclarationMethods();
			return;
		}

		IMethodBinding mBinding = node.resolveBinding();
		if (Bindings.isMethodInvoking(mBinding, "net.sf.j2s.ajax.SimpleRPCRunnable", "ajaxRun")) {
			if (getJ2STag(node, "@j2sKeep") == null) {
				addAnonymousClassDeclarationMethods();
				return;
			}
		}
		String[] pipeMethods = new String[] {
				"pipeSetup", 
				"pipeThrough", 
				"through",
				"pipeMonitoring",
				"pipeMonitoringInterval",
				"pipeWaitClosingInterval",
				"setPipeHelper"
		};
		for (int i = 0; i < pipeMethods.length; i++) {
			if (Bindings.isMethodInvoking(mBinding, "net.sf.j2s.ajax.SimplePipeRunnable", pipeMethods[i])) {
				if (getJ2STag(node, "@j2sKeep") == null) {
					addAnonymousClassDeclarationMethods();
					return;
				}
			}
		}
		if (Bindings.isMethodInvoking(mBinding, "net.sf.j2s.ajax.CompoundPipeSession", "convert")) {
			if (getJ2STag(node, "@j2sKeep") == null) {
				addAnonymousClassDeclarationMethods();
				return;
			}
		}
		if (mBinding != null) {
			methodDeclareStack.pop();
		}
		super.endVisit(node);
		addAnonymousClassDeclarationMethods();
	}
	
	protected void addAnonymousClassDeclarationMethods() {
//		if (methodBuffer != null && methodBuffer.length() != 0) {
//			buffer.append(methodBuffer.toString());
//			methodBuffer = null;
//		}
	}
	protected String[] getFilterMethods() {
		return new String[0];
	}

	public boolean visit(MethodDeclaration node) {
//		methodBuffer = new StringBuffer();
		if (getJ2STag(node, "@j2sIgnore") != null) {
			return false;
		}

		IMethodBinding mBinding = node.resolveBinding();
		if (Bindings.isMethodInvoking(mBinding, "net.sf.j2s.ajax.SimpleRPCRunnable", "ajaxRun")) {
			if (getJ2STag(node, "@j2sKeep") == null) {
				return false;
			}
		}
		String[] pipeMethods = new String[] {
				"pipeSetup", 
				"pipeThrough", 
				"through",
				"pipeMonitoring",
				"pipeMonitoringInterval",
				"pipeWaitClosingInterval",
				"setPipeHelper"
		};
		for (int i = 0; i < pipeMethods.length; i++) {
			if (Bindings.isMethodInvoking(mBinding, "net.sf.j2s.ajax.SimplePipeRunnable", pipeMethods[i])) {
				if (getJ2STag(node, "@j2sKeep") == null) {
					return false;
				}
			}
		}
		if (Bindings.isMethodInvoking(mBinding, "net.sf.j2s.ajax.CompoundPipeSession", "convert")) {
			if (getJ2STag(node, "@j2sKeep") == null) {
				return false;
			}
		}
		if (mBinding != null) {
			methodDeclareStack.push(mBinding.getKey());
		}
		
		if (node.getBody() == null) {
			/*
			 * Abstract or native method
			 */
			if (isMethodNativeIgnored(node)) {
				return false;
			}
		}
		/*
		 * To skip those methods or constructors which are just overriding with
		 * default super methods or constructors. 
		 */
		Block body = node.getBody();
		boolean needToCheckArgs = false;
		List argsList = null;
		if (body != null && containsOnlySuperCall(body)) {
			List sts = body.statements();
			Object statement = sts.get(sts.size() - 1);
			if (statement instanceof ReturnStatement) {
				ReturnStatement ret = (ReturnStatement) statement;
				Expression exp = ret.getExpression();
				if (exp instanceof SuperMethodInvocation) {
					SuperMethodInvocation superRet = (SuperMethodInvocation) exp;
					if (superRet.getName().toString().equals(node.getName().toString())) {
						// same method name
						needToCheckArgs = true;
						argsList = superRet.arguments();
					}
				}
			} else if (statement instanceof ExpressionStatement) {
				ExpressionStatement sttmt = (ExpressionStatement) statement;
				Expression exp = sttmt.getExpression();
				if (exp instanceof SuperMethodInvocation) {
					SuperMethodInvocation superRet = (SuperMethodInvocation) exp;
					if (superRet.getName().toString().equals(node.getName().toString())) {
						// same method name
						needToCheckArgs = true;
						argsList = superRet.arguments();
					}
				}
			} else if (statement instanceof SuperConstructorInvocation) {
				SuperConstructorInvocation superConstructor = (SuperConstructorInvocation) statement;
				needToCheckArgs = true;
				argsList = superConstructor.arguments();
				if (argsList.size() == 0) {
					IMethodBinding constructorBinding = superConstructor.resolveConstructorBinding();
					ITypeBinding declaringClass = constructorBinding.getDeclaringClass();
					if ("java.lang.Object".equals(declaringClass.getQualifiedName())) {
						needToCheckArgs = false;
					}
				}
			}
//		} else if (node.isConstructor() && (body != null && body.statements().size() == 0)) {
//			IMethodBinding superConstructorExisted = Bindings.findConstructorInHierarchy(mBinding.getDeclaringClass(), mBinding);
//			if (superConstructorExisted != null) {
//				needToCheckArgs = true;
//				argsList = new ArrayList();
//			}
		}
		if (needToCheckArgs) {
			List params = node.parameters();
			if (params.size() == argsList.size()) {
				// same parameters count
				boolean isOnlySuper = true;
				for (Iterator iter = params.iterator(), itr = argsList.iterator(); iter.hasNext();) {
					ASTNode astNode = (ASTNode) iter.next();
					ASTNode argNode = (ASTNode) itr.next();
					if (astNode instanceof SingleVariableDeclaration
							&& argNode instanceof SimpleName) {
						SingleVariableDeclaration varDecl = (SingleVariableDeclaration) astNode;
						String paramID = varDecl.getName().getIdentifier();
						String argID = ((SimpleName) argNode).getIdentifier();
						if (!paramID.equals(argID)) {
							// not with the same order, break out
							isOnlySuper = false;
							break;
						}
					} else {
						isOnlySuper = false;
						break;
					}
				}
				if (isOnlySuper && getJ2STag(node, "@j2sKeep") == null) {
					return false;
				}
			}
		}
		if ((node.getModifiers() & Modifier.PRIVATE) != 0) {
			if(mBinding != null){
				boolean isReferenced = MethodReferenceASTVisitor.checkReference(node.getRoot(), 
						mBinding.getKey());
				if (!isReferenced && getJ2STag(node, "@j2sKeep") == null) {
					return false;
				}
			}
		}
		
		if (node.isConstructor()) {
			buffer.append("Clazz.makeConstructor (");
		} else {
			if ((node.getModifiers() & Modifier.STATIC) != 0) {
				/* replace full class name with short variable name */
				buffer.append("cla$$");
				//buffer.append(fullClassName);
				buffer.append(".");
				//buffer.append(methods[i].getName());
				node.getName().accept(this);
				buffer.append(" = ");
			}
			if (getJ2STag(node, "@j2sOverride") != null) {
				buffer.append("Clazz.overrideMethod (");
			} else {
				boolean isOK2AutoOverriding = canAutoOverride(node);
				if (isOK2AutoOverriding) {
					buffer.append("Clazz.overrideMethod (");
				} else {
					buffer.append("Clazz.defineMethod (");
				}
			}
		}
		/* replace full class name with short variable name */
		buffer.append("cla$$");
		
		if (node.isConstructor()) {
			buffer.append(", ");
		} else {
			buffer.append(", \"");
			String identifier = getJ2SName(node.getName());
			if (checkKeyworkViolation(identifier)) {
				buffer.append('$');
			}
			buffer.append(identifier);
			buffer.append("\", ");
		}
		buffer.append("\r\n");
		boolean isPrivate = (node.getModifiers() & Modifier.PRIVATE) != 0;
		if (isPrivate) {
			buffer.append("($fz = ");
		}		
		buffer.append("function (");
		List parameters = node.parameters();
		visitList(parameters, ", ");
		buffer.append(") ");
		if (node.isConstructor()) {
			boolean isSuperCalled = false;
			List statements = node.getBody().statements();
			if (statements.size() > 0) {
				ASTNode firstStatement = (ASTNode) statements.get(0);
				if (firstStatement instanceof SuperConstructorInvocation
						|| firstStatement instanceof ConstructorInvocation) {
					isSuperCalled = true;
				}
			}
			if (getJ2STag(node, "@j2sIgnoreSuperConstructor") != null) {
				isSuperCalled = true;
			}
			boolean existedSuperClass = false;
			IMethodBinding binding = node.resolveBinding();
			if (binding != null) {
				ITypeBinding declaringClass = binding.getDeclaringClass();
				ITypeBinding superclass = declaringClass.getSuperclass();
				String qualifiedName = discardGenericType(superclass.getQualifiedName());
				existedSuperClass = superclass != null 
						&& !"java.lang.Object".equals(qualifiedName)
						&& !"java.lang.Enum".equals(qualifiedName);
			}
			if (!isSuperCalled && existedSuperClass) {
				buffer.append("{\r\n");
				buffer.append("Clazz.superConstructor (this, ");
				buffer.append(assureQualifiedName(shortenQualifiedName(getFullClassName())));
				buffer.append(", []);\r\n");
				boolean read = checkJ2STags(node, false);
				if (!read) {
					blockLevel++;
					visitList(statements, ""); 
					//buffer.append("}");
					endVisit(node.getBody());
				} else {
					buffer.append("}");
				}
			} else {
				boolean read = checkJ2STags(node, true);
				if (!read) {
					node.getBody().accept(this);
				}
			}
		} else if (node.getBody() == null) {
			blockLevel++;
			boolean read = checkJ2STags(node, true);
			if (!read) {
				buffer.append("{\r\n");
				visitNativeJavadoc(node.getJavadoc(), null, false);
				buffer.append("}");
			}
			List normalVars = ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).normalVars;
			for (int i = normalVars.size() - 1; i >= 0; i--) {
				ASTFinalVariable var = (ASTFinalVariable) normalVars.get(i);
				if (var.blockLevel >= blockLevel) {
					normalVars.remove(i);
				}
			}
			blockLevel--;
		} else {
			boolean read = checkJ2STags(node, true);
			if (!read) {
				node.getBody().accept(this);
			}
		}
		if (isPrivate) {
			buffer.append(", $fz.isPrivate = true, $fz)");
		}		
		if (parameters.size() != 0) {
			buffer.append(", \"");
			for (Iterator iter = parameters.iterator(); iter.hasNext();) {
				SingleVariableDeclaration element = (SingleVariableDeclaration) iter.next();
				boolean isArray = false;
				IBinding resolveBinding = element.getName().resolveBinding();
				if (resolveBinding instanceof IVariableBinding) {
					IVariableBinding varBinding = (IVariableBinding) resolveBinding;
					if (varBinding != null) {
						isArray = varBinding.getType().isArray();
						if (isArray) {
							//buffer.append("Array");
							buffer.append("~A");
						}
					}
				}
				if (!isArray) {
					Type type = element.getType();
					if (type.isPrimitiveType()){
						PrimitiveType pType = (PrimitiveType) type;
						if (pType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
							buffer.append("~B"); // Boolean
						} else {
							buffer.append("~N"); // Number
						}
					} else if (type.isArrayType()) {
						buffer.append("~A"); // Array
					} else {
						ITypeBinding binding = type.resolveBinding();
						if (binding != null) {
							if (binding.isTypeVariable()) {
								buffer.append("~O");
							} else {
								String name = binding.getQualifiedName();
								name = shortenQualifiedName(name);
								if ("String".equals(name)) {
									buffer.append("~S");
								} else if ("Object".equals(name)) {
									buffer.append("~O");
								} else {
									buffer.append(name);
								}
							}
						} else {
							buffer.append(type);
						}
					}
				}
				if (iter.hasNext()) {
					buffer.append(",");
				}
			}
			buffer.append("\"");
		}
		buffer.append(");\r\n");
		return false;
	}

	/*
	 * Check to see whether there are @j2s* and append sources to buffer
	 */
	private boolean checkJ2STags(MethodDeclaration node, boolean needScope) {
		String prefix = "{\r\n";
		String suffix = "\r\n}";
		if (!needScope) {
			prefix = "";
			suffix = "";
		}
		boolean read = false;
		if (isDebugging()) {
			read = readSources(node, "@j2sDebug", prefix, suffix, false);
		}
		if (!read) {
			boolean toCompileVariableName = ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).isToCompileVariableName();
			if (!toCompileVariableName) {
				read = readSources(node, "@j2sNativeSrc", prefix, suffix, false);
			}
		}
		if (!read) {
			read = readSources(node, "@j2sNative", prefix, suffix, false);
		}
		return read;
	}

	private boolean containsOnlySuperCall(Block body) {
		boolean isOnlyOneCall = false;
		List ss = body.statements();
		int size = ss.size();
		if (size == 1) {
			isOnlyOneCall = true;
		} else {
			/*
			 * If all method invocations before super call is filtered, then super call
			 * is still considered as the only one.
			 * 
			 * For example, the filtered methods may be:
			 * checkWidget();
			 * checkDevice();
			 */
			String[] filterMethods = getFilterMethods();
			if (filterMethods.length > 0 && size > 1) {
				Object obj = ss.get(size - 1);
				if (obj instanceof ExpressionStatement) {
					ExpressionStatement smt = (ExpressionStatement) obj;
					Expression e = smt.getExpression();
					if (e instanceof SuperMethodInvocation) { // the last is super call
						isOnlyOneCall = true;
						for (int i = 0; i < size - 1; i++) { // check previous calls
							Object statement = ss.get(i);
							MethodInvocation method = null;
							if (statement instanceof ExpressionStatement) {
								ExpressionStatement sttmt = (ExpressionStatement) statement;
								Expression exp = sttmt.getExpression();
								if (exp instanceof MethodInvocation) {
									method = (MethodInvocation) exp;
								}
							} else if (statement instanceof IfStatement) { // if (...) checkWidget();
								IfStatement ifSss = (IfStatement) statement;
								if (ifSss.getElseStatement() == null) {
									Statement thenStatement = ifSss.getThenStatement();
									if (thenStatement instanceof Block) {
										Block block = (Block) thenStatement;
										List statements = block.statements();
										if (statements.size() == 1) {
											thenStatement = (Statement) statements.get(0);
										}
									}
									if (thenStatement instanceof ExpressionStatement) {
										ExpressionStatement expStmt = (ExpressionStatement) thenStatement;
										Expression exp = expStmt.getExpression();
										if (exp instanceof MethodInvocation) {
											method = (MethodInvocation) exp;
										}
									}
								}
							}
							if (method != null) {
								boolean isFiltered = false;
								IMethodBinding methodBinding = method.resolveMethodBinding();
								for (int j = 0; j < filterMethods.length; j += 2) {
									if ("*".equals(filterMethods[i + 1])) {
										if (methodBinding == null) {
											continue;
										}
										ITypeBinding type = methodBinding.getDeclaringClass();
										if (type != null && filterMethods[i].equals(type.getQualifiedName())) {
											isFiltered = true;
											break;
										}
									} else if (Bindings.isMethodInvoking(methodBinding, filterMethods[j], filterMethods[j + 1])) {
										isFiltered = true;
										break;
									}
								}
								if (isFiltered) {
									continue;
								}
							}
							isOnlyOneCall = false;
							break;
						}
					}
				}
			}
		}
		return isOnlyOneCall;
	}

	public boolean visit(MethodInvocation node) {
		Expression expression = node.getExpression();
		if (expression != null) {
			/*
			 * Here?
			 */
			expression.accept(this);
			buffer.append(".");
		}
		
		String methodName = node.getName().getIdentifier();
		List args = node.arguments();
		int size = args.size();
		boolean isSpecialMethod = false;
		if (isMethodRegistered(methodName) 
				&& (size == 0 || methodName.equals("split") || methodName.equals("replace"))) {
			IBinding binding = node.getName().resolveBinding();
			if (binding != null && binding instanceof IMethodBinding) {
				IMethodBinding mthBinding = (IMethodBinding) binding;
				String className = mthBinding.getDeclaringClass().getQualifiedName();
				String propertyName = translate(className, methodName);
				if (propertyName != null) {
					if (propertyName.startsWith("~")) {
						buffer.append('$');
						buffer.append(propertyName.substring(1));
						isSpecialMethod = true;
					} else {
						buffer.append(propertyName);
						return false;
					}
				}
			}
		}
		if (!isSpecialMethod) {
			node.getName().accept(this);
		}
		buffer.append(" (");

		IMethodBinding methodBinding = node.resolveMethodBinding();
		if (methodBinding != null && methodBinding.isVarargs()) {
			ITypeBinding[] paramTypes = methodBinding.getParameterTypes();
			visitList(args, ", ", 0, paramTypes.length - 1);
			if (paramTypes.length - 1 > 0) {
				buffer.append(", ");
			}
			boolean needBrackets = true;
			//if (args.size() == 1) {
				Expression arg = (Expression) args.get(args.size() - 1);
				ITypeBinding resolveTypeBinding = arg.resolveTypeBinding();
				if (resolveTypeBinding != null && resolveTypeBinding.isArray()) {
					needBrackets = false;
				}
			//}
			if (needBrackets) buffer.append("[");
			//IMethodBinding methodDeclaration = node.resolveMethodBinding();
			//visitMethodParameterList(node.arguments(), methodDeclaration, paramTypes.length - 1, size);
			visitList(args, ", ", paramTypes.length - 1, size);
			if (needBrackets) buffer.append("]");
		} else {
			IMethodBinding methodDeclaration = node.resolveMethodBinding();
			visitMethodParameterList(node.arguments(), methodDeclaration);
			//visitList(args, ", ");
		}
		buffer.append(")");
		return false;
	}

	public boolean visit(SimpleName node) {
		String constValue = checkConstantValue(node);
		if (constValue != null) {
			buffer.append(constValue);
			return false;
		}
		IBinding binding = node.resolveBinding();
		if (binding != null
				&& binding instanceof ITypeBinding) {
			ITypeBinding typeBinding = (ITypeBinding) binding;
			if (typeBinding != null) {
				String name = typeBinding.getQualifiedName();
				if (name.startsWith("org.eclipse.swt.internal.xhtml")) {
					buffer.append(node.getIdentifier());
					return false;
				}
			}
		}
		ASTNode xparent = node.getParent();
		if (xparent == null) {
			buffer.append(node);
			return false;
		}
		char ch = 0;
		if (buffer.length() > 0) {
			ch = buffer.charAt(buffer.length() - 1);
		}
		if (ch == '.' && xparent instanceof QualifiedName) {
			if (binding != null && binding instanceof IVariableBinding) {
				IVariableBinding varBinding = (IVariableBinding) binding;
				IVariableBinding variableDeclaration = varBinding.getVariableDeclaration();
				ITypeBinding declaringClass = variableDeclaration.getDeclaringClass();
				String fieldName = getJ2SName(node);
				if (checkSameName(declaringClass, fieldName)) {
					buffer.append('$');
				}
				if (checkKeyworkViolation(fieldName)) {
					buffer.append('$');
				}
				if (declaringClass != null
						&& isInheritedFieldName(declaringClass, fieldName)) {
					fieldName = getFieldName(declaringClass, fieldName);
				}
				buffer.append(fieldName);
				return false;
			}
			buffer.append(node);
			return false;
		}
		if (xparent instanceof ClassInstanceCreation 
				&& !(binding instanceof IVariableBinding)) {
			ITypeBinding binding2 = node.resolveTypeBinding();
			if (binding != null) {
				String name = binding2.getQualifiedName();
				name = assureQualifiedName(shortenQualifiedName(name));
				buffer.append(name);
			} else {
				String nodeId = getJ2SName(node);
				buffer.append(assureQualifiedName(shortenQualifiedName(nodeId)));
			}
			return false;
		}
		if (binding == null) {
			String name = getJ2SName(node);
			name = shortenQualifiedName(name);
			if (checkKeyworkViolation(name)) {
				buffer.append('$');
			}
			buffer.append(name);
			return false;
		}
		if (binding instanceof IVariableBinding) {
			IVariableBinding varBinding = (IVariableBinding) binding;
			simpleNameInVarBinding(node, ch, varBinding);
		} else if (binding instanceof IMethodBinding) {
			IMethodBinding mthBinding = (IMethodBinding) binding;
			simpleNameInMethodBinding(node, ch, mthBinding);
		} else {
			ITypeBinding typeBinding = node.resolveTypeBinding();
//				String name = NameConverterUtil.getJ2SName(node);
			if (typeBinding != null) {
				String name = typeBinding.getQualifiedName();
				name = assureQualifiedName(shortenQualifiedName(name));
				if (checkKeyworkViolation(name)) {
					buffer.append('$');
				}
				buffer.append(name);
			} else {
				String name = node.getFullyQualifiedName();
				if (checkKeyworkViolation(name)) {
					buffer.append('$');
				}
				buffer.append(name);
			}
		}
		return false;
	}

	private void simpleNameInVarBinding(SimpleName node, char ch, IVariableBinding varBinding) {
		String thisClassName = getClassName();
		if ((varBinding.getModifiers() & Modifier.STATIC) != 0) {
			IVariableBinding variableDeclaration = varBinding.getVariableDeclaration();
			ITypeBinding declaringClass = variableDeclaration.getDeclaringClass();
			if (ch != '.' && ch != '\"'
					&& declaringClass != null) {
				String name = declaringClass.getQualifiedName();
				if ((name == null || name.length() == 0) 
						&& declaringClass.isAnonymous()) {
					// TODO: FIXME: I count the anonymous class name myself
					// and the binary name of the anonymous class will conflict
					// with my anonymous class name!
					name = declaringClass.getBinaryName();
				}
				name = assureQualifiedName(shortenQualifiedName(name));
				if (name.length() != 0) {
					buffer.append(name);
					buffer.append(".");
				}
			}
			String fieldName = getJ2SName(node);
			if (checkSameName(declaringClass, fieldName)) {
				buffer.append('$');
			}
			if (checkKeyworkViolation(fieldName)) {
				buffer.append('$');
			}
			if (declaringClass != null
					&& isInheritedFieldName(declaringClass, fieldName)) {
				fieldName = getFieldName(declaringClass, fieldName);
			}
			buffer.append(fieldName);				
		} else {
			ASTNode parent = node.getParent();
			if (parent != null && !(parent instanceof FieldAccess)) {
				IVariableBinding variableDeclaration = varBinding.getVariableDeclaration();
				ITypeBinding declaringClass = variableDeclaration.getDeclaringClass();
				if (declaringClass != null && thisClassName != null && ch != '.') {
					appendFieldName(parent, declaringClass);
				}
			}
			
			String fieldVar = null;
			if (((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).isFinalSensible 
					&& (varBinding.getModifiers() & Modifier.FINAL) != 0 
					&& varBinding.getDeclaringMethod() != null) {
				String key = varBinding.getDeclaringMethod().getKey();
				if (methodDeclareStack.size() == 0 || !key.equals(methodDeclareStack.peek())) {
					buffer.append("this.$finals.");
					if (currentBlockForVisit != -1) {
						List finalVars = ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).finalVars;
						List visitedVars = ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).visitedVars;
						int size = finalVars.size();
						for (int i = 0; i < size; i++) {
							ASTFinalVariable vv = (ASTFinalVariable) finalVars.get(size - i - 1);
							if (vv.variableName.equals(varBinding.getName())
									&& vv.blockLevel <= currentBlockForVisit) {
								if (!visitedVars.contains(vv)) {
									visitedVars.add(vv);
								}
								fieldVar = vv.toVariableName;
							}
						}
					}
				}
			}

			IVariableBinding variableDeclaration = varBinding.getVariableDeclaration();
			ITypeBinding declaringClass = variableDeclaration.getDeclaringClass();
//					String fieldName = node.getFullyQualifiedName();
			String fieldName = null;
			if (declaringClass != null) {
				fieldName = getJ2SName(node);
			} else if (fieldVar == null) {
				fieldName = getVariableName(node.getIdentifier());
			} else {
				fieldName = fieldVar;
			}
			//System.err.println(fieldName);
			if (checkKeyworkViolation(fieldName)) {
				buffer.append('$');
			}
			if (declaringClass != null 
					&& checkSameName(declaringClass, fieldName)) {
				buffer.append('$');
			}
			if (declaringClass != null
					&& isInheritedFieldName(declaringClass, fieldName)) {
				fieldName = getFieldName(declaringClass, fieldName);
			}
			buffer.append(fieldName);
		}
	}

	private void simpleNameInMethodBinding(SimpleName node, char ch, IMethodBinding mthBinding) {
		String thisClassName = getClassName();
		if ((mthBinding.getModifiers() & Modifier.STATIC) != 0) {
			IMethodBinding variableDeclaration = mthBinding.getMethodDeclaration();
			ITypeBinding declaringClass = variableDeclaration.getDeclaringClass();
			boolean isClassString = false;
			if (declaringClass != null) {
				isClassString = "java.lang.String".equals(declaringClass.getQualifiedName());
				ASTNode parent = node.getParent();
				if (parent instanceof MethodInvocation) {
					MethodInvocation mthInv = (MethodInvocation) parent;
					if (mthInv.getExpression() == null) {
						String name = declaringClass.getQualifiedName();
						name = assureQualifiedName(shortenQualifiedName(name));
						if (name.length() != 0) {
							buffer.append(name);
							buffer.append(".");
						}
					}
				}
			}
//					String name = variableDeclaration.getName();
			String name = getJ2SName(node);
			name = shortenQualifiedName(name);
			if (!(isClassString && "valueOf".equals(name)) && checkKeyworkViolation(name)) {
				buffer.append('$');
			}
			buffer.append(name);
		} else {
			ASTNode parent = node.getParent();
			boolean isClassString = false;
			if (parent != null && !(parent instanceof FieldAccess)) {
				IMethodBinding variableDeclaration = mthBinding.getMethodDeclaration();
				ITypeBinding declaringClass = variableDeclaration.getDeclaringClass();
				if (declaringClass != null && thisClassName != null && ch != '.') {
					isClassString = "java.lang.String".equals(declaringClass.getQualifiedName());
					appendFieldName(parent, declaringClass);
				}
			}
//					String name = node.getFullyQualifiedName();
			String name = getJ2SName(node);
			name = shortenQualifiedName(name);
			if (!(isClassString && "valueOf".equals(name)) && checkKeyworkViolation(name)) {
				buffer.append('$');
			}
			buffer.append(name);
		}
	}

	private void appendFieldName(ASTNode parent, ITypeBinding declaringClass) {
		String name = declaringClass.getQualifiedName();
		boolean isThis = false;
		int superLevel = 0;
		while (parent != null) {
			if (parent instanceof AbstractTypeDeclaration) {
				AbstractTypeDeclaration type = (AbstractTypeDeclaration) parent;
				ITypeBinding typeBinding = type.resolveBinding();
				superLevel++;
				if (Bindings.isSuperType(declaringClass, typeBinding)) {
					if (superLevel == 1) {
						buffer.append("this.");
						isThis = true;
					} else {
						name = typeBinding.getQualifiedName();
					}
					break;
				}
			} else if (parent instanceof AnonymousClassDeclaration) {
				AnonymousClassDeclaration type = (AnonymousClassDeclaration) parent;
				ITypeBinding typeBinding = type.resolveBinding();
				superLevel++;
				if (Bindings.isSuperType(declaringClass, typeBinding)) {
					if (superLevel == 1) {
						buffer.append("this.");
						isThis = true;
					} else {
						name = typeBinding.getQualifiedName();
						if ((name == null || name.length() == 0) && typeBinding.isLocal()) {
							name = typeBinding.getBinaryName();
							int idx0 = name.lastIndexOf(".");
							if (idx0 == -1) {
								idx0 = 0;
							}
							int idx1 = name.indexOf('$', idx0);
							if (idx1 != -1) {
								int idx2 = name.indexOf('$', idx1 + 1);
								String parentAnon = "";
								if (idx2 == -1) { // maybe the name is already "$1$2..." for Java5.0+ in Eclipse 3.2+
									parent = parent.getParent();
									while (parent != null) {
										if (parent instanceof AbstractTypeDeclaration) {
											break;
										} else if (parent instanceof AnonymousClassDeclaration) {
											AnonymousClassDeclaration atype = (AnonymousClassDeclaration) parent;
											ITypeBinding aTypeBinding = atype.resolveBinding();
											String aName = aTypeBinding.getBinaryName();
											parentAnon = aName.substring(aName.indexOf('$')) + parentAnon;
										}
										parent = parent.getParent();
									}
									name = name.substring(0, idx1) + parentAnon + name.substring(idx1);
								}
							}
						}
					}
					break;
				}
			}
			parent = parent.getParent();
		}
		if (!isThis) {
			buffer.append("this.callbacks[\"");
			buffer.append(shortenQualifiedName(name));
			buffer.append("\"].");
		}
	}

	public boolean visit(SimpleType node) {
		ITypeBinding binding = node.resolveBinding();
		if (binding != null) {
			buffer.append(assureQualifiedName(shortenQualifiedName(binding.getQualifiedName())));
		} else {
			buffer.append(node);
		}
		return false;
	}

	public boolean visit(SingleVariableDeclaration node) {
		SimpleName name = node.getName();
		IBinding binding = name.resolveBinding();
		if (binding != null) {
			String identifier = name.getIdentifier();
			ASTFinalVariable f = null;
			if (methodDeclareStack.size() == 0) {
				f = new ASTFinalVariable(blockLevel + 1, identifier, null);
			} else {
				String methodSig = (String) methodDeclareStack.peek();
				f = new ASTFinalVariable(blockLevel + 1, identifier, methodSig);
			}
			List finalVars = ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).finalVars;
			List normalVars = ((ASTVariableVisitor) getAdaptable(ASTVariableVisitor.class)).normalVars;
			f.toVariableName = getIndexedVarName(identifier, normalVars.size());
			normalVars.add(f);
			if ((binding.getModifiers() & Modifier.FINAL) != 0) {
				finalVars.add(f);
			}
		}
		name.accept(this);
		return false;
	}

	public boolean visit(SuperConstructorInvocation node) {
		IMethodBinding constructorBinding = node.resolveConstructorBinding();
		ITypeBinding declaringClass = constructorBinding.getDeclaringClass();
		if ("java.lang.Object".equals(declaringClass.getQualifiedName())) {
			return false;
		}
		ASTNode parent = node.getParent();
		if (parent instanceof Block) {
			Block methoBlock = (Block) parent;
			ASTNode methodParent = methoBlock.getParent();
			if (methodParent instanceof MethodDeclaration) {
				MethodDeclaration method = (MethodDeclaration) methodParent;
				if (getJ2STag(method, "@j2sIgnoreSuperConstructor") != null) {
					return false;
				}
			}
		}
		/*
		 * TODO: expression before the "super" should be considered.
		 */
		buffer.append("Clazz.superConstructor (this, ");
		buffer.append(assureQualifiedName(shortenQualifiedName(getFullClassName())));
		List arguments = node.arguments();
		if (arguments.size() > 0) {
			buffer.append(", [");
			IMethodBinding methodDeclaration = null;
			if (constructorBinding != null) {
				methodDeclaration = constructorBinding.getMethodDeclaration();
			}
			visitMethodParameterList(arguments, methodDeclaration);
			//visitList(arguments, ", ");
			buffer.append("]");
		}
		buffer.append(");\r\n");
		return false;
	}

	public boolean visit(SuperFieldAccess node) {
		ASTNode xparent = node.getParent();
		while (xparent != null 
				&& !(xparent instanceof AbstractTypeDeclaration)
				&& !(xparent instanceof AnonymousClassDeclaration)) {
			xparent = xparent.getParent();
		}
		ITypeBinding typeBinding = null;
		if (xparent != null) {
			if (xparent instanceof AbstractTypeDeclaration) {
				AbstractTypeDeclaration type = (AbstractTypeDeclaration) xparent;
				typeBinding = type.resolveBinding();
			} else if (xparent instanceof AnonymousClassDeclaration) {
				AnonymousClassDeclaration type = (AnonymousClassDeclaration) xparent;
				typeBinding = type.resolveBinding().getSuperclass();
			}
		}
		String fieldName = getJ2SName(node.getName());
		if (isInheritedFieldName(typeBinding, fieldName)) {
			if (typeBinding != null) {
				IVariableBinding[] declaredFields = typeBinding.getDeclaredFields();
				for (int i = 0; i < declaredFields.length; i++) {
					String superFieldName = getJ2SName(declaredFields[i]);
					if (fieldName.equals(superFieldName)) {
						buffer.append("this.");
						if (checkKeyworkViolation(fieldName)) {
							buffer.append('$');
						}
						fieldName = getFieldName(typeBinding.getSuperclass(), fieldName);
						buffer.append(fieldName);
						return false;
					}
				}
			}
		}
		buffer.append("this.");
		if (checkKeyworkViolation(fieldName)) {
			buffer.append('$');
		}
		buffer.append(fieldName);
		
		return false;
	}

	public boolean visit(SuperMethodInvocation node) {
		buffer.append("Clazz.superCall (this, ");
		buffer.append(assureQualifiedName(shortenQualifiedName(getFullClassName())));
		buffer.append(", \"");
		String name = getJ2SName(node.getName());
		buffer.append(name);
		buffer.append("\", [");
		IMethodBinding methodDeclaration = node.resolveMethodBinding();
		visitMethodParameterList(node.arguments(), methodDeclaration);
		//visitList(node.arguments(), ", ");
		buffer.append("])");
		return false;
	}

	public boolean visit(ThisExpression node) {
		Name qualifier = node.getQualifier();
		if (qualifier != null) {
			ASTNode xparent = node.getParent();
			while (xparent != null 
					&& !(xparent instanceof AbstractTypeDeclaration)
					&& !(xparent instanceof AnonymousClassDeclaration)) {
				xparent = xparent.getParent();
			}
			if (xparent == null 
					|| xparent.getParent() == null // CompilationUnit
							|| xparent.getParent().getParent() == null) {
				buffer.append("this");
			} else {
				/*
				 * only need callbacks wrapper in inner classes
				 * or anonymous classes.
				 */
				buffer.append("this.callbacks[\"");
				qualifier.accept(this);
				buffer.append("\"]");
			}
		} else {
			buffer.append("this");
		}
		return false;
	}

	public void endVisit(TypeDeclaration node) {
		if (node != rootTypeNode && node.getParent() != null 
				&& (node.getParent() instanceof AbstractTypeDeclaration
						|| node.getParent() instanceof TypeDeclarationStatement)) {
			return ;
		}
		if (!node.isInterface()) {
			buffer.append("Clazz.instantialize (this, arguments);\r\n");
			//buffer.append("};\r\n");
			buffer.append("}, ");
		}
		
		String emptyFun = "Clazz.decorateAsClass (function () {\r\n" +
				"Clazz.instantialize (this, arguments);\r\n" +
				"}, ";
		int idx = buffer.lastIndexOf(emptyFun);
		
		if (idx != -1 && idx == buffer.length() - emptyFun.length()) {
			buffer.replace(idx, buffer.length(), "Clazz.declareType (");
		}

		
		String fullClassName = null;
		String packageName = ((ASTPackageVisitor) getAdaptable(ASTPackageVisitor.class)).getPackageName();
		String className = ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class)).getClassName();
		if (packageName != null && packageName.length() != 0) {
			fullClassName = packageName + '.' + className;
		} else {
			fullClassName = className;
		}

		if (node.isInterface()) {
			boolean needReturn = false;
			for (Iterator iter = node.bodyDeclarations().iterator(); iter.hasNext();) {
				ASTNode element = (ASTNode) iter.next();
				if (element instanceof Initializer) {
					if (getJ2STag((Initializer) element, "@j2sIgnore") != null) {
						continue;
					}
					needReturn = true;
				} else if (element instanceof FieldDeclaration) {
					FieldDeclaration field = (FieldDeclaration) element;
					if (getJ2STag(field, "@j2sIgnore") != null) {
						continue;
					}
					if ((field.getModifiers() & Modifier.STATIC) != 0) {
						needReturn = true;
					} else if (node.isInterface()) {
						List fragments = field.fragments();
						needReturn = fragments.size() > 0;
					}
				}
				if (needReturn) {
					break;
				}
			}
			if (needReturn) {
				buffer.append("cla$$ = ");
			}
			buffer.append("Clazz.declareInterface (");
			int lastIndexOf = fullClassName.lastIndexOf ('.');
			if (lastIndexOf != -1) {
				buffer.append(assureQualifiedName(shortenPackageName(fullClassName)));
				buffer.append(", \"" + fullClassName.substring(lastIndexOf + 1) + "\"");
			} else {
				buffer.append("null, \"" + fullClassName + "\"");
			}
			
		} else {
			int lastIndexOf = fullClassName.lastIndexOf ('.');
			if (lastIndexOf != -1) {
				buffer.append(assureQualifiedName(shortenPackageName(fullClassName)));
				buffer.append(", \"" + fullClassName.substring(lastIndexOf + 1) + "\"");
			} else {
				buffer.append("null, \"" + fullClassName + "\"");
			}
			buffer.append(", ");

		}
		boolean defined = false;
		ITypeBinding typeBinding = node.resolveBinding();
		if (typeBinding != null) {
			ITypeBinding superclass = typeBinding.getSuperclass();
			if (superclass != null) {
				String clazzName = superclass.getQualifiedName();
				clazzName = assureQualifiedName(shortenQualifiedName(clazzName));
				if (clazzName != null && clazzName.length() != 0
						&& !"Object".equals(clazzName)) {
					buffer.append(clazzName);
					defined = true;
				}
			}
		}
		if (!defined && !node.isInterface()) {
			buffer.append("null");
		}
		buffer.append(", ");

		//List superInterfaces = node.superInterfaceTypes();
		List superInterfaces = node.superInterfaceTypes();
		int size = superInterfaces.size();
		if (size == 0) {
			buffer.append("null");
		} else if (size > 1) {
			buffer.append("[");
		}
		for (Iterator iter = superInterfaces.iterator(); iter
				.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			ITypeBinding binding = ((Type) element).resolveBinding();
			if (binding != null) {
				String clazzName = binding.getQualifiedName();
				clazzName = assureQualifiedName(shortenQualifiedName(clazzName));
				buffer.append(clazzName);
			} else {
				buffer.append(element);
			}
			if (iter.hasNext()) {
				buffer.append(", ");
			}
		}
		if (size > 1) {
			buffer.append("]");
		}
		ITypeBinding superclass = null;
		Type superType = node.getSuperclassType();
		if (superType != null) {
			superclass = superType.resolveBinding();
		}
		if (superclass != null) {
			ITypeBinding binding = superclass;//.resolveTypeBinding();
			if (binding != null && !binding.isTopLevel()) {
				if ((binding.getModifiers() & Modifier.STATIC) == 0) {
					buffer.append(", Clazz.innerTypeInstance (");
					buffer.append(assureQualifiedName(shortenQualifiedName(binding.getQualifiedName())));
					buffer.append(", this, null, Clazz.inheritArgs");
					buffer.append(")");
				}
			}
		}
		int len = buffer.length();
		// ", null, null"
		if (", null, null".equals(buffer.substring(len - 12))) {
			buffer.delete(len - 12, len);
		} else if (", null".equals(buffer.substring(len - 6))) {
			buffer.delete(len - 6, len);
		}
		buffer.append(");\r\n");
		
		buffer.append(laterBuffer);
		laterBuffer = new StringBuffer();
		// Enum is considered as static member!

		List bodyDeclarations = node.bodyDeclarations();
		StringBuffer tmpBuffer = buffer;
		StringBuffer tmpLaterBuffer = laterBuffer;
//		StringBuffer tmpMethodBuffer = methodBuffer;
//		buffer = new StringBuffer();
//		laterBuffer = new StringBuffer();
//		methodBuffer = new StringBuffer();
		boolean needPreparation = false;
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) element;
				if (getJ2STag(field, "@j2sIgnore") != null) {
					continue;
				}
				if (node.isInterface() || !isFieldNeedPreparation(field)) {
					continue;
				}
				needPreparation = true;
				//element.accept(this);
				break;
			} else if (element instanceof Initializer) {
				Initializer init = (Initializer) element;
				if (getJ2STag(init, "@j2sIgnore") != null) {
					continue;
				}
				if ((init.getModifiers() & Modifier.STATIC) == 0) {
					needPreparation = true;
					break;
				}
			}
		}
//		if (methodBuffer.length() > 0) {
//			tmpBuffer.append(methodBuffer.toString());
//		}
//		buffer = tmpBuffer;
//		laterBuffer = tmpLaterBuffer;
//		methodBuffer = tmpMethodBuffer;

		if (needPreparation) {
			buffer.append("Clazz.prepareFields (cla$$, function () {\r\n");
			for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
				ASTNode element = (ASTNode) iter.next();
				if (element instanceof FieldDeclaration) {
					FieldDeclaration field = (FieldDeclaration) element;
					if (getJ2STag(field, "@j2sIgnore") != null) {
						continue;
					}
					if (node.isInterface() || !isFieldNeedPreparation(field)) {
						continue;
					}
					element.accept(this);
				} else if (element instanceof Initializer) {
					Initializer init = (Initializer) element;
					if (getJ2STag(init, "@j2sIgnore") != null) {
						continue;
					}
					if ((init.getModifiers() & Modifier.STATIC) == 0) {
						element.accept(this);
					}
				}
			}
			buffer.append("});\r\n");
		}
		
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof EnumDeclaration) {
				element.accept(this);
			}
		}
		
		MethodDeclaration[] methods = node.getMethods();
		for (int i = 0; i < methods.length; i++) {
			// All the methods are defined outside the main function body! -- March 17, 2006
			methods[i].accept(this);
		}
		
		
		int staticCount = -1;
		ReferenceASTVisitor refVisitor = new ReferenceASTVisitor();
		/*
		 * Fixing bug#2797539 : Incorrect instantiation of member before inner class declaration inside interface
		 * http://sourceforge.net/tracker/?func=detail&aid=2797539&group_id=155436&atid=795800
		 * Interface's inner classes declaration is not in the correct order. Fix it by move codes a few lines
		 * ahead of member initialization. 
		 */
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof TypeDeclaration) {
				if (node.isInterface()) {
					/*
					 * Here will create a new visitor to do the Java2Script process
					 * and laterBuffer may be filled with contents.
					 */
					element.accept(this);
				}
			}
		}
		// Interface's inner interfaces or classes
		buffer.append(laterBuffer);

		tmpBuffer = buffer;
		tmpLaterBuffer = laterBuffer;
		buffer = new StringBuffer();
		laterBuffer = new StringBuffer();
		/* Testing class declarations in initializers */
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof TypeDeclaration) {
				if (node.isInterface()) {
					// the above codes have already dealt those inner classes inside interface
					// just ignore here
					continue;
				}
			} else if (element instanceof Initializer) {
				if (getJ2STag((Initializer) element, "@j2sIgnore") != null) {
					continue;
				}
				if ((((Initializer) element).getModifiers() & Modifier.STATIC) != 0) {
					element.accept(this);
				} else {
					continue; // ignore here
				}
			} else if (element instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) element;
				if (getJ2STag(field, "@j2sIgnore") != null) {
					continue;
				}
				if ((field.getModifiers() & Modifier.STATIC) != 0) {
					List fragments = field.fragments();
					for (int j = 0; j < fragments.size(); j++) {
						VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragments.get(j);
						Expression initializer = vdf.getInitializer();
						if (initializer != null) { 
							initializer.accept(this);
						}
					}
				} else if (node.isInterface()) {
					List fragments = field.fragments();
					for (int j = 0; j < fragments.size(); j++) {
						VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragments.get(j);
						Expression initializer = vdf.getInitializer();
						vdf.getName().accept(this);
						if (initializer != null) { 
							initializer.accept(this);
						}
					}
			
				}
			}
		}
		buffer = tmpBuffer;
		laterBuffer = tmpLaterBuffer;
		
		if (methodBuffer.length() > 0) {
			buffer.append(methodBuffer);
			methodBuffer = new StringBuffer();
		}
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof TypeDeclaration) {
				if (node.isInterface()) {
					// the above codes have already dealt those inner classes inside interface
					// just ignore here
					continue;
				}
			} else if (element instanceof Initializer) {
				if (getJ2STag((Initializer) element, "@j2sIgnore") != null) {
					continue;
				}
				if (staticCount != -1) {
					buffer.append(");\r\n");
					staticCount = -1;
				}
				if ((((Initializer) element).getModifiers() & Modifier.STATIC) != 0) {
					element.accept(this);
				} else {
					continue; // ignore here
				}
			} else if (element instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) element;
				if (getJ2STag(field, "@j2sIgnore") != null) {
					continue;
				}
			if ((field.getModifiers() & Modifier.STATIC) != 0) {
				List fragments = field.fragments();
				for (int j = 0; j < fragments.size(); j++) {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragments.get(j);
					if ("serialVersionUID".equals(vdf.getName().getIdentifier())) {
						continue;
					}
					Expression initializer = vdf.getInitializer();
					refVisitor.setReferenced(false);
					if (initializer != null) {
						initializer.accept(refVisitor);
					}
					if (refVisitor.isReferenced()) {
						if (staticCount != -1) {
							buffer.append(");\r\n");
							staticCount = -1;
						}
						buffer.append("cla$$");
						//buffer.append(fullClassName);
						buffer.append(".");
						//buffer.append(vdf.getName());
						vdf.getName().accept(this);
						buffer.append(" = ");
						buffer.append("cla$$");
						//buffer.append(fullClassName);
						buffer.append(".prototype.");
						vdf.getName().accept(this);
						buffer.append(" = ");
						initializer.accept(this);
						buffer.append(";\r\n");
						continue;
					} else {
						staticCount++;
						if (staticCount == 0) {
							buffer.append("Clazz.defineStatics (cla$$");
						}
					}
					buffer.append(",\r\n\"");
					vdf.getName().accept(this);
					buffer.append("\", ");
					
					if (initializer != null) { 
						initializer.accept(this);
					} else {
						Type type = field.getType();
						if (type.isPrimitiveType()){
							PrimitiveType pType = (PrimitiveType) type;
							if (pType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
								buffer.append("false");
							} else if (pType.getPrimitiveTypeCode() == PrimitiveType.CHAR) {
								buffer.append("'\\0'");
							} else {
								buffer.append("0");
							}
						} else {
							buffer.append("null");
						}
					}
				}
			} else if (node.isInterface()) {
				List fragments = field.fragments();
				for (int j = 0; j < fragments.size(); j++) {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragments.get(j);
					if ("serialVersionUID".equals(vdf.getName().getIdentifier())) {
						continue;
					}
					Expression initializer = vdf.getInitializer();
					refVisitor.setReferenced(false);
					if (initializer != null) {
						initializer.accept(refVisitor);
					}
					if (refVisitor.isReferenced()) {
						if (staticCount != -1) {
							buffer.append(");\r\n");
							staticCount = -1;
						}
						buffer.append("cla$$");
						buffer.append(".");
						vdf.getName().accept(this);
						buffer.append(" = ");
						buffer.append("cla$$");
						buffer.append(".prototype.");
						vdf.getName().accept(this);
						buffer.append(" = ");
						initializer.accept(this);
						buffer.append(";\r\n");
						continue;
					} else {
						staticCount++;
						if (staticCount == 0) {
							buffer.append("Clazz.defineStatics (cla$$");
						}
					}
					buffer.append(",\r\n\"");
					vdf.getName().accept(this);
					buffer.append("\", ");
					if (initializer != null) { 
						initializer.accept(this);
					} else {
						Type type = field.getType();
						if (type.isPrimitiveType()){
							PrimitiveType pType = (PrimitiveType) type;
							if (pType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
								buffer.append("false");
							} else if (pType.getPrimitiveTypeCode() == PrimitiveType.CHAR) {
								buffer.append("'\\0'");
							} else {
								buffer.append("0");
							}
						} else {
							buffer.append("null");
						}
					}
				}
		
			}
		}
		}
		if (staticCount != -1) {
			buffer.append(");\r\n");
		}
		
		String fieldsSerializables = prepareSimpleSerializable(node, bodyDeclarations);
		if (fieldsSerializables.length() > 0) {
			buffer.append("Clazz.registerSerializableFields(cla$$, ");
			buffer.append(fieldsSerializables.toString());
			buffer.append(");\r\n");
		}
		
		readSources(node, "@j2sSuffix", "\r\n", "\r\n", true);
		laterBuffer = new StringBuffer();
		super.endVisit(node);
	}

	private String prepareSimpleSerializable(TypeDeclaration node, List bodyDeclarations) {
		StringBuffer fieldsSerializables = new StringBuffer();
		ITypeBinding binding = node.resolveBinding();
		boolean isSimpleSerializable = binding != null
				&& (Bindings.findTypeInHierarchy(binding, "net.sf.j2s.ajax.SimpleSerializable") != null);
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof FieldDeclaration) {
				if (node.isInterface()) {
					/*
					 * As members of interface should be treated
					 * as final and for javascript interface won't
					 * get instantiated, so the member will be
					 * treated specially. 
					 */
					continue;
				}
				FieldDeclaration fieldDeclaration = (FieldDeclaration) element;
				
				if (isSimpleSerializable) {
					List fragments = fieldDeclaration.fragments();
					int modifiers = fieldDeclaration.getModifiers();
					if ((Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) 
							&& !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
						Type type = fieldDeclaration.getType();
						int dims = 0;
						if (type.isArrayType()) {
							dims = 1;
							type = ((ArrayType) type).getComponentType();
						}
						String mark = null;
						if (type.isPrimitiveType()) {
							PrimitiveType pType = (PrimitiveType) type;
							Code code = pType.getPrimitiveTypeCode();
							if (code == PrimitiveType.FLOAT) {
								mark = "F";
							} else if (code == PrimitiveType.DOUBLE) {
								mark = "D";
							} else if (code == PrimitiveType.INT) {
								mark = "I";
							} else if (code == PrimitiveType.LONG) {
								mark = "L";
							} else if (code == PrimitiveType.SHORT) {
								mark = "S";
							} else if (code == PrimitiveType.BYTE) {
								mark = "B";
							} else if (code == PrimitiveType.CHAR) {
								mark = "C";
							} else if (code == PrimitiveType.BOOLEAN) {
								mark = "b";
							} 
						}
						ITypeBinding resolveBinding = type.resolveBinding();
						if ("java.lang.String".equals(resolveBinding.getQualifiedName())) {
							mark = "s";
						}
						if (mark != null) {
							for (Iterator xiter = fragments.iterator(); xiter.hasNext();) {
								VariableDeclarationFragment var = (VariableDeclarationFragment) xiter.next();
								int curDim = dims + var.getExtraDimensions();
								if (curDim <= 1) {
									if (fieldsSerializables.length() > 0) {
										fieldsSerializables.append(", ");
									}
									/*
									 * TODO: What about when variable name is minimized?
									 */
									fieldsSerializables.append("\"" + var.getName() + "\", \"");
									if (mark.charAt(0) == 's' && curDim == 1) {
										fieldsSerializables.append("AX");
									} else if (curDim == 1) {
										fieldsSerializables.append("A");
										fieldsSerializables.append(mark);
									} else {
										fieldsSerializables.append(mark);
									}
									fieldsSerializables.append("\"");
								}
							}					
						}
					}
				}
			}
		}
		return fieldsSerializables.toString();
	}

	public boolean visit(TypeDeclaration node) {
		ITypeBinding binding = node.resolveBinding();
		ASTTypeVisitor typeVisitor = ((ASTTypeVisitor) getAdaptable(ASTTypeVisitor.class));
		if (binding != null) {
			if (binding.isTopLevel()) {
				typeVisitor.setClassName(binding.getName());
			} else {
			}
		}
		
		if ((node != rootTypeNode) && node.getParent() != null 
				&& (node.getParent() instanceof AbstractTypeDeclaration
				|| node.getParent() instanceof TypeDeclarationStatement)) {
			/* inner static class */
			ASTScriptVisitor visitor = null;
			try {
				visitor = (ASTScriptVisitor) this.getClass().newInstance();
			} catch (Exception e) {
				visitor = new ASTScriptVisitor(); // Default visitor
			}
			visitor.rootTypeNode = node;
			String className = typeVisitor.getClassName();
			String visitorClassName = null;
			if (node.getParent() instanceof TypeDeclarationStatement) {
//				typeVisitor.increaseAnonymousClassCount();
//				if (node.resolveBinding().getBinaryName().matches(".*\\$[0-9]+\\$.*")) {
//					visitorClassName = className + "$" + typeVisitor.getAnonymousCount() + "$" + node.getName();
//				} else {
//					visitorClassName = className + "$" + typeVisitor.getAnonymousCount() + node.getName();
//				}
				String anonClassName = null;
				if (binding.isAnonymous() || binding.isLocal()) {
					anonClassName = assureQualifiedName(shortenQualifiedName(binding.getBinaryName()));
				} else {
					anonClassName = assureQualifiedName(shortenQualifiedName(binding.getQualifiedName()));
				}
				int idx = anonClassName.lastIndexOf('.');
				if (idx == -1) {
					visitorClassName = anonClassName;
				} else {
					visitorClassName = anonClassName.substring(idx + 1);
				}
			} else {
				visitorClassName = className + "." + node.getName();
			}
			((ASTTypeVisitor) visitor.getAdaptable(ASTTypeVisitor.class)).setClassName(visitorClassName);
			((ASTPackageVisitor) visitor.getAdaptable(ASTPackageVisitor.class)).setPackageName(((ASTPackageVisitor) getAdaptable(ASTPackageVisitor.class)).getPackageName());
			node.accept(visitor);
			if (node.isInterface() || (node.getModifiers() & Modifier.STATIC) != 0 
					|| (node.getParent() instanceof TypeDeclaration 
							&& ((TypeDeclaration) node.getParent()).isInterface())) {
				String str = visitor.getBuffer().toString();
				if (!str.startsWith("cla$$")) {
					laterBuffer.append(str);
				} else {
					laterBuffer.append("Clazz.pu$h ();\r\n");
					laterBuffer.append(str);
					laterBuffer.append("cla$$ = Clazz.p0p ();\r\n");
				}
			} else {
				/*
				 * Never reach here!
				 * March 17, 2006
				 */
				/*
				 * It reaches here!
				 * Code examples:
				 * 
class CA {
	class State {}
}
public class CB extends CA {
	CA.State state = new CA.State() {
		public String toString() {
			return "CB.CA.State";
		}
	};
	State stt = new State() {
		public String toString() {
			return "State";
		};
	};
	public static void main(String[] args) {
		System.out.println(new CB().state);
		System.out.println(new CB().stt);
	}
}
				 */
				buffer.append("if (!Clazz.isClassDefined (\"");
				buffer.append(visitor.getFullClassName());
				buffer.append("\")) {\r\n");
				
				methodBuffer.append("cla$$.$");
				String targetClassName = visitor.getClassName();
//				String prefixKey = className + ".";
//				if (targetClassName.startsWith(prefixKey)) {
//					targetClassName = targetClassName.substring(prefixKey.length());
//				}
				targetClassName = targetClassName.replace('.', '$');
				methodBuffer.append(targetClassName);
				methodBuffer.append("$ = function () {\r\n");
				methodBuffer.append("Clazz.pu$h ();\r\n");
				methodBuffer.append(visitor.getBuffer().toString());
				methodBuffer.append("cla$$ = Clazz.p0p ();\r\n");
				methodBuffer.append("};\r\n");

				buffer.append(visitor.getPackageName());
				buffer.append(".");
				buffer.append(className);
				buffer.append(".$");
				buffer.append(targetClassName);
				buffer.append("$ ();\r\n");
				buffer.append("}\r\n");
				
			}
			return false;
		}
		
		if (node.isInterface()) {
			return false;
		}
		readSources(node, "@j2sPrefix", "", " ", true);
		buffer.append("cla$$ = ");
		
		buffer.append("Clazz.decorateAsClass (");
		
		buffer.append("function () {\r\n");
		if (node == rootTypeNode && (node.getModifiers() & Modifier.STATIC) == 0 
				&& ((node.getParent() instanceof TypeDeclaration 
						&& !((TypeDeclaration) node.getParent()).isInterface()) 
						|| node.getParent() instanceof TypeDeclarationStatement)) {
			buffer.append("Clazz.prepareCallback (this, arguments);\r\n");
		}
		List bodyDeclarations = node.bodyDeclarations();
		for (Iterator iter = bodyDeclarations.iterator(); iter.hasNext();) {
			ASTNode element = (ASTNode) iter.next();
			if (element instanceof MethodDeclaration) {
				continue;
			} else if (element instanceof Initializer) {
				continue;
			} else if (element instanceof EnumDeclaration) {
				continue;
			} else if (element instanceof FieldDeclaration) {
				if (node.isInterface()) {
					/*
					 * As members of interface should be treated
					 * as final and for javascript interface won't
					 * get instantiated, so the member will be
					 * treated specially. 
					 */
					continue;
				}
				FieldDeclaration fieldDeclaration = (FieldDeclaration) element;
				if (getJ2STag(fieldDeclaration, "@j2sIgnore") != null) {
					continue;
				}
				if (isFieldNeedPreparation(fieldDeclaration)) {
					visitWith(fieldDeclaration, true);
					continue;
				}
			} else if (element instanceof TypeDeclaration) {
				if (node.isInterface()) {
					/*
					 * As sub type of interface should be treated
					 * as final and for javascript interface won't
					 * get instantiated, so the type will be
					 * treated specially. 
					 */
					continue;
				}
			}
			element.accept(this);
		}
		return false;
	}

	public boolean visit(TypeLiteral node) {
		Type type = node.getType();
		if (type.isPrimitiveType()) {
			ITypeBinding resolveBinding = type.resolveBinding();
			String name = resolveBinding.getName();
			if ("boolean".equals(name)) {
				buffer.append("Boolean");
				return false;
			} else { // TODO: More types? Integer, Long, Double, ... ?
				buffer.append("Number");
				return false;
			}
		} else if (type.isArrayType()) {
			buffer.append("Array");
			return false;
		} else {
			ITypeBinding resolveBinding = type.resolveBinding();
			String name = resolveBinding.getName();
			if ("Object".equals(name) || "java.lang.Object".equals(name)) {
				buffer.append("JavaObject");
				return false;
			}
		}
		type.accept(this);
		return false;
	}

}
