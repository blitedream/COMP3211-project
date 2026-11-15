package cli;
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
/** Integration checks for command parsing, model routing and failure recovery. */
public final class CliTest {
 public static void main(String[] args) throws Exception {
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
   check(!cli.execute("quit"),"exit");
   cli.run(new BufferedReader(new StringReader("help\n")));
  } finally {Files.deleteIfExists(file);}
  System.out.println("CLI integration checks passed");
 }
 private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
