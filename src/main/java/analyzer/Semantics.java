package analyzer;

import java.util.*;
import common.astnodes.*;
import common.analysis.AbstractNodeAnalyzer;
import common.analysis.types.*;

/**
 * Analyzes declarations to create a top-level symbol table.
 */
public class Semantics extends AbstractNodeAnalyzer<Type> {

    /** Current symbol table. Changes with new declarative region. */
    private SymbolTable<Type> sym = new SymbolTable<>();
    private SymbolTable<Type> TOP_SYM = new SymbolTable<>();
    /** Global symbol table. */
    private final SymbolTable<Type> globals = sym;
    /** Receiver for semantic error messages. */
    private final Errors errors;
    private final SymbolTable<Type> keywords = new SymbolTable<>();
    private final SymbolTable<Type> classes = new SymbolTable<>();
    private final SymbolTable<Type> objects = new SymbolTable<>();
    private final HashMap<String, SymbolTable<Type>> allClassesSym;
    private final SymbolTable<Type> symGlobal;

    /** A new declaration analyzer sending errors to ERRORS0. */
    public Semantics(Errors errors0, HashMap<String, String> classMap, SymbolTable<Type> globalSymbols, HashMap<String, SymbolTable<Type>> classes_sym) {
        sym = globalSymbols;
        symGlobal = globalSymbols;
        errors = errors0;
        classMap.forEach((key, value) -> classes.put(key, new ClassValueType("int")));
        allClassesSym = classes_sym;
    }


    @Override
    public Type analyze(Program program) {

        classes.put("int", new ClassValueType("int"));
        classes.put("str", new ClassValueType("str"));
        classes.put("bool", new ClassValueType("bool"));
        classes.put("object", new ClassValueType("object"));
        classes.put("<None>", Type.NONE_TYPE);

        objects.put("object", new ClassValueType("object"));

        keywords.put("int", new ClassValueType("int"));
        keywords.put("str", new ClassValueType("str"));
        keywords.put("bool", new ClassValueType("bool"));

        // adding predefined functions: print, input, and len from language reference.py page 11
        TOP_SYM.put("print", null);
        TOP_SYM.put("input", null);
        TOP_SYM.put("len", null);
        TOP_SYM.put("str", null);
        TOP_SYM.put("object", null);
        TOP_SYM.put("bool", null);
        TOP_SYM.put("int", new ClassValueType("int"));

        for (Declaration decl : program.declarations) {
            Identifier id = decl.getIdentifier();
            String name = id.name;

            if (decl instanceof FuncDef) {
                FuncDef fun = (FuncDef) decl;
                SymbolTable<Type> simple_sym = new SymbolTable<>();
                TOP_SYM.addNestedSym(fun.name.name, simple_sym);
            }

            Type type = decl.dispatch(this);

            if (type == null) {
                continue;
            }
            if (TOP_SYM.declares(name)) {
                errors.semError(id,
                        "Duplicate declaration of identifier in same "
                                + "scope: %s",
                        name);
            } else {
                TOP_SYM.put(name, type);
                if (type instanceof ValueType & type.isSpecialType()) {
                    globals.put(name, type);
                }
            }
        }

        for (Stmt st : program.statements) {
            if (st instanceof ReturnStmt) {
                errors.semError(st,
                        "Return statement cannot appear at the top level");
            }
        }
        return null;
    }

    @Override
    public Type analyze(VarDef varDef) {
        Type annotationType = ValueType.annotationToValueType(varDef.var.type);
        String className = annotationType.className();
        if (annotationType.isListType()) {
            className = annotationType.elementType().className();
        }
        if (!classes.declares(className)) {
            errors.semError(varDef.var.type,
                    "Invalid type annotation; there is no class named: %s",
                    className);
        }
        return annotationType;
    }

    @Override
    public Type analyze(TypedVar typedVar) {
        return ValueType.annotationToValueType(typedVar.type);
    }

    @Override
    public Type analyze(GlobalDecl globalDecl) {
        Identifier id = globalDecl.getIdentifier();
        String name = id.name;
        if (keywords.declares(name)) {
            errors.semError(id,
                "Not a global variable: %s",
                name);
        } else if (!symGlobal.declares(name)) {
            errors.semError(id,
                "Not a global variable: %s",
                name);
            
        } else if (!(symGlobal.get(name) instanceof ValueType)) {
            errors.semError(id,
                "Not a global variable: %s",
                name);
        }
        return symGlobal.get(name);
    }

