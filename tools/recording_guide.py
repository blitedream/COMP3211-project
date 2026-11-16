"""Recording instructions and speaker notes for the current terminal interface."""
from pathlib import Path
from html import escape
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Preformatted, KeepTogether
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase.cidfonts import UnicodeCIDFont

TIMELINE = [
    ('0:00–0:10', '欢迎页', '显示空白会话和 pim> 提示符'),
    ('0:10–1:00', '创建', '依次创建 note、task、event、contact'),
    ('1:00–1:20', '查看', 'list 看短编号，print all 看完整字段'),
    ('1:20–1:55', '搜索', '组合条件、时间比较、NOT'),
    ('1:55–2:25', '更新与报错', '修改 #2 的 deadline，再输入非法日期'),
    ('2:25–3:10', '保存与重启', 'save、quit、重新启动、load'),
    ('3:10–3:35', '删除', '删除 #1，确认其余编号不变，再保存'),
    ('3:35–3:40', '结束', '停留在最终结果，停止录屏'),
]

STEPS = [
    ('创建四类记录', '0:10–1:00', [
        'create note "Buy milk"',
        'create task "Finish report" "2026-11-19 18:00"',
        'create event "Group meeting" "2026-11-20 15:00" "2026-11-20 14:30"',
        'create contact "Alice" "Hong Kong" "12345678"',
    ], '从新开的空白会话开始，四条记录应依次为 #1、#2、#3、#4。每执行一条停约 2 秒。日期前后的引号必须是英文双引号。',
     'Create a note, a task, an event and a contact.'),
    ('查看记录', '1:00–1:20', ['list', 'print all'],
     'list 显示四条摘要。print all 显示每条记录的完整字段及保存用的 ID。结果较长时缓慢滚动一次，让字段能看清。',
     'Use short numbers to select records. Print shows every stored field.'),
    ('搜索记录', '1:20–1:55', [
        'search (type = task || type = event) && description contains "report"',
        'search deadline < "2026-11-20 20:00"',
        'search ! type = contact',
    ], '依次得到 1、1、3 条结果。第一条只匹配 Finish report，因为活动名称是 Group meeting。搜索结果里的任务仍是 #2。',
     'Combine conditions, compare a deadline, and exclude contacts.'),
    ('修改任务并验证错误处理', '1:55–2:25', [
        'update 2 deadline "2026-11-20 18:00"',
        'update 2 deadline "2026-02-30 18:00"',
        'print 2',
    ], '第一次更新成功，描述仍是 Finish report。第二次会报 date does not exist，这是故意演示的错误。最后打印确认 deadline 仍是 2026-11-20T18:00。',
     'Update only the deadline. An invalid date leaves the task unchanged.'),
    ('保存，退出，再载入', '2:25–3:10', [
        'save "recording-demo.pim"', 'quit',
        'load "recording-demo.pim"', 'list', 'print 2',
    ], '输入 quit 后，重新双击 Start_PIM.cmd，再输入 load 及后面两条命令。重开的窗口放回录屏范围内。应载入 4 条记录，任务的修改仍在。这里没有在保存前删除记录，所以新会话的任务也是 #2。',
     'Save the collection, restart PIM, and load the same records.'),
    ('删除并保存最终结果', '3:10–3:35', [
        'delete 1', 'print 1', 'list', 'save "recording-demo.pim"',
    ], 'print 1 应报 No current record #1，说明删除生效。list 剩 #2、#3、#4 共三条；本次运行中的短编号不会前移。最后保存，停留 3 秒再停止录屏。录完后可 quit。',
     'Delete the note. Other record numbers stay unchanged.'),
]

