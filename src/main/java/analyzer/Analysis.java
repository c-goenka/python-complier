package analyzer;

import common.analysis.types.Type;
import common.astnodes.Program;

import java.util.HashMap;

/** Top-level class for performing semantic analysis. */
public class Analysis {

    /** Perform semantic analysis on PROGRAM, adding error messages and
     *  type annotations. Provide debugging output iff DEBUG. Returns modified
     *  tree. */
    public static Program process(Program program, boolean debug) {
        if (program.hasErrors()) {
            return program;
        }

        //Pass 1: Building Class Hierarchy - creating a map of classes and their respective superclass
        ClassHierarchyBuilder classHierarchyBuilder = new ClassHierarchyBuilder();
        program.dispatch(classHierarchyBuilder);

        //Pass 2: Declaration Analysis - building symbol tables
        DeclarationAnalyzer declarationAnalyzer =
            new DeclarationAnalyzer(program.errors);
        program.dispatch(declarationAnalyzer);

        HashMap<String, String> class_hierarchy = classHierarchyBuilder.getClassHierarchy(); // Map of classes and their superclasses
        SymbolTable<Type> globalSym = declarationAnalyzer.getGlobals(); // Symbol table for global scope
        HashMap<String, SymbolTable<Type>> allClassesSym = declarationAnalyzer.getClassesSym(); // Map of all classes and their symbol tables

        //Pass 3: Semantics - checking for semantic errors
        Semantics semantics =
            new Semantics(program.errors, class_hierarchy, globalSym, allClassesSym);
        program.dispatch(semantics);

        //Pass 4: Type Checking - adding inferred types and checking for type errors
        if (!program.hasErrors()) {
            TypeChecker typeChecker =
                new TypeChecker(globalSym, program.errors, class_hierarchy, allClassesSym);
            program.dispatch(typeChecker);
        }

        return program;
    }
}
