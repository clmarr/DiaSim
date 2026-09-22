import os
import shutil
import lingpy

#TODO BEWARE-- this requires your system to have bash.

STAGE_OUT_DELIM = " | "
LEX_STAGE_DELIM = " , "
GOLD_ONSET_FLAG = "{" # to filter out gold forms from result column of outgraph
ACC_REPORT_FILE = "goldAnalysis.txt" #file that will be used to see if match is 100% between two cascades
OVERALL_ACC_LINE = 3 #line within that that reports overall accuracy.
TOTAL_ACC_INDIC = "1" #100% match
ID_FLAG = "ɸ"
CMT_FLAG = "$"
RECONSTR_FLAG = "*"
ABSENT_INDIC = "..." # means word is not (yet) in lexicon at this stage
ONSET_INDIC = CODA_INDIC = "#"
PHONE_DELIM = " "
STAGE_HEADER_FLAG = "="
STAGE_HEADER_DELIM = ","



# ---------------- lexicon management methods

# true if there is a non empty form ID assigned to the lexeme line lxln
def lexemeHasID(lxln):
    if CMT_FLAG not in lxln:
        return False
    cmt = lxln[lxln.find(CMT_FLAG)+1:]
    return ID_FLAG in cmt

# get any tagged form ID in lexicon line. Return '' if there is none.
def getID (lex_line):
    if not lexemeHasID():
        return ''
    return lex_line[lex_line.rfind(ID_FLAG)+1:]

# make an alphabetized version of the lexicon file input
    # output_loc is where the output file will be
        # by default, output will be the input file name with "_alphasorted" added before the file extension
    # if "verbose" is true, it will report where two lines with identical content are
def alphabetize(input_loc,output_loc = False,verbose=False):
    if not output_loc:
        output_loc = os.path.splitext(input_loc)[0] + "_alphasorted" + str(os.path.splitext(input_loc)[1])

        lines_with_comments = [ln.strip() for ln in o.readlines()]
        lines = [ln.split(CMT_FLAG)[0].strip() for ln in lines_with_comments]
        lines = sorted(lines)

        lines_with_comments = sorted([ln for ln in lines_with_comments if ln != '' and ln[0] != CMT_FLAG])

        with open(output_loc, mode="w", encoding="utf-8") as o:
            for i in range(len(lines)-1):
                o.write(lines_with_comments[i] + "\n")
                if lines[i] == lines[i+1]:
                    # duplicate unless they BOTH already have designated IDs
                    if not (lexemeHasID(lines[i]) and lexemeHasID(lines[i+1]) and getID(lines[i]) != getID(lines[i+1])):
                        print("duplicate line at alphabetically sorted line number " + str(i) + ": " + lines[i])
            o.write(lines_with_comments[-1] + "\n")


# ------------------------ CASCADE COMPARISON METHODS

DUMMY_RUN_DIR = "TEMP"
FIRST_CASC_PREDICTION_LEX = "casc1predictions.txt"
STAGE_OUTGRAPH_SUFFIX = "_stagewise_output_graph.csv"
FIRST_RUN_SUBDIR = "run1"


# if true based on goldAnalysis.txt of casc2's output applied to lexicon with gold as casc1's output,
    # then the cascades are equivalent.
def cascMatch(overallAccLine):
    decimalLoc = overallAccLine.find(".")
    if decimalLoc < len(TOTAL_ACC_INDIC):
        print("ERROR: invalid overall accuracy line : "+overallAccLine)
    return overallAccLine[decimalLoc-len(TOTAL_ACC_INDIC):decimalLoc] == TOTAL_ACC_INDIC #100%

#THE FOLLOWING ARE CURRENTLY NOT IN USE BECAUSE ACCURACY REPORT IS USED INSTEAD
FED_COLUMN_HEADER = "featureED" #the header of the column in resultEditDistances.csv output that has feature EDs
NO_DIFF_INDIC = "0.0" # entry in feature edi distance column that indicates identicality.
    # identical forms should have 0
RESULT_ED_FILE = "resultEditDistances.csv" #location of file with resulting edit distances for comparison between runs
    # -- use to check etymon-wise equivalence.

RUNCALL_SUFFIX = " -diacrit -files_only"

# TODO need means of creating synthetic data

#remove gold form from CFR prediction cell -- e.g. "haja {GOLD: aga}" becomes "haja"
def rmv_gold(str):
    out = ""+str
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
        out = out[len(RECONSTR_FLAG)+1:]
    if out[:len(ONSET_INDIC)] == ONSET_INDIC:
        out = out[len(ONSET_INDIC):]
    if out[len(out)-len(CODA_INDIC):] == CODA_INDIC:
        out = out[:len(out)-len(CODA_INDIC)]

    #TODO DEBUGGING
    print("out : "+out)

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
        raise Exception("why is there only one stage in outgraph row? This should not have happened. Input: "+soRow)

    return LEX_STAGE_DELIM.join([stageOutToLexRowCell(stageOut) for stageOut in stageOuts]) + suffix