# A/B/C/D label speaking sections, not claimed identities or contribution percentages.
SCRIPTS = [
    ('A', '1', '25 秒', '先介绍自己的真实姓名并出示学生证，再接下文。',
     'Our project is a personal information manager written in Java. It keeps notes, tasks, events and contacts in one collection. The user enters commands in a terminal and saves the collection to a .pim file. I will start with the record types and explain how we identify records.'),
    ('A', '2', '45 秒', '说到四类记录时依次指对应字段。短编号与保存 ID 要分清。',
     'A note stores plain text. A task has a description and a deadline. An event has a description, a start time and an alarm time. A contact stores a name, an address and a mobile number. The stored ID stays the same when a record is updated or loaded. In the terminal, list also shows short numbers such as one and two. We can use those numbers without copying the long ID. They stay with their records during a session. After restarting, we check list again. Saving is explicit, so quit does not save changes automatically.'),
    ('B', '3', '35 秒', '先介绍自己的真实姓名并出示学生证。例子在 note #1 后创建 task #2。',
     'The first requirement is to create a record with all required fields. This example follows a note, so the new task receives number two. It stores Finish report and the deadline shown in the command. We can type print two to inspect it. The file still stores a stable ID. We also test a blank description and an impossible date. Both produce an error without changing the collection. A success message alone is not enough, so the tests compare the stored fields with the input.'),
    ('B', '4', '35 秒', '解释表达式含义，不逐个念符号。',
     'A search condition can check a type, find text in a field, or compare a date and time. We combine conditions using and, or and not. Not has the highest priority, followed by and, then or. Parentheses change the grouping. Here, the record must be a task or an event, and its description must contain report. A record of the right type without that word will not match.'),
    ('C', '5', '35 秒', '先介绍自己的真实姓名并出示学生证。页面展示输出摘录，程序会显示完整字段与 ID。',
     'The third requirement is to return matching records. The CLI sends the condition to the model, then displays the results. Here, Finish report matches and Group meeting does not, so we get one record. The result keeps its session number, two. The terminal also shows the full stored ID and fields. Search does not change any records. Results follow insertion order, with each match appearing once. If nothing matches, the program reports zero records and accepts the next command.'),
    ('C', '6', '40 秒', '顺架构图说明调用关系，指出 ConsoleView 只负责格式化。',
     'We use an MVC-inspired design to test domain rules separately from terminal input. ConsoleView is the view, PIMCli is the controller, and PIMModel is the model. PIMCli resolves short numbers to stored IDs. PIMModel owns the records and delegates searches to PIRSearch. PIMFileCodec saves and loads collections, using AtomicFileWriter for staged replacement. Updates validate replacement records before storing them. An ordered map keeps output predictable, and search scans the collection. The design needs extra record allocation and temporary disk space, which are acceptable for this local single-user application.'),
    ('D', '7', '35 秒', '先介绍自己的真实姓名并出示学生证。讲同一 ID 与先验证后替换，不逐个念 getter。',
     'All four record types extend the abstract PIR class. Their fields are immutable. Updating a task builds a replacement with the same stored ID and checks it before changing the collection. A failed update therefore leaves the original record intact. PIMModel keeps insertion order and returns immutable snapshots. In the file, each record has its own block. Text fields use Base64 so that spaces, newlines and Unicode can survive saving and loading. Times are stored to the minute.'),
    ('D', '8', '40 秒', '先讲保存失败的具体例子，再报测试数量和覆盖率。',
     'Our main testing lesson came from failed saves. An error message does not tell us whether the previous file survived. The test starts with an existing saved collection and injects a failure during the staged write. It then compares the original bytes and checks that the temporary file was removed. This tests the data-preservation requirement directly. All fifty-six model tests pass. The current Windows run reports ninety-nine point three zero percent line coverage and ninety-five point nine zero percent branch coverage. The assertions check the results behind those measurements. Thank you.'),
]

