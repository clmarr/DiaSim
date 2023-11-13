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
Technically '\~' is the gold stagename flag, but it will only be treated as a true gold stage if an identically named stage is present as a column header in the lexicon file; otherwise the stage will be "blackened" -- converted into a black box stage -- before computerized forward reconstruction commences. 
The black stage header "=" may be used instead of "~" for disambiguatory purposes within the lexicon file.

Do not flag the first stage name as this will cause issues as this is the name for the input, not a gold stage. In this way, in the CLEF cascades, there is no flag anywhere for Classical Latin, which is the input stage, before any rules are applied. 
Similarly, do not flag the name of the last stage in the lexicon if it is also the output stage (using a last stage in the lexicon that is not the output stage may be confusing, but you can do so: in this case, it should be flagged at a point such that at least one rule remains below it). 


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

The loss of phonemic vowel length is also an unconditioned sound change, as seen below, in terms of *phonetic features* (see respective section -- TODO identify section) 

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

Only a prior context will specified for a sound change conditioned only on earlier material, such as the closure of the reflex of *w*- sounds loaned into Proto-Gallo-Romance ([ɣʷ]) to the voiced velar stop [ɡ] word-initially except in some dialects like Norman, which led to minimal pairs in English like *warranty* and *guarantee*, etc. (here, phone symbols are used, and "#" marks the word boundary -- see the following section on non-phonetic notation): 

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

Another example would be the cluster simplification of /skl/ to /sl/ in the predecessor to French -- hence why Latin MASCVLVS became Early Old French *masle* (and not *mascle*, as it would have otherwise), whence *mâle* "male". This happened at the same time as /rgl/ > /rl/ cluster simplification as per Pope 1934, so they are placed in a single disjunctive rule, making use of the curly braces as supported for this by DiaSim, and used elsewhere in the field (see the appropriate subsections of *Supported notation for phonetic information*): 

```
{s k;r ɡ} > {s;r} / __ l
```

Many sound changes, however, will have both prior and posterior conditioning. 
For example, the early loss of /w/ between /k/ and a non-low front vowel at word beginnings, early enough to allow palatalization of the earlier /k/ (hence Latin QUINQUE > French *cinq* /sɛ̃k/), as seen below, with the null (∅) symbol used as per *Symbols for non-phones*...: 

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

## THE IMPORTANCE OF SPACING

Note that spaces necessarily delimit all functional symbols, phonetic/phonological or otherwise. Material not separated by a space is in most cases considered part of the same "symbol" functionally, and a space means items on either side will be considered functionally separate.
This applies to the lexicon files as well. 

## SYMBOLS FOR NON-PHONES:


Non-phonetic symbols used are each explained in their respective sections below. Some example rules here use feature matrices (e.g. "[+syl,+front]" -- a front vowel). See the respective section if this notation is unclear. 

### Null symbol '∅' 

The symbol '∅' ("null") is used to indicate deletion if after the arrow ('>') or insertion if before it. It is important not to confuse this with 'ø', which, in IPA and SymbolDefs.csv (the provided default symbol definitions file) is used for the mid-high front rounded vowel. 
Usage in the input signifies insertion, and in the output it is used for deletion.
It has no usage in the conditioning context. 
Its usage for a hypothetical unconditioned deletion of /h/ and a hypothetical insertion of schwa between consecutive consonantals are shown below: 

```
h > ∅ 
∅ > ə / [+cons] __ [+cons] 
```

### Word boundary '#'
The symbol '#' is used to mark a word boundary. For example, a rule to express word-final obstruent devoicing would be as follows: 

```
[-son] > [-voi] / __ #
```

Likewise, a word initial hardening rule making /j/ into a palatal stop /ɟ/ is demonstrated below: 

```
j > ɟ / # __ 
```

### Morpheme boundary '+' 