    @Override
    public Type analyze(FuncDef funcDef) {
        Identifier func_id = funcDef.getIdentifier();
        String func_name = func_id.name;
        ValueType returnType = ValueType.annotationToValueType(funcDef.returnType);

        SymbolTable<Type> func_sym = new SymbolTable<>();
        SymbolTable<Type> parent_sym = null;

        if (sym != null) {
            sym = sym.getNestedSym(func_name);
        }

        if (classes.declares(func_name)) {
            errors.semError(func_id,
                    "Cannot shadow class name: %s",
                    func_name);
        }

        for (Declaration decl : funcDef.declarations) {
            Identifier decl_id = decl.getIdentifier();
            String decl_name = decl_id.name;

            if (decl instanceof FuncDef) {
                SymbolTable<Type> func_child_sym = new SymbolTable<>(func_sym);
                TOP_SYM.addNestedSym(decl_name, func_child_sym);
            } 
            Type decl_type = decl.dispatch(this);

            if (decl instanceof NonLocalDecl) {
                if (sym != null) {
                    parent_sym = sym.getParent();
                    if (parent_sym.equals(symGlobal)) {
                        parent_sym = null;
                    }
                }
                if (parent_sym == null) {
                    errors.semError(decl_id,
                            "Not a nonlocal variable: " + decl_name);
                } else if (!parent_sym.declares(decl_name) ) {
                    errors.semError(decl_id,
                            "Not a nonlocal variable: " + decl_name);
                } else if (!(parent_sym.get(decl_name) instanceof ClassValueType)) {
                    errors.semError(decl_id,
                            "Not a nonlocal variable: " + decl_name);
                }
                else if (parent_sym.get(decl_name) instanceof ClassValueType){
                    sym.put(decl_name, parent_sym.get(decl_name));
                    func_sym.put(decl_name, parent_sym.get(decl_name));
                }
            } else if (func_sym.declares(decl_name)) {
                errors.semError(decl_id,
                        "Duplicate declaration of identifier in same "
                                + "scope: %s",
                        decl_name);
            } else if (decl instanceof GlobalDecl) {
                if (func_sym.declares(decl_name)) {
                    errors.semError(decl_id,
                            "Duplicate declaration of identifier in same "
                                    + "scope: %s", decl_name);
                }
                if (decl_type != null) {
                    func_sym.put(decl_name, symGlobal.get(decl_name));
                }
            } else if (!(decl_type instanceof FuncType) && classes.declares(decl_name)) {
                errors.semError(decl_id,
                        "Cannot shadow class name: %s",
                        decl_name);
            } else {
                func_sym.put(decl_name, decl_type);
            }
        }

        for (Stmt statement : funcDef.statements) {
            if (statement instanceof AssignStmt) {
                AssignStmt curr_stmt = (AssignStmt) statement;
                for (Expr curr : curr_stmt.targets) {
                    if (curr instanceof Identifier) {
                        Identifier id = (Identifier) curr;
                        if (!func_sym.declares(id.name)) {
                            errors.semError(id,
                                    "Cannot assign to variable that is not explicitly declared in this scope: %s",
                                    id.name);
                        }
                    }
                }
            }
        }

        if (returnType.isSpecialType()) {
            // check that all paths have explicit return stmt
            boolean explicitReturn = false;
            for (Stmt path : funcDef.statements) {
                if (path instanceof IfStmt) {
                    IfStmt pathIf = (IfStmt) path;
                    for (Stmt thenStmt : pathIf.thenBody) {
                        if (thenStmt instanceof ReturnStmt) {
                            explicitReturn = true;
                            break;
                        }
                    }
                    if (!explicitReturn) {
                        errors.semError(func_id,
                                "All paths in this function/method must have a return statement: " + func_name,
                                func_name);
                    } else {
                        explicitReturn = false;
                        for (Stmt elseStmt : pathIf.elseBody) {
                            if (elseStmt instanceof ReturnStmt) {
                                explicitReturn = true;
                                break;
                            }
                        }
                        if (!explicitReturn) {
                            errors.semError(func_id,
                                    "All paths in this function/method must have a return statement: " + func_name,
                                    func_name);
                        }
                    }
                }
            }
        }

        List<ValueType> paramTypes = new ArrayList<>();
        for (TypedVar param : funcDef.params) {
            Identifier param_id = param.identifier;
            String param_name = param_id.name;
            Type param_type = param.dispatch(this);

            Type annotationType = ValueType.annotationToValueType(param.type);

            String className2 = ValueType.annotationToValueType(param.type).className();

            if (annotationType.isListType()) {
                className2 = annotationType.elementType().className();
            }
            if (!classes.declares(className2)) {
                errors.semError(param.type,
                        "Invalid type annotation; there is no class named: %s",
                        className2);
            }

            if (classes.declares(param_name)) {
                errors.semError(param_id,
                        "Cannot shadow class name: %s",
                        param_name);
            }

            if (func_sym.declares(param_name)) {
                errors.semError(param_id,
                        "Duplicate declaration of identifier in same "
                                + "scope: %s",
                        param_name);
            } else {
                func_sym.put(param_name, param_type);
                paramTypes.add((ValueType) param_type);
            }
        }

        String className = ValueType.annotationToValueType(funcDef.returnType).className();
        if (!classes.declares(className)) {
            errors.semError(funcDef.returnType,
                    "Invalid type annotation; there is no class named: %s",
                    className);
        }

        sym = func_sym.getParent();
        return new FuncType(paramTypes, returnType);
    }

