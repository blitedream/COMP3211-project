from pathlib import Path
import html, re, json
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'presentation_v2'
OUT.mkdir(exist_ok=True)
NAVY = '#102B3F'
TEAL = '#007F79'
PALE = '#E9F3F1'
GRAY = '#526471'
W,H = 1280,720
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))

def text(c,x,y,content,size=25,width=1120,color=NAVY,bold=False,font=None):
    style=ParagraphStyle('s',fontName=font or ('Helvetica-Bold' if bold else 'Helvetica'),fontSize=size,leading=size*1.3,textColor=colors.HexColor(color))
    paragraph=Paragraph(html.escape(content).replace('\n','<br/>'),style)
    _,height=paragraph.wrap(width,600)
    paragraph.drawOn(c,x,H-y-height)
    return height

def base(c,number,title,subtitle,backup=False):
    c.setFillColor(colors.HexColor('#FAFCFB'));c.rect(0,0,W,H,fill=1,stroke=0)
    c.setFillColor(colors.HexColor(TEAL));c.rect(48,H-68,48,5,fill=1,stroke=0)
    text(c,48,86,title,40,bold=True)
    text(c,50,148,subtitle,19,color=GRAY)
    text(c,48,674,'COMP3211  /  PERSONAL INFORMATION MANAGEMENT',12,color=GRAY)
    text(c,1090,674,('BACKUP ' if backup else '')+f'{number:02}',12,width=140,color=GRAY)

def block(c,x,y,w,h,title,body,fill=PALE):
    c.setFillColor(colors.HexColor(fill));c.roundRect(x,H-y-h,w,h,8,fill=1,stroke=0)
    text(c,x+20,y+18,title,23,w-40,bold=True)
    text(c,x+20,y+60,body,18,w-40,color=GRAY)

def arrow(c,x,y,x2,y2):
    import math
    c.setStrokeColor(colors.HexColor(TEAL));c.setLineWidth(2)
    c.line(x,H-y,x2,H-y2)
    a=math.atan2(y2-y,x2-x);p=c.beginPath();p.moveTo(x2,H-y2)
    for sign in [-1,1]:p.lineTo(x2-10*math.cos(a+sign*.45),H-(y2-10*math.sin(a+sign*.45)))
    p.close();c.setFillColor(colors.HexColor(TEAL));c.drawPath(p,fill=1,stroke=0)

def code(c,x,y,lines,w=1120):
    h=30+len(lines)*35
    c.setFillColor(colors.HexColor(NAVY));c.roundRect(x,H-y-h,w,h,8,fill=1,stroke=0)
    for i,line in enumerate(lines):text(c,x+22,y+16+i*35,line,20,w-44,color='#EAF8F6',font='Courier')

def architecture(c,y=230):
    block(c,50,y,260,135,'PIMCli','Terminal view + controller\nParse input and show results')
    block(c,420,y,330,135,'PIMModel','Records and typed updates\nStable IDs and insertion order')
    block(c,880,y,340,135,'PIRSearch','Parse and evaluate criteria\nRead records through getters')
    arrow(c,310,y+65,420,y+65);arrow(c,750,y+65,880,y+65)
    block(c,420,y+220,330,125,'PIMFileCodec','Save and reconstruct records\nUTF-8 .pim files')
    arrow(c,180,y+135,180,y+280);arrow(c,180,y+280,420,y+280)
    text(c,60,y+292,'CLI calls codec for save/load',16,340,color=GRAY)
    text(c,810,y+220,'model package',25,380,bold=True)
    text(c,810,y+268,'Domain code runs independently of terminal input.',22,380)

def classes(c):
    block(c,425,220,430,120,'PIR  «abstract»','id: String\ngetId(), getType(), toDetailString()')
    children=[('Note','text'),('Task','description\ndeadline'),('Event','description\nstartTime, alarmTime'),('Contact','name, address\nmobileNumber')]
    for i,(name,fields) in enumerate(children):
        x=50+i*310
        block(c,x,440,270,130,name,fields)
        arrow(c,x+135,440,x+135,385)
    c.setStrokeColor(colors.HexColor(TEAL));c.line(185,H-385,1115,H-385);arrow(c,640,385,640,340)
    text(c,50,603,'Each subtype is immutable. PIMModel validates a replacement before changing the collection.',22,1170)

