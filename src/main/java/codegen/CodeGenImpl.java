package codegen;

import java.util.List;

import common.analysis.SymbolTable;
import common.analysis.AbstractNodeAnalyzer;
import common.analysis.types.Type;
import common.astnodes.*;
import common.codegen.CodeGenBase;
import common.codegen.FuncInfo;
import common.codegen.Label;
import common.codegen.RiscVBackend;
import common.codegen.SymbolInfo;

import static common.codegen.RiscVBackend.Register.*;


public class CodeGenImpl extends CodeGenBase {

    /** A code generator emitting instructions to BACKEND. */
    public CodeGenImpl(RiscVBackend backend) {
        super(backend);
    }

    /** Operation on None. */
    private final Label errorNone = new Label("error.None");
    /** Division by zero. */
    private final Label errorDiv = new Label("error.Div");
    /** Index out of bounds. */
    private final Label errorOob = new Label("error.OOB");

    /**
     * Emits the top level of the program.
     *
     * This method is invoked exactly once, and is surrounded
     * by code that: (1) initializes the heap before the
     * top-level begins and (2) exits after the top-level ends.
     *
     *
     * @param statements top level statements
     */
    protected void emitTopLevel(List<Stmt> statements) {
        StmtAnalyzer stmtAnalyzer = new StmtAnalyzer(null);
        backend.emitADDI(SP, SP, -2 * backend.getWordSize(),
                         "Saved FP and saved RA (unused at top level).");
        backend.emitSW(ZERO, SP, 0, "Top saved FP is 0.");
        backend.emitSW(ZERO, SP, 4, "Top saved RA is 0.");
        backend.emitADDI(FP, SP, 2 * backend.getWordSize(),
                         "Set FP to previous SP.");

        FuncInfo makeInt = makeFuncInfo("makeInt",0, Type.OBJECT_TYPE,
                globalSymbols,null,this::emitMakeInt);
        globalSymbols.put(makeInt.getBaseName(), makeInt);


        functions.add(makeInt);

        for (Stmt stmt : statements) {
            stmt.dispatch(stmtAnalyzer);
        }
        backend.emitLI(A0, EXIT_ECALL, "Code for ecall: exit");
        backend.emitEcall(null);
    }

    /**
     * Emits the code for a function described by FUNCINFO.
     *
     * This method is invoked once per function and method definition.
     * At the code generation stage, nested functions are emitted as
     * separate functions of their own. So if function `bar` is nested within
     * function `foo`, you only emit `foo`'s code for `foo` and only emit
     * `bar`'s code for `bar`.
     */
    protected void emitUserDefinedFunction(FuncInfo funcInfo) {
        backend.emitGlobalLabel(funcInfo.getCodeLabel());
        StmtAnalyzer stmtAnalyzer = new StmtAnalyzer(funcInfo);

        for (Stmt stmt : funcInfo.getStatements()) {
            stmt.dispatch(stmtAnalyzer);
        }

        backend.emitMV(A0, ZERO, "Returning None implicitly");
        backend.emitLocalLabel(stmtAnalyzer.epilogue, "Epilogue");

        backend.emitLW(FP,FP,0,"Restoring FP with FP");

        backend.emitJR(RA, "Return to caller");
    }

    void emitMakeInt(FuncInfo funcInfo) {

    }

    /** An analyzer that encapsulates code generation for statments. */
    private class StmtAnalyzer extends AbstractNodeAnalyzer<Void> {
        /*
         * A SymbolInfo can be one the following:
         * - ClassInfo: a descriptor for classes
         * - FuncInfo: a descriptor for functions/methods
         * - AttrInfo: a descriptor for attributes
         * - GlobalVarInfo: a descriptor for global variables
         * - StackVarInfo: a descriptor for variables allocated on the stack,
         *      such as locals and parameters
         *
         * At this point the input program is assumed to be semantically
         * valid and well-typed at this stage, so assume that
         * the symbol table contains valid information. For example, in
         * an expression `foo()` you KNOW that sym.get("foo") will either be
         * a FuncInfo or ClassInfo, but not any of the other infos
         * and never null.
         *
         * The symbol table in funcInfo - query it with an identifier name to
         * get a descriptor for a function, class, variable, etc.
         *
         * The symbol table also maps nonlocal and global vars, so
         * only need to lookup one symbol table and it will fetch the
         * appropriate info for the var that is currently in scope.
         */

        /** Symbol table for my statements. */
        private SymbolTable<SymbolInfo> sym;

        /** Label of code that exits from procedure. */
        protected Label epilogue;

        /** The descriptor for the current function, or null at the top
         *  level. */
        private FuncInfo funcInfo;

        /** An analyzer for the function described by FUNCINFO0, which is null
         *  for the top level. */
        StmtAnalyzer(FuncInfo funcInfo0) {
            funcInfo = funcInfo0;
            if (funcInfo == null) {
                sym = globalSymbols;
            } else {
                sym = funcInfo.getSymbolTable();
            }
            epilogue = generateLocalLabel();
        }

        @Override
        public Void analyze(ReturnStmt stmt) {
            stmt.value.dispatch(exprAnalyzer);
            backend.emitJ(epilogue,"jump to epilogue to avoid returning null");
            return null;
        }

