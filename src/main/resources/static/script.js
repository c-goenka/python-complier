// Example code snippets
const examples = {
    simple: `x:int = 42
print(x)
`,
    
    function: `def factorial(n: int) -> int:
    if n <= 1:
        return 1
    else:
        return n * factorial(n - 1)

print(factorial(5))
`,
    
    class: `class Counter(object):
    count:int = 0
    
    def __init__(self: "Counter"):
        self.count = 0
    
    def increment(self: "Counter") -> object:
        self.count = self.count + 1

counter:Counter = Counter()
counter.increment()
print(counter.count)
`,
    
    loop: `numbers:[int] = None
total:int = 0
numbers = [1, 2, 3, 4, 5]

for num in numbers:
    total = total + num

print(total)
`
};

// DOM elements
const codeInput = document.getElementById('codeInput');
const phaseSelect = document.getElementById('phaseSelect');
const compileBtn = document.getElementById('compileBtn');
const output = document.getElementById('output');
const error = document.getElementById('error');
const loading = document.getElementById('loading');
const outputTitle = document.getElementById('outputTitle');

// Example buttons
document.querySelectorAll('.example-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const example = btn.dataset.example;
        codeInput.value = examples[example];
    });
});

// Compile button handler
compileBtn.addEventListener('click', async () => {
    const code = codeInput.value.trim();
    const phase = phaseSelect.value;
    
    if (!code) {
        showError('Please enter some code to compile.');
        return;
    }
    
    showLoading();
    
    try {
        const response = await fetch('/api/compile', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                code: code,
                phase: phase
            })
        });
        
        const result = await response.json();
        
        if (response.ok) {
            showResult(result.result, result.phase);
        } else {
            showError(result.error || 'Compilation failed');
        }
    } catch (err) {
        showError('Network error: ' + err.message);
    }
});

// Helper functions
function showLoading() {
    loading.style.display = 'flex';
    output.style.display = 'none';
    error.style.display = 'none';
    compileBtn.disabled = true;
}

function showResult(result, phase) {
    loading.style.display = 'none';
    output.style.display = 'block';
    error.style.display = 'none';
    compileBtn.disabled = false;
    
    outputTitle.textContent = `Output - ${phase}`;
    
    // Pretty print JSON if it's JSON
    if (phase !== 'Code Generator') {
        try {
            const jsonObj = JSON.parse(result);
            output.textContent = JSON.stringify(jsonObj, null, 2);
        } catch (e) {
            output.textContent = result;
        }
    } else {
        output.textContent = result;
    }
}

function showError(errorMsg) {
    loading.style.display = 'none';
    output.style.display = 'none';
    error.style.display = 'block';
    compileBtn.disabled = false;
    
    outputTitle.textContent = 'Error';
    error.textContent = errorMsg;
}

// Allow Enter key to compile (Ctrl+Enter)
codeInput.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.key === 'Enter') {
        compileBtn.click();
    }
});

// Load initial example
window.addEventListener('load', () => {
    codeInput.value = examples.simple;
});