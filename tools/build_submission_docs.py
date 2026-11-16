from pathlib import Path
import re, xml.etree.ElementTree as ET, zipfile, html
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Preformatted, KeepInFrame
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
# Avoid a section heading stranded at the bottom of a page with its table on the next page.
styles['Heading1'].keepWithNext=1; styles['Heading2'].keepWithNext=1
def p(t,style='Normal'): return Paragraph(html.escape(t).replace('\n','<br/>'),styles[style])
def table(rows,widths=None):
 t=Table([[p(str(c)) for c in row] for row in rows],colWidths=widths,repeatRows=1,hAlign='LEFT')
 t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e8edf1')),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),7),('RIGHTPADDING',(0,0),(-1,-1),7),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),('LINEBELOW',(0,0),(-1,-1),.2,colors.lightgrey)]));return t
def footer(c,d):
 c.setFont('Helvetica',8); c.drawString(42,25,'COMP3211 PIM | 16 November 2026');c.drawRightString(d.pagesize[0]-42,25,str(d.page))
def build(path,title,items):
 while items and isinstance(items[-1], Spacer): items.pop()
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
 def arrow(x,y,xx,yy,label='',hollow=False):
  import math
  d.add(Line(x,y,xx,yy,strokeColor=colors.HexColor('#567086')));a=math.atan2(yy-y,xx-x);u=7
  d.add(Polygon([xx,yy,xx-u*math.cos(a-.4),yy-u*math.sin(a-.4),xx-u*math.cos(a+.4),yy-u*math.sin(a+.4)],fillColor=colors.white if hollow else colors.HexColor('#567086'),strokeColor=colors.HexColor('#567086')))
  if label:d.add(String(min(x,xx)+4,(y+yy)/2+5,label,fontSize=8))
 if kind=='architecture':
  box(5,165,110,60,'Terminal','Input and output');box(170,165,170,60,'PIMCli','View and controller');box(170,55,170,70,'PIMModel','Collection and updates');box(370,145,135,65,'PIRSearch','Criteria parser');box(370,30,135,65,'PIMFileCodec','File serialization')
  arrow(115,195,170,195);arrow(255,165,255,125);arrow(340,90,370,170);arrow(330,165,400,95)
 elif kind=='components':
  box(5,165,135,65,'PIMCli','model: PIMModel');box(190,165,140,65,'PIMModel','records: Map<String,PIR>');box(380,165,125,65,'PIRSearch','search(records, criteria)')
  box(190,40,140,65,'PIR abstract','id: String; four subtypes');box(5,40,135,65,'PIMFileCodec','save / load')
  arrow(140,195,190,195,'owns 1');arrow(330,195,380,195,'delegates');arrow(260,165,260,105,'holds 0..*');arrow(70,165,70,105,'calls');arrow(140,70,190,70,'reads/builds')
  d.add(String(170,12,'Search reads PIR getters; codec reconstructs typed records.',fontSize=9))
 elif kind=='classes':
  box(160,175,190,65,'PIR abstract','id: String; getId(); getType()\ntoDetailString()')
  for x,title,fields in [(0,'Note','text'),(130,'Task','description, deadline'),(260,'Event','description, start, alarm'),(390,'Contact','name, address, mobile')]:
   box(x,70,120,65,title,fields);arrow(x+60,135,255,175,hollow=True)
  d.add(String(15,30,'Arrows indicate inheritance. PIMModel holds PIRs in an ordered map.',fontSize=9))
 elif kind=='saving':
  d=Drawing(510,140)
  for x,title,sub in [(0,'PIMFileCodec','serialize all records'),(130,'AtomicFileWriter','stage and force'),(260,'Files.move','atomic replace'),(390,'Destination','new complete bytes')]:
   box(x,65,120,60,title,sub)
  for x in [120,250,380]:arrow(x,95,x+10,95)
  d.add(String(5,35,'Failure before replacement: preserve old destination; remove staged file.',fontSize=9))
  d.add(String(5,16,'Unsupported atomic replacement: report IOException; no unsafe fallback.',fontSize=9))
 else:
  d=Drawing(510,490)
  for title,x in [('User',40),('PIMCli',180),('PIMModel',325),('PIRSearch',460)]:
   d.add(Rect(x-44,456,88,26,fillColor=colors.HexColor('#eef3f7'),strokeColor=colors.grey))
   d.add(String(x,466,title,textAnchor='middle',fontName='Helvetica-Bold',fontSize=9))
   d.add(Line(x,15,x,456,strokeColor=colors.lightgrey,strokeDashArray=[3,3]))
  for x,y,h in [(180,332,108),(325,354,64),(460,376,20),(180,30,280),(325,53,235)]:
   d.add(Rect(x-3,y,6,h,fillColor=colors.HexColor('#e2edf0'),strokeColor=colors.grey))
  def msg(y,a,b,label,reply=False):
   d.add(Line(a,y,b,y,strokeColor=colors.HexColor('#567086'),strokeDashArray=[3,2] if reply else None))
   side=-1 if b>a else 1
   d.add(Line(b,y,b+side*5,y+3,strokeColor=colors.HexColor('#567086')))
   d.add(Line(b,y,b+side*5,y-3,strokeColor=colors.HexColor('#567086')))
   d.add(String(min(a,b)+6,y+5,label,fontSize=7.5))
  for y,a,b,label,reply in [(440,40,180,'search criterion',False),(418,180,325,'search(criterion)',False),(396,325,460,'search(snapshot, criterion)',False),(376,460,325,'matching PIRs',True),(354,325,180,'matches',True),(332,180,40,'IDs and details',True),(310,40,180,'update ID deadline value',False),(288,180,325,'getRecord(resolved ID)',False),(266,325,180,'current PIR',True),(236,180,325,'updateTask(id, description, deadline)',False)]:msg(y,a,b,label,reply)
  d.add(String(187,249,'preserve unspecified fields',fontSize=7.5))
  d.add(Rect(15,16,480,204,fillColor=None,strokeColor=colors.grey))
  d.add(String(21,207,'alt [replacement validates]',fontSize=8))
  d.add(Line(325,190,425,190,strokeColor=colors.grey));d.add(Line(425,190,425,173,strokeColor=colors.grey))
  msg(173,425,325,'validate; put same ID')
  msg(151,325,180,'replacement PIR',True);msg(129,180,40,'Updated ID',True)
  d.add(Line(15,115,495,115,strokeColor=colors.grey,strokeDashArray=[3,3]))
  d.add(String(21,102,'[validation fails]',fontSize=8))
  d.add(String(333,81,'no map replacement',fontSize=8))
  msg(64,325,180,'InvalidRecordException',True);msg(35,180,40,'Error: reason; old PIR retained',True)

 return d

