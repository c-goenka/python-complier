package analyzer;

import common.analysis.AbstractNodeAnalyzer;
import common.analysis.types.*;
import common.astnodes.*;
import java.util.*;
import static common.analysis.types.Type.*;

/** Analyzer that performs PyLang type checks on all nodes.  Applied after
 *  collecting declarations. */
public class TypeChecker extends AbstractNodeAnalyzer<Type> {

    /** The current symbol table (changes depending on the function
     *  being analyzed). */
    private SymbolTable<Type> sym;
    private final SymbolTable<Type> symGlobal;
    private final HashMap<String, SymbolTable<Type>> allClassesSym;
    private final HashMap<String, String> class_hierarchy;
    /** Collector for errors. */
    private final Errors errors;

    /** Creates a type checker using GLOBALSYMBOLS for the initial global
     *  symbol table and ERRORS0 to receive semantic errors. */
    public TypeChecker(SymbolTable<Type> globalSymbols, Errors errors0, HashMap<String, String> hierarchy, HashMap<String, SymbolTable<Type>> classes_sym) {
        sym = globalSymbols;
        symGlobal = globalSymbols;
        allClassesSym = classes_sym;
        class_hierarchy = hierarchy;
        errors = errors0;
    }

    /** Inserts an error message in NODE if there isn't one already.
     *  The message is constructed with MESSAGE and ARGS as for
     *  String.format. */
    private void err(Node node, String message, Object... args) {
        errors.semError(node, message, args);
    }

    /* ---------------PROGRAM------------------- */

    @Override
    public Type analyze(Program program) {
        for (Declaration decl : program.declarations) {
            decl.dispatch(this);
        }
        for (Stmt stmt : program.statements) {
            stmt.dispatch(this);
        }
        return null;
    }

    /* ---------------LITERALS------------------- */

    @Override
    public Type analyze(IntegerLiteral i) {
        return i.setInferredType(Type.INT_TYPE);
    }
    @Override
    public Type analyze(StringLiteral i) {
        return i.setInferredType(Type.STR_TYPE);
    }
    @Override
    public Type analyze(BooleanLiteral i) {
        return i.setInferredType(Type.BOOL_TYPE);
    }
    @Override
    public Type analyze(NoneLiteral i) {
        return i.setInferredType(Type.NONE_TYPE);
    }


    /* ---------------DEFINITIONS------------------- */

    @Override
    public Type analyze(VarDef varDef) {
        Identifier id = varDef.getIdentifier();
        String name = id.name;
        Type defType = sym.get(name);
        Type valInfType = varDef.value.dispatch(this);

        if (!isAssignmentCompatible(defType, valInfType)) {
            err(varDef, "Expected type `%s`; got type `%s`", defType, valInfType);
        }

        return null;
    }

    @Override
    public Type analyze(FuncDef funcDef) {
        Identifier func_id = funcDef.getIdentifier();
        String func_name = func_id.name;

        sym = sym.getNestedSym(func_name);

        for (Declaration decl : funcDef.declarations) {
            decl.dispatch(this);
        }
        for (Stmt stmt : funcDef.statements) {
            stmt.dispatch(this);
        }

        sym = sym.getParent();
        return null;
    }

    @Override
    public Type analyze(ClassDef classDef) {
        Identifier id = classDef.getIdentifier();
        String name = id.name;

        sym = allClassesSym.get(name);

        for (Declaration decl : classDef.declarations) {
            decl.dispatch(this);
        }

        sym = sym.getParent();
        return null;
    }

    /* ---------------EXPRESSIONS------------------- */

    @Override
    public Type analyze(Identifier id) {
        String varName = id.name;
        Type varType = sym.get(varName);


        if (varType != null) {
            return id.setInferredType(varType);
        } else if (symGlobal.declares(varName)) {
            return id.setInferredType(symGlobal.get(varName));
        }

        err(id, "Not a variable: %s", varName);
        return id.setInferredType(ValueType.OBJECT_TYPE);
    }

