package codegen;

import common.astnodes.Program;
import common.codegen.CodeGenBase;
import common.codegen.RiscVBackend;

/** Interface to code generator. */
public class CodeGen {

    /**
     * Perform code generation from PROGRAM, assumed to be well-typed,
     * to RISC-V, returning the assembly code.  DEBUG iff --debug was on the
     * command line.
     */
    public static String process(Program program, boolean debug) {
        /* Emit code into a ByteOutputStream, and convert to a string.*/
        try {
            RiscVBackend backend = new RiscVBackend();
            CodeGenBase cgen = new CodeGenImpl(backend);
            cgen.generate(program);

            return backend.toString();
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.err.println("Error performing code generation. "
                               + "Re-run with --debug to see stack trace.");
            if (debug) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
