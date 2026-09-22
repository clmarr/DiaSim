import UTILS
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("-p","--position",type=int,help="Position of new column relative to other columns (0 = first, 1 = after first)")
parser.add_argument("-n","--name",help="Name of stage represented by this column")
parser.add_argument("-l","--lexicon",help="Location of lexicon file we are building a new lexicon with this stage initialized out of")
parser.add_argument("-o","--output",help="location of where new lexicon with this stage inserted will be")
args =parser.parse_args()

UTILS.insert_empty_stage(args.position, args.name, args.lexicon, args.output)
