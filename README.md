# Python Compiler

A compiler for a dialect of Python (which I refer to as PyLang in this project), featuring lexical analysis, parsing, semantic analysis, and RISC-V code generation.

### Example Code

Here are some code examples of this dialect of Python:

**Simple Variable:**
```python
x:int = 42
print(x)
```

**Function with Recursion:**
```python
def factorial(n: int) -> int:
    if n <= 1:
        return 1
    else:
        return n * factorial(n - 1)

print(factorial(5))
```

**Class with Methods:**
```python
class Counter(object):
    count:int = 0

    def __init__(self: "Counter"):
        self.count = 0

    def increment(self: "Counter") -> object:
        self.count = self.count + 1

counter:Counter = Counter()
counter.increment()
print(counter.count)
```

You can try these examples in the web interface.

## Language Support

The compiler supports core language features:

- **Types**: `int`, `bool`, `str`, lists, and user-defined classes
- **Control Flow**: `if`/`elif`/`else`, `while`, `for` loops
- **Functions**: Nested functions with proper scoping
- **Classes**: Inheritance, method overriding, attribute access
- **Built-ins**: `print()`, `input()`, `len()`, type conversions

## How It Works

The compiler follows a traditional four phase approach to compile the code:

### 1. Lexical Analysis
Uses JFlex to tokenize source code, with special handling for Python-style indentation. The lexer manages INDENT/DEDENT tokens using a stack-based approach.

### 2. Syntax Analysis
A CUP generated parser builds abstract syntax trees from tokens. Includes comprehensive error recovery to handle malformed input gracefully.

### 3. Semantic Analysis
A Four pass analysis that builds symbol tables, validates types, and checks semantic rules:
- **Pass 1**: Build class hierarchy
- **Pass 2**: Create symbol tables with proper scoping
- **Pass 3**: Validate semantic rules
- **Pass 4**: Type checking and inference

### 4. Code Generation
Generates RISC-V assembly with a complete runtime system for memory management, I/O, and built-in functions.

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   ├── PyLang.java     # Main CLI entry point
│   │   ├── web/            # Web interface
│   │   ├── lexer/          # Lexical analysis
│   │   ├── analyzer/       # Semantic analysis
│   │   ├── codegen/        # Code generation
│   │   └── common/         # Shared components
│   ├── jflex/              # Lexer specification
│   ├── cup/                # Parser grammar
│   └── asm/                # RISC-V runtime
└── test/                   # Comprehensive test suite
    ├── parser/             # Parser tests
    ├── analyzer/           # Semantic tests
    ├── codegen/            # Code generation tests
    └── benchmarks/         # Performance tests
```

## Technology Stack

- **Language**: Java 8+
- **Build**: Maven with JFlex and CUP plugins
- **Web Interface**: Spring Boot 2.7
- **Parser Generator**: CUP
- **Lexer Generator**: JFlex
- **Target**: RISC-V assembly

## Getting Started

**Requirements:** Java 8+, Maven 3.6+

1. **Clone and build:**
   ```bash
   git clone https://github.com/c-goenka/python-complier.git
   cd python-compiler
   mvn clean package
   ```

2. **Try the web interface:**
   ```bash
   java -cp "target/compiler.jar" web.PyLangWeb
   # Open http://localhost:8080
   ```
   The web interface provides an interactive editor with examples and lets you see each compilation phase.

<img width="2190" height="1240" alt="compiler-web-interface" src="https://github.com/user-attachments/assets/3ccf9bfc-ec3a-4668-be74-ca7557b9868c" />

## Testing

The project includes comprehensive test cases:

- **Parser Tests** (`src/test/parser/`): Syntax validation and AST generation
- **Semantic Tests** (`src/test/analyzer/`): Type checking and error detection
- **Codegen Tests** (`src/test/codegen/`): Assembly generation and execution
- **Benchmarks** (`src/test/benchmarks/`): Performance testing programs

Run specific test cases:
```bash
# Parser tests (33 tests)
java -cp "target/compiler.jar" PyLang --pass=s --test --dir src/test/parser/

# Semantic analysis tests (59 tests)
java -cp "target/compiler.jar" PyLang --pass=.s --test --dir src/test/analyzer/

# Code generation tests (80 tests)
java -cp "target/compiler.jar" PyLang --pass=..s --test --dir src/test/codegen/
```

*Note: Tests currently show verbose output rather than pass/fail summaries.*

## CLI Usage

See `CLI.md` for a reference on how to use the command-line interface.

---