    @Override
    public Type analyze(BinaryExpr e) {
        Type t1 = e.left.dispatch(this);
        Type t2 = e.right.dispatch(this);

        switch (e.operator) {
            case "-":
            case "*":
            case "//":
            case "%":
                if (!INT_TYPE.equals(t1) || !INT_TYPE.equals(t2)) {
                    err(e, "Cannot apply operator `%s` on types `%s` and `%s`",
                            e.operator, t1, t2);
                }
                return e.setInferredType(INT_TYPE);
            case "+":
                if (INT_TYPE.equals(t1) && INT_TYPE.equals(t2)) {
                    return e.setInferredType(INT_TYPE);
                } else if (STR_TYPE.equals(t1) && STR_TYPE.equals(t2)) {
                    return e.setInferredType(STR_TYPE);
                } else if (t1.isListType() && t2.isListType()) {
                    String t1_name = t1.elementType().className();
                    String t2_name = t2.elementType().className();
                    Type lub = sym.get(getLeastUpperBound(t1_name, t2_name));
                    return e.setInferredType(new ListValueType(lub));
                } else if (INT_TYPE.equals(t1) || INT_TYPE.equals(t2)) {
                    err(e, "Cannot apply operator `%s` on types `%s` and `%s`", e.operator, t1, t2);
                    return e.setInferredType(INT_TYPE);
                }
                else {
                    err(e, "Cannot apply operator `%s` on types `%s` and `%s`", e.operator, t1, t2);
                    return e.setInferredType(OBJECT_TYPE);
                }
            case "<":
            case "<=":
            case ">":
            case ">=":
                if (!INT_TYPE.equals(t1) || !INT_TYPE.equals(t2)) {
                    err(e, "Cannot apply operator `%s` on types `%s` and `%s`", e.operator, t1, t2);
                }
                return e.setInferredType(BOOL_TYPE);
            case "==":
            case "!=":
                if ((!INT_TYPE.equals(t1) || !INT_TYPE.equals(t2)) &&
                        (!BOOL_TYPE.equals(t1) || !BOOL_TYPE.equals(t2)) &&
                        (!STR_TYPE.equals(t1) || !STR_TYPE.equals(t2))) {
                    err(e, "Cannot apply operator `%s` on types `%s` and `%s`", e.operator, t1, t2);
                }
                return e.setInferredType(BOOL_TYPE);
            case "and":
            case "or":
                if (!BOOL_TYPE.equals(t1) && !BOOL_TYPE.equals(t2)) {
                    err(e, "Cannot apply operator `%s` on types `%s` and `%s`", e.operator, t1, t2);
                }
                return e.setInferredType(BOOL_TYPE);
            case "is":
                if (t1.isSpecialType() || t2.isSpecialType()) {
                    err(e, "Cannot apply operator `%s` on types `%s` and `%s`", e.operator, t1, t2);
                }
                return e.setInferredType(BOOL_TYPE);
            default:
                return e.setInferredType(OBJECT_TYPE);
        }
    }

    @Override
    public Type analyze(UnaryExpr e) {
        Type operandType = e.operand.dispatch(this);
        String operator = e.operator;

        switch (operator) {
            case "-":
                if (!INT_TYPE.equals(operandType)) {
                    err(e, "Cannot apply operator `%s` on type `%s`", operator, operandType);
                }
                return e.setInferredType(INT_TYPE);
            case "not":
                if (!BOOL_TYPE.equals(operandType)) {
                    err(e, "Cannot apply operator `%s` on type `%s`", operator, operandType);
                }
                return e.setInferredType(BOOL_TYPE);
            default:
                return e.setInferredType(OBJECT_TYPE);
        }
    }

    @Override
    public Type analyze(IfExpr e) {
        e.condition.dispatch(this);
        Type thenType = e.thenExpr.dispatch(this);
        Type elseType = e.elseExpr.dispatch(this);
        Type lub = sym.get(getLeastUpperBound(thenType.className(), elseType.className()));
        return e.setInferredType(lub);
    }

    @Override
    public Type analyze(CallExpr e) {
        String funcName = e.function.name;
        Type funcType = sym.get(funcName);
        Type returnType;

        List<Type> arguments = new ArrayList<>();
        for (Expr ex : e.args) {
            arguments.add(ex.dispatch(this));
        }

        List<ValueType> parameters = null;
        if (funcType instanceof FuncType) {
            e.function.dispatch(this);
            parameters = ((FuncType) funcType).parameters;
            returnType = ((FuncType) funcType).returnType;
        }
        else if (funcType instanceof UserDefClassType) {
            returnType = new ClassValueType(funcType.className());
        }
        else {
            err(e, "Not a function or class: %s", funcName);
            return e.setInferredType(OBJECT_TYPE);
        }

        if (parameters != null) {
            if (parameters.size() != arguments.size()) {
                if (!(funcName.equals("print") && (arguments.size() == 1))){
                    err(e, "Expected %d arguments; got %d", parameters.size(), arguments.size());
                }
            }
            else {
                for (int i = 0; i < parameters.size(); i++) {
                    if (!isAssignmentCompatible(parameters.get(i), arguments.get(i))) {
                        err(e, "Expected type `%s`; got type `%s` in parameter %s", parameters.get(i), arguments.get(i), i);
                        break;
                    }
                }
            }
        } else if (!arguments.isEmpty()) {
            err(e, "Expected 0 arguments; got %d", arguments.size());
        }

        return e.setInferredType(returnType);
    }

