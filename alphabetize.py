import UTILS
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("-i", "--input", help="Location of lexicon file to alphabetize",
                    required=True)
parser.add_argument("-o", "--output", help="Location of output file", default=False)
parser.add_argument("-s", "--silent", help="Don't report duplications", action="store_true")
args = parser.parse_args()

UTILS.alphabetize(args.input,args.output,verbose=not args.silent)
