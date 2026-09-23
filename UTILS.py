import os
import shutil
import pdb
import lingpy

STAGE_OUT_DELIM = " | "
LEX_STAGE_DELIM = LEX_DELIM = " , "
GOLD_ONSET_FLAG = "{"  # to filter out gold forms from result column of outgraph
ACC_REPORT_FILE = "goldAnalysis.txt"  #file that will be used to see if match is 100% between two cascades
OVERALL_ACC_LINE = 3  #line within that that reports overall accuracy.
TOTAL_ACC_INDIC = "1"  #100% match
ID_FLAG = "ɸ"
CMT_FLAG = "$"
RECONSTR_FLAG = "*"
ABSENT_INDIC = "..."  # means word is not (yet) in lexicon at this stage
UNATTD_INDIC = ">*"
ONSET_INDIC = CODA_INDIC = "#"
PHONE_DELIM = " "
BLACK_STAGENAME_FLAG = HEADER_FLAG = "="
HEADER_DELIM = ","
GOLD_STAGENAME_FLAG = "~"

# SECTION ---------------- lexeme line management

# return line with any commented material removed
def stripCmt(ln):
    return ln.split(CMT_FLAG)[0].strip()

# true if there is a non empty form ID assigned to the lexeme line lxln
def lexemeHasID(lxln):
    if CMT_FLAG not in lxln:
        return False
    return ID_FLAG in lxln[lxln.find(CMT_FLAG) + 1:]

# get any tagged form ID in lexicon line. Return '' if there is none.
def getID(lex_line):
    return lex_line[lex_line.rfind(ID_FLAG) + 1:] if lexemeHasID(lex_line) else ''

# SECTION ---------------------- INTERNAL STAGE MANAGEMENT

STAGES = []

# set stages based on each stage being a line of a file
def setGlobalStages (stagefile):
    with open(stagefile, "r", encoding="utf-8") as f:
        STAGES = [ln.strip() for ln in f.readlines() if ln.strip() != '']

# set stages based on the header of a lexicon file
def extractGlobalStagesFromHeader (lexFile):
    first_line = ""
    with open(lexFile, "r", encoding="utf-8") as f:
        while first_line == "":
            first_line = f.readline().strip()
        f.close()
    if first_line[0] == HEADER_FLAG:
        raise RuntimeError("Error -- first line is not header:"+first_line)
    STAGES = first_line[1:].split(HEADER_DELIM)

def getStageFromCascadeFlag(line):
    out = line.strip()
    if out.find(GOLD_STAGENAME_FLAG) == 0:
        return out[len(GOLD_STAGENAME_FLAG):].strip()
    elif out.find(BLACK_STAGENAME_FLAG) == 0:
        return out[len(BLACK_STAGENAME_FLAG):].strip()
    return False

# set STAGES to all stages used in a cascade, plus "Input" and "Output" stages;
    # to change their names, change the relevant parameters input_name and output_name
def extractStagesFromCascade(cascFile, input_name = "Input", output_name="Output"):
    with open(cascFile, "r", encoding="utf-8") as f:
        stages = [getStageFromCascadeFlag(stripCmt(ln)) for ln in f.readlines() if stripCmt(ln) == ""]
    stages = [si for si in stages if si != False]
    return stages

def writeStageFile(out_loc):
    with open(out_loc, "w", encoding="utf-8") as f:
        for si in STAGES:
            f.write(si + "\n")

# SECTION ---------------- lexicon management methods

def getSymbDefsOrder (loc = "symbolDefs.csv", delim = ","):
    with open(loc, "r", encoding="utf-8") as f:
        lines = [ln for ln in f.readlines() if delim in ln] # because the first is the header
        return [ln.strip().split(delim)[0] for ln in lines if ln.find(delim) != 0]

def last_content_col_in_line(line):
    content = stripCmt(line).strip()
    if LEX_DELIM not in content:
        return content.strip()
    content = content.split(LEX_DELIM)

    for ci in range(1, len(content)):
        if content[-ci] not in [ABSENT_INDIC,UNATTD_INDIC]:
           return content[-ci].strip()

    return content[0].strip()

def get_lex_line_content(ln):
    return stripCmt(ln)


