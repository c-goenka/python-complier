package common.codegen;

import common.analysis.types.ValueType;
import common.astnodes.Literal;

/** Information concerning an instance variable. */
public class AttrInfo extends VarInfo {

    /**
     * A descriptor for an attribute named ATTRNAME of type VARTYPE whose
     * initial value, if any, is a constant specified by INITIALVALUE
     * (it is otherwise null). */
    public AttrInfo(String attrName, ValueType varType, Literal initialValue) {
        super(attrName, varType, initialValue);
    }
}