    @Override
    public Type analyze(ClassDef classDef) {
        Identifier id = classDef.getIdentifier();
        String name = id.name;

        sym = allClassesSym.get(name);

        Identifier superClass = classDef.superClass;
        String superClassName = superClass.name;
        Type superClassType = TOP_SYM.get(superClassName);

        if (!TOP_SYM.declares(superClassName)) {
            errors.semError(superClass,
                    "Super-class not defined: %s",
                    superClassName);
        } else if (keywords.declares(superClassName)) {
            errors.semError(superClass,
                    "Cannot extend special class: %s",
                    superClassName);
        } else if (!(superClassType instanceof UserDefClassType) && (!objects.declares(superClassName))) {
            errors.semError(superClass,
                    "Super-class must be a class: %s",
                    superClassName);
        }

        SymbolTable<Type> class_sym = new SymbolTable<>(TOP_SYM);
        TOP_SYM.addNestedSym(name, class_sym);

        SymbolTable<Type> super_class_sym = TOP_SYM.getNestedSym(superClassName);

        for (Declaration decl : classDef.declarations) {
            Identifier decl_id = decl.getIdentifier();
            String decl_name = decl_id.name;

            if (super_class_sym != null && super_class_sym.declares(decl_name)) {
                if (!((super_class_sym.get(decl_name) instanceof FuncType) && (decl instanceof FuncDef))) {
                    errors.semError(decl_id,
                            "Cannot re-define attribute: " + decl_name);
                }
            }

            // check if method definition follows semantics
            if (decl instanceof FuncDef) {
                FuncDef method = (FuncDef) decl;
                List<TypedVar> params = method.params;

                if (method.getIdentifier().name.equals("__init__") ) {
                    if (params.size() > 1) {
                        errors.semError(decl_id,
                                "Method overridden with different type signature: %s",
                                decl_name);
                    }
                }

                if (params.isEmpty()) {
                    errors.semError(decl_id,
                            "First parameter of the following method must be of the enclosing class: %s",
                            decl_name);
                } else {
                    String classNameParam1 = ValueType.annotationToValueType(params.get(0).type).className();
                    if (!classNameParam1.equals(name)) {
                        errors.semError(decl_id,
                                "First parameter of the following method must be of the enclosing class: %s",
                                decl_name);
                    }
                }

                // check for overrides
                if (super_class_sym != null) {
                    Type past = super_class_sym.get(method.name.name);
                    if (past instanceof FuncType) {
                        FuncType pastFunc = (FuncType) super_class_sym.get(method.name.name);
                        if (pastFunc != null) {
                            Type pastReturn = pastFunc.returnType;
                            Type currReturn = ValueType.annotationToValueType(method.returnType);
                            // check that they have same amount of params
                            if (pastFunc.parameters.size() != method.params.size()) {
                                errors.semError(decl_id,
                                        "Method overridden with different type signature: %s",
                                        decl_name);
                            } else if (!pastReturn.equals(currReturn)) {
                                errors.semError(decl_id,
                                        "Method overridden with different type signature: %s",
                                        decl_name);
                            } else {
                                // check that params are of same type
                                for (int index = 1; index < pastFunc.parameters.size(); index++) {
                                    ValueType pastValueType = pastFunc.parameters.get(index);
                                    ValueType currValueType = ValueType
                                            .annotationToValueType(method.params.get(index).type);
                                    if (!pastValueType.equals(currValueType)) {
                                        errors.semError(decl_id,
                                                "Method overridden with different type signature: %s",
                                                decl_name);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Type decl_type = decl.dispatch(this);

            if (decl_type == null) {
                continue;
            }
            if (class_sym.declares(decl_name)) {
                errors.semError(decl_id,
                        "Duplicate declaration of identifier in same "
                                + "scope: %s",
                        decl_name);
            } else {
                class_sym.put(decl_name, decl_type);
            }
        }

        if (sym != null) {
            sym = sym.getParent();
        }
        TOP_SYM = class_sym.getParent();
        return new UserDefClassType(name, superClassName);
    }
}