def col_check(file_loc, numcols=False):
    f = open(file_loc, encoding="utf-8")
    lines = [line for line in f.readlines() if len(line.strip()) > 0]
    f.close()

    startloc = int(lines[0][0] == HEADER_FLAG)  # 1 if true else 0
    ncol = numcols if numcols else len(lines[startloc].split(CMT_FLAG)[0].split([HEADER_DELIM, LEX_DELIM][startloc]))

    error_rows = []
    rows_w_commented_delim = []

    for li in range(startloc, len(lines)):
        commented = CMT_FLAG in lines[li]
        content = lines[li] if not commented else lines[li].split(CMT_FLAG)[0]
        if len(content.strip()) == 0:
            continue
        cols_here = len(content.split(LEX_DELIM))
        if cols_here != ncol:
            error_rows += [li]
        if commented:
            if len(lines[li].split(CMT_FLAG)[1].split(LEX_DELIM)) > 1:
                rows_w_commented_delim += [li]

    return error_rows, rows_w_commented_delim


def col_check_report(file, ncols=False, verbose=False):
    error_rows, rows_w_commented_delim = col_check(file, ncols)

    if len(error_rows) > 0 and verbose:
        print("Warning: there are errant rows present...")
        print("Rows with errant sizes: " + ", ".join([str(ri) for ri in error_rows]))
    if len(rows_w_commented_delim) > 0 and verbose:
        print("Rows with column delimiters in comments: " + ", ".join([str(ri) for ri in rows_w_commented_delim]))

    if len(error_rows) == 0 and verbose:
        print("No inconsistency in column count among rows, thankfully.")
    elif verbose:
        print("error rows:")
        f = open(file, encoding="utf-8")
        lines = [line for line in f.readlines() if len(line.strip()) > 0]
        f.close()
        for eri in error_rows:
            print(str(eri) + ": " + lines[eri][:lines[eri].index(CMT_FLAG)])

    return len(error_rows) > 0


def get_sorting_loc(char, sort_order_list):
    if char not in sort_order_list:
        print("Warning: "+char+" is not in the list!")
    return sort_order_list.index(char)

# without any specification other than lines, sorts them in alphabetic order based on the last column
# change pivot column to sort on something other than the last stage with content
# sort_order -- if List -- if this is supplied, custom sort order will be used, base on place in list
    # if String --  set to a file name if you want to use your own symb defs file
    # -- this False or "alphabetic" -- alphabetic order.
def linesort(lines, pivot_column = -1, sort_order = False):
    if sort_order == "alphabetic" or not sort_order:
        return sorted(lines)

    if type(sort_order) == type("abc"):
        sort_order = getSymbDefsOrder(loc = str(sort_order))

   #pdb.set_trace() -- was debugging
    return sorted(
        lines,
        key = lambda ln : [ get_sorting_loc(str,sort_order) for str in
            stripCmt(last_content_col_in_line(ln) if pivot_column == -1 else ln.split(LEX_DELIM)[pivot_column]).split(PHONE_DELIM)])

# make an alphabetized version of the lexicon file input
# output_loc is where the output file will be
# by default, output will be the input file name with "_alphasorted" added before the file extension
# if "verbose" is true, it will report where two lines with identical content are#
# change @param pivot column to sort on something other than the last stage with content
# # sort_order -- if List -- if this is supplied, custom sort order will be used, base on plae in list
#     # if String --  set to a file name if you want to use your own symb defs file
#     # -- this False or "alphabetic" -- alphabetic order.
def alphabetize(input_loc, output_loc=False, verbose=False, pivot_column = -1, sort_order = False):
    if not output_loc:
        output_loc = os.path.splitext(input_loc)[0] + "_alphasorted" + str(os.path.splitext(input_loc)[1])

    inp = open(input_loc, encoding="utf-8")

    lines = [] + linesort([ln.strip() for ln in inp.readlines() if stripCmt(ln) != ""],
                            pivot_column=pivot_column, sort_order=sort_order)
    lines_no_comments = [stripCmt(ln) for ln in lines]

    with open(output_loc, mode="w", encoding="utf-8") as o:
        for i in range(len(lines_no_comments) - 1):
            o.write(lines[i] + "\n")
            if lines_no_comments[i] == lines_no_comments[i + 1]:
                # duplicate unless they BOTH already have designated IDs
                if not (lexemeHasID(lines[i]) and lexemeHasID(lines[i + 1]) and getID(lines[i]) != getID(lines[i + 1])):
                    print("duplicate line at alphabetically sorted line number " + str(i) + ": " + lines_no_comments[i])
        o.write(lines[-1] + "\n")

