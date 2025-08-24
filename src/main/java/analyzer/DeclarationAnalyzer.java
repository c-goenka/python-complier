package analyzer;

import static common.analysis.types.Type.*;
import java.util.*;
import common.analysis.AbstractNodeAnalyzer;
import common.analysis.types.*;
import common.astnodes.*;

/**
 * Analyzes declarations to create a top-level symbol table.
 */
public class DeclarationAnalyzer extends AbstractNodeAnalyzer<Type> {

    /** Current symbol table.  Changes with new declarative region. */
    private SymbolTable<Type> sym = new SymbolTable<>();

    /** Global symbol table. */
    private final SymbolTable<Type> globals = sym;

    /** Receiver for semantic error messages. */
    private final Errors errors;
    private final HashMap<String, SymbolTable<Type>> allClassesSym = new HashMap<>();
    /** A new declaration analyzer sending errors to ERRORS0. */
    public DeclarationAnalyzer(Errors errors0) {
        errors = errors0;
    }
    public SymbolTable<Type> getGlobals() {
        return globals;
    }
    public HashMap<String, SymbolTable<Type>> getClassesSym() {
        return allClassesSym;
    }


    @Override
    public Type analyze(Program program) {
        // adding predefined functions and classes
        List<ValueType> single_param = new ArrayList<>();
        single_param.add(OBJECT_TYPE);
        sym.put("print", new FuncType(single_param, NONE_TYPE));
        sym.put("input", new FuncType(STR_TYPE));
        sym.put("len", new FuncType(single_param, INT_TYPE));
        sym.put("str", STR_TYPE);
        sym.put("object", OBJECT_TYPE);
        sym.put("bool", BOOL_TYPE);
        sym.put("int", INT_TYPE);
        sym.put("<None>", NONE_TYPE);

        for (Declaration decl : program.declarations) {
            Identifier id = decl.getIdentifier();
            String name = id.name;

            Type type = decl.dispatch(this);

            if (type == null) {
                continue;
            }

            if (sym.declares(name)) {
                errors.semError(id,
                                "Duplicate declaration of identifier in same "
                                + "scope: %s",
                                name);
            } else {
                sym.put(name, type);
            }
        }

        return null;
    }

    @Override
    public Type analyze(VarDef varDef) {
        return ValueType.annotationToValueType(varDef.var.type);
    }

    @Override
    public Type analyze(TypedVar typedVar) {
        return ValueType.annotationToValueType(typedVar.type);
    }

    @Override
    public Type analyze(FuncDef funcDef) {
        Identifier func_id = funcDef.getIdentifier();
        String func_name = func_id.name;

        SymbolTable<Type> func_sym = new SymbolTable<>(sym);

        ValueType returnType = ValueType.annotationToValueType(funcDef.returnType);
        func_sym.put("return_val", returnType);

        sym.addNestedSym(func_name, func_sym);
        sym = func_sym;

        List<ValueType> paramTypes = new ArrayList<>();
        for (TypedVar param : funcDef.params) {
            Identifier param_id = param.identifier;
            String param_name = param_id.name;
            Type param_type = param.dispatch(this);

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

        for (Declaration decl : funcDef.declarations) {
            Identifier decl_id = decl.getIdentifier();
            String decl_name = decl_id.name;
            Type decl_type = decl.dispatch(this);

            if (func_sym.declares(decl_name)) {
                errors.semError(decl_id,
                        "Duplicate declaration of identifier in same "
                                + "scope: %s",
                        decl_name);
            } else {
                func_sym.put(decl_name, decl_type);
            }
        }

        sym = func_sym.getParent();
        return new FuncType(paramTypes, returnType);
    }

    @Override
    public Type analyze(ClassDef classDef) {
        Identifier id = classDef.getIdentifier();
        String name = id.name;

        Identifier superClass = classDef.superClass;
        String superClassName = superClass.name;

        SymbolTable<Type> class_sym = new SymbolTable<>(sym);
        allClassesSym.put(name, class_sym);
        sym.addNestedSym(name, class_sym);
        sym = class_sym;

        for (Declaration decl : classDef.declarations) {
            Identifier decl_id = decl.getIdentifier();
            String decl_name = decl_id.name;

            Type decl_type = decl.dispatch(this);

            if (decl_type == null) {
                continue;
            }
            if (sym.declares(decl_name)) {
                errors.semError(decl_id,
                        "Duplicate declaration of identifier in same "
                                + "scope: %s",
                        decl_name);
            } else {
                sym.put(decl_name, decl_type);
            }
        }

        sym = class_sym.getParent();
        return new UserDefClassType(name, superClassName);
    }

}