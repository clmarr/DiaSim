import os
import shutil
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
HEADER_FLAG = "="
HEADER_DELIM = ","


# TODO ---------------- lexicon management methods

# true if there is a non empty form ID assigned to the lexeme line lxln
def lexemeHasID(lxln):
    if CMT_FLAG not in lxln:
        return False
    cmt = lxln[lxln.find(CMT_FLAG) + 1:]
    return ID_FLAG in cmt


# get any tagged form ID in lexicon line. Return '' if there is none.
def getID(lex_line):
    if not lexemeHasID(lex_line):
        return ''
    return lex_line[lex_line.rfind(ID_FLAG) + 1:]


# make an alphabetized version of the lexicon file input
# output_loc is where the output file will be
# by default, output will be the input file name with "_alphasorted" added before the file extension
# if "verbose" is true, it will report where two lines with identical content are
def alphabetize(input_loc, output_loc=False, verbose=False):
    if not output_loc:
        output_loc = os.path.splitext(input_loc)[0] + "_alphasorted" + str(os.path.splitext(input_loc)[1])

    inp = open(input_loc, encoding="utf-8")

    lines_with_comments = [ln.strip() for ln in inp.readlines()]
    lines = [ln.split(CMT_FLAG)[0].strip() for ln in lines_with_comments]
    lines = sorted(lines)

    lines_with_comments = sorted([ln for ln in lines_with_comments if ln != '' and ln[0] != CMT_FLAG])

    with open(output_loc, mode="w", encoding="utf-8") as o:
        for i in range(len(lines) - 1):
            o.write(lines_with_comments[i] + "\n")
            if lines[i] == lines[i + 1]:
                # duplicate unless they BOTH already have designated IDs
                if not (lexemeHasID(lines[i]) and lexemeHasID(lines[i + 1]) and getID(lines[i]) != getID(lines[i + 1])):
                    print("duplicate line at alphabetically sorted line number " + str(i) + ": " + lines[i])
        o.write(lines_with_comments[-1] + "\n")


def get_lex_line_content(ln):
    if CMT_FLAG in ln:
        ln = ln[:ln.index(CMT_FLAG)]
    return ln


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

# input sources -- HashMap, for each lexicon file, the language it comes from
# stages -- global stages in use, if any
def digest_file_lines(path,input_sources,STAGES = []):
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
        if len(working_header) != len(STAGES) if len(STAGES) > 0 else False:
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

# TODO ------------------------ CASCADE COMPARISON METHODS

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
