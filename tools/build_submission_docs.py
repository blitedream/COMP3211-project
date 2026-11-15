from pathlib import Path
import re, xml.etree.ElementTree as ET, zipfile, html
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Preformatted
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.graphics.shapes import Drawing, Rect, String, Line, Polygon
R=Path(__file__).resolve().parents[1]
OUT=R/'docs/final'; OUT.mkdir(parents=True,exist_ok=True)
styles=getSampleStyleSheet()
styles['Normal'].fontSize=10; styles['Normal'].leading=14
styles.add(ParagraphStyle(name='CodeSmall',fontName='Courier',fontSize=8,leading=11))
for n in ['Title','Heading1','Heading2']: styles[n].textColor=colors.black
def p(t,style='Normal'): return Paragraph(html.escape(t).replace('\n','<br/>'),styles[style])
def table(rows,widths=None):
 t=Table([[p(str(c)) for c in row] for row in rows],colWidths=widths,repeatRows=1,hAlign='LEFT')
 t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e8edf1')),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),7),('RIGHTPADDING',(0,0),(-1,-1),7),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),('LINEBELOW',(0,0),(-1,-1),.2,colors.lightgrey)]));return t
def footer(c,d):
 c.setFont('Helvetica',8); c.drawString(42,25,'COMP3211 PIM | 2 November 2026');c.drawRightString(d.pagesize[0]-42,25,str(d.page))
def build(path,title,items):
 SimpleDocTemplate(str(path),pagesize=A4,rightMargin=42,leftMargin=42,topMargin=40,bottomMargin=42).build([p(title,'Title'),Spacer(1,12)]+items,onFirstPage=footer,onLaterPages=footer)
def section(title,text):return [p(title,'Heading1'),p(text),Spacer(1,8)]
def md(text):
 result=[];lines=text.splitlines();i=0
 while i<len(lines):
  line=lines[i].strip();i+=1
  if not line:continue
  if line.startswith('```'):
   code=[]
   while i<len(lines) and not lines[i].startswith('```'):code.append(lines[i]);i+=1
   i+=1
   if line=='```mermaid':continue
   for row in code:result.append(Preformatted(row,styles['CodeSmall'],maxLineLength=88))
  elif line.startswith('|'):
   rows=[line]
   while i<len(lines) and lines[i].strip().startswith('|'):rows.append(lines[i].strip());i+=1
   rows=[x.strip('|').split('|') for x in rows if not re.fullmatch(r'[| :\-]+',x)]
   if rows and len({len(x) for x in rows})==1:result.append(table([[c.strip().replace('`','') for c in x] for x in rows],[511/len(rows[0])]*len(rows[0])))
  elif line.startswith('#'):result.append(p(line.lstrip('# ').replace('`',''),'Heading1' if line.startswith(('## ','# ')) else 'Heading2'))
  else:
   while i<len(lines) and lines[i].strip() and not lines[i].startswith(('#','```','|','- ')):line+=' '+lines[i].strip();i+=1
   result.append(p(line.replace('`','').replace('**','')))
  result.append(Spacer(1,5))
 return result
