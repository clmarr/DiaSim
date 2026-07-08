import pdb
import csv

INP_S_M_DELIM = ','  # symbol map delimiter for input language character strings mapped to phone(t/m)ic values
OUTFILE_DELIM = ','  # column delimiter for output file columns
MORPH_CLAUSE_FLAG = '%'
CMT_FLAG = '$'
FORM_ID_FLAG = "ɸ"

# parse phonemes out of a string (@param chars), given a symbol map (@param symbol_map)
# @return list of phones
#  i.e. transforms symbols that are keys in symbol_map into a list of their constituent phones (which may be a list of 1)
# for those that aren't keys, they are assumed to be the phone with the same IPA symbol
# note that digraphs will have priority over single characters, trigraphs over digraphs, etc.
# if @param symbol map is set to False, then
# if @param rules_to_impose is not False, then it should be a list of functions to be operated on the input in order
# currently it is being used to automatically impose latin stress rules, and/or dental diacritics for n d t
def phoneme_parse(chars, symbol_map, rules_to_impose=False):
    if not symbol_map or symbol_map == {}:  # i.e. if symbol_map is uninitialized.
        return chars.split()

    if chars == "#DEF#":
        pdb.set_trace()
    output = []

    mapped_inputs = list(symbol_map.keys())
    mi_lens = sorted(list(set([len(mi) for mi in mapped_inputs])), reverse=True)
    chars_left = "" + chars
    while len(chars_left) > 0:
        mi_iter = 0
        while mi_iter < len(mi_lens):
            milen_i = mi_lens[mi_iter]
            mapped_symb_detected = False if milen_i > len(chars_left) else chars_left[:milen_i] in mapped_inputs
            if mapped_symb_detected:
                output += symbol_map[chars_left[:milen_i]]
                chars_left = chars_left[milen_i:]
                mi_iter = 0  # reset, to continue giving precedence to longer sequences for the next-up (set of) character(s)...
            else:
                mi_iter += 1
        # if reached end of input symbols/sequences to be mapped
        # ...   then just pop and parse single existing symbol at front as itself.
        # but only if there's something left
        if len(chars_left) > 0:
            output += [chars_left[0]]
            chars_left = chars_left[1:]

    if rules_to_impose:
        for ri in rules_to_impose:
            output = ri(output)
    return output


# input: a list of phones (each in string form), @param ph_seq
# @param enclosure -- if there is a character ([ or /) to enclose them ([ will pair with ])
# returns the string of the list joined by the delimiter @param delim
def str_ph_seq(ph_seq, enclosure="", delim=" "):
    output = delim.join(ph_seq)
    if enclosure == "[":
        output = "[" + output + "]"
    elif enclosure != "":
        output = enclosure + output + enclosure
    return output


# place a stress marker at the assigned spot (@param target_loc)
# will place primary stress unless @param secondary is True
# destructively modifies @param ph_string
def assign_stress_to_ph(ph_string, target_loc, secondary=False):
    if len(ph_string) <= target_loc:
        pdb.set_trace()
        raise RuntimeError("tried to assign stress to an invalid location!")
    if len(ph_string[target_loc]) == 0:
        pdb.set_trace()
        raise RuntimeError("found nothing in slot for phone to assign stress to ! ")

    stress_mark = "ˌ" if secondary else "ˈ"
    if ph_string[target_loc][0] in "ˈˌ":
        ph_string[target_loc] = stress_mark + ph_string[target_loc][1:]
    else:
        ph_string[target_loc] = stress_mark + ph_string[target_loc]


def vowel_is_long(vowel):
    return "ː" in vowel


TO_DENTALIZE = ["n", "d", "t"]


# impose dental place diacritic on phones that were dentally articulated (rather than alveolar) in Latin, if necessary
# @param inp_phones should be a list of strings.
def impose_latin_dentals(inp_phones):
    if len(inp_phones) == 0:
        raise RuntimeError("tried to assign stress to an empty sequence of phones!")
    output = [] + inp_phones
    for opi in range(len(output)):
        ph_here = output[opi]
        if ph_here in TO_DENTALIZE:
            output[opi] += "̪"
    return output


LATIN_S_NUCLEI = ["ɑ", "a", "ɑː", "e", "eː", "i", "iː", "y", "yː", "o", "oː", "u", "uː"]
LATIN_STOPS = ["b", "p", "d", "d̪", "t", "t̪", "ɡ", "k"]


