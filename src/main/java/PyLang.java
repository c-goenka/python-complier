import lexer.Parser;
import analyzer.Analysis;
import codegen.CodeGen;
import common.astnodes.Program;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Main entry point for the PyLang compiler. */
public class PyLang {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java PyLang [options] <input.py>");
            System.err.println("Options:");
            System.err.println("  --pass=s     Run lexer/parser only");
            System.err.println("  --pass=.s    Run through semantic analysis");
            System.err.println("  --pass=..s   Run full compilation to assembly");
            System.err.println("  --run        Execute the compiled program");
            System.err.println("  --out FILE   Output to specified file");
            System.err.println("  --debug      Enable debug output");
            System.err.println("  --dir DIR    Process all .py files in directory");
            System.err.println("  --test       Test mode (use with --dir)");
            return;
        }
        
        String inputFile = null;
        String outputFile = null;
        String inputDir = null;
        String pass = "..s"; // Default to full compilation
        boolean run = false;
        boolean debug = false;
        boolean test = false;
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--pass=")) {
                pass = args[i].substring(7);
            } else if (args[i].equals("--run")) {
                run = true;
            } else if (args[i].startsWith("--out")) {
                if (i + 1 < args.length) {
                    outputFile = args[++i];
                } else {
                    outputFile = args[i].substring(5);
                }
            } else if (args[i].equals("--debug")) {
                debug = true;
            } else if (args[i].startsWith("--dir")) {
                if (i + 1 < args.length) {
                    inputDir = args[++i];
                } else {
                    inputDir = args[i].substring(5);
                }
            } else if (args[i].equals("--test")) {
                test = true;
            } else if (!args[i].startsWith("--")) {
                inputFile = args[i];
            }
        }
        
        if (inputFile == null && inputDir == null) {
            System.err.println("Error: No input file or directory specified");
            return;
        }
        
        if (inputDir != null) {
            // Process directory
            processDirectory(inputDir, outputFile, pass, run, debug, test);
            return;
        }
        
        try {
            // Read input file
            String input = new String(Files.readAllBytes(Paths.get(inputFile)));
            
            // Process single file
            processFile(inputFile, input, outputFile, pass, run, debug);
            
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Compilation error: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
        }
    }
    
    private static void processDirectory(String inputDir, String outputFile, String pass, boolean run, boolean debug, boolean test) {
        try {
            Path dirPath = Paths.get(inputDir);
            if (!Files.isDirectory(dirPath)) {
                System.err.println("Error: " + inputDir + " is not a directory");
                return;
            }
            
            // Find all .py files
            try (Stream<Path> paths = Files.walk(dirPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".py"))
                     .sorted()
                     .forEach(path -> {
                         try {
                             String input = new String(Files.readAllBytes(path));
                             String fileName = path.getFileName().toString();
                             
                             if (test) {
                                 System.out.println("Processing: " + fileName);
                             }
                             
                             String fileOutputFile = null;
                             if (outputFile != null && !test) {
                                 fileOutputFile = outputFile + "." + fileName;
                             }
                             
                             processFile(fileName, input, fileOutputFile, pass, run, debug);
                             
                         } catch (Exception e) {
                             System.err.println("Error processing " + path + ": " + e.getMessage());
                             if (debug) {
                                 e.printStackTrace();
                             }
                         }
                     });
            }
        } catch (IOException e) {
            System.err.println("Error reading directory: " + e.getMessage());
        }
    }
    
    private static void processFile(String fileName, String input, String outputFile, String pass, boolean run, boolean debug) throws Exception {
        // Phase 1: Lexer/Parser
        Program program = Parser.process(input, debug);
        
        if (pass.equals("s")) {
            // Output AST and stop
            if (outputFile != null) {
                try (PrintWriter out = new PrintWriter(outputFile)) {
                    out.println(program.toJSON());
                }
            } else {
                System.out.println(program.toJSON());
            }
            return;
        }
        
        // Phase 2: Semantic Analysis
        program = Analysis.process(program, debug);
        
        if (pass.equals(".s")) {
            // Output typed AST and stop
            if (outputFile != null) {
                try (PrintWriter out = new PrintWriter(outputFile)) {
                    out.println(program.toJSON());
                }
            } else {
                System.out.println(program.toJSON());
            }
            return;
        }
        
        // Phase 3: Code Generation
        if (pass.equals("..s")) {
            String assembly = CodeGen.process(program, debug);
            
            if (assembly == null) {
                System.err.println("Code generation failed for " + fileName);
                return;
            }
            
            if (outputFile != null) {
                try (PrintWriter out = new PrintWriter(outputFile)) {
                    out.print(assembly);
                }
            } else if (!run) {
                System.out.print(assembly);
            }
            
            if (run) {
                // Execute the assembly code (this would typically use Venus simulator)
                System.out.println("Execution not yet implemented for " + fileName);
            }
        }
    }
}