import inspect
import sys
sys.path.insert(0, "resources/default/script")
sys.path.insert(0, "classes")

def colored(x):
    return "\033[1;31m"+x+"\033[0;0m"
    
    
class langs:
    LATEX = 0
    HTML = 1
    
class Scanner:
    def __init__(self, str):
        self.str = str
        self.l = len(str)
        self.idx = 0
        
    def consume(self, s):
        if self.str.startswith(s, self.idx):
            self.idx += len(s)
        else:
            raise Exception("Expecting '"+s+"' at index "+str(self.idx)+".\n"+self.str[0:self.idx]+"\n>>>"+self.str[self.idx:])
            
    def __iter__(self):
        return self
        
    def next(self):
        if self.idx >= self.l:
            raise StopIteration
        else:
            self.idx += 1
            self.wasEscaped = self.idx > 1 and self.str[self.idx-2] == '\\'
            return self.str[self.idx-1]
            
    def lastWasEscaped(self):
        return self.wasEscaped
        
    def startsWith(self, s):
        return self.str.startswith(s, self.idx)

    def getBlock(self):
        self.consume('{')
        cnt = 1
        idx = self.idx
        r = range(self.idx, self.l)
        for self.idx in r:
            c = self.str[self.idx] 
            if c == '{':
                cnt += 1
            elif c == '}':
                cnt -= 1         
                if cnt == 0:
                    self.idx += 1
                    return self.str[idx:self.idx-1]
        raise Exception("missing matching '}' for '{':\n"+self.str[0:self.idx]+"\n>>>"+self.str[self.idx-1]+"\n"+self.str[self.idx+1:])


            
def texifyParam(scan):
    scan.consume('param')
    name = getArg(scan)
    type = getArg(scan)
    descr = getArg(scan)
    sys.stdout.write('\\begin{indentation}{.4in}{0in}')
    sys.stdout.write('\\noindent \\texttt{\\textbf{')
    texify(name)
    sys.stdout.write('}(')
    texify(type)
    sys.stdout.write(')}\n')
    if len(descr) < 40:
        sys.stdout.write(': ')
        texify(descr)
    sys.stdout.write('\\end{indentation}')
    if len(descr) >= 40:
        sys.stdout.write('\n\\begin{indentation}{.6in}{0in}\n')
        sys.stdout.write('\\noindent ')
        texify(descr)
        sys.stdout.write('\\end{indentation}\n')

def texifyExample(scan):
    scan.consume('example')
    example = getArg(scan)
  
def texifyBold(scan):
    scan.consume('b')
    text = getArg(scan)
    sys.stdout.write('\\textbf{')
    texify(text)
    sys.stdout.write('}')

def texifyEmph(scan):
    scan.consume('e')
    text = getArg(scan)
    sys.stdout.write('\\textit{')
    texify(text)
    sys.stdout.write('}')

def texifyHeader(scan):
    scan.consume('header')
    hd = getArg(scan)
    sys.stdout.write('\\begin{indentation}{.2in}{0in}\n')
    sys.stdout.write('\\noindent \\textbf{')
    texify(hd)
    sys.stdout.write('} \n')
    sys.stdout.write('\\end{indentation}\n')

def texifyDescr(scan):
    scan.consume('descr')
    descr = getArg(scan)
    sys.stdout.write('\\begin{indentation}{.2in}{0in}\n')
    sys.stdout.write("\\noindent ")
    texify(descr)
    sys.stdout.write("\\\\\n\\end{indentation}\n")
       
def texifyCode(scan):
    scan.consume('code')
    code = getArg(scan)
    sys.stdout.write('\\begin{indentation}{.4in}{0in}\n')
    sys.stdout.write('\\begin{lstlisting}[frame=none]\n')
    sys.stdout.write(code)
    sys.stdout.write('\n\\end{lstlisting}\n')
    sys.stdout.write('\\end{indentation}')
    
def texify(str):
    res = []
    l = len(str)
    scan = Scanner(str)
    for c in scan:
        if c == '_':
            sys.stdout.write('\\')
            sys.stdout.write('_')
        elif c == '$' and not scan.lastWasEscaped():
            if scan.startsWith('descr{'):
                texifyDescr(scan)
            elif scan.startsWith('header{'):
                texifyHeader(scan)
            elif scan.startsWith('param{'):
                texifyParam(scan)
            elif scan.startsWith('code{'):
                texifyCode(scan)
            elif scan.startsWith('b{'):
                texifyBold(scan)  
            elif scan.startsWith('e{'):
                texifyEmph(scan)  
            else:
                sys.stdout.write('$')
        else:
            sys.stdout.write(c)     
    