# impose latin stress system on sequence of input phones (@inp_phones, a list of strings)
# return same list but with Latin stress rules imposed (as formulated in -CLEF, per Pope, etc...)
# ... i.e. primary stress to penult if heavy, else to antepenult
# secondary stress to initial syllable if not already stressed
# rules for secondary may have been more complicated as it may also apply to stem initial syllables in prefixed derivations
# TODO fix this at a later point if necessary...
def impose_latin_stress(inp_phones):
    if len(inp_phones) == 0:
        raise RuntimeError("tried to assign stress to an empty sequence of phones!")

    output = [] + inp_phones

    # first get the locations of vowels.
    vowel_locs = []
    for ipi in range(len(inp_phones)):
        if inp_phones[ipi] in LATIN_S_NUCLEI:
            vowel_locs += [ipi]

    # if there are no vowels: just return
    if len(vowel_locs) == 0:
        print("No vowels found in " + str_ph_seq(inp_phones) + "!")
        pdb.set_trace()

    # assign primary stress...
    # if only one vowel present here, assign primary to it and return.
    # this is actually the same scenario functionally if there are two vowels, wherein there is automatic primary stress
    #   on the penult, which is also the first syllable vowel.
    elif len(vowel_locs) < 3:
        output[vowel_locs[0]] = "ˈ" + output[vowel_locs[0]]

    else:  # apply Latin stress rule considerations -- penult iff heavy, else antepenult, secondary stress on initial if unstressed.
        # all long vowels are heavy
        primary_stress_target = -2  # index of vowel_locs whose loc will have stress applied ~ -2 for penult, -3 for penult
        if not vowel_is_long(output[vowel_locs[-2]]):
            # a short vowel is still light/weak -- i.e. if a penult, stress can thus be attracted away from it--
            # -- if only one consonant or semivowel stands between it and the next vowel
            # or if it is specifically a stop followed by r that stands between.
            if vowel_locs[-1] - vowel_locs[-2] <= 2:  # one item in between, or hiatus -- must be weak syllable.
                primary_stress_target = -3
            elif vowel_locs[-1] - vowel_locs[-2] == 3:  # two items in between
                if inp_phones[vowel_locs[-1] - 1] == "r" and inp_phones[vowel_locs[-1] - 2] in LATIN_STOPS:
                    primary_stress_target = -3
        assign_stress_to_ph(output, vowel_locs[primary_stress_target])
        if -1 * primary_stress_target < len(vowel_locs):  # i.e. there is an unstressed initial vowel...
            assign_stress_to_ph(output, vowel_locs[0], secondary=True)

    return output


# detect if a geminate consonant pair begins in the list of phones @param inp_phones
#   at the index (int) indicated by @param index
#   internally, this will be triggered by a metasymbol "CC"
def detect_geminate(inp_phones, index):
    if index > len(inp_phones) - 2:
        return False

    return inp_phones[index] == inp_phones[index + 1]


# for now, it seems more efficient to just hardcode the (relatively limited) set of possible Latin vowels and consonants
# rather than using symbolDefs (AKA segments.csv) to detect their phonetic features and thus phonetic class membership online.
# this may however change in the future
# TODO write methods to build a HashMap/dictionary of symbol-to-feature-values, and reference it for these purposes.
LATIN_VOWELS = ["a", "aː", "ɑ", "ɑː", "ˌa", "ˌaː", "ˌɑ", "ˌɑː", "ˈa", "ˈaː", "ˈɑ", "ˈɑː",
                "e", "eː", "ˌe", "ˌeː", "ˈe", "ˈeː",
                "i", "iː", "ˌi", "ˌiː", "ˈi", "ˈiː",
                "y", "yː", "ˌy", "ˌyː", "ˈy", "ˈyː",
                "o", "oː", "ˌo", "ˌoː", "ˈo", "ˈoː",
                "u", "uː", "ˌu", "ˌuː", "ˈu", "ˈuː"]
LATIN_CONSONANTS = ["b", "p", "m", "f",
                    "w",  # nonconsonantal, may merit reconsideration
                    "d", "t", "n", "r", "l", "s", "z", "ts", "t͡s",
                    "j",  # this one may need reconsideration as the data often treats unstressed i in hiatus as j
                    "ɡ", "g", "k", "ŋ",
                    "h"  # nonconsonantal, may merit reconsideration
                    ]  # dtn assumed to not already be dentalized
LATIN_SOURDS = ["p", "f",
                "t", "s", "ts", "t͡s",
                "k",
                "h"  # nonconsonantal, may merit reconsideration
                ]  # t assumed to not already be dentalized
