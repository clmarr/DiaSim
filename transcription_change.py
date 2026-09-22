import argparse
import UTILS

#TODO check that this handles unicode in the argparse...?

parser = argparse.ArgumentParser(
    description="Script to change a phoneme's transcription (beware of accidentally merging transcriptions!)")
parser.add_argument("-f","--file", help="Lexicon file in which to carry out the change", default=False)
parser.add_argument("-s","--stage", help="Name of at which to carry out the transcription change", default=False)
parser.add_argument("--stage_file", help="File with stage names on each line in order", default=False)
parser.add_argument("-c","--column", help="(stage) Column (number) in which to carry out the transcription change", default=False, type=int)
parser.add_argument("-m","--mutandum", help="The phoneme for which transcription is being changed", default=False)
parser.add_argument("-r","--result", help="The new transcription for this phoneme", default=False)
parser.add_argument("-n","--numcols", default=False)
args = parser.parse_args()

if args.stage:
    args.stage = args.stage.strip()

if not args.file or not args.mutandum or not args.result:
    raise Exception("Error: need to specify file (-f), transcription to change(-m), and result(-r)")

if UTILS.LEX_DELIM in args.mutandum or UTILS.CMT_FLAG in args.mutandum:
    raise Exception("Can't have lex delim or cmt delim in content being changed!")
if UTILS.LEX_DELIM in args.result or UTILS.CMT_FLAG in args.result:
    raise Exception("Can't have lex delim or cmt delim in content being put in!")

f = open(args.file, encoding="utf-8")
header = ""
while len(header.strip()) == 0 :
    header = f.readline()

headed_by_header = header[0] == UTILS.HEADER_FLAG
if headed_by_header:
    header = header[len(UTILS.HEADER_FLAG):]

if UTILS.CMT_FLAG in header:
    header = header[:header.index(UTILS.CMT_FLAG)]

header = header.strip()
single_column_lex = UTILS.HEADER_DELIM not in header
header_stages = header.split(UTILS.HEADER_DELIM)

if args.stage_file:
    UTILS.setGlobalStages(args.stage_file)


if single_column_lex:  # then have to be using only one column and that's the stage
    # can't be output
    if args.stage == UTILS.STAGES[-1]:
        raise Exception("Error: can't use output stage for file with only one column...")
    args.numcols = 1
    args.column = 0

    if headed_by_header:
        if header != args.stage:
            raise Exception("Error: set stage as '" + args.stage + "' but only stage in header is " + header)

if not args.column and not args.stage and not single_column_lex:
    raise Exception("Error: need to specify stage (-s) or column (-c)!")

if not args.column and not single_column_lex:

    args.stage = args.stage.strip()
    if args.stage not in UTILS.STAGES:
        raise Exception("Error: stage '"+args.stage+"' not in stage list!")

    if args.numcols:
        if args.numcols != len(header_stages):
            raise Exception("Error: mismatch in numcols!")
    else:
        args.numcols = len(header_stages)

    if headed_by_header:# then will be able to get column by what's there.
        header_stages = [si.strip() for si in header_stages]
        if args.stage not in header_stages:
            raise Exception("Error: set stage as '"+args.stage+"' but it's not detected in the header! (: "+header+")")
        args.column = header_stages.index(args.stage)

    else: # must be same number of stages then.
        if len(UTILS.STAGES) != len(header_stages):
            raise Exception("Error: set stage but no header in this lex file, and number of columns != number of globally specified stages!")
        if args.stage not in UTILS.STAGES:
            raise Exception("Error: set stage but there's no header and the stage set ('"+args.stage+"') is not a globally specified stage!")
        args.column = UTILS.STAGES.index(args.stage)

UTILS.change_transcription(args.file, args.column, args.mutandum, args.result, n_cols = args.numcols)
