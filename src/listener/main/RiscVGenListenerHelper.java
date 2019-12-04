package listener.main;

import java.util.Hashtable;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;
import listener.main.SymbolTable;
import listener.main.SymbolTable.VarInfo;

public class RiscVGenListenerHelper {
	
	// <boolean functions>
	
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}
	
	// type_spec IDENT '[' ']'
	static boolean isArrayParamDecl(ParamContext param) {
		return param.getChildCount() == 4;
	}
	
	// global vars
	static int initVal(Var_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	// var_decl	: type_spec IDENT '=' LITERAL ';
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	// var_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 6;
	}

	// <local vars>
	// local_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static int initVal(Local_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	static boolean isArrayDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 6;
	}
	
	static boolean isDeclWithInit(Local_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	
	static boolean isVoidF(Fun_declContext ctx) {
			// <Fill in>
		return ctx.type_spec().VOID() != null;
	}
	
	static boolean isIntReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() ==3;
	}


	static boolean isVoidReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() == 2;
	}
	
	// <information extraction>
	static String getStackSize(Fun_declContext ctx) {
		return "32";
	}
	static String getLocalVarSize(Fun_declContext ctx) {
		return "32";
	}

	// params
	static String getParamName(ParamContext param) {
		// <Fill in>
		return param.IDENT().getText();
	}
	
	static String getLocalVarName(Local_declContext ctx) {
		// <Fill in>
		return ctx.IDENT().getText();
	}
	
	static String getFunName(Fun_declContext ctx) {
		// <Fill in>
		return ctx.IDENT().getText();
	}
	
	static String getFunName(ExprContext ctx) {
		// <Fill in>
		return ctx.IDENT().getText();
	}
	
	static boolean noElse(If_stmtContext ctx) {
		return ctx.getChildCount() < 5;
	}
	
	static String getFunProlog() {
		StringBuffer sb = new StringBuffer();
		sb.append(".class public Test\n" +
				".super java/lang/Object\n" +
				"\n" +
				".method public <init>()V\n" +
				"aload_0\n" +
				"invokenonvirtual java/lang/Object/<init>()V\n" +
				"return\n" +
				".end method\n");
		return sb.toString();
		// return ".class public Test .....
		// ...
		// invokenonvirtual java/lang/Object/<init>()
		// return
		// .end method"
	}
	
	static String getCurrentClassName() {
		return "Test";
	}
}