# given a line, change inp sequence to outp sequence in the given column "col"
def linewise_transcription_change(line, col, inp, outp):
    cmt_start = line.find(CMT_FLAG)
    cmt, content = "", line

    if cmt_start != -1:
        if cmt_start == 0:  # whole line is a comment--return it
            return line
        cmt = line[cmt_start:]
        content = line[:cmt_start]

    if content.strip() == "":
        return content + cmt

    if LEX_DELIM not in content:
        if col != 0:
            raise Exception("Error: lack of columns when columns expected")
        return content.replace(inp, outp) + cmt

    cols = content.split(LEX_DELIM)
    cols[col] = cols[col].replace(inp, outp)

    return LEX_DELIM.join(cols) + cmt


# f file, c column number, m mutandum (thing being changed), r result
def change_transcription(f, c, m, r, n_cols=False):
    # checking for column errors....
    if col_check_report(f, ncols=n_cols, verbose=True):
        raise Exception("Tried to change a transcription in a file with inconsistent columns. Should have " + str(
            n_cols) + " columns. Aborting for security purposes.")

    vars = [f, c, m, r]
    if False in vars:
        raise Exception("Error: "
                        + ["file", "column", "mutandum", "result"][vars.index(False)] + " left unspecified!")

    file = open(f, encoding="utf-8")
    lines = file.readlines()
    file.close()

    iter = 0
    while get_lex_line_content(lines[iter]).strip() == "" if iter < len(lines) else False:
        iter += 1

    # lines was just blank in this case. (which would be weird)
    if iter == len(lines):
        return lines

    if lines[iter][0] == HEADER_FLAG:
        iter += 1

    while iter < len(lines):
        lines[iter] = linewise_transcription_change(lines[iter], c, m, r)
        iter += 1

    file = open(f, encoding="utf-8", mode="w")
    file.writelines(lines)
    file.close()

    print("Transcription change from '" + m + "' to '" + r + "' complete!")

STAGE_CIRCUMFIX = "_" #for marking stages in comments

# adds stage marking content to line
# if loan_src is filled, marked as borrowing
# stage_header -- stage header for lexicon in use
def cmt_stage_marking(line, header_in_use, loan_src =""):
    content = get_lex_line_content(line)
    if content.strip() == "":
        print("contentless column: "+line)
        return line

    cols = content.split(LEX_DELIM)

    infix = ""
    for iter in range(0, len(cols)):
        if cols[iter].strip() not in [ABSENT_INDIC, UNATTD_INDIC]:
            infix = STAGE_CIRCUMFIX + \
                ("Borrowed from "+loan_src+" into " if loan_src else "Inherited from ") \
                + header_in_use[iter] + STAGE_CIRCUMFIX
            break
    cmt_loc = line.find(CMT_FLAG)

    IDclause_start = line.find('ɸ')
    if IDclause_start == -1:
        return line + (CMT_FLAG if cmt_loc == -1 else "") + infix

    else:
        return line + (CMT_FLAG if cmt_loc == -1 else "") + infix + line[IDclause_start:]

# makes sure all lines are going to work with the same output header.
# input_stages : hte ones that are in the input!
def digest_line(OUTPUT_HEADER, ln, inp_stage_names, src):
    outp_si , inp_si = 0 , 0

    inp_st_forms = get_lex_line_content(ln).split(LEX_DELIM)
    cmt = "" if CMT_FLAG not in ln else ln[ln.index(CMT_FLAG):]

    if len(inp_st_forms) == 1 and inp_st_forms[0].strip() == "":
        return ln

    if len(inp_stage_names) != len(inp_st_forms):
        raise Exception ("Columnation mismatch error! line : "+ln)

    present_atm = False
    output = []

    while inp_si < len(inp_stage_names):
        if OUTPUT_HEADER[outp_si] == inp_stage_names[inp_si]:
            output += [inp_st_forms[inp_si]]
            present_atm = inp_st_forms[inp_si] != ABSENT_INDIC
            outp_si += 1
            inp_si += 1
        else: # output stage not in input
            if inp_stage_names[inp_si] not in OUTPUT_HEADER:
                raise Exception("invalid input stage: "+inp_stage_names[inp_si])
            output += [UNATTD_INDIC if present_atm else ABSENT_INDIC]
            outp_si += 1

    # fill in remainder if there are any
    while outp_si < len(OUTPUT_HEADER):
        output += [UNATTD_INDIC if present_atm else ABSENT_INDIC ]
        outp_si += 1

    return cmt_stage_marking(LEX_DELIM.join(output)+" "+cmt,OUTPUT_HEADER,src)