    @Override
    public Type analyze(MethodCallExpr e) {
        Type methodInfType = methodCallHelp(e);

        if (!methodInfType.isFuncType()) {
            Type objInfType = e.method.object.dispatch(this);
            String objClassName = objInfType.className();
            String memName = e.method.member.name;
            err(e, "There is no method named `%s` in class `%s`", memName, objClassName);
            return e.setInferredType(OBJECT_TYPE);
        }

        e.method.dispatch(this);

        List<Type> arguments = new ArrayList<>();
        for (Expr ex : e.args) {
            arguments.add(ex.dispatch(this));
        }

        List<ValueType> parameters = ((FuncType) methodInfType).parameters;
        Type returnType = ((FuncType) methodInfType).returnType;

        ValueType first_param = null;
        if (!parameters.isEmpty()) {
            first_param = parameters.get(0);
            parameters.remove(0); // remove the first argument (the object itself)
        }

        if (!parameters.isEmpty()) {
            if (parameters.size() == arguments.size()) {
                for (int i = 0; i < parameters.size(); i++) {
                    if (!isAssignmentCompatible(parameters.get(i), arguments.get(i))) {
                        err(e, "Expected type `%s`; got type `%s` in parameter %s", parameters.get(i), arguments.get(i), i+1);
                        break;
                    }
                }
            }
            else {
                err(e, "Expected %d arguments; got %d", parameters.size(), arguments.size());
            }
        } else if (!arguments.isEmpty()) {
            err(e, "Expected 0 arguments; got %d", arguments.size());
        }

        if (first_param != null) {
            parameters.add(0, first_param);
        }

        return e.setInferredType(returnType);
    }

    @Override
    public Type analyze(IndexExpr e) {
        Type listType = e.list.dispatch(this);
        Type indexType = e.index.dispatch(this);

        if (!STR_TYPE.equals(listType) && !listType.isListType()) {
            err(e, "Cannot index into type `%s`", listType);
            return e.setInferredType(OBJECT_TYPE);
        }

        if (listType.isListType()) {
            listType = listType.elementType();
        }

        if (!INT_TYPE.equals(indexType)) {
            err(e, "Index is of non-integer type `%s`", indexType);
        }

        return e.setInferredType(listType);
    }

    @Override
    public Type analyze(MemberExpr e) {
        Type objInfType = e.object.dispatch(this);
        String objClassName = objInfType.className();
        String memName = e.member.name;

        return findMemberType(objClassName, memName, e);
    }

    @Override
    public Type analyze(ListExpr e) {
        if (e.elements.isEmpty()) {
            return e.setInferredType(EMPTY_TYPE);
        }
        Type lub = new ClassValueType((String) null);
        for (Expr elem : e.elements) {
            Type elem_type = elem.dispatch(this);
            lub = sym.get(getLeastUpperBound(lub.className(), elem_type.className()));
        }
        return e.setInferredType(new ListValueType(lub));
    }


    /* ---------------STATEMENTS------------------- */

    @Override
    public Type analyze(ExprStmt s) {
        s.expr.dispatch(this);
        return null;
    }

    @Override
    public Type analyze(AssignStmt s) {
        Type valInfType = s.value.dispatch(this);
        if (s.targets.size() > 1 && valInfType.isListType() && NONE_TYPE.equals(valInfType.elementType())) {
            err(s, "Special error case of [<None>] multiple assignment");
        }
        for (Expr target : s.targets) {
            Type targetType = target.dispatch(this);
            if (target instanceof IndexExpr && STR_TYPE.equals(valInfType)) {
                err(target, "`%s` is not a list type", valInfType);
            }
            if (!isAssignmentCompatible(targetType, valInfType)) {
                err(s, "Expected type `%s`; got type `%s`", targetType, valInfType);
            }
        }
        return null;
    }

    @Override
    public Type analyze(IfStmt s) {
        s.condition.dispatch(this);
        for (Stmt stmt : s.thenBody) {
            stmt.dispatch(this);
        }
        for (Stmt stmt : s.elseBody) {
            stmt.dispatch(this);
        }
        return null;
    }

