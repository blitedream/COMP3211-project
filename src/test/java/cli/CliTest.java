package cli;
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
/** Integration checks for command parsing, model routing and failure recovery. */
public final class CliTest {
 public static void main(String[] args) throws Exception {
  testBlankIds();
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
 private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