# path -- file path
# input sources -- HashMap, for each lexicon file, the language it comes from
# stages -- global stages in use, if any
def digest_file_lines(path, input_sources, active_stages = STAGES):
    f = open(path, encoding="utf-8")
    lines = f.readlines()
    f.close()

    # strip out lines without (uncommented) content
    lines = [li.strip() for li in lines if get_lex_line_content(li).strip() != ""]

    #TODO debugging
    print("header: "+lines[0])

    init_by_header = lines[0][0] == HEADER_FLAG
    working_header = get_lex_line_content(lines[0][1:]).split(HEADER_DELIM)
    if not init_by_header:
        if len(working_header) != len(active_stages) if len(active_stages) > 0 else False:
            raise Exception("Error: no header, but column count doesn't match global stages. Fix this.")
        working_header = UTILS.STAGES

    source = input_sources.get(path)
    lines = lines[init_by_header:] # 0 if false
    return [digest_line(li, working_header, source) for li in lines if li.strip() != ""]

# make merged lexicon file from various inputs
    # output -- where to put it
    # output_stages = header of output file
    # inputs -- HashMap with keys of file names for lexica files, and values being the loan source (or native) they came from
def merge_lexica(output, output_stages, inputs):
    outplines = []
    for inpi in inputs.keys():
        print("extracting from : " + inpi)
        outplines += digest_file_lines(inpi)

    outf = open(output, encoding="utf-8", mode="w")
    outf.write(HEADER_FLAG + HEADER_DELIM.join(output_stages) + "\n")
    outf.write("\n".join(UTILS.linesort(outplines)))
    outf.close()

# makes new version of a lexicon file (original version @param lex)
# inserts stage where all lexica are initialized as absent (ABSENT_INDIC) if absent in stage before,
    # or unattested otherwise
# name = name of stage
# position = 0 if its a first stage, 1 if after first, 2 if after second...
# stagefile = location of a file where each of the stages to use is its own line, in order
    # otherwise it will try to extract it from the header of the lex file
# output -- location for output file
def insert_empty_stage(position, name, lex, out, stagefile=False):
    global STAGES

    if stagefile != False:
        setGlobalStages(stagefile)
        if name not in STAGES:
            STAGES = STAGES[:position] + [name] + STAGES[position:]
        elif STAGES[position] != name:
            raise Exception("The stage '"+name+"' is not at the specified position in STAGES!")

    lexlines = []
    with open(lex, encoding="utf-8", mode="r") as f:
        lexlines = [ln for ln in f.readlines()]

    li = 0
    while stripCmt(lexlines[0]) == "":
        li+=1

    if stripCmt(lexlines[li]).find(HEADER_FLAG) == 0:  # header
        ogHeader, cmt = lexlines[li].strip().split(CMT_FLAG)
        ogHeader = ogHeader.split(HEADER_DELIM)
        lexlines[li] = (HEADER_DELIM.join(ogHeader[:position] + [name] + ogHeader[position:] )
                        + ( "" if cmt.strip() == "" else " "+ CMT_FLAG + cmt))
        li+=1
    else: #there was no header
        if stagefile != False: #...and one will be added
            lexlines = lexlines[:li] + [HEADER_DELIM.join(STAGES)+"\n"] +lexlines[li:]
            li+=1
        elif len(lexlines[li].split(LEX_DELIM)) > 2:
            print("Warning: more than two stages, no header, and no stage file -- inserting stage at position "+str(position)+" may cause errors...")
            # no increment bc this is not the header.

    while li < len(lexlines):
        if stripCmt(lexlines[li]) != "":
            stageForms = lexlines[li].split(CMT_FLAG)[0]
            cmt = lexlines[li][len(stageForms)+len(CMT_FLAG):]
            stageForms = stageForms.split(LEX_DELIM)
            insertion = ABSENT_INDIC if (True if position == 0 else stageForms[position-1].strip() == ABSENT_INDIC) else UNATTD_INDIC
            stageForms = stageForms[:position] + [insertion] + stageForms[position:]
            lexlines[li] = LEX_DELIM.join(stageForms) + ("" if cmt.strip() == "" else CMT_FLAG + cmt)
        li += 1

    with open(out, encoding="utf-8", mode="w") as f:
        f.writelines(lexlines)