def outGraphToComparisonLex(outGraphLoc, lexDest):
    f = open(outGraphLoc,"r",encoding="utf-8")
    lines = f.readlines()
    f.close()

    header = LEX_STAGE_DELIM.join(rmv_gold(lines[0]).split(STAGE_OUT_DELIM)[1:])
    lines = [stageOutToLexRow(li) for li in lines[1:]]

    g = open(lexDest,"w",encoding="utf-8")
    g.write("\n".join(lines))
    g.close()

# DiaSim run wrapper, with error handling. Prerequisite: directories in any file path names must exist.
def diaSimRun(saveTo, lex, casc, otherSettings = "", errorIntro = ""):
    if errorIntro == "":
        errorIntro = "Couldn't run DiaSim on cascade file "+casc+" for lexicon "+lex+"; run produced an error."

    if otherSettings != "" and otherSettings[0] != " ":
        otherSettings = " "+otherSettings
    try:
        #os.system("bash derive.sh -out " + saveTo + " -lexicon " + lex + " -rules " + casc + RUNCALL_SUFFIX)
        os.system("java -cp bin DiachronicSimulator -out " + saveTo + " -lexicon " + lex + " -rules " + casc + otherSettings + RUNCALL_SUFFIX)
    except Exception as e:
        print(errorIntro+". "+str(e))

#to make "gold" of first compared cascade's output, primarily
# makes lex where first line is inputs, second is CFR predictions of this cascade
#       -- for the purposes of comparison to another upon same data
# returns location of resulting lexicon
def makeReferencePredictionLex(saveTo, lex, casc, etc=""):

    os.makedirs(os.path.join(saveTo,FIRST_RUN_SUBDIR),exist_ok=True)

    diaSimRun(os.path.join(saveTo,FIRST_RUN_SUBDIR), lex, casc, otherSettings=etc, errorIntro="Error making reference prediction lexicon...")

    outlex_loc = os.path.join(saveTo,FIRST_RUN_SUBDIR,FIRST_CASC_PREDICTION_LEX)
    outGraphToComparisonLex(os.path.join(saveTo,FIRST_RUN_SUBDIR,FIRST_RUN_SUBDIR+STAGE_OUTGRAPH_SUFFIX),
                             outlex_loc)

    return outlex_loc

# TRUE if they match, else FALSE
# will save outputs only if saveTo is given a value
def compareCascades(lex , #lex to compare on
                    casc1 , #location of text file for first cascade to compare
                    casc2 , #location of text file for second cascade to compare
                    saveTo= False, #true if we want to actually keep these files
                    symbDefsLoc = False, #location if we want to usurp the normal file
                    impls = False #location of feature implications file if we want to usurp the normal one.
                    ):
    out = str(saveTo) if saveTo else DUMMY_RUN_DIR

    if out not in os.listdir():
        os.makedirs(out, exist_ok=True)

    etj = "" + [""," -impl " + str(impls)][impls] + [""," -symbols "+str(symbDefsLoc)][symbDefsLoc]

    casc1_pred_lex = makeReferencePredictionLex(out,lex,casc1,etj)

    diaSimRun(saveTo, casc1_pred_lex, casc2,
              otherSettings= etj,
              errorIntro="Error making comparison lexicon.")

    comparisonFile = os.path.join(out, ACC_REPORT_FILE)
    with open(comparisonFile,"r") as f:
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
    f = open(text_lex_loc,"w",encoding="utf-8")
    f.write("=In,Stage1,Out\n"
            "o w i,>*,o ʍ\n"
            "i w o,>*,w o\n"
            "...,a w i,t r o l o l o l\n");
    f.close()

    casc1_loc = os.path.join(DUMMY_RUN_DIR, "casc1.txt")
    casc2_loc = os.path.join(DUMMY_RUN_DIR, "casc2.txt")

    f = open(casc1_loc,"w",encoding="utf-8")
    f.write("i > ∅ / __ #\n"
            "w > ʍ / __ #\n"
            "=Stage1\n"
            "a > o")
    f.close()

    f = open(casc2_loc,"w",encoding="utf-8")
    f.write("w i > w̥ / __ #\n=Stage1\na > o")
    f.close()

    result = compareCascades(text_lex_loc,casc1_loc,casc2_loc,saveTo=os.path.join(DUMMY_RUN_DIR,"cascadeComparisonTest"))
    if result: #True -- passed
        print("compareCascades works (it seems)")
        shutil.rmtree(DUMMY_RUN_DIR)
    else:
        print("there is a bug with compareCascades -- check results in "+os.path.join(DUMMY_RUN_DIR,"cascadeComparisonTest"))

