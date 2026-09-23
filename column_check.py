import UTILS
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("-f", "--file", help="Path of the lexicon file to check", required=True)
parser.add_argument("-n","--numcols", help="Number of columns it should have", default=False, type=int)
args = parser.parse_args()

UTILS.col_check_report(args.file,ncols=args.numcols,verbose=True)