LATIN_NASALS = ['m', 'n']
LATIN_CORONALS = ['n', 'd', 't', 's', 'l', 'r']
LATIN_PHON_CLASS_ABBREVS = {
    'V': LATIN_VOWELS,
    'C': LATIN_CONSONANTS,
    'CC': [[cons, cons] for cons in LATIN_CONSONANTS],
    'S': LATIN_SOURDS,
    'N': LATIN_NASALS,
    'T': LATIN_STOPS,
    'D': LATIN_CORONALS
}

# list of (String) 'phones', assuming that impose_latin_stress has already been called (hence lexeme-initial secondary stress),
# for prefixes that are to trigger the method impose_secondary_root_stress to act upon the root initial syllable's vowel.
# metasymbols at use:
# needs to operate BEFORE latin dentals are imposed
# C -- any consonant (list of consecutive Cs -- a cluster (at least) that number of consonants long
# CC -- a geminate consonant
# S -- any voiceless consonant  (sourd)
# T -- any stop
# N -- any nasal
# D -- any coronal
# prefixes currently excluded: bene- (bene-dicere), para- (para-bolare), cum- (no examples yet; cumulare < cuulus)
#   prae- (prae-dicare) -- although in French the issue with this may just be the special -ika- sequence reductions
#   r(h)o-    -- in some marginal loans (Greek? Gaulish?) items potentially, but not likely relevant for the time being.
# not yet included for lack of examples, but consider inclusion: ob-, ambi-, inter-, intra-
# not yet handled: stacked preverbs
#   one known case that is relevant: com-prae-hendere.
ROOT_STRESS_TRIGGERING_PREFIXES = [
    ['ˌɑ', 'b'], ['ˌɑ', 'p', 'T'], ['ˌa', 'b'], ['ˌa', 'p', 'T'], # ab-
    ['ˌɑ', 'CC'], ['ˌɑ', 'd'], ['ˌɑ', 'C', 'C', 'C'], ['ˌa', 'CC'], ['ˌa', 'd'], ['ˌa', 'C', 'C', 'C'],   # ad-
    ['ˌɑ', 'w', 's'], ['ˌa', 'w', 's'],  # aus- (compounding)
    ['ˌɑ', 'm', 'p', 'l', 'i'], ['ˌa', 'm', 'p', 'l', 'i'], # ampli-
    ['k', 'ˌi', 'r', 'k', 'u', 'm'],  # circum- (compounding)
    ['k', 'ˌo', 'N'],  # con-. Excluding col- cases, and opaque cō-C/co-C cases.
    ['d', 'ˌeː'],  # dē-
    ['d', 'ˌi', 's'], ['d', 'ˌi', 'CC'],  # dis-
    ['ˌeː', 'N'], ['ˌe', 'k', 's'],  # ex- (ē- before nasals)
    ['ˌi', 'N'],  # in-
    ['p', 'ˌe', 'r'],  # per-
    ['p', 'r', 'ˌoː'],  # prō-
    ['r', 'ˌe'],  # re-
    ['s', 'ˌu', 'b'], ['s', 'u', 'CC']  # sub-
]


# prefix_phs should be a list of phones (as Strings) and/or phonetic class abbreviations (cf. LATIN_PHON_CLASS_ABBREVS)
# candidate_phs is the list of phones (as Strings) in the word that is being checked if it is prefixed with the prefix of interest
# it will NOT include any abbreviations of the sort used as keys in LATIN_PHON_CLASS_ABBREVS!
# in an Indo-European sense -- and perhaps even a causal one here! -- these may rather be *preverbs* not just prefixes
def check_for_prefix(prefix_phs, candidate_phs):
    if len(candidate_phs) <= len(prefix_phs):
        return False

    prefix_i, cand_i = 0, 0
    while prefix_i < len(prefix_phs):
        if cand_i >= len(candidate_phs):  # theoretically could happen if we somehow have a geminate-heavy prefix.
            # Currently impossible though and doesn't seem like a plausible scenario.
            return False

        pref_ph_i, cand_ph_i = prefix_phs[prefix_i], candidate_phs[cand_i]
        if pref_ph_i in LATIN_PHON_CLASS_ABBREVS.keys():
            if pref_ph_i == 'CC':
                if not detect_geminate(candidate_phs, cand_i):
                    return False
                prefix_i += 1
                cand_i += 2
            elif cand_ph_i not in LATIN_PHON_CLASS_ABBREVS.get(pref_ph_i):
                return False
            else:
                prefix_i += 1
                cand_i += 1
        else:
            if pref_ph_i != cand_ph_i:
                return False
            prefix_i += 1
            cand_i += 1

    return True


