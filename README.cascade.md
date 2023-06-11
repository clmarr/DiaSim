# README.cascade :

This file details how to properly construct a cascade file so that it can be read by DiaSim.

The file should be a ".txt" file, with each non-blank and non-commented line consisting of either a rule or a stage flag. 

# MOTIVATIONS:
DiaSim operates in the Neogrammarian tradition.
Thus, it sees diachronic phonology as primarily consisting of an ordered set of algorithmic sound rules. 
The set is ordered in that each of its rules operates after the one before it and before the one after it. 
This fact is of special importance where bleeding, feeding, counterbleeding, and counterfeeding phenomena are concerned.
Each of these diachronic rules is considered by the Neogrammarian tradition (and thus DiaSim) to be regular.
This means that they effect a distinct segment in a predictable way, perhaps operating only in a certain context. 
When "rules" are not regular in ways not "regularizable" by specifying context, they are held to be due to various non-phonetic motivations, including syntactic interference, analogy, sociolinguistic prestige, restoration, hypercorrection, folk analogy, morphological interference, and so forth.
These exceptional cases are not of interest for the use of DiaSim and are (unfortunately) impossible to model using only phonological means; DiaSim is a purely phonological model. 
The ruleset file is precisely this ordered set of rules, additionally with ordered declarations of "stages" where they exist among the rules optionally in use, if the user so desires.

# STAGE FLAGGING: 

Stages are flagged on their own line, between rules, in the cascade file, to indicate at what point the phonological forms of each etyma are to be treated as those at a "stage", a certain point in diachronic "time" relative to the chronolgoy of different rules. 


```
~Middle French
```

If a stage with the same name is in the column header of the lexicon file (see README.lexicon.md), this will be treated as a "gold" stage, upon which systematic comparison between forward-reconstructed forms and the "gold" forms provided in the lexicon can be performed. It will otherwise be treated as a "black" stage (black box stage), which exists for purposes of easy extraction of forms and grounding for analyses, but does not have associated "gold" forms. 
Technically '~' is the gold stagename flag, but it will only be treated as a true gold stage if an identically named stage is present as a column header in the lexicon file; otherwise the stage will be "blackened" -- converted into a black box stage -- before computerized forward reconstruction commences. 
The black stage header "=" may be used instead of "~" for disambiguatory purposes within the lexicon file.

Do not flag the first stage name as this will cause issues as this is the name for the input, not a gold stage. In this way, in the CLEF cascades, there is no flag anywhere for Classical Latin, which is the input stage, before any rules are applied. 
Similarly, do not flag the name of the last stage in the lexicon if it is also the output stage (using a last stage in the lexicon that is not the output stage may be confusing, but you an do so: in this case, it should be flagged at a point such that at least one rule remains below it). 


# COMMENTING:
One flags a comment by using the dollar sign "$", just like in the lexicon file.
After seeing this flag, the system erases from its memory the character and everything on the same line to the right of it.
Hence a line that starts with the comment flag "$" will be considered blank by DiaSim.

# RULE FORMAT: 
All rules (to reiterate -- each being exactly one line) should be of the following format: 

```
Target Source > Destination / Prior Context __ Posterior Context
```

There are seven elements to note here: the input or *target source*, the *arrow* ">", the output or *destination*, the conditioning flag "/", the *prior context*, the *locus* ("__"), and the *posterior context*. 
Each of these elements should be delimited by from the following or preceding one by a space. 