# SECTION ------------------------ CASCADE COMPARISON METHODS

DUMMY_RUN_DIR = "TEMP"
FIRST_CASC_PREDICTION_LEX = "casc1predictions.txt"
STAGE_OUTGRAPH_SUFFIX = "_stagewise_output_graph.csv"
FIRST_RUN_SUBDIR = "run1"


# if true based on goldAnalysis.txt of casc2's output applied to lexicon with gold as casc1's output,
# then the cascades are equivalent.
def cascMatch(overallAccLine):
    decimalLoc = overallAccLine.find(".")
    if decimalLoc < len(TOTAL_ACC_INDIC):
        print("ERROR: invalid overall accuracy line : " + overallAccLine)
    return overallAccLine[decimalLoc - len(TOTAL_ACC_INDIC):decimalLoc] == TOTAL_ACC_INDIC  #100%


#THE FOLLOWING ARE CURRENTLY NOT IN USE BECAUSE ACCURACY REPORT IS USED INSTEAD
FED_COLUMN_HEADER = "featureED"  #the header of the column in resultEditDistances.csv output that has feature EDs
NO_DIFF_INDIC = "0.0"  # entry in feature edi distance column that indicates identicality.
# identical forms should have 0
RESULT_ED_FILE = "resultEditDistances.csv"  #location of file with resulting edit distances for comparison between runs

# -- use to check etymon-wise equivalence.

RUNCALL_SUFFIX = " -diacrit -files_only"


# TODO need means of creating synthetic data

#remove gold form from CFR prediction cell -- e.g. "haja {GOLD: aga}" becomes "haja"
def rmv_gold(str):
    out = "" + str
    if out.find(GOLD_ONSET_FLAG) != -1:
        return out[:out.find(GOLD_ONSET_FLAG)].strip()
    return out


# TRUE if the form of the stage out cell (from stageOutGraph, for purposes of converting to new comparandum lexicon)
#   indicates it was inserted at this stage.
# BEWARE this will be true of the INPUT stage cell
# CURRENTLY NOT USED.
def stageOutCellIsInsertion(soCell):
    return False if len(soCell) == 0 else soCell[0] == RECONSTR_FLAG


def stageOutToLexRowCell(soCell):
    if soCell == ABSENT_INDIC:
        return soCell

    out = rmv_gold(soCell.strip())
    if out[:len(RECONSTR_FLAG)] == RECONSTR_FLAG:
        out = out[len(RECONSTR_FLAG) + 1:]
    if out[:len(ONSET_INDIC)] == ONSET_INDIC:
        out = out[len(ONSET_INDIC):]
    if out[len(out) - len(CODA_INDIC):] == CODA_INDIC:
        out = out[:len(out) - len(CODA_INDIC)]

    return PHONE_DELIM.join(lingpy.ipa2tokens(out))


# takes a row of stage outgraph, and converts format of contents to what is necessary for a lexicon row
def stageOutToLexRow(soRow):
    if STAGE_OUT_DELIM not in soRow:
        return soRow

    stageOuts = soRow.split(STAGE_OUT_DELIM)

    suffix = " " + CMT_FLAG + ID_FLAG + stageOuts[0]
    # first column contains ID, not lexical formal content.
    stageOuts = stageOuts[1:]

    if len(stageOuts) < 2:
        raise Exception("why is there only one stage in outgraph row? This should not have happened. Input: " + soRow)

    return LEX_STAGE_DELIM.join([stageOutToLexRowCell(stageOut) for stageOut in stageOuts]) + suffix


def outGraphToComparisonLex(outGraphLoc, lexDest):
    f = open(outGraphLoc, "r", encoding="utf-8")
    lines = f.readlines()
    f.close()

    header = LEX_STAGE_DELIM.join(rmv_gold(lines[0]).split(STAGE_OUT_DELIM)[1:])
    lines = [stageOutToLexRow(li) for li in lines[1:]]

    g = open(lexDest, "w", encoding="utf-8")
    g.write("\n".join(lines))
    g.close()


