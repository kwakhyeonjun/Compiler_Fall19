package listener.main;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import static listener.main.RiscVGenListenerHelper.*;

public class RiscVGenListner extends MiniCBaseListener implements ParseTreeListener {
    ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
    SymbolTable symbolTable = new SymbolTable();

    @Override
    public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
        symbolTable.initFunDecl();

        String fname = getFunName(ctx);
        MiniCParser.ParamsContext params;

        symbolTable.putFunSpecStr(ctx);
        params = (MiniCParser.ParamsContext) ctx.getChild(3);
        symbolTable.putParams(params);

    }


    // var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
    @Override
    public void enterVar_decl(MiniCParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();

        if (isArrayDecl(ctx)) {
            symbolTable.putGlobalVar(varName, SymbolTable.Type.INTARRAY);
        }
        else if (isDeclWithInit(ctx)) {
            symbolTable.putGlobalVarWithInitVal(varName, SymbolTable.Type.INT, initVal(ctx));
        }
        else  { // simple decl
            symbolTable.putGlobalVar(varName, SymbolTable.Type.INT);
        }
    }


    @Override
    public void enterLocal_decl(MiniCParser.Local_declContext ctx) {
        if (isArrayDecl(ctx)) {
            symbolTable.putLocalVar(getLocalVarName(ctx), SymbolTable.Type.INTARRAY);
        }
        else if (isDeclWithInit(ctx)) {
            symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), SymbolTable.Type.INT, initVal(ctx));
        }
        else  { // simple decl
            symbolTable.putLocalVar(getLocalVarName(ctx), SymbolTable.Type.INT);
        }
    }


    @Override
    public void exitProgram(MiniCParser.ProgramContext ctx) {


        String fun_decl = "", var_decl = "";

        for(int i = 0; i < ctx.getChildCount(); i++) {
            if(isFunDecl(ctx, i))
                fun_decl += newTexts.get(ctx.decl(i));
            else
                var_decl += newTexts.get(ctx.decl(i));
        }

        newTexts.put(ctx, var_decl + fun_decl);

        System.out.println(newTexts.get(ctx));
    }


    // decl	: var_decl | fun_decl
    @Override
    public void exitDecl(MiniCParser.DeclContext ctx) {
        String decl = "";
        if(ctx.getChildCount() == 1)
        {
            if(ctx.var_decl() != null)				//var_decl
                decl += newTexts.get(ctx.var_decl());
            else							//fun_decl
                decl += newTexts.get(ctx.fun_decl());
        }
        newTexts.put(ctx, decl);
    }

    // stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
    @Override
    public void exitStmt(MiniCParser.StmtContext ctx) {
        String stmt = "";
        if(ctx.getChildCount() > 0)
        {
            if(ctx.expr_stmt() != null)				// expr_stmt
                stmt += newTexts.get(ctx.expr_stmt());
            else if(ctx.compound_stmt() != null)	// compound_stmt
                stmt += newTexts.get(ctx.compound_stmt());
                // <(0) Fill here>
            else if(ctx.if_stmt() != null)
                stmt += newTexts.get(ctx.if_stmt());
            else if(ctx.while_stmt()!= null)
                stmt += newTexts.get(ctx.while_stmt());
            else if(ctx.return_stmt() != null)
                stmt += newTexts.get(ctx.return_stmt());
        }
        newTexts.put(ctx, stmt);
    }

    // expr_stmt	: expr ';'
    @Override
    public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
        String stmt = "";
        if(ctx.getChildCount() == 2)
        {
            stmt += newTexts.get(ctx.expr());	// expr
        }
        newTexts.put(ctx, stmt);
    }


    // while_stmt	: WHILE '(' expr ')' stmt
    @Override
    public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
        // <(1) Fill here!>
        String stmt = "";
        String condExpr = newTexts.get(ctx.expr());
        String loopStmt = newTexts.get(ctx.stmt());

        String Lloop = symbolTable.newLabel();
        String Lend = symbolTable.newLabel();

        stmt += Lloop + ":" + "\n"
                + condExpr + "\n"
                + "bne " + Lend + "\n"
                + loopStmt + "\n"
                + "beq x0, x0," + Lloop + "\n"
                + Lend + ":" + "\n";

        newTexts.put(ctx, stmt);
    }


    @Override
    public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
        // <(2) Fill here!>
        String stmt = "";

        String funcHeader = funcHeader(ctx, getFunName(ctx));
        stmt += funcHeader
                + newTexts.get(ctx.compound_stmt());

        stmt += "addi sp, sp, " + symbolTable.getParamsSize(ctx.params()) +"\n"
                + "jalr x0, 0(x1)";

        newTexts.put(ctx, stmt);
    }


    private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
        String label = symbolTable.getFunLabel(fname);
        String stmt = "";
        int sp = symbolTable.getParamsSize(ctx.params())*8; // x1 저장해야함
        stmt = "label" + label + "\n"
                + "addi sp, sp, " + sp + "\n"
                + "sd x1, "+sp + "(sp)\n";
        sp-=8;
        for(int i = 0; i < ctx.params().param().size(); i++){
            stmt += "sd x" + symbolTable.getVarId(getParamName(ctx.params().param(i))) + " " + sp + "(sp)\n";
            sp -= 8;
        }
        return stmt;
    }



    @Override
    public void exitVar_decl(MiniCParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();
        String varDecl = "";

        if (isDeclWithInit(ctx)) {
            varDecl += "putfield " + varName + "\n";
            // v. initialization => Later! skip now..:
        }
        newTexts.put(ctx, varDecl);
    }


    @Override
    public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
        String varDecl = "";

        if (isDeclWithInit(ctx)) {
            String vId = symbolTable.getVarId(ctx);
            varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
                    + "istore_" + vId + "\n";
        }

        newTexts.put(ctx, varDecl);
    }


    // compound_stmt	: '{' local_decl* stmt* '}'
    @Override
    public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
        // <(3) Fill here>
        String stmt = "";
        for(int i = 0; i < ctx.local_decl().size(); i++){
            stmt += newTexts.get(ctx.local_decl().get(i)) + "\n";
        }
        for(int i = 0; i < ctx.stmt().size(); i++){
            stmt += newTexts.get(ctx.stmt().get(i)) + "\n";
        }

        newTexts.put(ctx, stmt);
    }

    // if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
    @Override
    public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
        String stmt = "";
        String condExpr= newTexts.get(ctx.expr());
        String thenStmt = newTexts.get(ctx.stmt(0));

        String lend = symbolTable.newLabel();
        String lelse = symbolTable.newLabel();


        if(noElse(ctx)) {
            stmt += condExpr + "\n"
                    + "ifeq " + lend + "\n"
                    + thenStmt + "\n"
                    + lend + ":"  + "\n";
        }
        else {
            String elseStmt = newTexts.get(ctx.stmt(1));
            stmt += condExpr + "\n"
                    + "ifeq " + lelse + "\n"
                    + thenStmt + "\n"
                    + "goto " + lend + "\n"
                    + lelse + ": " + elseStmt + "\n"
                    + lend + ":"  + "\n";
        }

        newTexts.put(ctx, stmt);
    }


    // return_stmt	: RETURN ';' | RETURN expr ';'
    @Override
    public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
        // <(4) Fill here>

        //load -> return
        String stmt = "iload_" + symbolTable.getVarId(ctx.expr().getText())+"\n"
                + "ireturn\n";
        newTexts.put(ctx, stmt);
    }


    @Override
    public void exitExpr(MiniCParser.ExprContext ctx) {
        String expr = "";

        if(ctx.getChildCount() <= 0) {
            newTexts.put(ctx, "");
            return;
        }

        if(ctx.getChildCount() == 1) { // IDENT | LITERAL
            if(ctx.IDENT() != null) {
                String idName = ctx.IDENT().getText();
                if(symbolTable.getVarType(idName) == SymbolTable.Type.INT) {
                    expr += "iload_" + symbolTable.getVarId(idName) + " \n";
                }
                //else	// Type int array => Later! skip now..
                //	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
            } else if (ctx.LITERAL() != null) {
                String literalStr = ctx.LITERAL().getText();
                expr += "ldc " + literalStr + " \n";
            }
        } else if(ctx.getChildCount() == 2) { // UnaryOperation
            expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
        }
        else if(ctx.getChildCount() == 3) {
            if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
                expr = newTexts.get(ctx.expr(0));

            } else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
                expr = newTexts.get(ctx.expr(0))
                        + "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";

            } else { 											// binary operation
                expr = handleBinExpr(ctx, expr);

            }
        }
        // IDENT '(' args ')' |  IDENT '[' expr ']'
        else if(ctx.getChildCount() == 4) {
            if(ctx.args() != null){		// function calls
                expr = handleFunCall(ctx, expr);
            } else { // expr
                // Arrays: TODO
                String varName = ctx.IDENT().getText();
                expr += "iload_" + symbolTable.getVarId(ctx.expr().get(0).getText())+ "\n"
                        +"iaload_" + symbolTable.getVarId(ctx.expr(0).getText());
            }
        }
        // IDENT '[' expr ']' '=' expr
        else { // Arrays: TODO
            String varName = ctx.IDENT().getText();
            expr += "iload_" + symbolTable.getVarId(ctx.expr().get(0).getText())+ "\n"
                    + "iaload" + symbolTable.getVarId(ctx.expr(0).getText()) + "\n"
                    + "iload_" + symbolTable.getVarId(ctx.expr(1).getText()) + "\n"
                    + "iastore\n";
        }
        newTexts.put(ctx, expr);
    }


    private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
        String l1 = symbolTable.newLabel();
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();

        expr += newTexts.get(ctx.expr(0));
        switch(ctx.getChild(0).getText()) {
            case "-":
                expr += "           ineg \n"; break;
            case "--":
                expr += "ldc 1" + "\n"
                        + "isub" + "\n";
                break;
            case "++":
                expr += "ldc 1" + "\n"
                        + "iadd" + "\n";
                break;
            case "!":
                expr += "ifeq " + l2 + "\n"
                        + l1 + ": " + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;
        }
        return expr;
    }


    private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();

        expr += newTexts.get(ctx.expr(0));
        expr += newTexts.get(ctx.expr(1));

        switch (ctx.getChild(1).getText()) {
            case "*":
                expr += "imul \n"; break;
            case "/":
                expr += "idiv \n"; break;
            case "%":
                expr += "irem \n"; break;
            case "+":		// expr(0) expr(1) iadd
                expr += "iadd \n"; break;
            case "-":
                expr += "isub \n"; break;

            case "==":
                expr += "isub " + "\n"
                        + "ifeq l2"+ "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;
            case "!=":
                expr += "isub " + "\n"
                        + "ifne l2"+ "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;
            case "<=":
                // <(5) Fill here>
                expr += "isub " + "\n"
                        + "ifle 12" + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": " + "\n";
                break;
            case "<":
                // <(6) Fill here>
                expr += "isub " + "\n"
                        + "iflt 12" + "\n"
                        + "ldc 0"+"\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": "+ "\n"
                        + lend + ": " + "\n";
                break;

            case ">=":
                // <(7) Fill here>
                expr += "isub " + "\n"
                        + "ifge 12" + "\n"
                        + "ldc 0"+"\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": "+ "\n"
                        + lend + ": " + "\n";
                break;

            case ">":
                // <(8) Fill here>
                expr += "isub " + "\n"
                        + "ifgt 12" + "\n"
                        + "ldc 0"+"\n"
                        + "goto " + lend + "\n"
                        + l2 + ": " + "ldc 1" + "\n"
                        + lend + ": "+ "\n"
                        + lend + ": " + "\n";
                break;

            case "and":
                expr += "iand \n";
            case "or":
                // <(9) Fill here>
                expr += "ior "+ "\n";
                break;

        }
        return expr;
    }
    private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
        String fname = getFunName(ctx);
        expr = "jal x1,  label" + symbolTable.getFunLabel(fname) + "\n";

        return expr;

    }

    // args	: expr (',' expr)* | ;
    @Override
    public void exitArgs(MiniCParser.ArgsContext ctx) {

        String argsStr = "\n";

        for (int i=0; i < ctx.expr().size() ; i++) {
            argsStr += newTexts.get(ctx.expr(i)) ;
        }
        newTexts.put(ctx, argsStr);
    }

}