def diagram(kind):
 d=Drawing(510,250)
 def box(x,y,w,h,title,sub=''):
  d.add(Rect(x,y,w,h,rx=4,fillColor=colors.HexColor('#eef3f7'),strokeColor=colors.HexColor('#567086')))
  d.add(String(x+w/2,y+h-18,title,textAnchor='middle',fontName='Helvetica-Bold',fontSize=10))
  for i,line in enumerate(sub.split('\n')):d.add(String(x+8,y+h-34-i*13,line,fontSize=8))
 def arrow(x,y,xx,yy,label=''):
  import math
  d.add(Line(x,y,xx,yy,strokeColor=colors.HexColor('#567086')));a=math.atan2(yy-y,xx-x);u=7
  d.add(Polygon([xx,yy,xx-u*math.cos(a-.4),yy-u*math.sin(a-.4),xx-u*math.cos(a+.4),yy-u*math.sin(a+.4)],fillColor=colors.HexColor('#567086')))
  if label:d.add(String(min(x,xx)+4,(y+yy)/2+5,label,fontSize=8))
 if kind=='architecture':
  box(5,165,110,60,'Terminal','Input and output');box(170,165,170,60,'PIMCli','View and controller');box(170,55,170,70,'PIMModel','Collection and updates');box(370,145,135,65,'PIRSearch','Criteria parser');box(370,30,135,65,'PIMFileCodec','File serialization')
  arrow(115,195,170,195);arrow(255,165,255,125);arrow(340,90,370,170);arrow(330,165,400,95)
 elif kind=='classes':
  box(160,175,190,65,'PIR abstract','id: String; getId(); getType()\ntoDetailString()')
  for x,title,fields in [(0,'Note','text'),(130,'Task','description, deadline'),(260,'Event','description, start, alarm'),(390,'Contact','name, address, mobile')]:
   box(x,70,120,65,title,fields);arrow(x+60,135,255,175)
  d.add(String(15,30,'Arrows indicate inheritance. PIMModel holds PIRs in an ordered map.',fontSize=9))
 else:
  for title,x in [('User',45),('PIMCli',190),('PIMModel',335),('PIRSearch',465)]:d.add(String(x,235,title,textAnchor='middle',fontName='Helvetica-Bold',fontSize=10));d.add(Line(x,25,x,220,strokeColor=colors.lightgrey))
  for y,a,b,label in [(205,45,190,'search criteria'),(177,190,335,'search(criteria)'),(149,335,465,'evaluate'),(121,335,190,'matching PIRs'),(93,190,45,'IDs and details'),(65,45,190,'update ID fields'),(37,190,335,'getRecord; typed update')]:arrow(a,y,b,y,label)
 return d

# Preserve the source DOCX and export the requirements into the formal PDF.
z=zipfile.ZipFile(R/'docs/PIM_SRS_Draft.docx');root=ET.fromstring(z.read('word/document.xml'));ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'};items=[]
for el in root.find('w:body',ns):
 if el.tag.endswith('}p'):
  text=''.join(t.text or '' for t in el.findall('.//w:t',ns))
  if not text or text.startswith(('Personal Information Management System','Software Requirements Specification')):continue
  if text.startswith('This specification'):text='This specification defines the required behavior of the COMP3211 command-line PIM. It covers all eleven user stories and the integrated A, B, C and D modules.'
  text=text.replace('Search, file, and CLI owners should add tests','Search, file, and CLI tests provide verification')
  items.append(p(text,'Heading1' if re.match(r'^\d+ ',text) else 'Heading2' if re.match(r'^\d+\.\d+',text) else 'Normal'));items.append(Spacer(1,7))
 elif el.tag.endswith('}tbl'):
  rows=[[''.join(t.text or '' for t in c.findall('.//w:t',ns)).replace('. pim','.pim') for c in row.findall('w:tc',ns)] for row in el.findall('w:tr',ns)]
  items.append(table(rows,{2:[90,421],3:[55,60,396]}.get(len(rows[0]))));items.append(Spacer(1,10))
items+=section('5 Command contract','The CLI user manual fixes the supported commands, quoting rules, date syntax and replacement semantics. Search follows the B grammar: ! binds before &&, which binds before ||. Parentheses override precedence. Duplicate keys in a record block are invalid and cause loading to fail.')
build(OUT/'01_SRS.pdf','PIM Software Requirements Specification',items)
items=section('1 Architecture','The system uses an MVC-inspired separation. PIMCli combines terminal view and controller in one adapter. The model package owns validation, records, searching and persistence and can run without terminal input. This separates interaction from domain tests. The view is not a separate class; this is a deliberate small-system tradeoff.')+[diagram('architecture')]
items+=section('2 Record structure','PIR is an immutable abstract base class. Four concrete types retain their fields and stable ID. PIMModel owns a LinkedHashMap, preserves insertion order and returns immutable list snapshots. Updates validate a replacement before storing it.')+[diagram('classes')]
items+=section('3 Search and update collaboration','The CLI sends expressions unchanged to PIMModel.search. The model delegates parsing and matching to PIRSearch. The user copies a result ID and supplies changed fields. The CLI reads unchanged fields and calls the type-specific update. Expected errors preserve the command loop.')+[diagram('sequence')]
for f in ['A_core_design.md','B_search_design.md','C_persistence_design.md']:
 text=(R/'docs'/f).read_text(encoding="utf-8").split('## Integration contract')[0]
 text=text.replace("It can be\nmerged into the group's design document.",'').replace('Later search code must follow\nthis contract.','The implementation follows this contract.')
 items+=[PageBreak()]+md(text)
