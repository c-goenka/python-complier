package web;

import lexer.Parser;
import analyzer.Analysis;
import codegen.CodeGen;
import common.astnodes.Program;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class CompilerController {
    
    @PostMapping("/compile")
    public ResponseEntity<Map<String, Object>> compile(@RequestBody CompileRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String code = request.getCode();
            String phase = request.getPhase();
            
            // Ensure code ends with a newline for the parser
            if (code != null && !code.endsWith("\n")) {
                code = code + "\n";
            }
            
            if (code == null || code.trim().isEmpty()) {
                response.put("error", "No code provided");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Phase 1: Parser
            Program program = Parser.process(code, false);
            
            if ("parser".equals(phase)) {
                response.put("result", program.toJSON());
                response.put("phase", "Parser");
                return ResponseEntity.ok(response);
            }
            
            // Phase 2: Semantic Analysis
            program = Analysis.process(program, false);
            
            if ("analyzer".equals(phase)) {
                response.put("result", program.toJSON());
                response.put("phase", "Semantic Analyzer");
                return ResponseEntity.ok(response);
            }
            
            // Phase 3: Code Generation
            if ("codegen".equals(phase)) {
                String assembly = CodeGen.process(program, false);
                if (assembly == null) {
                    response.put("error", "Code generation failed");
                    return ResponseEntity.badRequest().body(response);
                }
                response.put("result", assembly);
                response.put("phase", "Code Generator");
                return ResponseEntity.ok(response);
            }
            
            response.put("error", "Invalid phase: " + phase);
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            response.put("error", "Compilation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    public static class CompileRequest {
        private String code;
        private String phase;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }
    }
}