package analyzer;

import common.analysis.types.Type;

// Type for user-defined classes
public class UserDefClassType extends Type {

    private final String className;
    private final String superClassName;

    public UserDefClassType(String className) {
        this.className = className;
        this.superClassName = "object"; // default super class is object
    }
    public UserDefClassType(String className, String superClassName) {
        this.className = className;
        this.superClassName = superClassName;
    }

    @Override
    public String className() {
        return className;
    }
    public String superClassName() {
        return superClassName;
    }
    public boolean isUserDefClassType() {
        return true;
    }
    @Override
    public String toString() {
        return "<user-defined-class>";
    }


}
