# Command Line Interface

**Requirements:** Java 8+, Maven 3.6+

```bash
# Build the project
mvn clean package

# Create a test file
echo 'x:int = 42
print(x)' > test.py

# Parse only (generate AST)
java -cp "target/compiler.jar" PyLang --pass=s test.py

# Semantic analysis (type-checked AST)
java -cp "target/compiler.jar" PyLang --pass=.s test.py

# Generate assembly
java -cp "target/compiler.jar" PyLang --pass=..s test.py

# Compile and execute
java -cp "target/compiler.jar" PyLang --pass=..s --run test.py

# Run test suites
java -cp "target/compiler.jar" PyLang --pass=s --test --dir src/test/parser/
java -cp "target/compiler.jar" PyLang --pass=.s --test --dir src/test/analyzer/
java -cp "target/compiler.jar" PyLang --pass=..s --test --dir src/test/codegen/
```

## Pass Options

- `--pass=s` - Lexer/Parser (generate AST)
- `--pass=.s` - Semantic analysis (type checking)
- `--pass=..s` - Code generation (RISC-V assembly)

## Flags

- `--run` - Execute the compiled program
- `--test --dir <path>` - Run test suite
- `--out <file>` - Output to file
