package analyzer;

import java.util.*;
import common.analysis.AbstractNodeAnalyzer;
import common.analysis.types.*;
import common.astnodes.Declaration;
import common.astnodes.Identifier;
import common.astnodes.Program;
import common.astnodes.ClassDef;

/**
 * Creates list of classes and builds HashMap with class hierarchy
 */
public class ClassHierarchyBuilder extends AbstractNodeAnalyzer<Type> {

    private final HashMap<String, String> class_hierarchy = new HashMap<>();
    public ClassHierarchyBuilder(){}

    @Override
    public Type analyze(Program program) {

        // Adding predefined classes
        class_hierarchy.put("object", null);
        class_hierarchy.put("int", "object");
        class_hierarchy.put("bool", "object");
        class_hierarchy.put("str", "object");

        for (Declaration decl : program.declarations) {
            decl.dispatch(this);
        }
        return null;
    }

    @Override
    public Type analyze(ClassDef classDef) {
        Identifier id = classDef.getIdentifier();
        String name = id.name;

        Identifier superClass = classDef.superClass;
        String superClassName = superClass.name;

        // adding class and superclass to class_hierarchy
        class_hierarchy.put(name, superClassName);
        return null;
    }

    // Returns the class hierarchy
    public HashMap<String, String> getClassHierarchy() {
        return class_hierarchy;
    }

}