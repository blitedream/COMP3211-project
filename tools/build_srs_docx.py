"""Rebuild docs/PIM_SRS_Draft.docx with the structure required by Lecture 04.

Run once:

    python tools/build_srs_docx.py

WARNING: running this again overwrites docs/PIM_SRS_Draft.docx, including any
edits made in Word. It is kept in the repository to document how the current
draft was produced; from now on edit the .docx and regenerate the PDF with
tools/build_submission_docs.py.
"""

from pathlib import Path

from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "PIM_SRS_Draft.docx"

USER_STORIES = [
    ("US1", "As a user, I want to create different types of personal information records (PIRs) in the PIM so that all the information I care about can be managed in a single location."),
    ("US2", "As a user, I want to create new plain texts as PIRs so that I can use the PIM to take quick notes."),
    ("US3", "As a user, I want to create new tasks with the corresponding descriptions and deadlines as PIRs so that I can use the PIM to manage my to-dos."),
    ("US4", "As a user, I want to create new events with the corresponding descriptions, starting times, and alarms as PIRs so that I can use the PIM to manage my schedule."),
    ("US5", "As a user, I want to create new contacts with the corresponding names, addresses, and mobile numbers as PIRs so that I can use the PIM to manage my contacts."),
    ("US6", "As a user, I want to modify the data in existing PIRs so that I can keep the PIRs up to date."),
    ("US7", "As a user, I want to search for PIRs based on criteria concerning their types and the data stored in their fields, combining conditions with and (&&), or (||) and negation (!)."),
    ("US8", "As a user, I want to print out detailed information about a specific PIR or all PIRs."),
    ("US9", "As a user, I want to delete a specified PIR."),
    ("US10", "As a user, I want to store the PIRs in a file with the extension name \u201c.pim\u201d so that I can access them using the PIM in the future."),
    ("US11", "As a user, I want to load the PIRs from a file with the extension name \u201c.pim\u201d so that I can continue working with the PIRs I stored earlier."),
]

FUNCTIONAL = [
    ("FR-01", "US1", "The system shall represent notes, tasks, events, and contacts as distinct PIR types and assign each created PIR a unique, stable ID."),
    ("FR-02", "US2", "The user shall be able to create a note containing nonblank plain text. The system shall return its ID and retain its text."),
    ("FR-03", "US3", "The user shall be able to create a task with a nonblank description and a deadline. The system shall retain both fields."),
    ("FR-04", "US4", "The user shall be able to create an event with a nonblank description, starting time, and alarm time. The system shall retain all three fields."),
    ("FR-05", "US5", "The user shall be able to create a contact with a nonblank name, address, and mobile number. The system shall retain all three fields."),
    ("FR-06", "US6", "The user shall be able to update one or more data fields of an existing PIR by ID. The ID and PIR type shall remain unchanged. Invalid updates shall leave the prior PIR intact."),
    ("FR-07", "US7", "The user shall be able to search by PIR type; by substring in note text, descriptions, names, addresses, or mobile numbers; and by deadline, starting time, or alarm time using <, >, or =."),
    ("FR-08", "US7", "Search criteria shall support grouping and combination with &&, ||, and !. The system shall reject malformed criteria with an error rather than returning misleading results."),
    ("FR-09", "US8", "The user shall be able to display every stored field of one PIR by ID, or of all PIRs in insertion order."),
    ("FR-10", "US9", "The user shall be able to delete one PIR by ID. Deleting an unknown ID shall produce an explicit error; a later lookup of that ID shall report that it is absent, and other records shall remain unchanged."),
    ("FR-11", "US10", "The user shall be able to save all PIRs to a file with extension .pim, including each record's ID, type, and all fields, and to overwrite an existing file."),
    ("FR-12", "US11", "The user shall be able to load PIRs from a .pim file, restoring IDs, types, field values, and insertion order. An empty file shall load as an empty model; a missing file, unknown type, missing field, invalid time, or duplicate ID shall produce a clear error without changing records already in memory."),
]

NONFUNCTIONAL = [
    ("NFR-01", "The application shall run from a command line using Java 8 or newer and only Java standard libraries."),
    ("NFR-02", "The model code shall be in a package named model and shall be callable without starting the CLI."),
    ("NFR-03", "Invalid commands, field values, search criteria, and files shall produce readable errors without an uncaught traceback in normal CLI use."),
    ("NFR-04", "Automated unit tests shall exercise the model, and the submitted project shall include a model line-coverage report."),
]