    @Override
    public Type analyze(ForStmt s) {
        s.identifier.dispatch(this);
        s.iterable.dispatch(this);
        for (Stmt stmt : s.body) {
            stmt.dispatch(this);
        }
        return null;
    }

    @Override
    public Type analyze(WhileStmt s) {
        s.condition.dispatch(this);
        for (Stmt stmt : s.body) {
            stmt.dispatch(this);
        }
        return null;
    }

    @Override
    public Type analyze(ReturnStmt s) {
        Expr value = s.value;
        Type returnInfType = null;
        if (value != null) {
            returnInfType = value.dispatch(this);
        }
        if (returnInfType != null) {
            Type returnDefType = sym.get("return_val");
            if (!isAssignmentCompatible(returnDefType, returnInfType)) {
                err(s, "Expected type `%s`; got type `%s`", returnDefType, returnInfType);
            }
        } else if (sym.get("return_val") != null) {
            err(s, "Expected type `%s`; got `None`", sym.get("return_val"));

        }
        return null;
    }


    /* ---------------TYPE-CHECKING-HELPER-METHODS------------------- */

    // Check if the value type is assignment compatible with the definition type
    public Boolean isAssignmentCompatible(Type defType, Type valInfType) {
        if (defType == null) {
            return false;
        }
        if (!defType.isSpecialType() && NONE_TYPE.equals(valInfType)) {
            return true;
        }
        if (NONE_TYPE.equals(valInfType) && !defType.isSpecialType()) {
            return true;
        }
        if (defType instanceof ListValueType && EMPTY_TYPE.equals(valInfType)) {
            return true;
        }
        if (defType instanceof ListValueType && valInfType instanceof ListValueType) {
            if (NONE_TYPE.equals(valInfType.elementType()) && isAssignmentCompatible(valInfType.elementType(), NONE_TYPE)) {
                return true;
            }
            if (defType.elementType().equals(valInfType.elementType())) {
                return true;
            }
        }
        return valInfType != null && conformsTo(defType.className(), valInfType.className());
    }

    // Checks if the value type conforms to the definition type
    public Boolean conformsTo(String target, String value) {
        if (target != null && target.equals("object")) {
            return true;
        }
        if (target != null && target.equals(value)) {
            return true;
        }
        String value_ancestor = class_hierarchy.get(value);
        while (value_ancestor != null) {
            if (value_ancestor.equals(target)) {
                return true;
            }
            value_ancestor = class_hierarchy.get(value_ancestor);
        }
        return false;
    }

    // Returns the least upper bound of two classes
    public String getLeastUpperBound(String class1, String class2) {
        if (class1 == null) {
            return class2;
        } else if (class2 == null) {
            return class1;
        }

        List<String> class1_path = generatePathToRoot(class1);
        List<String> class2_path = generatePathToRoot(class2);

        int min_len = Math.min(class1_path.size(), class2_path.size());
        String lub = null;

        for (int i = 0; i < min_len; i++) {
            if (class1_path.get(i).equals(class2_path.get(i))) {
                lub = class1_path.get(i);
            } else {
                break; // Paths diverge, no point in continuing
            }
        }
        return lub;
    }

    // Helper for getLeastUpperBound
    private List<String> generatePathToRoot(String className) {
        List<String> path = new ArrayList<>();
        while (!className.equals("object")) {
            path.add(className);
            className = class_hierarchy.get(className);
            if (className == null) {
                break;
            }
        }
        path.add("object"); // Assuming "object" is the ultimate root
        Collections.reverse(path);
        return path;
    }

    // Helper for methodCallExpr to avoid dispatch when method is not found
    public Type methodCallHelp(MethodCallExpr e) {
        Type objInfType = e.method.object.dispatch(this);
        String objClassName = objInfType.className();
        String memName = e.method.member.name;

        return findMemberType(objClassName, memName, e);
    }

    // Helper for finding the type of member in a class
    private Type findMemberType(String objClassName, String memName, Expr e) {
        String original_name = objClassName;
        SymbolTable<Type> objClassSym = allClassesSym.get(objClassName);

        while (objClassSym != null) {
            if (objClassSym.declares(memName)) {
                Type memType = objClassSym.get(memName);
                return e.setInferredType(memType);
            }
            objClassName = class_hierarchy.get(objClassName); // Move up in the class hierarchy
            objClassSym = allClassesSym.get(objClassName);
        }

        if (e instanceof MemberExpr) {
            err(e, "There is no attribute named `%s` in class `%s`", memName, original_name);
        }
        e.setInferredType(OBJECT_TYPE);
        return OBJECT_TYPE; // Return default type if the member was not found in the hierarchy
    }

}