        @Override
        public Void analyze(AssignStmt stmt) {
            return null;
        }

        ExprAnalyzer exprAnalyzer = new ExprAnalyzer(funcInfo);

        @Override
        public Void analyze(ExprStmt stmt) {
            stmt.expr.dispatch(exprAnalyzer);
            return null;
        }

        @Override
        public Void analyze(ForStmt stmt) {
            return null;
        }

        @Override
        public Void analyze(IfStmt stmt) {
            return null;
        }

        @Override
        public Void analyze(WhileStmt stmt) {
            return null;
        }



    }

    private class ExprAnalyzer extends AbstractNodeAnalyzer<Void> {
        private SymbolTable<SymbolInfo> sym;
        private FuncInfo funcInfo;
        ExprAnalyzer(FuncInfo funcInfo0) {
            funcInfo = funcInfo0;
            if (funcInfo == null) {
                sym = globalSymbols;
            } else {
                sym = funcInfo.getSymbolTable();
            }
        }

        //EXPRESSIONS

        @Override
        public Void analyze(CallExpr expr) {
            backend.emitSW(RA,SP,-4,"Saving return address");
            backend.emitSW(FP,SP,-8,"Saving frame pointer");
            backend.emitMV(FP,SP,"Setting fp to sp");
            backend.emitADDI(SP,SP,-8,"SP pointing to top of stack (currently at the return address)");
            for(Expr e:expr.args) {
                e.dispatch(this);
                backend.emitSW(A0,SP,-4,"pushing next param onto stack");
                backend.emitADDI(SP,SP,-4,"shifting SP onto end of stack");
            }
            backend.emitJAL(((FuncInfo)sym.get(expr.function.name)).getCodeLabel(),
                    String.format("Invoke function %s",expr.function.name));
            backend.emitLW(RA,FP,-4,"Restoring RA");
            backend.emitMV(SP,FP,"Restoring SP");
            return null;
        }

        @Override
        public Void analyze(BinaryExpr expr) {
            expr.right.dispatch(this);
            backend.emitSW(A0,SP,-4,"Pushing right summand onto the stack");
            backend.emitADDI(SP,SP,-4,"Adjusting SP");
            expr.left.dispatch(this);
            backend.emitLW(T0,SP,0,"Popping right summand from stack");
            if(expr.operator.equals("+")) {
                backend.emitADD(A0,A0,T0,"Operator +");
            }
            if(expr.operator.equals("*")) {
                backend.emitMUL(A0,A0,T0,"Operator *");
            }
            if(expr.operator.equals("-")) {
                backend.emitSUB(A0,A0,T0,"Operator -");
            }
            return null;
        }

        @Override
        public Void analyze(IfExpr expr) {
            return null;
        }

        @Override
        public Void analyze(IndexExpr expr) {
            return null;
        }

        @Override
        public Void analyze(ListExpr expr) {
            return null;
        }

        @Override
        public Void analyze(MemberExpr expr) {
            return null;
        }

        @Override
        public Void analyze(MethodCallExpr expr) {
            return null;
        }

        @Override
        public Void analyze(UnaryExpr expr) {
            expr.operand.dispatch(this);
            backend.emitADD(T0,A0,ZERO,"loading argument into temp register");
            backend.emitSUB(A0,ZERO,T0,"replacing A0 with negative A0");
            return null;
        }

        @Override
        public Void analyze(Identifier id) {
            return null;
        }

        // LITERALS

        @Override
        public Void analyze(BooleanLiteral literal) {
            backend.emitLA(A0,constants.fromLiteral(literal),
                    "Load the address of the literal into a0");
            return null;

        }

        @Override
        public Void analyze(IntegerLiteral literal) {
            backend.emitLA(A0,constants.fromLiteral(literal),
                    "Load the address of the literal into a0");
            return null;
        }

        @Override
        public Void analyze(NoneLiteral literal) {
            backend.emitLA(A0,constants.fromLiteral(literal),
                    "Load the address of the literal into a0");
            return null;
        }

        @Override
        public Void analyze(StringLiteral literal) {
            backend.emitLA(A0,constants.fromLiteral(literal),
                    "Load the address of the literal into a0");
            return null;
        }
    }

    /*
    Takes an integer in register A0 and converts it to a pointer to an int object in memory containing that integer
     */
    public Void boxInt() {

        return null;
    }

    /**
     * Emits custom code in the CODE segment.
     */
    protected void emitCustomCode() {
        emitErrorFunc(errorNone, "Operation on None");
        emitErrorFunc(errorDiv, "Division by zero");
        emitErrorFunc(errorOob, "Index out of bounds");
    }

    /** Emit an error routine labeled ERRLABEL that aborts with message MSG. */
    private void emitErrorFunc(Label errLabel, String msg) {
        backend.emitGlobalLabel(errLabel);
        backend.emitLI(A0, ERROR_NONE, "Exit code for: " + msg);
        backend.emitLA(A1, constants.getStrConstant(msg),
                       "Load error message as str");
        backend.emitADDI(A1, A1, getAttrOffset(strClass, "__str__"),
                         "Load address of attribute __str__");
        backend.emitJ(abortLabel, "Abort");
    }
}