def write_text_guides(root):
    text = ['PIM 录制步骤（短编号和彩色终端版）', '目标 3:40，演示视频须为 MP4，最长 4 分钟。', '',
            '准备：先保存原来的工作并退出。重新双击 Start_PIM.cmd，确认 0 record(s)。不要载入原 demo.pim。',
            '下面使用 recording-demo.pim；重复排练会覆盖这个演示文件。如果它已有需要保留的数据，先换一个新文件名。',
            '每次只复制一条命令，不复制 pim>，也不要在末尾添加反斜杠。clear 仅重画界面，不会清空记录或重置编号。',
            'Windows 11：Win+Shift+R，框选 PIM 区域，开始录制。需要跨窗口重启时可录整个已整理的屏幕。',
            '先试录 10 秒并回放。关闭通知，调大终端字体；讲解可后配字幕，不必逐条念命令。', '', '时间安排：']
    text += [f'{time}  {name}：{detail}' for time,name,detail in TIMELINE]
    for i,(name,time,commands,detail,caption) in enumerate(STEPS,1):
        text += ['', f'{i}. {name}（{time}）', detail, '', *commands, '', '可用英文字幕：'+caption]
    text += ['', '录完：原始录像先保留；添加字幕并核对时长后，最终文件放 video/demo.mp4。',
             '另录 presentation.mp4，最长 5 分钟，每人发言至少 1 分钟。各自开始时出示真实学生证和人脸，讲者人脸全程可见。',
             '演示录像与小组汇报是两个独立文件。视频中用最终 PDF 或 Canva 放映，避免把编辑器、备注、脚本窗口录进去。',
             '课程允许使用 GenAI，但要求如实确认其帮助及填写 Honour Declaration。不要修改真实声明或把生成内容说成独立完成。',
             '参考：Project Description - 2026.pdf，第 2–3 页；Microsoft Support: Use Snipping Tool to capture screenshots。',
             'https://support.microsoft.com/en-us/windows/apps/use-snipping-tool-to-capture-screenshots', '']
    (root/'video/Recording_Steps.txt').write_text('\n'.join(text),encoding='utf-8')
    script=['COMP3211 PIM 讲稿', '', '排练目标 4:50。A/B 各约 70 秒，C/D 各约 75 秒。实际以录像计时为准。',
            '每位同学先介绍自己的真实姓名并出示学生证，再讲自己的两页。讲者人脸始终保留在画面内。',
            'A/B/C/D 是讲述段落，不代表贡献比例。无需把字母或提示语念出来。',
            'A：第 1–2 页，0:00–1:10；B：第 3–4 页，1:10–2:20；C：第 5–6 页，2:20–3:35；D：第 7–8 页，3:35–4:50。']
    for member,page,time,cue,words in SCRIPTS:
        script += ['',f'第 {page} 页 / {member} / 约 {time}',cue,'',words]
    script += ['', '录前核对：四人的实际发言均至少 1 分钟，总时长不超过 5 分钟。',
               '按自己的表达调整句子并排练。短编号只在当前运行中有效，存入 .pim 的 ID 保持不变。',
               '录制时采用新版 Canva 或 docs/final/05_Presentation.pdf。第 8 页统计来自 17 November 2026 的 Windows / JDK 21.0.3 验证。', '']
    (root/'presentation/Speaker_Scripts.txt').write_text('\n'.join(script),encoding='utf-8')