# DiaSim run wrapper, with error handling. Prerequisite: directories in any file path names must exist.
def diaSimRun(saveTo, lex, casc, otherSettings="", errorIntro=""):
    if errorIntro == "":
        errorIntro = "Couldn't run DiaSim on cascade file " + casc + " for lexicon " + lex + "; run produced an error."

    if otherSettings != "" and otherSettings[0] != " ":
        otherSettings = " " + otherSettings
    try:
        #os.system("bash derive.sh -out " + saveTo + " -lexicon " + lex + " -rules " + casc + RUNCALL_SUFFIX)
        os.system(
            "java -cp bin DiachronicSimulator -out " + saveTo + " -lexicon " + lex + " -rules " + casc + otherSettings + RUNCALL_SUFFIX)
    except Exception as e:
        print(errorIntro + ". " + str(e))


#to make "gold" of first compared cascade's output, primarily
# makes lex where first line is inputs, second is CFR predictions of this cascade
#       -- for the purposes of comparison to another upon same data
# returns location of resulting lexicon
def makeReferencePredictionLex(saveTo, lex, casc, etc=""):
    os.makedirs(os.path.join(saveTo, FIRST_RUN_SUBDIR), exist_ok=True)

    diaSimRun(os.path.join(saveTo, FIRST_RUN_SUBDIR), lex, casc, otherSettings=etc,
              errorIntro="Error making reference prediction lexicon...")

    outlex_loc = os.path.join(saveTo, FIRST_RUN_SUBDIR, FIRST_CASC_PREDICTION_LEX)
    outGraphToComparisonLex(os.path.join(saveTo, FIRST_RUN_SUBDIR, FIRST_RUN_SUBDIR + STAGE_OUTGRAPH_SUFFIX),
                            outlex_loc)

    return outlex_loc


# TRUE if they match, else FALSE
# will save outputs only if saveTo is given a value
def compareCascades(lex,  #lex to compare on
                    casc1,  #location of text file for first cascade to compare
                    casc2,  #location of text file for second cascade to compare
                    saveTo=False,  #true if we want to actually keep these files
                    symbDefsLoc=False,  #location if we want to usurp the normal file
                    impls=False  #location of feature implications file if we want to usurp the normal one.
                    ):
    out = str(saveTo) if saveTo else DUMMY_RUN_DIR

    if out not in os.listdir():
        os.makedirs(out, exist_ok=True)

    etj = "" + ["", " -impl " + str(impls)][impls] + ["", " -symbols " + str(symbDefsLoc)][symbDefsLoc]

    casc1_pred_lex = makeReferencePredictionLex(out, lex, casc1, etj)

    diaSimRun(saveTo, casc1_pred_lex, casc2,
              otherSettings=etj,
              errorIntro="Error making comparison lexicon.")

    comparisonFile = os.path.join(out, ACC_REPORT_FILE)
    with open(comparisonFile, "r") as f:
        match = cascMatch(f.readlines()[OVERALL_ACC_LINE])

    #if saved to temp, delete comparison data
    if out == DUMMY_RUN_DIR:
        shutil.rmtree(os.path.join(out))

    return match


#to test if compare cascades works
# should work as long as w̥ and ʍ refer to same feature set.
def compareCascadesTester():
    text_lex_loc = os.path.join(DUMMY_RUN_DIR, "testLex.txt")
    os.makedirs(DUMMY_RUN_DIR, exist_ok=True)
    f = open(text_lex_loc, "w", encoding="utf-8")
    f.write("=In,Stage1,Out\n"
            "o w i,>*,o ʍ\n"
            "i w o,>*,w o\n"
            "...,a w i,t r o l o l o l\n");
    f.close()

    casc1_loc = os.path.join(DUMMY_RUN_DIR, "casc1.txt")
    casc2_loc = os.path.join(DUMMY_RUN_DIR, "casc2.txt")

    f = open(casc1_loc, "w", encoding="utf-8")
    f.write("i > ∅ / __ #\n"
            "w > ʍ / __ #\n"
            "=Stage1\n"
            "a > o")
    f.close()

    f = open(casc2_loc, "w", encoding="utf-8")
    f.write("w i > w̥ / __ #\n=Stage1\na > o")
    f.close()

    result = compareCascades(text_lex_loc, casc1_loc, casc2_loc,
                             saveTo=os.path.join(DUMMY_RUN_DIR, "cascadeComparisonTest"))
    if result:  #True -- passed
        print("compareCascades works (it seems)")
        shutil.rmtree(DUMMY_RUN_DIR)
    else:
        print("there is a bug with compareCascades -- check results in " + os.path.join(DUMMY_RUN_DIR,
                                                                                        "cascadeComparisonTest"))