# return length of detected input triggering prefix, if detected
# if not detected, return -1
def detect_rootstress_triggering_prefix_length(inp_phone_list):
    for prefix in ROOT_STRESS_TRIGGERING_PREFIXES:
        if check_for_prefix(prefix, inp_phone_list):
            return len(prefix)
    return -1


# root beginnings that appear to not get the root-stress -- should probably be handled with a diachronic/phonological rule, but hard-coded for now.
NO_ROOT_STRESS_FOR = [
    ['p', 'u', 't'],  # computare > compter/conter, imputare > enter
    ['t', 'uː', 's'],  # to dodge the false friend pertūsiare < pertūsus.
    ['i', 'uː', 'D', 'ˈɑː'], ['j', 'uː', 'D', 'ˈɑː'],
    # adiūtāre > aider, disiūnare > dîner. All coronals roped in for a (less un-)falsifiable hypothesis.
]


# @return index of the (first) primary stressed vowel of the phones in @param ph_list
# @return -1 if no phone here has primary stress
# should not be called before stress is imposed.
def find_primary_stress (ph_list):
    for i in range(len(ph_list)):
        if "ˈ" in ph_list[i]:
            return i
    return -1


# impose secondary stress on the first vowel found in @param inp_phones, after the index indicated by @param ind
#   ... if it is unstressed. If it is stressed (primary or secondary), do nothing.
#   @return the result.
# currently reliant on hardcoded latin vowel list, but if necessary (TODO) recruit symbolDefs/segments.csv-derived detection method
# currently not doing so if a vowel before @param ind has primary stress either.
def impose_countertonic_on_first_vowel_after_ind(inp_phones, ind):
    # block any action by this method if there is an earlier stressed syllable nucleus.
    # though in practice this seems unlikely as primary stressed vowels are not present in any of the phone lists u
    #  ... in the list of hardcoded preverbs currently (as of Feb 4, 2024) triggering root-initial secondary stress

    if find_primary_stress(inp_phones) < ind:
        return inp_phones

    i = ind + 0
    while i < len(inp_phones):
        ph_here = inp_phones[i]
        if ph_here in LATIN_VOWELS:
            if 'ˈ' in ph_here or 'ˌ' in ph_here:  # already stressed.
                return inp_phones
            out = [] + inp_phones
            out[i] = "ˌ" + out[i]
            return out
        i += 1
    return inp_phones


# for a list of (String) phones, @param inp_phones
# @return version of it
# whereby we have imposed secondary stress on root-initial syllables, if they are not already stressed,...
# ... if they are prefixed with one of a certain set of prefixes
# current policy : won't trigger if the prefix is STRESSEDǃ
def impose_secondary_root_stress(input_phones):
    pref_len = detect_rootstress_triggering_prefix_length(input_phones)
    if pref_len == -1:
        return input_phones

    skip_place = 0
    if len(input_phones) > pref_len + 2:
        if input_phones[pref_len] in ['i','u']:
            if input_phones[pref_len+1] in LATIN_VOWELS:
                skip_place = 1

    return impose_countertonic_on_first_vowel_after_ind(input_phones, pref_len+skip_place)


# build a mapping between characters in a certain language's annotation and their phonemic values
# based on lines in the file (@param) map_file_loc
# delimited by the character @param map_delim
def build_symbol_mapping(map_file_loc, map_delim):
    generated_mapping = {}
    infile_lns = (open(map_file_loc).read().splitlines())
    infile_lns = [ln.split(map_delim) for ln in infile_lns if map_delim in ln]
    for symbol_def in infile_lns:
        generated_mapping[symbol_def[0].strip()] = symbol_def[1].strip().split(" ")
        # note that the input could also be a sequence... TODO does this cause problems?
        # the above should return a list of one item in the case that there is no ' '.
        # otherwise, i.e. where a sequence of phones is being mapped to,
        # it returns an ordered list of each phone in the sequence of phones.
    return generated_mapping


# returns string form in proper format for diasim lexicon file, for an ordered list of phonemes (@param ph_list)
# that comprise an etymon's (or cell's) phonological form at a given point in time.
def diasim_lexicon_phrep(ph_list):
    return " ".join([ph.strip() for ph in ph_list])