This notation for a diachronic sound change is that which is most commonly used in historical phonology, and, in most of its aspects, agrees also in form with the format of synchronic phonological rules as well. 
The material before (but not including) the */* is the **transformation**, which stipulates what is changed, and what it changes into.
The transformation is mandatory for all sound changes; it is further described in the section below. 

After the "/" lies the **conditioning context**, which stipulates where the sound change happens, in terms of nearby phonetic material. 
This only, and only this, flags the beginning of the conditioning context specifications, which indicate where the rule operates, in terms of the neighboring phonetic material, in terms of exact phone symbols or their broad phonetic properties, in terms of features (as will be discussed below in *Supported notation for phonetic information*). 
Whether to include information on a conditioning context depends on whether or not the phonetic context matters: that is to say, whether the sound change you want to write is *conditioned*, or *unconditioned*. 
Some sound changes are *unconditioned*: for example, the ultimate reflex of Latin R in French, which was originally coronal, ultimately became uvular everywhere it occurred, irrespective of what was or was not nearby (that is to say, it was **unconditioned*). 
For such *unconditioned sound changes*, no conditioning context is to be specified, and the "/" is not to be used. 
On the other hand, for those sound changes that do indeed only occur in certain phonetic contexts (the majority of sound changes), the conditioning context must be explicitly specified, and must be flagged with "/". 
Another example of an unconditioned sound change -- h-deletion -- is shown below. 

```
h > ∅
```

The loss of phonemic vowel length is also an unconditioned sound change, as seen below, in terms of *phonetic features* (see respective section. 

```
[+syl] > [-long] 
```

A coalescence of a sequence of two sounds to a single one, like the sound law forming the palatal lateral [ʎ] shown below, is also often (though far from *always*) unconditioned... 

```
l ʝ > ʎ
```

One important thing to note is that the "/" can be used *exclusively* to delimit the transformation from its conditioning context.
While in (especially synchronic) phonological analyses, a pair of "/" may surround a symbol (typically in IPA) to denote that it is a *phoneme*, this notation is not to be used here.
Similarly, for reasons elaborated below (*Symbol and feature notation notes*), the square brackets *[* and *]* are not to be used to denote *phonetic realizations* (*phones*); this is a diachronic system, and those symbols are used by the system to identify notation in terms of *phonetic features*, as will be described below (*Phonetic feature notation*). 


## THE TRANSFORMATION, AND USAGE OF ">":

Before the ">" lies the input *source* material: the earlier form or set of forms which the sound change will cause to mutate. 
After the ">" lies the output *destination* material, which determines how the input will be mutated.
As will be discussed below (*Supported notation for phonetic information*), DiaSim supports not only "phone symbol to phone symbol" transformations, but also ones that transform sequences of phones, ones that map multiple options of inputs onto their respective outputs, as well as ones written in terms of *phonetic features* as well as *alpha notation*, and all of these are also supported as well in the *conditioning context*. 
The ">" is the *arrow* which signifies diachronic transformation from the input *source* form (before it) to the output *destination* form.
DiaSim is a diachronic system, and one where all steps must be explicated. 
Thus, only ">" is to be used here: not the synchronic "->" for phonological rules mapping "*underlying*" forms to surface realizations, or the *long distance* (with intermediate steps implicit) diachronic "> ... >" notation introduced by Janda and Joseph 2003. 
 

## THE CONDITIONING CONTEXT, AND USAGE OF "__": 

As noted before, a conditioning context must be used if and only if the rule you are writing is *conditioned*. 
Use "/" if and only if the rule is *conditioned*; all of the *transformation* must lie before it, and all of the *conditioning context* must lie *after* it. 
The one element of the notation for the conditioning context that must always be present is the *locus*, which must be written with **two underscores** ("__"). 

Beyond this there must be either a prior context specified (i.e. phonetic information about what comes *before* the sound or sounds being mutated), a posterior context specified (i.e. about what comes *afterward*), or both, but *never neither* (which would be either errant underspecification of a conditioned sound change, or errant notation of an unconditioned sound change). 

Only a prior context will specified for a sound change conditioned only on earlier material, such as the closure of the reflex of *w*- sounds loaned into Proto-Gallo-Romance ([ɣʷ]) to the voiced velar stop [ɡ] word-initially except in some dialects like Norman, which led to minimal pairs in English like *warranty* and *guarantee*, etc. (here, phone symbols are used, and "#" marks the word boundary -- see *Supported notation for phonetic information*): 

```
ɣʷ > ɡ / # __
```

Only a posterior context will be specified for a sound change conditioned only on later material, such as the regressive nasalization rule affecting low vowels of 10th century Early Old French displayed below, or the darkening of L before consonants beginning in the 9th century (with phonetic feature notation used -- see *Supported notation for phonetic information*): 

```
[+lo] > [+nas] / __ [+nas,-syl]
```

```
[+lat] > ɫ / __ [+cons]
```

Another example would be the cluster simplification of /skl/ to /sl/ in the predecessor to French -- hence why Latin MASCVLVS became Early Old French *masle* (and not *mascle*, as it would have otherwise...), whence *mâle* "male". This happened at the same time as /rgl/ > /rl/ cluster simplification as per Pope 1934, so they are placed in a single disjunctive rule, making use of the curly braces as supported for this by DiaSim, and used elsewhere in the field (see the appropriate subsections of *Supported notation for phonetic information*): 

```
{s k;r ɡ} > {s;r} / __ l
```


Many sound changes, however, will have both prior and posterior conditioning. 
For example, the early loss of /w/ between /k/ and a non-low front vowel at word beginnings, early enough to allow palalatalization of the earlier /k/ (hence Latin QUINQUE > French *cinq* /sɛ̃k/), as seen below, with the null (∅) symbol used as per *Symbols for non-phones*...: 

```
w > ∅ / k __ [+front,-lo]
```

Another example would be the earliest stages of the first Western Romance lenition, which could be attested as early as the 2nd century AD, and likely saw Latin B become (at first) a bilabial fricative [β] when after a vowel and before a vowel or R, as seen below... 

```
b > β / [+syl] __ [+son,-lat,-nas]
```

Likewise, the opening of final voiceless velar stops [k] to fricatives [x] word-finally (i.e. the posterior context being a word bound) when ``unsupported'', i.e. after vowels (i.e. the prior context being a syllabic element in this case)... 

```
k > x / [+syl] __ # 
```

### THE IMPORTANCE OF SPACING

Note that spaces necessarily delimit all functional symbols, phonetic/phonological or otherwise. Material not separated by a space is in most cases considered part of the same "symbol" functionally, and a space means items on either side will be considered functionally separate.
This applies to the lexicon files as well. 


### PHONETIC SYMBOLS AND FEATURES:

(elaborate...) 

how to modify... 

SymbolDefs.csv
FeatTranslations
FeatImplications


### SYMBOLS FOR NON-PHONES (BOUNDARY MARKERS, NULL, ETC):


Non-phonetic symbols used include the following: 

* '∅' ("null") is used to indicate deletion if after the arrow ('>') or insertion if before it. It is important not to confuse this with 'ø', which, in IPA and SymbolDefs.csv (the provided default symbol definitions file) is used for the mid-high front rounded vowel. 

* '#' is used to mark a word boundary 

* '+' is used to mark a morphemic boundary (as of June 2023 is currently not in use and may be subject to change).

* '@' is used to mean 'anything except a word boundary'. 

* '[' and ']' enclose a feature matrix. Features therein are written in terms of '+' or '-' and then the feature (abbreviated) name as used in SymbolDefs.csv (or a file used in its stead, declared in the functional call), and are delimited by ','. For more info, see the section above ("PHONETIC SYMBOLS and FEATURES").

* items (phones, boundary symbols...) or sequences thereof between '(' and ')' can optionally occur never or once (this can be used in the input, or the context)

* items or sequences between '(' and ')*' can optionally never or any number of times

* items or sequences between '(' and ')+' can optionally occur any number of times, but not never. This is a case where the (non-)use of a space (' ') is critical, as ') +' means the close of optional never or once clause followed by a morpheme boundary, whereas ')+' closes an "any positive number of times" clause as stipulated in this bulleted explanation. 

* '{' and '}' enclose a *disjunction*. A disjunction consists of two or more items or item sequences, delimited by ';'. 


(TODO make this its own section) 

### ALPHA FEATURE NOTATION:

Alpha features are used to ... 
(TODO elaborate...) 

