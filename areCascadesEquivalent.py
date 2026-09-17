import UTILS
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("-l", "--lex", help="Location of file with words to apply Computerized Forward Reconstruction upon")
parser.add_argument("--casc1", help="Location of the first cascade to compare")
parser.add_argument("--casc2", help="Location of the second cascade to compare")
parser.add_argument("-s", "--saveTo", default=False, help="Location to store run outputs for comparison. "
                                                          "By default, they are deleted after this concludes.")
parser.add_argument("-i","--impls", default=False, help="Location of the implications file to usurp the default")
parser.add_argument("-f","--features", default=False, help="Location of the feature-symbol mapping file to usurp the default (symbolDefs.csv)")

args = parser.parse_args()

UTILS.compareCascades(args.lex,args.casc1,args.casc2,args.saveTo,args.impls,args.features)