def build_recording_guide(root, destination):
    """Four-page Chinese rehearsal guide; command strings stay copyable."""
    font=Path('C:/Windows/Fonts/msyh.ttc'); bold=Path('C:/Windows/Fonts/msyhbd.ttc')
    if font.exists():
        pdfmetrics.registerFont(TTFont('GuideText',str(font),subfontIndex=0))
        pdfmetrics.registerFont(TTFont('GuideBold',str(bold),subfontIndex=0))
        regular,heavy='GuideText','GuideBold'
    else:
        pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light')); regular=heavy='STSong-Light'
    base=dict(fontName=regular,fontSize=10,leading=16,wordWrap='CJK',spaceAfter=7)
    normal=ParagraphStyle('GuideBody',**base)
    title=ParagraphStyle('GuideTitle',fontName=heavy,fontSize=22,leading=30,spaceAfter=14)
    heading=ParagraphStyle('GuideHeading',fontName=heavy,fontSize=14,leading=21,spaceBefore=10,spaceAfter=7,keepWithNext=True)
    code=ParagraphStyle('GuideCode',fontName='Courier',fontSize=10.5,leading=16,spaceAfter=8)
    small=ParagraphStyle('GuideSmall',fontName=regular,fontSize=8,leading=12,wordWrap='CJK',spaceAfter=5)
    def p(t,style=normal): return Paragraph(escape(t).replace('\n','<br/>'),style)
    def commands(lines): return KeepTogether([Preformatted('\n'.join(lines),code)])
    def step(i):
        name,time,cmd,detail,caption=STEPS[i]
        content=[p(f'{i+1}. {name}  {time}',heading),p(detail)]
        if i==4:
            content += [commands(cmd[:2]),p('重新双击 Start_PIM.cmd，看到新的 pim> 后继续：'),commands(cmd[2:])]
        else: content.append(commands(cmd))
        content.append(p('英文字幕：'+caption,small))
        return content
    story=[p('PIM 演示视频录制指南',title),p('适用版本：短编号和彩色终端 / 17 November 2026'),
           p('准备录制',heading),
           p('先保存当前工作并退出。重新双击 Start_PIM.cmd，确认欢迎页显示 0 record(s)。本流程从空白会话创建四条记录，不载入原来的 demo.pim。'),
           p('演示使用 recording-demo.pim。重复排练会覆盖这个演示文件；已有需要保留的数据时，先换一个新文件名。'),
           p('在 Windows 11 按 Win+Shift+R，框选 PIM 所在区域并开始录制。先试录 10 秒，回放确认文字清晰。关闭通知，将指南放到录屏范围外。'),
           p('如果要录入退出后重新启动的过程，可录整个已整理的屏幕。再次启动后，将 PIM 放回原位置。仅录使用演示时可不说话，之后添加字幕。'),
           p('镜头与时间',heading)]
    rows=[[p('时间',small),p('镜头',small),p('画面内容',small)]]+[[p(a,small),p(b,small),p(c,small)] for a,b,c in TIMELINE]
    t=Table(rows,colWidths=[85,90,336],repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e8edf1')),('VALIGN',(0,0),(-1,-1),'TOP'),('TOPPADDING',(0,0),(-1,-1),7),('BOTTOMPADDING',(0,0),(-1,-1),4),('LINEBELOW',(0,0),(-1,-1),.25,colors.lightgrey)]))
    story += [t,Spacer(1,10),p('目标 3:40，预留 20 秒。最终 demo.mp4 必须不超过 4 分钟。时间表是排练目标，录完后以文件实际时长为准。'),
              p('命令一次输入一条。不要复制 pim>，不要在行末加反斜杠。clear 只重画界面，不会清空记录或重置编号。'),PageBreak(),
              p('创建、查看和搜索',title)]
    for i in range(3): story += step(i)
    story += [PageBreak(),p('修改、保存和恢复',title)]
    for i in range(3,6): story += step(i)
    story += [PageBreak(),p('小组汇报与录后检查',title),
              p('另录一份 presentation.mp4',heading),
              p('汇报最长 5 分钟，每位同学发言至少 1 分钟。各自开始时出示学生证和人脸，正在讲述的人脸全程保留在视频中。演示视频与汇报视频分别提交。'),
              p('A：第 1–2 页，0:00–1:10。介绍系统与四类记录。'),
              p('B：第 3–4 页，1:10–2:20。创建要求与搜索条件。'),
              p('C：第 5–6 页，2:20–3:35。搜索结果与系统架构。'),
              p('D：第 7–8 页，3:35–4:50。记录模型与保存失败测试。'),
              p('采用新版 Canva 或最终 PDF 放映。讲稿在 presentation/Speaker_Scripts.txt。姓名由本人开场介绍，讲稿中的 A/B/C/D 不需要念出。录制前实际排练计时，不能仅凭字数判断每人是否满一分钟。'),
              p('容易录错的地方',heading),
              p('1. #2 是这套空白会话流程里的任务编号。如果曾额外创建记录，重新启动后再从第一步开始。'),
              p('2. 非法日期报错和删除后查不到 #1 都是预期演示结果。出现其他错误时，先停止并检查输入。'),
              p('3. 保存后再退出。输入 load 会替换当前集合，不能用它来合并文件。'),
              p('4. 截图工具只负责录屏；用现有会议或录制工具把摄像头画面加入小组汇报。先回放确认讲者与学生证清楚。'),
              p('5. 原始录像先保留。字幕应与实际操作对应，不遮住输入命令、Error 或记录字段。最终回放检查声音、画面及总时长。'),
              p('最终文件',heading),
              p('video/demo.mp4：真实使用演示，最长 4 分钟。\nvideo/presentation.mp4：小组汇报，最长 5 分钟。\nHonour_Declaration.pdf：小组完成后放在项目根目录。'),
              p('课程允许 GenAI，但要求如实确认其帮助并填写 Honour Declaration。技术验证记录和必要声明保持真实。贡献比例由组员共同确认。'),
              p('来源',heading),
              p('Project Description - 2026.pdf，第 2–3 页。命令已对当前版本执行验证，记录在 build/recording-check。',small),
              p('Windows 操作参考：Microsoft Support, Use Snipping Tool to capture screenshots。',small),
              p('https://support.microsoft.com/en-us/windows/apps/use-snipping-tool-to-capture-screenshots',small)]
    def footer(c,d):
        c.setFont('Helvetica',8);c.drawString(42,24,'COMP3211 PIM / 17 November 2026');c.drawRightString(A4[0]-42,24,str(d.page))
    SimpleDocTemplate(str(destination),pagesize=A4,leftMargin=42,rightMargin=42,topMargin=38,bottomMargin=40,title='PIM Recording Guide').build(story,onFirstPage=footer,onLaterPages=footer)
    write_text_guides(root)

if __name__=='__main__':
    root=Path(__file__).resolve().parents[1]
    build_recording_guide(root,root/'docs/final/06_Recording_Guide.pdf')