# Preserve the source DOCX and export the requirements into the formal PDF.
z=zipfile.ZipFile(R/'docs/PIM_SRS_Draft.docx');root=ET.fromstring(z.read('word/document.xml'));ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'};# Column widths per table, chosen from the header text so that narrow ID
# columns stay narrow while command and component names get room to breathe.
def word_cell_text(cell):
 parts=[]
 for node in cell.iter():
  if node.tag=='{'+ns['w']+'}t': parts.append(node.text or '')
  elif node.tag=='{'+ns['w']+'}br': parts.append('\n')
 return ''.join(parts)

def table_widths(header):
 first=str(header[0]).strip().lower()
 if len(header)==2:
  return {'id':[60,451],'command and syntax':[150,361],'pir type':[120,391],'component':[140,371]}.get(first,[120,391])
 return {'version and date':[80,65,366],'requirements':[85,80,346],'requirement':[85,80,346]}.get(first,[60,70,381])
items=[]
for el in root.find('w:body',ns):
 if el.tag.endswith('}p'):
  text=''.join(t.text or '' for t in el.findall('.//w:t',ns))
  if not text:
   if el.find('.//w:br[@w:type="page"]',ns) is not None:
    items=[KeepInFrame(511,700,items,mode='shrink'),PageBreak()]
   continue
  if text.startswith(('Personal Information Management System','Software Requirements Specification')):continue
  text=text.replace('Search, file, and CLI owners should add tests','Search, file, and CLI tests provide verification')
  style_node=el.find('w:pPr/w:pStyle',ns)
  word_style=style_node.get('{'+ns['w']+'}val','') if style_node is not None else ''
  pdf_style='Heading1' if word_style=='Heading1' or re.match(r'^\d+ ',text) else 'Heading2' if word_style=='Heading2' or re.match(r'^\d+\.\d+',text) else 'Normal'
  items.append(p(text,pdf_style))
  gap=Spacer(1,7);gap.keepWithNext=pdf_style.startswith('Heading')
  items.append(gap)
 elif el.tag.endswith('}tbl'):
  rows=[[word_cell_text(c).replace('. pim','.pim') for c in row.findall('w:tc',ns)] for row in el.findall('w:tr',ns)]
  items.append(table(rows,table_widths(rows[0])));items.append(Spacer(1,10))
