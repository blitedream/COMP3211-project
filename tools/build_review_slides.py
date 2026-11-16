from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
R=Path(__file__).resolve().parents[1]; OUT=R/'build/audit'; OUT.mkdir(parents=True,exist_ok=True)
TEAL=HexColor('#197b82'); DARK=HexColor('#252c35'); LIGHT=HexColor('#eef5f6')
def text(c,x,y,t,size=15,bold=False,color=DARK):
 c.setFillColor(color);c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.drawString(x,y,t)
def base(name,kicker,title):
 c=canvas.Canvas(str(OUT/name),pagesize=(960,540));c.setFillColorRGB(1,1,1);c.rect(0,0,960,540,fill=1,stroke=0)
 text(c,60,493,kicker,10,True,TEAL);text(c,60,448,title,30,True)
 c.setStrokeColor(TEAL);c.setLineWidth(2);c.line(60,414,125,414);return c
def box(c,x,y,w,title,sub):
 c.setFillColor(LIGHT);c.setStrokeColor(TEAL);c.setLineWidth(.8);c.roundRect(x,y,w,58,4,fill=1,stroke=1)
 text(c,x+12,y+35,title,16,True);text(c,x+12,y+15,sub,11)
def arrow(c,x,y,xx,yy):
 import math
 c.setStrokeColor(TEAL);c.setLineWidth(1);c.line(x,y,xx,yy);a=math.atan2(yy-y,xx-x)
 for d in [-.45,.45]:c.line(xx,yy,xx-6*math.cos(a+d),yy-6*math.sin(a+d))
c=base('PIM_Architecture_Quality.pdf','DESIGN / ARCHITECTURE','Separate terminal input from domain rules')
box(c,60,316,140,'Terminal','commands / output');box(c,264,316,180,'PIMCli','view + controller');box(c,520,316,180,'PIMModel','ordered collection');box(c,757,316,143,'PIRSearch','criteria parser')
arrow(c,200,345,264,345);arrow(c,444,345,520,345);arrow(c,700,345,757,345)
box(c,264,217,180,'PIMFileCodec','encode / decode .pim');box(c,520,217,200,'AtomicFileWriter','stage / flush / replace')
arrow(c,354,316,354,275);arrow(c,444,246,520,246)
text(c,60,172,'WHY THESE BOUNDARIES?',11,True,TEAL)
text(c,60,142,'CLI stays replaceable; model tests run without terminal input.',17)
text(c,60,112,'Immutable replacements preserve the old record when validation fails.',17)
text(c,60,82,'Ordered storage makes output predictable; linear search keeps this scope simple.',17)
text(c,60,40,'Trade-off: extra record allocation and temporary disk space; no concurrent-user service.',11)
c.save()
c=base('PIM_Testing_Quality.pdf','LESSON / UNIT TESTING','Test the saved state after a failure')
text(c,60,378,'A reported error alone does not prove that user data survived.',20,True)
for x,n,title,body in [(60,'1','Given','An existing saved collection'),(350,'2','When','A staged write fails midway'),(640,'3','Then','Original bytes are identical')]:
 c.setFillColor(LIGHT);c.roundRect(x,234,260,113,5,fill=1,stroke=0)
 text(c,x+16,315,n+'  '+title,19,True,TEAL);text(c,x+16,284,body,14)
text(c,60,199,'Also assert: no abandoned staging file; failed loads and updates retain records.',15)
text(c,60,160,'The same contract now protects saving: stage first, then atomically replace.',17,True)
for x,val,label in [(60,'56','named model cases'),(350,'99.30%','model line coverage'),(640,'95.90%','model branch coverage')]:
 text(c,x,96,val,30,True,TEAL);text(c,x,72,label,13)
text(c,60,34,'Measured 16 Nov 2026. Coverage is execution evidence, not proof of correctness.',11)
c.save()
print('Generated architecture and testing slides')