items+=section('D CLI component API','PIMCli stores a private PIMModel and final PrintWriter. PIMCli(PrintWriter out) accepts the output destination. static void main(String[] args) throws IOException starts UTF-8 streams. void run(BufferedReader input) throws IOException drives the loop. boolean execute(String line) returns false for quit or exit. The controller catches expected argument, missing-record, date and file exceptions. Load assigns a new model only after successful decoding.')
items+=section('Persistence failures','Duplicate keys, unknown fields, malformed values and duplicate record IDs are rejected. Save overwrites the destination; atomic replacement after a disk write failure is not guaranteed. Normal round trips preserve IDs and order. Timestamps use minute precision.')
build(OUT/'02_Design.pdf','PIM Design Document',items)
manual=(R/'docs/D_CLI_manual.md').read_text(encoding="utf-8")
build(OUT/'03_User_Manual.pdf','PIM User Manual',section('Getting started','From PowerShell in the project root, run .\\run.ps1. Enter commands at pim>. Type help to list commands.')+md(manual.split('## User guide')[1].split('## Design')[0])+section('Input errors','Correct the input and try again after Error:. Search comparisons require spaces around operators, for example type = task. Empty matches display 0 record(s). ID commands accept the full ID or any unique prefix; an ambiguous prefix is rejected.'))
build(OUT/'04_Developer_Manual.pdf','PIM Developer Manual',md(manual.split('## Developer guide')[1].split('## User guide')[0])+section('Coverage','Run .\\run_coverage.ps1. It compiles with debug information, runs all checks, then instruments only the three model test suites. JaCoCo 0.8.11 produces build/coverage.xml and coverage-report/index.html. CLI integration execution is excluded from model unit coverage.')+section('Debugging','In IntelliJ IDEA configure JDK 21, mark src/main/java as sources and src/test/java as tests, then run or debug cli.PIMCli.main. Set breakpoints in execute or PIMModel. Test entry points are self-contained main methods with explicit AssertionError checks; -ea is not required.')+section('Compatibility','Windows with JDK 21.0.3 is the verified build platform. UTF-8 compilation targets Java 8 bytecode, and the CLI and full test suite were also executed on a Java 8 runtime (1.8.0_503). The runtime uses standard libraries only. JaCoCo is a test tool.')+section('Document generation','tools/build_submission_docs.py generates PDFs using Python and ReportLab as authoring tools only. They are not application dependencies. Regenerate coverage before rebuilding the PDF reports.'))
rows=[['Requirement','Implemented behavior','Verification']]
data=[('Four types and stable IDs','ModelCoreTest'),('Notes','ModelCoreTest; CliTest'),('Tasks and deadlines','ModelCoreTest; CliTest'),('Events and alarm fields','ModelCoreTest; CliTest'),('Contacts','ModelCoreTest; CliTest'),('Modify with stable ID and type','ModelCoreTest; CliTest'),('Field and boolean search','SearchTest; CliTest'),('Print one or all','PersistenceTest; CliTest'),('Delete by ID','PersistenceTest; CliTest'),('Save .pim files','PersistenceTest'),('Load .pim files','PersistenceTest; CliTest')]
for i,(label,test) in enumerate(data,1):rows.append([f'US{i}',label,test])
items=[p('All eleven user stories are implemented. The following executable checks provide evidence for tested scenarios, not proof of absence of defects.'),Spacer(1,10),table(rows,[60,245,206])]
items+=section('SRS traceability','FR-01 through FR-06 map to US1 through US6. FR-07 and FR-08 map to US7. FR-09, FR-10, FR-11 and FR-12 map to US8, US9, US10 and US11 respectively. All are implemented. Invalid updates and failed loads preserve stored records.')
items+=[table([['Requirement','Status and evidence'],['NFR-01','Standard-library application verified on JDK 21 and executed on a Java 8 runtime; CLI and tests pass on both.'],['NFR-02','Domain code is isolated in model and tested without CLI.'],['NFR-03','Expected input and file failures show readable errors; recovery checks pass.'],['NFR-04','Automatic model tests and current line coverage report supplied.']],[100,411])]
build(R/'Requirements_Coverage.pdf','PIM Requirements Coverage',items)
xml=ET.parse(R/'build/coverage.xml').getroot();counts={c.attrib['type']:(int(c.attrib['missed']),int(c.attrib['covered'])) for c in xml.findall('counter')};m,c=counts['LINE'];pct=100*c/(c+m)
rows=[['Measure','Covered','Total','Coverage']]+[[k,str(c),str(c+m),f'{100*c/(c+m):.2f}%'] for k,(m,c) in counts.items() if k in ['LINE','BRANCH','INSTRUCTION','METHOD','CLASS']]
build(R/'Model_Line_Coverage.pdf','PIM Model Line Coverage',[p(f'Model line coverage is {pct:.2f}%. Measured on 2 November 2026 using JaCoCo 0.8.11 and JDK 21.0.3.'),Spacer(1,12),table(rows,[160,100,100,151])]+section('Reproduce the measurement','Execute .\\run_coverage.ps1. Scope: build/classes/model; instrumented suites: ModelCoreTest, SearchTest and PersistenceTest. CliTest is excluded. Detailed results: coverage-report/index.html and build/coverage.xml. Regenerate this PDF after source or test changes.')+section('Interpretation','Coverage measures executed lines, not assertion strength or requirement completeness. These tests compare values and expected exceptions. Full coverage is not claimed; the project description does not prescribe a minimum percentage.'))
# The course requests offline presentation slides in PDF format.
from reportlab.pdfgen import canvas
slides=[('Personal information records','Speaker A  65 seconds',['Creation requirement: retain every field and return a stable unique ID.','Four immutable types: note, task, event and contact.','PIMModel preserves order and validates replacements before updates.']),('Search criteria and results','Speaker B  65 seconds',['Criterion requirement: combine type, text and time tests with &&, || and !.','Search requirement: return matching records without changing stored data.','Example: type = task && description contains "report"','Precedence: ! before && before ||. Parentheses override it.']),('Architecture and persistence','Speaker C  65 seconds',['MVC-inspired separation isolates domain code in model.','PIMCli delegates to PIMModel. PIRSearch evaluates criteria.','PIMFileCodec stores records in UTF-8 .pim files.','Base64 text fields preserve Unicode and newlines.']),('Integration and testing lesson','Speaker D  65 seconds',['Search returns IDs. Partial updates preserve unchanged fields.','Failed loads retain the collection. Invalid input keeps the CLI usable.',f'Model line coverage: {pct:.2f}% from model unit tests only.','Testing lesson: assert every persisted field and failure outcome.'])]
cv=canvas.Canvas(str(OUT/'05_Presentation.pdf'),pagesize=(960,540))
for index,(title,speaker,bullets) in enumerate(slides):
 cv.setFillColor(colors.HexColor('#f7f9fb'));cv.rect(0,0,960,540,fill=1,stroke=0);cv.setFillColor(colors.HexColor('#17334b'));cv.setFont('Helvetica-Bold',30);cv.drawString(45,470,title);cv.setFont('Helvetica',14);cv.drawString(45,435,speaker);yy=375
 for b in bullets:
  para=Paragraph(html.escape(b),ParagraphStyle(name='slide',fontSize=21,leading=29,textColor=colors.HexColor('#17334b')));w,h=para.wrap(850,100);para.drawOn(cv,50,yy-h);yy-=h+25
 cv.setFont('Helvetica',11);cv.drawString(45,25,'COMP3211 PIM');cv.drawRightString(915,25,f'{index+1} / 4');cv.showPage()
cv.save()
build(OUT/'06_Recording_Guide.pdf','PIM Recording and Submission Guide',section('System demonstration','Target 3 minutes 40 seconds: start (20 seconds); create four types (50); print and copy ID (20); search and update (50); save, restart and load (45); invalid input and delete (35). Record the actual program as MP4. This guide is not the video.')+section('Presentation recording','Use four slides, approximately 65 seconds per member, totaling 4 minutes 20 seconds plus brief transitions within five minutes. Each presenter must speak for at least one minute. All presenters show faces and student IDs at the beginning; the current presenter remains visible. A/B/C/D labels are speaking sections, not contribution percentages.')+section('Example demonstration','Create a task called Finish report, search type = task && description contains "report", copy its ID, and update only deadline. Print it to show the description remains. Save demo.pim, restart and load it. Try an unknown ID or invalid date, then print all to show recovery.')+section('Human completion','Review group facts, record the two MP4 videos, agree contributions and complete the official Honour Declaration with signatures and accurate AI acknowledgement. Add these materials before final submission. No names, student IDs, signatures or contribution percentages have been fabricated.'))
print(f'Generated eight PDFs. Model line coverage: {pct:.2f}%')

