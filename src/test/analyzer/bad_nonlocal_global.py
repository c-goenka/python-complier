x:int = 1
y:int = 2
z:int = 3

def foo() -> object:
    global x # OK
    nonlocal y # No such nonlocal var       | ok
    global w # No such global var           | ok
    global int # No such global var         | ok

    z:bool = True # OK

    def bar() -> object:
        global x # OK
        nonlocal z # OK                     
        nonlocal y # No such nonlocal var   | 
        global foo # No such global var     | ok
        nonlocal bar # No such nonlocal     | ok

        pass

    bar()

foo()

