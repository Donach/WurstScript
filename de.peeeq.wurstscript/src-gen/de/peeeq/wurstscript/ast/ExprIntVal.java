//generated by parseq
package de.peeeq.wurstscript.ast;

public interface ExprIntVal extends AstElement, ExprAtomic {
	AstElement getParent();
	void setSource(WPos source);
	WPos getSource();
	void setVal(Integer val);
	Integer getVal();
	ExprIntVal copy();
	public abstract void accept(WPackage.Visitor v);
	public abstract void accept(NativeFunc.Visitor v);
	public abstract void accept(StmtIf.Visitor v);
	public abstract void accept(ExprBinary.Visitor v);
	public abstract void accept(ExprIntVal.Visitor v);
	public abstract void accept(OptExpr.Visitor v);
	public abstract void accept(ExprVarArrayAccess.Visitor v);
	public abstract void accept(VarRef.Visitor v);
	public abstract void accept(Indexes.Visitor v);
	public abstract void accept(ClassMember.Visitor v);
	public abstract void accept(ArraySizes.Visitor v);
	public abstract void accept(StmtReturn.Visitor v);
	public abstract void accept(StmtExitwhen.Visitor v);
	public abstract void accept(TypeExpr.Visitor v);
	public abstract void accept(FuncSignature.Visitor v);
	public abstract void accept(JassGlobalBlock.Visitor v);
	public abstract void accept(TopLevelDeclaration.Visitor v);
	public abstract void accept(ExprMemberMethod.Visitor v);
	public abstract void accept(ExprAtomic.Visitor v);
	public abstract void accept(StmtDestroy.Visitor v);
	public abstract void accept(Expr.Visitor v);
	public abstract void accept(StmtWhile.Visitor v);
	public abstract void accept(WStatements.Visitor v);
	public abstract void accept(OnDestroyDef.Visitor v);
	public abstract void accept(ExprMemberArrayVar.Visitor v);
	public abstract void accept(StmtCall.Visitor v);
	public abstract void accept(WScope.Visitor v);
	public abstract void accept(VarDef.Visitor v);
	public abstract void accept(ExprMemberVar.Visitor v);
	public abstract void accept(WParameter.Visitor v);
	public abstract void accept(ClassDef.Visitor v);
	public abstract void accept(ExprNewObject.Visitor v);
	public abstract void accept(WEntity.Visitor v);
	public abstract void accept(ExprFunctionCall.Visitor v);
	public abstract void accept(ConstructorDef.Visitor v);
	public abstract void accept(WStatement.Visitor v);
	public abstract void accept(JassToplevelDeclaration.Visitor v);
	public abstract void accept(ClassSlot.Visitor v);
	public abstract void accept(FunctionDefinition.Visitor v);
	public abstract void accept(TypeDef.Visitor v);
	public abstract void accept(ExprUnary.Visitor v);
	public abstract void accept(WParameters.Visitor v);
	public abstract void accept(Arguments.Visitor v);
	public abstract void accept(ClassSlots.Visitor v);
	public abstract void accept(NativeType.Visitor v);
	public abstract void accept(InitBlock.Visitor v);
	public abstract void accept(ExprCast.Visitor v);
	public abstract void accept(FuncRef.Visitor v);
	public abstract void accept(FuncDef.Visitor v);
	public abstract void accept(PackageOrGlobal.Visitor v);
	public abstract void accept(LocalVarDef.Visitor v);
	public abstract void accept(OptTypeExpr.Visitor v);
	public abstract void accept(StmtLoop.Visitor v);
	public abstract void accept(GlobalVarDef.Visitor v);
	public abstract void accept(StmtSet.Visitor v);
	public abstract void accept(TypeRef.Visitor v);
	public abstract void accept(WEntities.Visitor v);
	public abstract void accept(ExprAssignable.Visitor v);
	public abstract void accept(CompilationUnit.Visitor v);
	public interface Visitor {
		void visit(ExprIntVal exprIntVal);
		void visit(WPos wPos);
	}
	public static abstract class DefaultVisitor implements Visitor {
		@Override public void visit(ExprIntVal exprIntVal) {}
		@Override public void visit(WPos wPos) {}
	}
}