def slide(c,n):
    titles={1:('One collection for everyday information','A  /  Scope and record creation'),2:('Creation becomes a verifiable requirement','A  /  US1–US5 and FR-01–FR-05'),3:('A criterion describes what should match','B  /  Defining search conditions'),4:('Search results lead to a precise update','B  /  US7, then US6'),5:('The CLI delegates domain work','C  /  MVC-inspired architecture'),6:('Stable records connect the components','C  /  Main classes and their relationships'),7:('Integration preserves data across commands','D  /  Successful and failed operations'),8:('Assertions make coverage meaningful','D  /  Test evidence and engineering lesson'),9:('Command reference','Backup  /  Supported interactions'),10:('The file contract','Backup  /  Persistence details'),11:('Requirements and evidence','Backup  /  Traceability'),12:('Questions worth preparing for','Backup  /  Design tradeoffs')}
    title,sub=titles[n];base(c,n,title,sub,n>8)
    if n==1:
        text(c,50,215,'Create. Find. Update. Save. Resume.',36,bold=True)
        for i,(name,body) in enumerate([('NOTE','Plain text'),('TASK','Description + deadline'),('EVENT','Description + start + alarm'),('CONTACT','Name + address + mobile')]):
            x=50+(i%2)*610;y=300+(i//2)*145;block(c,x,y,565,115,name,body)
        text(c,50,615,'Java command-line application with explicit local file storage',21,color=GRAY)
    elif n==2:
        text(c,50,218,'The system shall create a task with its description and deadline, return a unique ID and retain both fields.',29,1160,bold=True)
        code(c,50,330,['create task "Finish report" "2026-11-19 18:00"','Created <generated UUID>'])
        block(c,50,465,565,145,'Accepted input','Description is nonblank.\nDeadline is a valid calendar date and time.')
        block(c,660,465,565,145,'Observable result','The ID retrieves the same task.\nInvalid creation leaves the collection unchanged.')
    elif n==3:
        for i,(a,b) in enumerate([('TYPE','type = task'),('TEXT','description contains "report"'),('TIME','deadline < "2026-11-20 20:00"')]):block(c,50+i*400,220,375,135,a,b)
        code(c,50,400,['(type = task || type = event)','&& description contains "report"'])
        text(c,50,530,'Combining rule   !  before  &&  before  ||',29,bold=True)
        text(c,50,585,'Parentheses override precedence. Malformed criteria produce an error.',23)
    elif n==4:
        code(c,50,218,['search type = task && description contains "report"','update <ID> deadline "2026-11-20 18:00"','print <ID>'])
        steps=[('1  Parse','CLI forwards the criteria'),('2  Match','Search reads the model'),('3  Inspect','Show matching IDs + fields'),('4  Update','Preserve ID and other fields')]
        for i,(a,b) in enumerate(steps):block(c,50+i*305,405,280,135,a,b)
        text(c,50,590,'Search preserves stored records and insertion order. Zero matches is a normal result.',23,1170)
    elif n==5:architecture(c)
    elif n==6:classes(c)
    elif n==7:
        block(c,50,220,565,210,'Normal round trip','Create records → save demo.pim\nQuit → restart → load demo.pim\nVerify IDs, types, fields and order')
        block(c,660,220,565,210,'Failure recovery','Reject invalid dates and unknown IDs\nReject malformed or duplicate file fields\nAssign loaded model only after success')
        code(c,50,475,['save "demo.pim"','load "demo.pim"'])
        text(c,50,600,'Save overwrites its file. Successful load replaces the collection. Quit requires an explicit save.',20,1170,color=GRAY)
    elif n==8:
        text(c,50,220,'96.11%',80,bold=True);text(c,50,325,'Model line coverage',25)
        text(c,50,370,'395 / 411 lines\nJaCoCo 0.8.11 · measured 2 Nov 2026',20,470,color=GRAY)
        text(c,600,230,'22 model test groups passed',31,620,bold=True)
        text(c,600,295,'6 core + 7 search + 9 persistence\nAdditional CLI integration checks passed',25,600)
        text(c,50,480,'Lesson learned',29,bold=True)
        text(c,50,535,'Check every persisted field and the outcome of failed operations. Executed lines alone cannot prove correct behavior.',29,1150)
    elif n==9:
        code(c,50,215,['create note TEXT','create task DESCRIPTION DEADLINE','create event DESCRIPTION START ALARM','create contact NAME ADDRESS MOBILE','update ID FIELD VALUE [FIELD VALUE ...]','search CRITERIA','print ID|all     delete ID','save FILE.pim   load FILE.pim   help   quit'])
        text(c,50,575,'Quote values containing spaces. Dates use yyyy-MM-dd HH:mm. Copy the complete ID.',22)
    elif n==10:
        block(c,50,220,565,280,'Text and time','UTF-8 key=value blocks\nBase64 for free-text values\nTime values use minute precision\nBlank line separates records\nOrder follows insertion order')
        block(c,660,220,565,280,'Load validation','Reject unknown type or field\nReject missing required data\nReject duplicate keys and IDs\nReject malformed Base64 or time\nEmpty file loads an empty collection')
        text(c,50,555,'A write failure can leave a partial file. Atomic filesystem replacement remains a limitation.',24)
    elif n==11:
        for i,(a,b) in enumerate([('US1–US6','Record creation and update · ModelCoreTest'),('US7','Type, text, time and logic · SearchTest'),('US8–US11','Print, delete, save and load · PersistenceTest'),('CLI integration','Routing, partial updates and recovery · CliTest')]):
            text(c,50,225+i*90,a,25,260,bold=True);text(c,340,225+i*90,b,25,870)
    else:
        questions=[('Why immutable records?','Validation completes before replacing a record, preserving failed-update behavior.'),('Why use Base64?','It preserves free text with newlines and = characters in the line-based file format.'),('Why combine view and controller?','One small terminal adapter keeps interaction simple while model code remains separate.'),('What does coverage prove?','It records executed lines. Assertions and requirement-based cases supply correctness evidence.')]
        for i,(q,a) in enumerate(questions):text(c,50,220+i*100,q,25,bold=True);text(c,50,258+i*100,a,21,1170,color=GRAY)
    c.showPage()

for filename,numbers in [('COMP3211_Main_8slides.pdf',range(1,9)),('COMP3211_Reference_12slides.pdf',range(1,13))]:
    c=canvas.Canvas(str(OUT/filename),pagesize=(W,H))
    for n in numbers:slide(c,n)
    c.save()

speeches=[
('A','00:00–01:08','Slides 1–2',
'''Hello everyone. Our project is a command-line personal information manager. It brings notes, tasks, events and contacts into one collection. A user can create information, find it later, update it, and save it for another session. I will begin with the creation requirement.
For a task, the system must retain a description and a valid deadline, and return a unique identifier. The command shown creates a task called Finish report. The generated identifier allows us to retrieve exactly that record later. We can verify this requirement by checking the stored description, deadline and ID. We also check invalid inputs, such as a blank description or an impossible date. In those cases, the system must report an error and leave the collection unchanged. These observable outcomes make the requirement testable. B will now explain how users find the records they need.''',
'第 1 页先说用途与四类记录。第 2 页指向命令，再解释可验证的输入、输出和失败结果。UUID 不需要逐字读出。'),
('B','01:08–02:16','Slides 3–4',
'''A search criterion describes which records should match. Our system supports three kinds of comparisons. Users can select a record type, check whether a text field contains a string, or compare a time field with a given time. They can combine conditions using AND, OR and NOT. Parentheses make the intended grouping explicit.
The search requirement is to return matching records without changing the stored collection. Here, we search for tasks whose descriptions contain report. The model evaluates the expression and the CLI displays matching IDs and details. A user can then copy an ID and update only its deadline. The CLI preserves the description and passes the complete replacement values to the model. The record keeps its ID and type. If there are no matches, the system reports zero records. A malformed expression instead produces a clear error. C will explain how the components support this behavior.''',
'第 3 页解释三类比较和逻辑组合，不需要读完所有语法。第 4 页按搜索、显示 ID、修改、打印的顺序讲，区分零结果与语法错误。'),
('C','02:16–03:24','Slides 5–6',
'''Our design separates terminal interaction from domain behavior. PIMCli acts as the view and controller. It reads commands, handles expected errors and displays results. PIMModel manages the record collection and typed updates. It delegates searching to PIRSearch. For file operations, the CLI calls PIMFileCodec to save or reconstruct a model. This separation lets us test domain behavior without starting the terminal interface.
The class structure uses an abstract PIR base class with a stable identifier and common operations. Note, Task, Event and Contact extend it with their own fields. The records are immutable. To update one, the model first constructs and validates a replacement, and stores it only after validation succeeds. A linked map preserves insertion order. Search reads records through their getters, while persistence reconstructs the same types and identifiers. These shared interfaces allow the four modules to work together. D will finish with integration and testing.''',
'第 5 页跟随箭头讲调用关系，强调保存由 CLI 调用 Codec。第 6 页指向父类、四个子类和不可变记录。不要把 MVC 说成三个独立 Java 包。'),
('D','03:24–04:32','Slides 7–8',
'''Integration connects these components into a complete user workflow. We create records, save them, restart the application and load the file. We then check that identifiers, record types, field values and insertion order survive. Error handling also matters. A failed load must preserve the current collection, so the CLI replaces its model only after decoding succeeds. Invalid commands should leave the command loop usable.
Our twenty-two model test groups passed, together with additional CLI integration checks. The model unit tests achieved ninety-six point one one percent line coverage, or three hundred and ninety-five of four hundred and eleven lines. This measurement excludes the CLI checks. The main lesson is that a coverage number needs meaningful assertions. We strengthened the tests to compare every persisted field and verify what remains after a failed operation. This gives us clearer evidence that the system meets its requirements. Thank you.''',
'你负责 D。第 7 页讲完整保存恢复流程与失败后保留数据。第 8 页说明 22 是测试组数，96.11% 是模型行覆盖率。结尾说测试经验，避免声称百分之百无 bug。')]

body=ParagraphStyle('body',fontName='STSong-Light',fontSize=11,leading=16)
heading=ParagraphStyle('heading',parent=body,fontSize=18,leading=24,spaceAfter=10,keepWithNext=True)
english=ParagraphStyle('english',fontName='Helvetica',fontSize=11,leading=17)
def para(s,style=body):return Paragraph(html.escape(s).replace('\n','<br/>'),style)
story=[para('COMP3211 Presentation Production Pack',heading),para('正式展示 8 页，建议全程 4 分 32 秒，预留 28 秒处理换人和语速浮动。A/B/C/D 每人目标 68 秒。逐字稿按约 130 词/分钟设计，实际必须计时排练。备用页另存 12 页参考版，正式录制在第 8 页结束。'),Spacer(1,14),para('录制规则',heading),para('每位同学开始自己部分时展示脸和学生证，发言全程保持脸可见。建议统一把摄像头小窗放在左下或单独视频侧栏，避开正文和数字。按实际录制画面调整位置。不得为了凑时长加速到难以理解。请自行填入真实姓名和学号。')]
for who,time,slides,script,tip in speeches:
    words=len(re.findall(r"\b[\w'-]+\b",script))
    story += [PageBreak(),para(f'Speaker {who}  |  {time}  |  {slides}',heading),para(f'英文稿约 {words} 词。目标 68 秒，请根据排练速度微调停顿。'),Spacer(1,12),para(script,english),Spacer(1,18),para('中文讲解提示',heading),para(tip),Spacer(1,12),para('镜头与翻页',heading),para('前半段约 30 秒讲第一张，后半段约 38 秒讲第二张。每页开头停顿半秒，让观众先看到标题。手动切页时先结束当前句，再切下一页。')]
story += [PageBreak(),para('正式展示分镜',heading)]
shots=[('00:00–00:28','A / slide 1','中景脸与证件开场，随后幻灯片占主体。依次点出四类记录。'),('00:28–01:08','A / slide 2','指向 create 命令，然后比较 accepted input 和 observable result。'),('01:08–01:38','B / slide 3','展示本人身份，指向 TYPE / TEXT / TIME，再解释逻辑组合。'),('01:38–02:16','B / slide 4','沿四步流程移动指针，停在 update 与保留字段的说明。'),('02:16–02:51','C / slide 5','展示本人身份，沿 CLI → model → search 的箭头讲，再讲 CLI → codec。'),('02:51–03:24','C / slide 6','从 PIR 到四个子类，最后说明先验证再替换。'),('03:24–03:58','D / slide 7','展示本人身份，先讲正常恢复，再讲失败恢复。'),('03:58–04:32','D / slide 8','短暂停在 96.11%，随后强调字段断言与失败结果，以 Thank you 结束。')]
for time,who,action in shots:story += [para(f'{time}   {who}',heading),para(action),Spacer(1,7)]
story += [PageBreak(),para('系统演示视频分镜',heading),para('这是另一个文件，要求不超过四分钟。建议录制 3 分 35 秒。录制真实终端操作，使用独立 demo.pim，放大终端字体，隐藏无关窗口。不要用幻灯片代替运行画面。')]
demo=[('00:00–00:20','启动与帮助','运行 .\\run.ps1，然后 help。旁白说明四类记录与主要操作。'),('00:20–01:05','创建与查看','依次创建 note/task/event/contact，再 print all。每次创建后停一秒让 ID 显示。'),('01:05–01:40','组合搜索','输入组合条件并解释结果。再展示一个返回零结果的有效条件。'),('01:40–02:10','部分修改','复制任务完整 UUID，用 update 修改 deadline，再 print ID，指向未变的 description。'),('02:10–02:45','保存与重启恢复','save demo.pim，quit，重新启动，load demo.pim，再 print all。'),('02:45–03:15','错误恢复','输入无效日期与不存在的 ID，展示 Error 后程序继续接受 print all。'),('03:15–03:35','删除与结束','delete ID，再 print all，说明数量减少。保存并退出。')]
for t,title,action in demo:story += [para(t+'  '+title,heading),para(action),Spacer(1,6)]
story += [PageBreak(),para('可复制的演示命令',heading)]
commands=['help','create note "Buy milk"','create task "Finish report" "2026-11-19 18:00"','create event "Group meeting" "2026-11-18 15:00" "2026-11-18 14:30"','create contact Alice "Hong Kong" 12345678','print all','search (type = task || type = event) && description contains "report"','search type = note && text contains "nothing"','update <TASK_ID> deadline "2026-11-20 18:00"','print <TASK_ID>','save "demo.pim"','quit','# Restart with .\\run.ps1','load "demo.pim"','print all','create task bad "2026-02-30 10:00"','print missing','print all','delete <TASK_ID>','print all','save "demo.pim"','quit']
for cmd in commands:story += [para(cmd,ParagraphStyle('code',fontName='Courier',fontSize=9,leading=14))]
story += [Spacer(1,12),para('将 <TASK_ID> 替换成程序生成的完整 UUID，重启后仍使用同一 ID。标有 # 的一行是录制说明，不要输入到 PIM。演示过程中每条命令的输出应与画面一致。')]
story += [PageBreak(),para('答辩准备',heading)]
qa=[('Why MVC-inspired instead of strict MVC?','The terminal view and controller share PIMCli. Domain behavior remains isolated in model, allowing direct unit tests.','这是明确的设计取舍，不要说有独立 View 类。'),('How do partial updates preserve fields?','The CLI retrieves the current record and passes unchanged values with the edited fields to a typed update method.','用任务只改 deadline 的例子回答。'),('What happens when loading fails?','Decoding completes before the CLI assigns the replacement model, so a failed load preserves the current collection.','成功 load 会替换全部集合。'),('Why is the alarm not ringing?','The alarm is stored as event data. The supplied user story requests that field and does not specify a background notification service.','依据项目范围回答。'),('What remains imperfect?','Save overwrites a file directly. A filesystem failure can leave a partial file. Full line coverage and Java 8 runtime execution are not claimed.','承认具体限制，不要编造保证。'),('What does 96.11 percent mean?','The three model unit-test suites executed 395 of 411 model source lines in the measured run.','22 是测试组数，不能说 22 个断言。')]
for q,a,tip in qa:story += [para(q,heading),para(a,english),para(tip),Spacer(1,12)]
story += [PageBreak(),para('最后检查与交接',heading),para('正式演示 PDF 使用 8 页版本。完整参考版含 4 页答辩备用内容。录制前由四位同学确认英文稿符合真实理解，并计时至少两次。确认每人不少于一分钟，总长不超过五分钟。系统演示另录 MP4，不超过四分钟。检查摄像头、声音、字幕、学生证展示和最后导出文件能否正常播放。所有演示数据使用示例联系人，不录入真实联系方式。'),Spacer(1,15),para('证据来源',heading),para('项目说明 Appendix B 与 Deliverables 3–5；本地 PIMModel、PIRSearch、PIMFileCodec、PIMCli；build/coverage.xml 中 LINE covered=395, missed=16。覆盖率日期为 2026-11-02，采用现有测量结果。')]
def footer(c,d):c.setFont('Helvetica',9);c.drawString(42,24,'COMP3211 / Speaker scripts and storyboard');c.drawRightString(550,24,str(d.page))
SimpleDocTemplate(str(OUT/'Speaker_Scripts_and_Storyboard.pdf'),pagesize=(595,842),leftMargin=42,rightMargin=42,topMargin=42,bottomMargin=42).build(story,onFirstPage=footer,onLaterPages=footer)
(OUT/'demo_commands.txt').write_text('\n'.join(commands),encoding='utf-8')
(OUT/'speeches.json').write_text(json.dumps(speeches,ensure_ascii=False,indent=2),encoding='utf-8')
print('Created 8-slide main deck, 12-slide reference deck and speaker/storyboard pack.')