# return line in diasim lexicon format
# @param src_from -- a list of strings each being a phone(me), together comprising the phonological form in source language
# @param reflex -- a list of strings each being a phone(me), together comprising the phonological form in source language
# @param intermediate_stage_forms -- a list of lists, each nested list of the format described above. For intermediate stage forms.
# currently not in use in this project.
# If it is to be used, note that the same number of stages must be used for each line.
# @param morph_clause -- for paradigm info clause. Currently not in use for this project
# @param comment -- for comment clause
def diasim_lex_line(src_form, reflex, intermediate_stage_forms=False, morph_clause=False, comment=False):
    forms_by_stage = []
    forms_by_stage += [diasim_lexicon_phrep(src_form)]
    if intermediate_stage_forms:
        for st_f in intermediate_stage_forms:
            forms_by_stage += [diasim_lexicon_phrep(st_f)]
    forms_by_stage += [diasim_lexicon_phrep(reflex)]
    output = (" " + OUTFILE_DELIM + " ").join(forms_by_stage)

    return output + diasim_lex_line_suffix(morph_clause=morph_clause,comment=comment)

def inp_only_diasim_lex_line(src_form, morph_clause=False, comment=False):
    output = "" + diasim_lexicon_phrep(src_form)
    return output + diasim_lex_line_suffix(morph_clause=morph_clause,comment=comment)

def diasim_lex_line_suffix(morph_clause = False, comment = False):
    outp = ""
    if morph_clause:
        outp += " " + MORPH_CLAUSE_FLAG + morph_clause
    if comment:
        outp += " " + CMT_FLAG + comment
    return outp

## non-smart quotation character that surrounds etymologies with ',' or quotes in them, it seems...?
# to be used in handling this column, to make sure it is not accidentally counted as multiple columns due to use of
# same character as delimiter INP_DELIM
INP_DELIM = ','  # delimiter for input file columns
CSV_QU_START, CSV_QU_END = '“', "”"  # quote characters used to detect enclosed delimiter character, to avoid errant column split
CELL_START_QU, CELL_END_QU = '"', '"'  # tend to lurk at the end of cells in a csv, will frustrate analyses if not removed. At present seem to be distinct from CSV_QU_START and CSV_QU_END above...


# deprecated method -- currently just using csv reader.
# @param line : line to be parsed
# parse the line while handling issues that might arise due to the use of the delimiter within what is actually a single column
# because (e.g.) it is used in quoted text.
def parse_line(line, csv=True, strip_default_enclosing_quotes=True, etymology_col=5):
    if strip_default_enclosing_quotes:
        # currently, this is operating under the assumption that any use of the delimiter character in a way that does not delimit
        #   columns will be within a quoted string, with all cells in the csv enlcoses by CELL_(START;END)_QU.
        #   But note that there may be more complicated stuff involved where CSV_QU_START and CSV_QU_END are involved.
        #   pandas had a weird error. May have to revisit this.
        output = line.split(CELL_END_QU + INP_DELIM + CELL_START_QU)
        output[etymology_col] = output[etymology_col].replace(INP_DELIM, ";:;")
        output = (CELL_END_QU + INP_DELIM + CELL_START_QU).join(output).split(INP_DELIM)
        output = [cell[1:-1] if cell[0] == CELL_START_QU else cell for cell in output]
        return output

    # code below here is essentially legacy and will not be reached along as strip_default_enclosing_quotes is True
    output, input_left = [], "" + line
    need_to_finish_last_col = False

    while CSV_QU_START in input_left:
        quote_start = input_left.index(CSV_QU_START)

        # absorb columns before column with quote
        last_col_break_pre_qu = input_left[:quote_start].rfind(INP_DELIM)
        if last_col_break_pre_qu != -1:
            to_add = input_left[:last_col_break_pre_qu].split(INP_DELIM)
            input_left = input_left[last_col_break_pre_qu + 1:]

            if need_to_finish_last_col:
                output[-1] += to_add.pop(0)
                need_to_finish_last_col = False
            if len(to_add) > 0:
                output += to_add

        if CSV_QU_END not in input_left:
            pdb.set_trace()
            raise RuntimeError("Error in input line -- unclosed quotation. Original line : " + line)

        quote_end = input_left.index(CSV_QU_END)
        if need_to_finish_last_col:
            output[-1] += input_left[:quote_end + 1]
        else:
            output += [input_left[:quote_end + 1]]
        input_left = input_left[quote_end + 1:]

        need_to_finish_last_col = input_left.find(INP_DELIM) != 0
        if not need_to_finish_last_col:
            input_left = input_left[1:]

    if len(input_left) > 0:
        to_add = input_left.split(INP_DELIM)
        if need_to_finish_last_col:
            output[-1] += to_add.pop(0)
        if len(to_add) > 0:
            output += to_add

    output = [cell[1:-1] if cell[0] == CELL_START_QU else cell for cell in output]
    return output