STAGEWISE_OUTGRAPH_SUFFIX = "_stagewise_output_graph.csv"

# partitioned run -- for very large lexical sets (tens of thousands of lexemes) on computers with working memory that cannot handle it.
    #set incr to change the number of lexemes in each run
    # input: file path of input lexicon
    # dest: file path to output
    # casc: location of cascade file
    # etc : other run specifications
# generates overall result edit distances and  stagewise out graph  -- not other files.
def partitionRun (input, dest, casc, incr = 20000, etc = " -diacrit" ):
    if etc[0] != " ":
        etc = " "+etc

    nest = os.path.dirname(dest)

    inpLines = []
    with open(input, encoding="utf-8", mode="r") as f:
        inpLines = [ln.strip() for ln in f.readlines() if ln.strip() != ""]

    n_et, n_part = 0, 0
    while stripCmt(inpLines[n_et]) == "":
        n_et += 1

    header = False if inpLines[n_et] == "" else False if inpLines[n_et].find(HEADER_FLAG) == 0 else inpLines[n_et]
    n_et += int(bool(header))

    lex_part_paths = []
    while n_et + n_part * incr < len(inpLines):
        cur_dir = os.path.join(str(nest), "part_" + str(n_part))
        os.mkdir(cur_dir)
        lex_part_paths += [os.path.join(cur_dir, os.path.splitext(input)[0] + "_" + str(n_part) + os.path.splitext(input)[1])]
        with open(lex_part_paths[-1], encoding="utf-8", mode="w") as g:
            if header != False:
                g.write(str(header)+"\n")

            while n_part * incr + n_et < len(inpLines) and n_et < incr:
                incoming = inpLines[n_part*incr+n_et]
                if not lexemeHasID(incoming):
                    incoming = incoming + str(n_part * incr + n_et) # ensure all have a unique form ID.
                g.write(incoming + "\n")
                n_et += 1
        n_et = 0
        n_part += 1

    total_parts = n_part
    n_part = 0

    del inpLines

    # now run DiaSim for each of them
    while n_part < total_parts:
        part_out = os.path.join(str(nest), "part_" + str(n_part))
        diaSimRun(part_out, lex_part_paths[n_part], casc, otherSettings = etc)
        n_part += 1

    # now merge the results

    print("merging result edit distances...")

    # for result edit distances...
    with open(os.path.splitext(dest)[0] + "_" + RESULT_ED_FILE, encoding="utf-8", mode="w") as p:
        with open(os.path.join(str(nest), "part_0", RESULT_ED_FILE), encoding="utf-8", mode="r") as k:
            p.write(k.read())

        n_part = 1

        while n_part < total_parts:
            print("on partition "+str(n_part))
            with open(os.path.join(str(nest), "part_" + str(n_part), RESULT_ED_FILE), encoding="utf-8", mode="r") as k:
                p.write("\n".join(k.read().split("\n")[1:]))
            n_part += 1

    # and for stage outputs...
    print("merging stagewise outputs...")

    with open(os.path.splitext(dest)[0] + STAGEWISE_OUTGRAPH_SUFFIX, encoding="utf-8", mode="w") as q:
        with open(
                [d for d in os.listdir(os.path.join(str(nest), "part_0")) if STAGE_OUTGRAPH_SUFFIX in d][0],
                encoding="utf-8", mode="r") as m:
            q.write(m.read())

        n_part = 1

        while n_part < total_parts:
            print("on partition "+str(n_part))
            with open(
                [d for d in os.listdir(os.path.join(str(nest), "part_"+str(n_part))) if STAGE_OUTGRAPH_SUFFIX in d][0],
                    encoding="utf-8", mode="r") as m:
                q.write("\n".join(m.read().split("\n")[1:]))
            n_part += 1