The symbol '+' is used to mark a morphemic boundary. This symbol is not used in any of the -CLEF cascades as Pope 1934 did not assert any sound changes that were conditioned in terms of a morpheme boundary (unlike a word boundary) and it did not seem necessary for the debugging process in making DiaCLEF. Unlike processes conditioned on word boundaries, that sound changes have ever been or could ever be conditioned on morpheme boundaries is contentious ; this is part of a broader debate concerning the possibility of grammatically conditioned sound change. 
Both reports of such morpheme-boundary conditioned phenomena and competing analyses that offer other explanations for these abound (see Hill 2014; Enger 2013; Anttila 1972; Hock 1976). 
In addition to views that the possibility of morpheme-boundary-conditioned sound change is either impossible (Kiparsky 1973) or necessarily possible (Hyman & Moxley 1996; Anttila 1972), another view asserts that sound change necessarily originates in exclusively phonetically-motivated (synchronic) processes that cannot be morphologically conditioned *at first* but can acquire such extra-phonological conditioning (including in morphology) as it passes from synchrony into diachrony (Janda & Joseph 2003). 

DiaSim is built in the Neogrammarian tradition but is not intended to be restricted to those who adhere to any side of this debate: the user can opt to use or not use the morpheme bound. 
For how to use it correctly, we will refer to one well-known proposal of a morpheme-bound conditioned sound change in a branch of Bantu, whereby /k/ palatalized before /i/, across a morpheme boundary (Hyman & Moxley 1996: 274). This would be expressed as follows ([c] being a voiceless palatal stop): 

```
k > c / + __ i
```

Further examples in this section will use synchronic processes, whose conditioning on morphological factors is less controversial as they are not claimed to constitute proper sound changes, but they are intended to illustrate how to express morphological conditioning in the same environments *diachronically* should it be deemed necessarily. 

Note that some processes that are described as operating at the morpheme boundary in fact operate at the morpheme boundary *and* the word boundary. 
Although we are not aware of any cases where this became diachronically relevant, in case such a case arises, this must be explicitly specified using a bracketed disjunction. 
For example, many English varieties are analyzed as having a *synchronic* rule that inserts a glide before a morpheme boundary *or* a word boundary; typically this occurs after vowels that are high (or underlyingly high -- see Uffmann 2010), but in non-rhotic dialects [ɹ] may be inserted after non-high vowels (in many dialects, especially /ɔ/ and/or /ə/), leading to [ɹ]-insertion both before a morpheme boundary ("draw-[ɹ]-ing") and at a word boundary ("law-[ɹ]-and order"). Thus, to capture this behavior, should it for some reason become necessary for a diachronic system for reasons unclear at present but not impossible, the following rule formulation would be necessary, with a *bracketed disjunction* (see below) clarifying that this sandhi process operates across either a word boundary or a morpheme boundary. 

```
∅ > ɹ / [+syl,-hi] __ {+;#} [+syl] 
```

This bracketed disjunction is likewise the way to express analyses that treat Spanish e-prosthesis as both word-initial and morpheme-initial given cases like *Checoeslovaquia* (Janda & Joseph 2003): 

```
∅ > e / {+;#} __ s [+cons] 
```

