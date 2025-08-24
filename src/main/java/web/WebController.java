package web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Controller
public class WebController {
    
    private String loadResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    @GetMapping("/")
    public ResponseEntity<String> index() {
        try {
            String content = loadResource("static/index.html");
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(content);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/styles.css")
    public ResponseEntity<String> styles() {
        try {
            String content = loadResource("static/styles.css");
            return ResponseEntity.ok().contentType(MediaType.valueOf("text/css")).body(content);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/script.js")
    public ResponseEntity<String> script() {
        try {
            String content = loadResource("static/script.js");
            return ResponseEntity.ok().contentType(MediaType.valueOf("application/javascript")).body(content);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}