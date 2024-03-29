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
        //symbolTable.initFunDecl();
        //함수에 들어갈 때 parameter를 x10~x17에 저장해야함.

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
            int size = Integer.parseInt(ctx.LITERAL().getText());
            symbolTable.putMem(varName, size*0x4);
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
            symbolTable.putMem(getLocalVarName(ctx), Integer.parseInt(ctx.LITERAL().getText())*0x4);
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
                + "beq " + symbolTable.getVarId(ctx.expr().getText()) +", x0, " + Lend + "\n"
                + loopStmt + "\n"
                + "beq x0, x0, " + Lloop + "\n"
                + Lend + ":" + "\n";

        newTexts.put(ctx, stmt);
    }


    @Override
    public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
        // <(2) Fill here!>
        String stmt = "";
        int sp = symbolTable.getParamsSize(ctx.params())*8;

        String funcHeader = funcHeader(ctx, getFunName(ctx));
        stmt += funcHeader
                + newTexts.get(ctx.compound_stmt());

        if(sp != 0) {
            stmt += "addi sp, sp, " + sp + "\n";
        }
        stmt += "jalr x0, 0(x1)\n\n";
        newTexts.put(ctx, stmt);
    }


    private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
        String label = symbolTable.getFunLabel(fname);
        String stmt = "";
        int sp = symbolTable.getParamsSize(ctx.params())*8; // x1 저장해야함
        if(sp == 0) sp = 8;
        stmt = "label" + label + ":\n"
                + "addi sp, sp, -" + sp + "\n"
                + "sd x1, "+sp + "(sp)\n";
        sp-=8;
        for(int i = 0; i < ctx.params().param().size(); i++){
            stmt += "sd " + symbolTable.getVarId(getParamName(ctx.params().param(i))) + " " + sp + "(sp)\n";
            sp -= 8;
        }
        return stmt;
    }


    // 전역변수
    // virtual memory 구현? - spilling
    // if global reg > 9 인 경우에 0x50000000에 data를 저장 - symboltable에 저장된 위치를 저장
    @Override
    public void exitVar_decl(MiniCParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();
        String varDecl = "";
        String temp = symbolTable.getNewTempVar();

        if (isDeclWithInit(ctx)) {
            if(symbolTable.getVarId(ctx.IDENT().getText()) == "ERROR"){
                varDecl += "addi " + temp + ", x0, " + ctx.LITERAL().getText() + "\n"
                        + "sd " + temp + ", 0(0x" + Integer.toHexString(symbolTable.getVirtualMemAddr(varName)) + ")\n";
            }else{
                varDecl += "addi " + symbolTable.getVarId(varName) + ", x0, " + ctx.LITERAL().getText() + "\n";
            }

            //varDecl += "putfield " + varName + "\n";
            // v. initialization => Later! skip now..:
        }
        newTexts.put(ctx, varDecl);
    }


    @Override
    public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
        String varDecl = "";

        if (isDeclWithInit(ctx)) {
            String vId = symbolTable.getVarId(ctx);
            varDecl += "addi "+ vId + ", x0, " + ctx.LITERAL().getText();
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
                    + "bne " + symbolTable.getVarId(ctx.expr().getText()) + ", x0, " + lend +"\n"
                    + thenStmt + "\n"
                    + lend + ":"  + "\n";
        }
        else {
            String elseStmt = newTexts.get(ctx.stmt(1));
            stmt += condExpr + "\n"
                    + "bne " + symbolTable.getVarId(ctx.expr().getText()) + ", x0 " + lelse +"\n"
                    + thenStmt + "\n"
                    + "beq x0, x0, " + lend + "\n"
                    + lelse + ": \n"
                    + elseStmt + "\n"
                    + lend + ":"  + "\n";
        }

        newTexts.put(ctx, stmt);
    }


    // return_stmt	: RETURN ';' | RETURN expr ';'
    @Override
    public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
        // <(4) Fill here>

        //load -> return
        String stmt = "add x10, " +  symbolTable.getVarId(ctx.expr().getText()) + ", x0\n";
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
                    expr += symbolTable.getVarId(idName);
                }
                //else	// Type int array => Later! skip now..
                //	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
            } else if (ctx.LITERAL() != null) {
                expr += ctx.LITERAL().getText();
//                String temp = symbolTable.getNewTempVar();
//                expr += "addi x" + temp + ", x0, " +ctx.LITERAL().getText() + "\n";
//                symbolTable.putTempVar(ctx.getText(), Integer.parseInt(temp));
            }
        } else if(ctx.getChildCount() == 2) { // UnaryOperation
            expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
        }
        else if(ctx.getChildCount() == 3) {
            if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
                expr = newTexts.get(ctx.expr(0));

            } else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
                if(ctx.expr(0).LITERAL() != null){
                    expr = "addi " + symbolTable.getVarId(ctx.IDENT().getText()) + ", x0, " + ctx.expr(0).LITERAL().getText();
                }else{
                    expr = newTexts.get(ctx.expr(0))
                            + "add " + symbolTable.getVarId(ctx.IDENT().getText()) + ", " + symbolTable.getVarId(ctx.expr(0).getText()) + ", x0" + "\n";
                }


            } else { 											// binary operation
                expr = handleBinExpr(ctx, expr);

            }
        }
        // IDENT '(' args ')' |  IDENT '[' expr ']'
        else if(ctx.getChildCount() == 4) {
            if(ctx.args() != null){		// function calls
                expr = handleFunCall(ctx, expr);
            } else { // expr
                // Arrays:
                String varName = ctx.IDENT().getText();
                String temp = symbolTable.getNewTempVar();
                expr += "ld " + temp + ", 0x" + Integer.parseInt(ctx.expr(0).LITERAL().getText()) + "(0x" + Integer.toHexString(symbolTable.getMemAddr(varName)) + ")\n";
            }
        }
        // IDENT '[' expr ']' '=' expr
        else { // Arrays:
            String varName = ctx.IDENT().getText();
            String temp = symbolTable.getNewTempVar();
            String imm = " ";
            String ex = newTexts.get(ctx.expr(1));
            if(ex.charAt(0) != 'x') imm = "i ";
            expr += "add" + imm + temp + ", x0, " + ex + "\n"
                    +"sd " + temp + ", " + Integer.parseInt(ctx.expr(0).LITERAL().getText()) + "(" + Integer.toHexString(symbolTable.getMemAddr(varName)) + ")\n";
        }
        newTexts.put(ctx, expr);
    }


    private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
        String l1 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();

        int intOP = Integer.parseInt(newTexts.get(ctx.expr(0)));
        String op = newTexts.get(ctx.expr(0));
        String temp = symbolTable.getNewTempVar();
        switch(ctx.getChild(0).getText()) {
            case "-":
                expr += "addi " + op + ", " + op + ", " + (-2*intOP) + "\n"; break;
            case "--":
                expr += "addi " + op + ", " + op + ", -1\n";
                break;
            case "++":
                expr += "addi " + op + ", " + op + ", 1\n";
                break;
            case "!":
                expr += "beq " + op + ", x0, " + l1 + "\n"
                        + "addi " + temp + ", x0, 1\n"
                        + "beq x0, x0, " +lend + "\n"
                        + l1 + ": \n"
                        + "addi " + temp + ", x0, 0\n"
                        + lend + ": \n";
                symbolTable.putTempVar(ctx.getText(),temp);
                break;
        }
        return expr;
    }


    private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();
        String imm = " ";
        String op1 = newTexts.get(ctx.expr(0));
        String op2 = newTexts.get(ctx.expr(1));

        String temp1 = symbolTable.getNewTempVar();
        String temp2 = symbolTable.getNewTempVar();

        if(op2.charAt(0) != 'x') imm = "i ";


        switch (ctx.getChild(1).getText()) {
            case "*":
                expr += "mul" + imm + temp1 + ", " + op1 + ", " + op2 + "\n";
                break;
            case "/":
                expr += "div" + imm + temp1 +", " + op1 + ", " + op2 + "\n"; break;
            case "%":
                expr += "rem" + imm + temp1 +", " + op1 + ", " + op2 + "\n"; break;
            case "+":
                expr += "add" + imm + temp1 + ", " + op1 + ", " + op2 + "\n"; break;
            case "-":
                expr += "sub" + imm + temp1 +", " + op1 + ", " + op2 + "\n"; break;

            case "==": //같으면 1, 같지 않으면 0
                expr += "sub" + imm + temp2 +", "+ op1 + ", "+ op2 + "\n"
                        + "bne " + temp2 + ", x0, "+l2 +"\n"
                        + "addi " + temp1 +", x0, 1\n"
                        + "bne x0, x0, "+ lend +"\n"
                        + l2 + ": \n"
                        + "add " + temp1 + ", x0, x0\n"
                        + lend + ": \n";
                break;
            case "!=": //같지않으면 1, 같으면 0
                expr += "sub" + imm + temp2 + ", " + op1 + ", " + op2 + "\n"
                        + "beq " + temp2 + ", x0, "+l2 +"\n"
                        + "addi "+ temp1 +", x0, 1\n"
                        + "bne x0, x0, "+ lend +"\n"
                        + l2 + ": \n"
                        + "add " + temp1 + ", x0, x0\n"
                        + lend + ": \n";
                break;
            case "<=": //작거나 같으면 1, 크면 0
                // <(5) Fill here>
                expr += "sub" + imm + temp2 + ", " + op1 + ", " + op2 + "\n"
                        + "bgt " + temp2 + ", x0, "+l2 +"\n"
                        + "addi " + temp1 + ", x0, 1\n"
                        + "bne x0, x0, "+ lend +"\n"
                        + l2 + ": \n"
                        + "add " + temp1 + ", x0, x0\n"
                        + lend + ": \n";
                break;
            case "<": // 작으면 1, 크거나 같으면 0
                // <(6) Fill here>
                expr += "sub" + imm + temp2 + ", " + op1 + ", " + op2 + "\n"
                        + "bge " + temp2 + ", x0, "+l2 +"\n"
                        + "addi " + temp1 + ", x0, 1\n"
                        + "bne x0, x0, "+ lend +"\n"
                        + l2 + ": \n"
                        + "add " + temp1 +", x0, x0\n"
                        + lend + ": \n";
                break;

            case ">=": //크거나 같으면 1, 작으면 0
                // <(7) Fill here>
                expr += "sub" + imm + temp2 + ", " + op1 + ", " + op2 + "\n"
                        + "blt " + temp2 + ", x0, "+l2 +"\n"
                        + "addi "+ temp1 +", x0, 1\n"
                        + "bne x0, x0, "+ lend +"\n"
                        + l2 + ": \n"
                        + "add " + temp1 +", x0, x0\n"
                        + lend + ": \n";
                break;

            case ">": //크면 1, 작거나 같으면 0
                // <(8) Fill here>
                expr += "sub" + imm + temp2 + ", " + op1 + ", " + op2 + "\n"
                        + "ble " + temp2 + ", x0, "+l2 +"\n"
                        + "addi " + temp1 +", x0, 1\n"
                        + "bne x0, x0, "+ lend +"\n"
                        + l2 + ": \n"
                        + "add " + temp1 + ", x0, x0\n"
                        + lend + ": \n";
                break;

            case "and":
                expr += "and " + temp1 + ", " + op1 + ", " + op2 + "\n";
            case "or":
                // <(9) Fill here>
                expr += "or " + temp1 +", " + op1 + ", " + op2 + "\n";
                break;
        }
        symbolTable.putTempVar(ctx.getText(), temp1);
        return expr;
    }

    //function 호출
    private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
        symbolTable.setParamID();
        String fname = getFunName(ctx);
        for(int i = 0; ctx.args().expr(i) != null; i++){
            if(ctx.args().expr(i).LITERAL() != null){
                String temp = symbolTable.getNewTempVar();
                expr += "addi " + temp + ", x0, " + ctx.args().expr(i).LITERAL() +"\n"
                        + "add " + symbolTable.getParam() + ", " + temp + ", x0\n";
            }
            else{
                expr += "add " + symbolTable.getParam() + ", " + symbolTable.getVarId(ctx.args().expr(i).getText()) + ", x0\n";
            }
        }
        expr += "jal x1, label" + symbolTable.getFunLabel(fname) + "\n";

        return expr;
    }

    // args	: expr (',' expr)* | ;
    @Override
    public void exitArgs(MiniCParser.ArgsContext ctx) {

        String argsStr = "";

        for (int i=0; i < ctx.expr().size() ; i++) {
            argsStr += newTexts.get(ctx.expr(i)) + "\n" ;
        }
        newTexts.put(ctx, argsStr);
    }

}
