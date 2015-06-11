# to be executed with jython

import urllib2
import json
import re
import sys
import string

BASE = "https://api.github.com/repos/rattias/mscviewer"


HTML = """
<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"><head>


<title>MSCViewer $VERSION</title>
</head><body>
<h1><span>Welcome to MSCViewer $VERSION</span></h1>

Welcome to MSCViewer, a Message Sequence Chart Visualization and Analysis Tool.

<ul>
    <li>MSCViewer leverages <b>Jython</b> for Python integration. See <a href='http://www.jython.org/license.html'>license here</a>
    <li>MSCViewer leverages <b>SwingX</b> for some GUI elements. See <a href='http://opensource.org/licenses/lgpl-2.1.php'>license here</a>
    <li>MSCViewer makes use of some icons from the <b>Nuvola</b> icon set by David Vignoni. For more information see <a href='http://www.icon-king.com/projects/nuvola/'>the Nuvola Home Page</a>
 </ul>
<h2><span>Release History</span></h2>

$ISSUES
"""


def filter(issues, type, milestone):
    res = []
    for issue in issues:
        labels = issue['labels']
        valid = False
        for l in labels:
            if l['name'] == type:
                valid = True
                break
        if valid:
            ms = issue['milestone']
            if ms != None and ms['title'] == milestone:
                res.append(issue)
    return res

def get_milestones():
    response = urllib2.urlopen(BASE + "/milestones?state=all")
    data = response.read();
    milestones = json.loads(data)
    return milestones


def get_issues():
    response = urllib2.urlopen(BASE + "/issues?state=closed")
    data = response.read();
    issues = json.loads(data)
    return issues
    
def issue_to_html(issues):
    res = ""    
    for issue in issues:
        res += '<li> '
        res += "<a href='"+issue['html_url']+"'>"+str(issue['number'])+": "+issue['title']+"</a></li>\n"
    return res
                
    
if __name__ == "__main__":
    VERSION = sys.argv[1]
    milestones = get_milestones()
    issues = get_issues()
    html = ""
    for m in sorted(milestones, key= lambda ms: ms['created_at'], reverse=True):
        release = m['title']
        match = re.match('V([0-9]+)\\.([0-9]+)\\.([0-9])+(.*)', release)
        if match:
            html += "<h3>"+release+"</h3>\n"
            html += "<ul style='list-style: none;'>"
            html += "<li><h4>Enhancements</h4></li>\n"
            html += "<ul>\n"
            enh = filter(issues, 'enhancement', release)
            html += issue_to_html(enh)
            html += "</ul>\n"
            html += "<li><h4>Fixed Bugs</h4></li>\n"
            html += "<ul>\n"
            bugs = filter(issues, 'bug', release)
            html += issue_to_html(bugs)
            html += "</ul></ul>\n"
    
    res = string.Template(HTML).substitute({"ISSUES":html, "VERSION":VERSION})
    print res

    
 