COMMANDS = [
    ("help", "Print the list of commands."),
    ("create note TEXT", "Create a note. TEXT must be nonblank."),
    ("create task DESCRIPTION DEADLINE", "Create a task with a nonblank description and a deadline."),
    ("create event DESCRIPTION START ALARM", "Create an event with a nonblank description, a start time and an alarm time."),
    ("create contact NAME ADDRESS MOBILE", "Create a contact with a nonblank name, address and mobile number."),
    ("update ID FIELD VALUE [FIELD VALUE ...]", "Replace the named fields of one record. The ID and the PIR type do not change; fields that are not named keep their values."),
    ("search CRITERIA", "Print the records that satisfy the criterion expression."),
    ("print ID", "Print every stored field of one record."),
    ("print all", "Print every record in insertion order."),
    ("delete ID", "Delete one record."),
    ("save FILE.pim", "Write all records to a .pim file, replacing an existing file."),
    ("load FILE.pim", "Replace the in-memory collection with the file contents. A failed load leaves the current collection unchanged."),
    ("quit, exit", "Leave the program without saving automatically."),
]


def add_table(doc, rows, widths=None):
    table = doc.add_table(rows=0, cols=len(rows[0]))
    table.style = "Table Grid"
    header = rows[0][0]
    widths = widths or {
        "Version and date": [1.1, 0.7, 4.7],
        "ID": [0.7, 5.8] if len(rows[0]) == 2 else [0.7, 0.7, 5.1],
        "PIR type": [1.3, 5.2],
        "Component": [1.6, 4.9],
        "Command and syntax": [2.0, 4.5],
        "Requirements": [1.3, 1.0, 4.2],
    }.get(header)
    if widths:
        table.autofit = False
        for column, width in zip(table.columns, widths): column.width = Inches(width)
    for index, row in enumerate(rows):
        cells = table.add_row().cells
        if index == 0:
            repeat = OxmlElement("w:tblHeader")
            table.rows[-1]._tr.get_or_add_trPr().append(repeat)
        cant_split = OxmlElement("w:cantSplit")
        table.rows[-1]._tr.get_or_add_trPr().append(cant_split)
        if widths:
            for cell, width in zip(cells, widths): cell.width = Inches(width)
        for cell, value in zip(cells, row):
            cell.text = value
            if index == 0:
                for paragraph in cell.paragraphs:
                    paragraph.paragraph_format.keep_with_next = True
                    for run in paragraph.runs:
                        run.bold = True
    doc.add_paragraph()
    return table


