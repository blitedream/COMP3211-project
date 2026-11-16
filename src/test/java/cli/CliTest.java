package cli;
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
/** Integration checks for command parsing, model routing and failure recovery. */
public final class CliTest {
 public static void main(String[] args) throws Exception {
  testBlankIds();
  testErrorRecoveryContract();
  testShortReferences();
  testNumericIdCollision();
  testStyledConsole();
  StringWriter text=new StringWriter(); PIMCli cli=new PIMCli(new PrintWriter(text,true));
  cli.execute("create note \"buy milk\"");
  Matcher m=Pattern.compile("Created ([a-f0-9-]+)").matcher(text.toString()); check(m.find(),"created ID"); String id=m.group(1);
  cli.execute("create task report \"2026-11-20 20:00\"");
  cli.execute("create event meeting \"2026-11-20 10:00\" \"2026-11-20 09:00\"");
  cli.execute("create contact Alice \"Hong Kong\" 12345678");
  cli.execute("update "+id+" text \"buy tea\"");
  text.getBuffer().setLength(0);cli.execute("search (type = note || type = task) && ! text contains \"milk\"");
  check(text.toString().contains("2 record(s)"),"compound search");
  Matcher ids=Pattern.compile("Created ([a-f0-9-]+)").matcher("");
  String[] commands={"create task old \"2026-11-20 20:00\"", "create event old \"2026-11-20 10:00\" \"2026-11-20 09:00\"", "create contact Alice HK 123"};
  String[] changes={"description revised", "description revised", "name Grace mobile 456"};
  String[] retained={"2026-11-20T20:00", "2026-11-20T09:00", "Address: HK"};
  for(int i=0;i<commands.length;i++) {
   text.getBuffer().setLength(0);cli.execute(commands[i]);ids.reset(text.toString());check(ids.find(),"created extra type");String key=ids.group(1);
   cli.execute("update "+key+" "+changes[i]);text.getBuffer().setLength(0);cli.execute("print "+key);
   check(text.toString().contains(retained[i]) && text.toString().contains(i==2?"Grace":"revised"),"partial update preserves fields");
   String before=text.toString();cli.execute("update "+key+" unknown bad");text.getBuffer().setLength(0);cli.execute("print "+key);check(before.equals(text.toString()),"invalid update atomic");
   cli.execute("delete "+key);
  }
  Path file=Files.createTempFile("pim-cli-", ".pim");
  try {
   cli.execute("save \""+file+"\"");cli.execute("delete "+id);cli.execute("load \""+file+"\"");
   text.getBuffer().setLength(0);cli.execute("print all");check(text.toString().contains("4 record(s)")&&text.toString().contains("buy tea"),"round trip");
   Files.write(file,"invalid".getBytes("UTF-8"));
   cli.execute("load \""+file+"\"");
   cli.execute("update "+id+" text \"\"");cli.execute("update "+id+" deadline bad");
   cli.execute("create task bad \"2026-02-30 10:00\"");cli.execute("create note \"unclosed");cli.execute("delete missing");
   text.getBuffer().setLength(0);cli.execute("print all");check(text.toString().contains("4 record(s)")&&text.toString().contains("buy tea"),"failed operations preserve collection");
   text.getBuffer().setLength(0);cli.execute("save bad.txt");check(text.toString().contains("Error:"),"extension rejected");
   text.getBuffer().setLength(0);cli.execute("print "+id.substring(0,8));check(text.toString().contains("buy tea"),"unique ID prefix resolves");
   text.getBuffer().setLength(0);cli.execute("print \"\"");check(text.toString().contains("ID must be nonblank"),"empty ID rejected");
   text.getBuffer().setLength(0);cli.execute("create task bad2 \"2026-02-30 10:00\"");check(text.toString().contains("Invalid date/time"),"date error is readable");
   check(!cli.execute("quit"),"exit");
   cli.run(new BufferedReader(new StringReader("help\n")));
  } finally {Files.deleteIfExists(file);}
  System.out.println("CLI integration checks passed");
 }
 /** Empty/blank IDs must fail for all ID commands at every collection size. */
 private static void testBlankIds() throws Exception {
  for(int size=0;size<=2;size++) {
   StringWriter output=new StringWriter(); PIMCli subject=new PIMCli(new PrintWriter(output,true));
   for(int i=0;i<size;i++) subject.execute("create note record"+i);
   output.getBuffer().setLength(0);subject.execute("print all");String before=output.toString();
   for(String blank:new String[]{"\"\"","\"   \"","\"\t\""}) {
    for(String command:new String[]{"print "+blank,"delete "+blank,"update "+blank+" text changed"}) {
     output.getBuffer().setLength(0);subject.execute(command);
     check(output.toString().contains("Error: ID must be nonblank"),"blank ID rejected: "+command);
     output.getBuffer().setLength(0);subject.execute("print all");
     check(before.equals(output.toString()),"blank ID preserves collection size "+size);
    }
   }
  }
  // Use deterministic imported IDs to test ambiguity without random UUID collisions.
  Path fixture=Files.createTempFile("pim-id-prefix-", ".pim");
  try {
   model.PIMModel records=new model.PIMModel();records.addRecord(new model.Note("shared-one","first"));records.addRecord(new model.Note("shared-two","second"));
   model.PIMFileCodec.save(records,fixture);
   StringWriter output=new StringWriter();PIMCli subject=new PIMCli(new PrintWriter(output,true));
   subject.execute("load \""+fixture+"\"");
   output.getBuffer().setLength(0);subject.execute("delete shared-");
   check(output.toString().contains("Ambiguous ID prefix"),"nonempty ambiguous prefix rejected");
   output.getBuffer().setLength(0);subject.execute("print shared-o");
   check(output.toString().contains("first"),"unique prefix resolves");
   output.getBuffer().setLength(0);subject.execute("print all");
   check(output.toString().contains("2 record(s)"),"ambiguous deletion preserves records");
  } finally {Files.deleteIfExists(fixture);}
 }
 /** NFR-03: an error has a reason, no stack trace, preserves data, and allows help next. */
 private static void testErrorRecoveryContract() throws Exception {
  String[] invalid = {"unknown", "create note", "create task x bad-date", "search type = alien", "delete missing", "save bad.txt"};
  for(String command : invalid) {
   StringWriter output = new StringWriter(); PIMCli subject = new PIMCli(new PrintWriter(output,true));
   subject.execute("create note keep"); output.getBuffer().setLength(0);
   subject.execute("print all"); String before = output.toString(); output.getBuffer().setLength(0);
   check(subject.execute(command), "continues after " + command);
   String error = output.toString().trim();
   check(error.startsWith("Error: ") && error.length() > 7, "reason after Error for " + command);
   check(!error.contains("Exception in thread") && !error.contains("\tat "), "no stack trace");
   output.getBuffer().setLength(0); subject.execute("print all");
   check(before.equals(output.toString()), "error preserves every field");
   output.getBuffer().setLength(0); check(subject.execute("help"), "help remains available");
   check(output.toString().contains("create"), "help output after error");
  }
 }
 private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}

 /** Short numbers identify records, never positions in a filtered or shortened list. */
 private static void testShortReferences() throws Exception {
  StringWriter output=new StringWriter();PIMCli subject=new PIMCli(new PrintWriter(output,true));
  Path file=Files.createTempFile("pim-reference-roundtrip-", ".pim");
  try {
   subject.execute("create note first");
   subject.execute("create task report \"2026-11-19 18:00\"");
   subject.execute("create note third");
   String original=call(subject,output,"print 2");
   check(original.contains("[TASK]")&&original.contains("report"),"bare short number resolves task");
   check(call(subject,output,"print #2").equals(original),"explicit short number resolves same record");
   check(call(subject,output,"list").contains("#2  TASK"),"list exposes usable reference");
   check(call(subject,output,"search type = task").contains("Record #2"),"filtered result keeps original number");
   subject.execute("save \""+file+"\"");
   subject.execute("delete 1");subject.execute("create note fourth");
   check(call(subject,output,"print 2").equals(original),"delete does not shift numbers");
   check(call(subject,output,"print #1").startsWith("Error:"),"deleted number remains unavailable");
   check(call(subject,output,"print 4").contains("fourth"),"new records do not reuse deleted numbers");
   subject.execute("load \""+file+"\"");
   check(call(subject,output,"print 1").contains("first"),"reloaded same ID recovers same number");
   check(call(subject,output,"print 2").equals(original),"load retains existing reference identity");
   check(call(subject,output,"print 4").startsWith("Error:"),"load removes absent record without remapping its number");
   subject.execute("create note fifth");
   check(call(subject,output,"print 5").contains("fifth"),"numbers removed by load are not reused");
   subject.execute("update 2 deadline \"2026-11-20 18:00\"");
   String updated=call(subject,output,"print 2");
   check(updated.contains("report")&&updated.contains("2026-11-20T18:00"),"short-number partial update");
   check(call(subject,output,"update #2 deadline \"2026-02-30 18:00\"").startsWith("Error:"),"invalid short-number update rejected");
   check(call(subject,output,"print 2").equals(updated),"invalid update preserves short reference and fields");
   Files.write(file,"invalid".getBytes("UTF-8"));
   check(call(subject,output,"load \""+file+"\"").startsWith("Error:"),"invalid load rejected");
   check(call(subject,output,"print 2").equals(updated),"failed load preserves aliases");
   for(String reference:new String[]{"0","#0","#999","999999999999999999999"})
    check(call(subject,output,"delete "+reference).startsWith("Error:"),"invalid reference fails safely: "+reference);
   check(call(subject,output,"update TASK_ID deadline \"2026-11-20 18:00\"").contains("example placeholder"),"copying placeholder gives actionable error");
   check(call(subject,output,"help").contains("update 2 deadline"),"help uses real task number");
   subject.execute("delete #2");check(call(subject,output,"print 2").startsWith("Error:"),"explicit short-number deletion");
  } finally {Files.deleteIfExists(file);}
 }

 /** Imported numeric IDs cannot silently redirect a destructive short-number command. */
 private static void testNumericIdCollision() throws Exception {
  Path file=Files.createTempFile("pim-numeric-id-", ".pim");
  try {
   model.PIMModel records=new model.PIMModel();
   records.addRecord(new model.Note("alpha","first"));
   records.addRecord(new model.Note("1","numeric stored ID"));
   records.addRecord(new model.Note("123abc","numeric prefix"));
   model.PIMFileCodec.save(records,file);
   StringWriter output=new StringWriter();PIMCli subject=new PIMCli(new PrintWriter(output,true));
   subject.execute("load \""+file+"\"");
   check(call(subject,output,"delete 1").contains("Ambiguous reference"),"collision refuses destructive action");
   check(call(subject,output,"print #1").contains("first"),"hash chooses reference namespace");
   check(call(subject,output,"print id:1").contains("numeric stored ID"),"id prefix chooses exact stored ID");
   check(call(subject,output,"print id:123").contains("numeric prefix"),"numeric stored-ID prefix remains available");
   check(call(subject,output,"print all").contains("3 record(s)"),"collision preserves all records");
  } finally {Files.deleteIfExists(file);}
 }

 /** Presentation mode adds ANSI styling but keeps records, control flow and errors real. */
 private static void testStyledConsole() throws Exception {
  StringWriter output=new StringWriter();PIMCli subject=new PIMCli(new PrintWriter(output,true),true);
  subject.run(new BufferedReader(new StringReader("create note visible\nlist\nprint 1\nupdate 1 text revised\nclear\nprint #1\nupdate 1 text \"\"\nquit\n")));
  String text=output.toString();
  check(text.contains("\u001b[48;2;")&&text.contains("Created note #1."),"styled output enabled");
  check(text.contains("YOUR RECORDS")&&text.contains("#1"),"styled list exposes reference");
  check(text.contains("Text: revised")&&text.contains("ID: "),"styled details retain data and stored ID");
  check(text.contains("Error: ")&&text.contains("Unsaved changes"),"styled failure and save status visible");
  check(text.endsWith(ConsoleView.RESET),"terminal colors restored after exit");
  output.getBuffer().setLength(0);subject.execute("create note \"\u001b[31mcontrol\"");subject.execute("print 2");
  check(!output.toString().contains("\u001b[31m")&&output.toString().contains("\\u001b[31m"),"stored controls cannot inject terminal escapes");
  StringWriter plain=new StringWriter();new PIMCli(new PrintWriter(plain,true)).run(new BufferedReader(new StringReader("quit\n")));
  check(!plain.toString().contains("\u001b["),"plain mode has no ANSI sequences");
 }

 private static String call(PIMCli subject,StringWriter output,String command) {
  output.getBuffer().setLength(0);subject.execute(command);return output.toString();
 }
}
