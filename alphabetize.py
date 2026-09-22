import UTILS
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("-i", "--input", help="Location of lexicon file to alphabetize",
                    required=True)
parser.add_argument("-o", "--output", help="Location of output file", default=False)
parser.add_argument("-s", "--silent", help="Don't report duplications", action="store_true")
parser.add_argument("-c", "--column", help="Sort by form at the given stage number (otherwise sorts by last stage with content", default = -1, type = int)
parser.add_argument("-a", "--alphabet", help = "Location of symbol defs file whose symbol order will be used. Otherwise, sorting is alphabetical", default = False)
args = parser.parse_args()

UTILS.alphabetize(args.input,args.output,verbose=not args.silent,pivot_column=args.column,sort_order=args.alphabet)