Note also that '+' notation is not to be used for processes that, while perhaps initially phonetically motivated, are conditioned on specific morphemes and not others in a way that is no longer phonetically or even phonologically predictable. 
For example, consider the case of the (synchronic) Jita rhotic assibiliation process (Downing 2007). This process changes /ɾ/ into /s/ before a set of morphemes starting in /i/ or /j/ cannot actually be represented by a (synchronic) rule "ɾ -> s / __ + [+hi,+front]", because there are other morphemes starting in /i/ or /j/ that do not trigger assibilation. In this case, the process clearly involves morphological knowledge of which affixes (don't) trigger the process, (Hamann 2014) and thus it lies outside the purview of DiaSim, a diachronic phonological system. 

### Amorphous segmental '@' 

The symbol '@' ("segment") is used to mean 'anything except a word boundary'. 

For example, the outcome of French /v/ for Latin "v", which is /w/ in many accounts but /ɣ̬ʷ/ for Pope 1934(:s192ii), is demonstrated below: 

```
ɣʷ > v / @ __ 
```

Essentially "@" means "there is something, anythign, segmental between this spot and the nearest word boundary". A rule conditioned on it will still operate if a word boundary is immediately at its specified location. As thus it is not usable for expressing proposed morphological boundary conditioning as '+' is (see above). At present there is no way in DiaSim to express conditioning in "anything other than a morpheme boundary (or word boundary)", but should this become necessary, please request it, as it is expected to be quite easy to add.  

### Optionality parentheses for once or never: (...) 

Items (phones, boundary symbols...) or sequences thereof between '(' and ')' can optionally occur never or once. 
This can be used in the conditioning context, but cannot be used for the output side of the arrow as rules in DiaSim are deterministic. 


Usage in the conditioning context, the more frequent usage, means the rule can "see through" the parenthesized material so that it will still operate if the necessary conditioning elements are on the other side of it. 
This is seen in the centralization of countertonic /e/ in Early Old French, which occurred before either another vowel, or a consonant then another vowel, as shown below:

```
ˌe > ˌə / __ ([+cons]) [+syl]
```

A classic example would be West Romance/Brythonic intervocalic lenition, which "saw through" posterior liquids so that -VCRV- segments were still treated as between vowels. While this process involved both voicing and spirantization, the former alone is demonstrated below:  

``` 
[-voi] > [+voi] / [-cons] __ ({r;l}) [-cons]  
```

Usage on the input side of the arrow will mean that the material enclosed will be absorbed into the scope of the segment *sequence* that is replaced (deterministically) with the material on the output side. 
An example in a k-palatalization process that absorbs a prior /j/: is shown below: 

```
(j) k > c / __ [+syl,+front]
```

Note however that as of August 2023 the capacity to use optionality parentheses in the input is blocked as it is unclear that it would operate without errors. 
Instead, if feature matrices are not involved (see parenthetial statement below) , disjunctions should should instead to accomplish the same transformation. 
(Feature matrices and disjunctions also cannot currently be implemented at the same time in the input; hopefully this will soon be fixed. )
For example the above rule would be formulated as follows below: 

```
{j k;k} > {c;c} / __ [+syl,+front] 
```

The disjunctions should be ordered so that the more specific scenario comes first, as otherwise the more general scenario will effectively bleed it. 

### Optionality parentheses for never or any number of repeats: (...)*

Items or sequences between '(' and ')*' can optionally never, once, or any number of times. 
For a usage example, regressive vowel harmony is a process whereby vowels change under the influence of the next vowel, "seeing through" any number of consonantals in the process. 
This can be expressed, using an example of fronting harmony (i.e. "umlaut", "i-mutation", seen historically in Germanic, Turkic, Finnic, etc. in different ways), as follows: 

```
[+syl] > [+front] / __ ([+cons])* [-cons,+front] 
```

### "Any number of repeats" clause (...)+

Items or sequences between '(' and ')+' can optionally occur any number of times, but not never. This is a case where the (non-)use of a space (' ') is critical, as ') +' means the close of optional never or once clause followed by a morpheme boundary, whereas ')+' closes an "any positive number of times" clause as stipulated in this bulleted explanation. 

(TODO examples) 

* '{' and '}' enclose a *disjunction*. A disjunction consists of two or more items or item sequences, delimited by ';'.  (TODO difference in disjunction behavior in input/output, vs. conditioning context.)
	(TODO make this its own section? ) 

* '[' and ']' enclose a *feature matrix*. Features therein are written in terms of '+' or '-' and then the feature (abbreviated) name as used in SymbolDefs.csv (or a file used in its stead, declared in the functional call), and are delimited by ','. For more info, see the section "PHONETIC SYMBOLS AND FEATURES" and its subsection "FEATURE MATRICES".


## PHONETIC SYMBOLS AND FEATURES:

(TODO elaborate...) 

how to modify... 

SymbolDefs.csv
FeatTranslations
FeatImplications

(not committing to features as innate despite the choice of features favored as innate by some nativists -- not a theoretical stance)

(citations on features? )

(user can use their own) 

### FEATURE MATRICES: 

TODO explanation

Note: currently, feature matrices cannot be used within a disjunction except in the context specification. 
The ability put feature matrices in disjunctions in an input will hopefully be added soon. 
Doing so for the destination is probably not advisable and will not be implemented -- these are best handled with separate rules, which reflects the fact that they probably operate separately anyways. (if this found to be not true, this may be amended) 


### ALPHA FEATURE NOTATION:
While in the default  (TODO finish...) 

The use of features valued with 


Alpha features are a notation device used by phonologists to indicate a linkage between different feature-value mapping. 


(TODO rework 
(TODO 1 explain alpha features)
	(1a alpha features within the same feature matrix -- agreement -- Surselvan example -- alas the author there finds it awkward) 
	(1b alpha features across between feature matrices)
		(1b between output and context -- assimilaton)
		(1c between input and output -- French case from DiaCLEF )
		
	multiple alpha features... 
		
	(negative alpha values...) 
	(TODO need to ensure coverage of negated alpha values works)
		(negated alpha values in debugging suite...) 
		
	
Alpha features are a device used by phonologists to indicate that the multiple feature 


express feature spreading phenomena that occurs regardless of the specific features that are being spread. 
Assimilatory phenomena (both contact and long distance) often such patterns.
This is economic, as otherwise separate rules for the spreading of the '+' and '-' values respectively would be necessary.  

(TODO examples...) 

(TODO also used for feature preservation in movement: y(βstres) j > ɥ i(βstres) case from 13th century French from CLEF 

(TODO Surselvan example p37 for example of alpha features only input expressing "same value of round as for back" -- if round has to be back, if unround cannot be back, for it to be a valid target of the rule) 

(TODO negative alpha notation as in Akan example -- p34 from that Schachter and Fromkin) 

(2 as in DiaSim -

	
(TODO elaborate...) 


(TODO -- negative alpha feats not currently supported) 

# Cited works

Anttila 1972 = Anttila, R. 1972. "An introduction to historical and comparative linguistics". New York: MacMillan. 

Downing 2007 = Downing, L. 2007. "Explaining the role of the morphological continuum in Bantu spirantization". *Africana Linguistica* 13: 53-78. 

Enger 2013 = Enger, H-O. 2013 "Inflectional change, 'sound laws' and the autonomy of morphology: the case of Scandinavian case and gender reduction". *Diachronica* 30(1): 1-26. 

Hamann 2014 = Hamann, S. 2014. "Phonological changes". In Bowern, C. and Evans, B. (eds.), *The Routledge Handbook of Historical Linguistics*. 

Hill 2014 = Hill, N. W. 2014. "Grammatically conditioned sound change". *Language and Linguistics Compass* 8(6): 211-229.

Hock 1976 = Hock, H. H. 1976. Review of An introduction to historical and comparative linguiistics by Raimo Anttila. *Language* 52(1): 202-220. 

Hyman & Moxley 1996 = Hyman, L. and Moxley, J. 1996. "The morpheme in phonological change: velar palatalization in Bantu." *Diachronica* 13(2): 259-82.  

Janda & Joseph 2003 = Janda, R. & Joseph, B. D. 2003. "Reconsidering the Canons of Sound CHange: Towards a 'Big Bang' Theory". In Blake, B. and Burridge, K. (eds), *Selected Papers from the 15th International Conference on Historical Linguistics, Melbourne, 13--17 August 2001*. Amsterdam: John Benjamins Co. Pp205-219. 

Kiparsky 1973 = Kiparsky, P. "Abstractness, opacity and global rules. Three dimensions of linguistic theory". Ed. by Fujimura, O. Tokyo: TEC. 57-86. 

Uffmann 2010 = Uffmann, C. 2010. "The non-trivialness of segmental representations". Paper presented at *Old World Conference on Phonology (OCP) 7*, Nice. 
