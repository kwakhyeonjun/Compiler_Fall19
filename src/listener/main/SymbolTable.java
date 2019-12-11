package listener.main;

import java.util.HashMap;
import java.util.Map;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.Var_declContext;

import static listener.main.RiscVGenListenerHelper.*;


public class SymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}
	
	static public class VarInfo {
		Type type; 
		String id;
		int initVal;


        //변수에 저장될 값을 같이 입력하는 경우 ( ex: int a = 2;
        public VarInfo(Type type,  String id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
        //변수만 선언하는 경우
		public VarInfo(Type type,  String id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}
	
	static public class FInfo {
		public int label;
		public int paramsSize;
	}
	
	private Map<String, VarInfo> _register = new HashMap<>();	// register (x0~x31)
	private Map<String, FInfo> _fsymtable = new HashMap<>();

	//x0 register는 0을 저장, x1 ~ x4는 용례가 특수한 register
	private final int _registerX0 = 0;
	//따라서 register에 저장하는 것은 5부터
	private int _globalRegisterID = 5; //5~9의 레지스터 사용
	private int _localRegisterID = 18; //함수 내에서 사용할 수 있는 x18~x27 레지스터
	private int _tempRegisterID = 28; //함수 내에서 임시로 사용할 수 있는 x28~x31 레지스터
	private int _paramsID = 10; //parameter를 저장하는 register : x10~x17
	private int _labelID = 0; //label num 저장
	
//	SymbolTable(){
//		initFunDecl();
//		initFunTable();
//	}
//
//	void initFunDecl(){		// at each func decl
//		_localRegisterID = 18;
//		_tempRegisterID = 28;
//		_paramsID = 10;
//	}
	
	void putLocalVar(String varname, Type type){
		//<Fill here>
		_register.put(varname, new VarInfo(type, "x"+_localRegisterID++));
	}
	
	void putGlobalVar(String varname, Type type){
		//<Fill here>
		_register.put(varname, new VarInfo(type, "x"+_globalRegisterID++));
	}
	
	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here>
		_register.put(varname, new VarInfo(type, "x"+_localRegisterID++, initVar));
	}
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here>
		_register.put(varname, new VarInfo(type, "x"+_globalRegisterID++, initVar));
	}

	void putTempVar(String varname, String register){
	    _register.put(varname, new VarInfo(Type.INT, register));
    }

    String getTempVar(String varname){
	    return _register.get(varname).id;
    }

	 String getNewTempVar() {
	    if(_tempRegisterID >= 32){
	        setTempVar();
        }
		return "x"+_tempRegisterID++;
	}

	void setTempVar(){
		_tempRegisterID = 28;
	}

	int getParamsSize(MiniCParser.ParamsContext params){
		int paramsSize = 0;
		for(int i = 0; i < params.param().size(); i++){
			if(params.param(i).type_spec().INT() != null
				|| isArrayParamDecl(params.param(i)))
				paramsSize++;
		}
		return paramsSize;
	}


	void putParams(MiniCParser.ParamsContext params) {
		for(int i = 0; i < params.param().size(); i++) {
		//<Fill here>
			Type type;
			if(params.param(i).type_spec().VOID() != null)
				type = Type.VOID;

			else if(params.param(i).type_spec().INT() != null) {
				type = Type.INT;
			}
			else if(isArrayParamDecl(params.param(i))) {
				type = Type.INTARRAY;
			}
			else
				type = Type.ERROR;

			_register.put(getParamName(params.param(i)), new VarInfo(type, "x"+_paramsID++));
		}
	}

	void setParamID(){
	    _paramsID = 10;
    }


	String getParam(){
	    return "x"+_paramsID++;
    }

	public String getFunLabel(String fname) {
		// <Fill here>
		return Integer.toString(_fsymtable.get(fname).label);
	}

	public String getFunLabel(Fun_declContext ctx) {
		// <Fill here>
		return getFunLabel(getFunName(ctx));
	}

	/**
	 * 함수를 table에 저장 - label과 size필요
	 * @param ctx
	 * @return
	 */
	public String putFunSpecStr(Fun_declContext ctx) {
		String fname = getFunName(ctx);

		int paramsSize = getParamsSize(ctx.params());
		FInfo finfo = new FInfo();
		finfo.paramsSize = paramsSize;
		finfo.label = _labelID++;

		_fsymtable.put(fname, finfo);

		return Integer.toString(finfo.label);
	}
	
	String getVarId(String name){
		// <Fill here>
		VarInfo var = (VarInfo) _register.get(name);
		if(var != null){
			return var.id;
		}

		return "ERROR";
	}
	
	Type getVarType(String name){
		VarInfo var = (VarInfo) _register.get(name);
		if (var != null) {
			return var.type;
		}
		return Type.ERROR;	
	}

	String newLabel() {
		return "label" + _labelID++;
	}

	// global
	public String getVarId(Var_declContext ctx) {
		// <Fill here>
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
	
}