build(OUT/'01_SRS.pdf','PIM Software Requirements Specification',items)
from document_details import design, coverage
items=design(R,p,section,table,diagram,PageBreak)
build(OUT/'02_Design.pdf','PIM Design Document',items)
manual=(R/'docs/D_CLI_manual.md').read_text(encoding="utf-8")
styles['Normal'].fontSize=9.5; styles['Normal'].leading=12.5
build(OUT/'03_User_Manual.pdf','PIM User Manual',section('Getting started','On Windows, double-click the startup CMD file in the project folder, or run .\\run.ps1 from PowerShell. A JDK is required. Enter commands at pim>, not in PowerShell. Type help to list commands. The double-click launcher saves relative paths in the project folder.')+md(manual.split('## User guide')[1].split('## Design')[0]))
styles['Normal'].fontSize=10; styles['Normal'].leading=14
build(OUT/'04_Developer_Manual.pdf','PIM Developer Manual',md(manual.split('## Developer guide')[1].split('## User guide')[0])+section('Coverage','Run .\\run_coverage.ps1. It compiles with debug information, runs all checks, then instruments only the four model test suites. JaCoCo 0.8.11 produces build/coverage.xml and coverage-report/index.html. CLI integration execution is excluded from model unit coverage.')+section('Debugging','In IntelliJ IDEA configure JDK 21, mark src/main/java as sources and src/test/java as tests, then run or debug cli.PIMCli.main. Set breakpoints in execute or PIMModel. Test entry points are self-contained main methods with explicit AssertionError checks; -ea is not required.')+section('Compatibility','Windows with JDK 21.0.3 is the verified build platform. UTF-8 compilation targets Java 8 bytecode. This revision was tested on JDK 21.0.3; a Java 8 runtime was not re-tested. The runtime uses standard libraries only. JaCoCo is a test tool.')+section('Document generation','tools/build_submission_docs.py generates PDFs using Python and ReportLab as authoring tools only. They are not application dependencies. Regenerate coverage before rebuilding the PDF reports.'))
build(R/'Requirements_Coverage.pdf','PIM Requirements Coverage',coverage(p,section,table,PageBreak))
xml=ET.parse(R/'build/coverage.xml').getroot();counts={c.attrib['type']:(int(c.attrib['missed']),int(c.attrib['covered'])) for c in xml.findall('counter')};m,c=counts['LINE'];pct=100*c/(c+m)
rows=[['Measure','Covered','Total','Coverage']]+[[k,str(c),str(c+m),f'{100*c/(c+m):.2f}%'] for k,(m,c) in counts.items() if k in ['LINE','BRANCH','INSTRUCTION','METHOD','CLASS']]
build(R/'Model_Line_Coverage.pdf','PIM Model Line Coverage',[p(f'Model line coverage is {pct:.2f}%. Measured on 16 November 2026 using JaCoCo 0.8.11, javac 21.0.3 and Java 21.0.3.'),Spacer(1,12),table(rows,[160,100,100,151])]+section('Reproduce the measurement','Execute .\\run_coverage.ps1. Scope: build/classes/model; instrumented suites: ModelCoreTest, SearchTest, PersistenceTest and AtomicSaveTest. CliTest is excluded. Detailed results: coverage-report/index.html and build/coverage.xml. Regenerate this PDF after source or test changes.')+section('Interpretation','56 named model cases across four suites pass; CLI checks are separate. Coverage measures executed lines, not assertion strength or requirement completeness. These tests compare values and expected exceptions. Full coverage is not claimed; the project description does not prescribe a minimum percentage.'))
# Preserve the reviewed Canva deck; never regenerate obsolete slides.
import shutil
shutil.copy2(R/'presentation_final/PIM_Presentation.pdf', OUT/'05_Presentation.pdf')
build(OUT/'06_Recording_Guide.pdf','PIM Recording and Submission Guide',section('System demonstration','Target 3 minutes 40 seconds: start (20 seconds); create four types (50); print and copy ID (20); search and update (50); save, restart and load (45); invalid input and delete (35). Record the actual program as MP4. This guide is not the video.')+section('Presentation recording','Use the reviewed eight-slide deck: A pages 1-2, B pages 3-4, C pages 5-6, D pages 7-8. Target A/B at 70 seconds each and C/D at 75 seconds each (4 minutes 50 seconds including ID displays), leaving 10 seconds for transitions. Rehearsal targets are not measured durations. Each presenter must speak for at least one minute. All presenters show faces and student IDs at the beginning; the current presenter remains visible. A/B/C/D labels are speaking sections, not contribution percentages.')+section('Example demonstration','Create a task called Finish report, search type = task && description contains "report", copy its ID, and update only deadline. Print it to show the description remains. Save demo.pim, restart and load it. Try an unknown ID or invalid date, then print all to show recovery.')+section('Human completion','Review group facts, record the two MP4 videos, agree contributions and complete the official Honour Declaration with signatures and accurate AI acknowledgement. Add these materials before final submission. No names, student IDs, signatures or contribution percentages have been fabricated.'))
print(f'Generated eight PDFs. Model line coverage: {pct:.2f}%')