def getArg(c):
    return c.getBlock()
        
def startDoc(lang, name, doc):
    if lang == langs.LATEX:
        if mm.__doc__:
            texify(mm.__doc__)
            sys.stdout.write("\\\\\\\\")
            
def endDoc(lang, name):
    pass
        
        
def functionBegin(lang, name, args, cl):
    if lang == langs.LATEX:
        if not cl:
            sys.stdout.write('\\noindent\\makebox[\\linewidth]{\\rule{\\columnwidth}{0.4pt}}\n')
        sys.stdout.write("\\noindent {\\color{blue}\\textbf{\\texttt{")
        if cl and name == "__init__":
            name = cl.__name__
        texify(name)
        sys.stdout.write("(")
        name_l = len(name)+1
        first = True
        l = 0
        for arg in args:
            if not first:
                sys.stdout.write(", ")
                l += 3
            if l > 40:
                sys.stdout.write('\\\\\n')
                l = 0
                sys.stdout.write(' '*name_l)
            first = False
            texify(arg[0])
            l += len(arg[0])
            if arg[1]:
                sys.stdout.write("=")
                texify(arg[1])
                l += len(arg[1])+1
        sys.stdout.write(")}}}\n")

def functionDescr(lang, descr):
    if lang == langs.LATEX: 
        texify(descr)

def functionEnd():
    sys.stdout.write("\n")
import importlib

def constructor_filter(v):
    return inspect.ismethod(v) and v.__name__ == "__init__"
def getConstructor(c):
    hier = inspect.getmro(c)
    for c1 in hier:
        init = inspect.getmembers(c1, constructor_filter)
        if len(init) > 0:
            return init[0]
    return None

def texifyClass(name, c):
    if (not c.__doc__) or (not "$descr{" in c.__doc__):
        return
    sys.stdout.write('\\noindent\\makebox[\\linewidth]{\\rule{\\columnwidth}{0.4pt}}\n')
    sys.stdout.write("\\noindent {\\color{blue}\\textbf{\\texttt{class ")
    texify(name)
    sys.stdout.write("}}}\\\\\n")
    funcs = inspect.getmembers(c, inspect.ismethod)
    f = getConstructor(c)  
    texify_function(f, c, c.__doc__)
    
    for f in funcs:
        if f[0] != "__init__":
            texify_function(f, c)
    
    
    
def texify_function(f, cl, doc=None):
    if (not doc) and ((not f[1].__doc__) or (not "$descr{" in f[1].__doc__)):
        return
    if not doc:
        doc = f[1].__doc__
        
    args = []
    argspec = inspect.getargspec(f[1])
    # in jython this returns a tuple, while in python it returns a class. this file 
    # is intended for use in jython.
    if argspec[0]:
        if argspec[3]:
            defaultStartIdx = len(argspec[0])-len(argspec[3])
        else:
            defaultStartIdx = len(argspec[0])
        idx = 0;
        count = len(argspec[0])
        if cl:
            start = 1
        else:
            start = 0
        for idx in range(start, count):
            if idx >= defaultStartIdx:
                args.append((argspec[0][idx], str(argspec[3][idx-defaultStartIdx])))
            else:
                args.append((argspec[0][idx], None))
    if argspec[1]:
        args.append(("*args", None))
    functionBegin(lang, f[0], args, cl)
    if doc:
        functionDescr(lang, doc)
    else:
        functionDescr(lang, "")   
    functionEnd()

    
if __name__ == "__main__":
    lang = langs.LATEX
    mm =  importlib.import_module(sys.argv[1])
    startDoc(lang, "Python API", mm.__doc__)    
    all_classes = inspect.getmembers(mm, inspect.isclass)
    for c in all_classes:
        texifyClass(c[0], c[1])    
        
    funcs = inspect.getmembers(mm, inspect.isfunction)
    for f in funcs:
        texify_function(f, None, False)
    endDoc(lang, "Python API") 
    sys.stdout.flush()            

