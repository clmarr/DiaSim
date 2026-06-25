import os
import shutil
import lingpy

DUMMY_RUN_DIR = "TEMP"
STAGE_OUTGRAPH_SUFFIX = "_stagewise_output_graph.csv"
FIRST_RUN_SUBDIR = "run1"
STAGE_OUT_DELIM = " | "
LEX_STAGE_DELIM = " , "
FIRST_CASC_PREDICTION_LEX = "casc1predictions.txt"
GOLD_ONSET_FLAG = "{" # to filter out gold forms from result column of outgraph
ACC_REPORT_FILE = "goldAnalysis.txt" #file that will be used to see if match is 100% between two cascades
OVERALL_ACC_LINE = 3 #line wihtin that that reports overall accuracy.
TOTAL_ACC_INDIC = "1" #100% match
ID_FLAG = "ɸ"
RECONSTR_FLAG = "*"
ABSENT_INDIC = "..." # means word is not (yet) in lexicon at this stage
ONSET_INDIC = CODA_INDIC = "#"
PHONE_DELIM = " "
CMT_FLAG = "$"

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
# BEwARE this will be true of the INPUT stage cell
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
    f = open(outGraphLoc,"r")
    lines = f.readlines()
    f.close()

    header = LEX_STAGE_DELIM.join(rmv_gold(lines[0]).split(STAGE_OUT_DELIM)[1:])
    lines = [stageOutToLexRow(li) for li in lines[1:]]

    g = open(lexDest,"w")
    g.write("\n".join(lines))
    g.close()


#to make "gold" of first compared cascade's output, primarily
# makes lex where first line is inputs, second is CFR predictions of this cascade
#       -- for the purposes of comparsion to another upon same data
# returns location of resulting lexicon
def makeReferencePredictionLex(saveTo, lex, casc):

    os.makedirs(os.path.join(saveTo,FIRST_RUN_SUBDIR),exist_ok=True)

    os.system("bash derive.sh -out "+os.path.join(saveTo,FIRST_RUN_SUBDIR)+" -lexicon "+lex+" -rules "+casc+RUNCALL_SUFFIX)

    outlex_loc = os.path.join(saveTo,FIRST_RUN_SUBDIR,FIRST_CASC_PREDICTION_LEX)
    outGraphToComparisonLex(os.path.join(saveTo,FIRST_RUN_SUBDIR,FIRST_RUN_SUBDIR)+STAGE_OUTGRAPH_SUFFIX,
                             outlex_loc)

    return outlex_loc

# TRUE if they match, else FALSE
# will save outputs only if saveTo is given a value
def compareCascades(lex , #lex to compare on
                    casc1 , #location of text file for first cascade to compare
                    casc2 , #location of text file for second cascade to compare
                    saveTo= False, #true if we want to actually keep these files
                    ):
    out = saveTo if saveTo else DUMMY_RUN_DIR

    if out not in os.listdir():
        os.makedirs(out, exist_ok=True)

    casc1_pred_lex = makeReferencePredictionLex(out,lex,casc1)

    os.system("bash derive.sh -out "+saveTo+" -lexicon "+ casc1_pred_lex + " -rules "+casc2 +RUNCALL_SUFFIX)

    comparisonFile = os.path.join(saveTo, ACC_REPORT_FILE)
    with open(comparisonFile,"r") as f:
        match = cascMatch(f.readlines()[OVERALL_ACC_LINE])

    #if saved to temp, delete comparison data
    if saveTo == DUMMY_RUN_DIR:
        shutil.rmtree(os.path.join(saveTo))

    return match

#to test if compare cascades works
# should work as long as w̥ and ʍ refer to same feature set.
def compareCascadesTester():
    text_lex_loc = os.path.join(DUMMY_RUN_DIR, "testLex.txt")
    os.makedirs(DUMMY_RUN_DIR, exist_ok=True)
    f = open(text_lex_loc,"w")
    f.write("=In,Stage1,Out\n"
            "o w i,>*,o ʍ\n"
            "i w o,>*,w o\n"
            "...,a w i,t r o l o l o l\n");
    f.close()

    casc1_loc = os.path.join(DUMMY_RUN_DIR, "casc1.txt")
    casc2_loc = os.path.join(DUMMY_RUN_DIR, "casc2.txt")

    f = open(casc1_loc,"w")
    f.write("i > ∅ / __ #\n"
            "w > ʍ / __ #\n"
            "=Stage1\n"
            "a > o")
    f.close()

    f = open(casc2_loc,"w")
    f.write("w i > w̥ / __ #\n=Stage1\na > o")
    f.close()

    result = compareCascades(text_lex_loc,casc1_loc,casc2_loc,saveTo=os.path.join(DUMMY_RUN_DIR,"cascadeComparisonTest"))
    if result: #True -- passed
        print("compareCascades works (it seems)")
        shutil.rmtree(DUMMY_RUN_DIR)
    else:
        print("there is a bug with compareCascades -- check results in "+os.path.join(DUMMY_RUN_DIR,"cascadeComparisonTest"))