def main():
    doc = Document()
    doc.styles["Normal"].font.size = Pt(10)
    for name in ["Title", "Subtitle", "Heading 1", "Heading 2"]:
        doc.styles[name].font.color.rgb = RGBColor(0, 0, 0)
    for style in doc.styles:
        for border in list(style.element.iter(qn("w:pBdr"))):
            border.getparent().remove(border)
    doc.core_properties.title = "PIM Software Requirements Specification"
    doc.core_properties.version = "1.0"
    doc.core_properties.comments = "Version history and blank-ID rules synchronized on 15 November 2026."

    doc.add_paragraph("Personal Information Management System", style="Title")
    doc.add_paragraph("Software Requirements Specification", style="Subtitle")
    doc.add_paragraph("COMP3211 Software Engineering, Group Project, Fall 2026")

    def h1(text):
        doc.add_paragraph(text, style="Heading 1")

    def h2(text):
        doc.add_paragraph(text, style="Heading 2")

    def para(text):
        doc.add_paragraph(text)

    def bullets(items):
        for item in items:
            doc.add_paragraph("\u2022 " + item)

    h1("Preface")
    h2("Intended readers")
    para("This specification defines the required behavior of the COMP3211 command-line Personal "
         "Information Management system. The instructor and markers assess its scope and quality; "
         "developers implement its interfaces and validation rules; test authors derive expected "
         "outcomes from its numbered requirements. End users should use the User Manual.")
    h2("Document version and scope")
    para("Version 1.0, revised 15 November 2026. Covers US1-US11, FR-01-FR-12 and NFR-01-NFR-04. "
         "Follows Lecture 04; omits System models, System evolution, Appendices and Index as "
         "permitted by the project description. A document version does not certify test completeness.")
    h2("Version history")
    para("Earlier rows identify Git snapshots by commit date. Version labels are assigned here "
         "for traceability; they do not imply that earlier files carried these labels or had formal approval.")
    add_table(doc, [
        ("Version and date", "Source", "Changes and reason"),
        ("0.1\n26 Sep 2026", "e8d9940", "Initial committed SRS snapshot with the record core. Establishes the earliest requirements baseline."),
        ("0.2\n29 Oct 2026", "b033c9e", "SRS snapshot included with the persistence upload. Records an intermediate baseline before consolidation."),
        ("0.3\n15 Nov 2026", "6bf0f7f", "Adds Lecture 04 structure, glossary, user requirements, architecture, commands and ID-prefix rules. Makes interaction and verification expectations explicit."),
        ("1.0\n15 Nov 2026", "This revision", "Adds Preface, readership, history and change rationale; clarifies blank-ID rejection. Aligns the specification with the CLI fix while preserving requirement identifiers."),
    ])
    h2("Changes in version 1 0")
    para("The Preface completes Lecture 04 document-control information. Sections 5.2 and 5.3 now "
         "require print, update and delete to reject empty or whitespace-only IDs without changing "
         "records, even in a one-record collection. This matches CLI fix 8f06151 and its regression "
         "checks, clarifying FR-06, FR-09, FR-10 and NFR-03 without adding a user story or identifier.")
    h2("Related documents and maintenance")
    para("The Design Document explains architecture and APIs; the User Manual explains commands "
         "and search. Requirements_Coverage.pdf maps requirements to evidence; Model_Line_Coverage.pdf "
         "reports model coverage. Maintain docs/PIM_SRS_Draft.docx and docs/final/01_SRS.pdf together. "
         "For future revisions, retain unchanged requirement identifiers and add a dated history entry "
         "explaining the reason and impact on requirements, manuals and tests.")
    doc.add_page_break()

    h1("1 Introduction")
    h2("1.1 Purpose and scope")
    para("The system lets a single user create, modify, search, inspect, delete, save and load "
         "personal information records (PIRs) through a command-line interface. It supports four "
         "PIR types: notes, tasks, events and contacts. A graphical interface, a network service "
         "and multi-user access are outside the scope of the course project.")

    h2("1.2 References")
    bullets([
        "COMP3211 Course Project Description, Fall 2026, Appendix B (user stories).",
        "Lecture 04: Requirements Engineering, including \u201cThe Structure of A Requirements Specification\u201d and \u201cNatural Language Specification\u201d.",
        "ISO/IEC/IEEE 29148-2018 and IEEE Std 830-1998, used as guidance on requirement quality.",
        "Sommerville, I., Software Engineering, 10th edition, Pearson, 2016.",
        "The group design document, which defines the module structure and the .pim file format.",
    ])

    h2("1.3 Glossary")
    bullets([
        "PIR \u2014 personal information record: a note, task, event or contact.",
        "PIM \u2014 personal information management: the system described in this document.",
        "ID \u2014 the stable identifier that the system assigns to a PIR and never changes.",
        "Criterion \u2014 an expression that selects PIRs by type, text content or time comparison.",
        ".pim \u2014 the plain-text file format in which the system stores a collection of PIRs.",
        "CLI \u2014 command-line interface, the only user interface of the system.",
    ])

    h1("2 Overall description")
    h2("2.1 Product perspective")
    para("The system is a standalone console application. All data is kept in memory during a "
         "session and is written to one local file with the extension .pim when the user asks. "
         "The program does not use a database server, a network connection or a graphical "
         "toolkit, and it does not depend on any library outside the Java standard library.")

    h2("2.2 Product functions")
    para("The system manages four kinds of personal information record. Each type stores the "
         "fields listed below; all four types share a stable ID.")
    add_table(doc, [
        ("PIR type", "Required data fields"),
        ("Note", "Text"),
        ("Task", "Description; deadline"),
        ("Event", "Description; starting time; alarm time"),
        ("Contact", "Name; address; mobile number"),
    ])

    h2("2.3 User characteristics")
    para("The intended user is one person who is comfortable typing commands in a terminal and "
         "who knows the file system of the host platform. The user is not expected to know the "
         "internal data structures of the system, and no training beyond the user manual is "
         "assumed.")

    h2("2.4 Constraints and assumptions")
    bullets([
        "The implementation uses Java 8 or newer and only the Java standard library.",
        "Windows is the development and demonstration platform; one platform is documented.",
        "One user works with the collection at a time; no concurrency control is provided.",
        "Date and time values are local and carry no timezone; the system does not convert them.",
        "A record keeps its ID when it is updated, so references to the ID remain valid.",
        "The alarm time of an event is stored data. The system is not a background reminder service.",
        "The collection is saved only when the user issues the save command.",
    ])

    h1("3 User requirements")
    h2("3.1 User stories")
    para("The eleven user stories below are taken from the course project description and define "
         "the user-visible scope of the system.")
    add_table(doc, [("ID", "User story")] + USER_STORIES)

    h1("4 System architecture")
    h2("4.1 Architectural overview")
    para("The system follows an MVC-inspired architecture. PIMCli is the view and controller: it "
         "reads a command, validates its arguments, calls the model and prints the result. The "
         "model package holds the domain: the record types, validation, search and file "
         "persistence. PIRSearch parses and evaluates criteria, and PIMFileCodec serializes the "
         "collection. The view depends on the model, but the model does not depend on the view, "
         "so the domain can be unit tested without a terminal. A dedicated view class was not "
         "introduced because the system has a single, small output surface.")
    add_table(doc, [
        ("Component", "Responsibility"),
        ("PIR and its subclasses", "Immutable record types and their fields."),
        ("PIMModel", "The ordered collection, ID assignment and typed updates."),
        ("PIRSearch", "Parsing and evaluation of search criteria."),
        ("PIMFileCodec", "Reading and writing .pim files."),
        ("PIMCli", "Command parsing, error reporting and formatted output."),
    ])

    h1("5 System requirements")
    h2("5.1 Functional requirements")
    add_table(doc, [("ID", "Story", "Verifiable requirement")] + FUNCTIONAL)

    h2("5.2 External interface: commands")
    para("Commands are entered one per line at the pim> prompt. Command words, type names and "
         "field names are case insensitive. Arguments that contain spaces are enclosed in double "
         "quotes; two double quotes inside a quoted argument represent one literal quote. An ID "
         "may be given in full or as any unique nonblank prefix; an empty, whitespace-only or ambiguous prefix is rejected. Date "
         "and time values use exactly yyyy-MM-dd HH:mm and are interpreted as local time. An "
         "unknown command, a wrong number of arguments, an invalid field or an invalid value "
         "prints an error and leaves the collection unchanged.")
    add_table(doc, [("Command and syntax", "Behavior")] + COMMANDS)

    h2("5.3 Data and validation rules")
    bullets([
        "Required text fields reject empty or whitespace-only values.",
        "Date and time values use yyyy-MM-dd HH:mm; an invalid calendar date such as 30 February is rejected.",
        "For print, update and delete, an empty or whitespace-only ID is rejected without changing the collection, even if only one record exists.",
        "An unknown ID produces an explicit error. No record is silently created by update, print or delete.",
        "An ambiguous ID prefix produces an error that reports how many records match.",
        "A .pim file stores one record per block, with a blank line between blocks and one key=value pair per line.",
        "The keys type and id are present in every block; the remaining keys are the fields of the record type.",
        "Free-text values in a .pim file are Base64-encoded so that newlines, Unicode and = survive a round trip.",
        "Loading a .pim file rejects an unknown type, a missing field, a duplicate field, an invalid time and a duplicate ID, and leaves the current collection unchanged.",
        "An empty .pim file loads as an empty collection.",
    ])

    h2("5.4 Nonfunctional requirements")
    add_table(doc, [("ID", "Requirement")] + NONFUNCTIONAL)

    h1("6 Verification and traceability")
    para("Every functional requirement is exercised by an automated test. The model tests run "
         "without the CLI and cover the record types, validation, updates, search and file "
         "handling. The CLI integration checks drive the same entry point that a user types "
         "into, so the command syntax and the error behavior are tested as well. The mapping "
         "below records where each group of requirements is verified.")
    add_table(doc, [
        ("Requirements", "Evidence", "Verification"),
        ("FR-01 to FR-06", "US1 to US6", "ModelCoreTest, CliTest"),
        ("FR-07, FR-08", "US7", "SearchTest, CliTest"),
        ("FR-09, FR-10", "US8, US9", "PersistenceTest, CliTest"),
        ("FR-11, FR-12", "US10, US11", "PersistenceTest, CliTest"),
        ("NFR-01 to NFR-04", "Whole system", "run_d_tests.ps1, run_coverage.ps1, coverage report"),
    ])

    # Keep the Preface together on a readable first page.
    for sec in doc.sections:
        sec.left_margin = Inches(1)
        sec.right_margin = Inches(1)
    in_preface = False
    for para_item in doc.paragraphs:
        if para_item.text == "Preface": in_preface = True
        if para_item.text == "1 Introduction": in_preface = False
        if in_preface:
            para_item.paragraph_format.space_before = Pt(5 if para_item.style.name.startswith("Heading") else 0)
            para_item.paragraph_format.space_after = Pt(4)
            if not para_item.style.name.startswith("Heading"):
                para_item.paragraph_format.line_spacing = 1.0
                for run in para_item.runs: run.font.size = Pt(9.5)
    for row in doc.tables[0].rows:
        for cell in row.cells:
            for para_item in cell.paragraphs:
                para_item.paragraph_format.space_after = Pt(2)
                para_item.paragraph_format.line_spacing = 1.0
                for run in para_item.runs: run.font.size = Pt(9)
    doc.save(OUT)
    print("Wrote", OUT)


if __name__ == "__main__":
    main()
