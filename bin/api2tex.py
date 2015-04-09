import inspect
import sys
import mscviewer

class langs:
    LATEX = 0
    HTML = 1
    
def texify(str):
    return str.replace("_", "\\_")
          

def startDoc(lang, name):
    if lang == langs.LATEX:
        sys.stdout.write("\\begin{center}\n")
        sys.stdout.write("    \\begin{longtable}{ll}\n")

def endDoc(lang, name):
    if lang == langs.LATEX:
        #sys.stdout.write("        \\hline\n")
        sys.stdout.write("    \\end{longtable}\n")
        sys.stdout.write("\\end{center}\n")
        
        
def functionBegin(lang, name, args):
    if lang == langs.LATEX:
        #sys.stdout.write("        \\hline\n")
        sys.stdout.write("            \multicolumn{2}{l}{\\textbf{\\texttt{"+texify(name)+"(")
        first = True
        for arg in args:
            if not first:
                sys.stdout.write(", ")
            first = False
            sys.stdout.write(texify(arg[0]))
            if arg[1]:
                sys.stdout.write("="+texify(arg[1]))
        
        sys.stdout.write(")}}}\\\\ \\\\\n")


def functionArg(lang, name, default):
    if lang == langs.LATEX: 
        sys.stdout.write("        \\hline\n")
        sys.stdout.write("            \multicolumn{2}{l}{\\textbf{"+texify(name)+"}}\\\\\n")

def functionDescr(lang, descr):
    if lang == langs.LATEX: 
        sys.stdout.write("            Description: & \\begin{minipage}[t]{0.8\columnwidth}\n")
        sys.stdout.write(texify(descr)+"\n")
        sys.stdout.write("                           \\end{minipage}\\\\\n")

def functionEnd():
    sys.stdout.write("\\\\\n")
              
if __name__ == "__main__":
    lang = langs.LATEX
    startDoc(lang, "Python API")    
    all_funcs = inspect.getmembers(mscviewer, inspect.isfunction)
    for f in all_funcs:
        args = []
        argspec = inspect.getargspec(f[1])
        if argspec.args:
            if argspec.defaults:
                defaultStartIdx = len(argspec.args)-len(argspec.defaults)
            else:
                defaultStartIdx = len(argspec.args)
            idx = 0;
            count = len(argspec.args)
            for idx in range(count):
                if idx >= defaultStartIdx:
                    args.append((argspec.args[idx], str(argspec.defaults[idx-defaultStartIdx])))
                else:
                    args.append((argspec.args[idx], None))
        functionBegin(lang, f[0], args)
        if f[1].__doc__:
            functionDescr(lang, f[1].__doc__)
        else:
            functionDescr(lang, "")   
        functionEnd()
    endDoc(lang, "Python API") 
    sys.stdout.flush()            

