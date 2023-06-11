# README.lexicon:

This file details how to properly construct a lexicon file so that it can be read by DiaSym.
 
The file should be a tab delimited value file with the extension .tsv.

# THE ROWS

Each row should represent the development, over the different diachronic stages, of an etymon, with each stage being indicated by a column.


# COMMENTING

Any and all comments can only be placed in the final column and must be flagged with a "$". Typically they are used to cite the phonological forms and etymology of a word. For example, from our demonstration simulation for Classical Latin going to French: 

```
s ˌɑ ɡ m ˈɑː r i u m	s ɔ m j e $sommier. Pope s674.
```

# THE COLUMNS

Each column represents a different diachronic stage. 
If any form is given for a particular diachronic stage, a form must be given for every etymon at that stage.
In future versions of DiaSim, it will be possible to indicate that an etymon has either fallen out of use or entered the vocabulary late by specifying "..." for stages where it is not present in the lexicon -- however, this feature is not supported by the current release of DiaSim. 

The first column, the only required one, must contain the initial forms for each etymon at the beginning of the diachronic simulation. 
Specifically, in the case of our French demonstration set, this is the Classical Latin form. 
When and if other columns are specified, they should contain the phonological forms each etymon has at the stage corresponding to the column.
The forms entered in these columns will be the gold standard phonological forms to which the forms obtained by the diachronic simulation at those stages will be compared. 
In order for simulation results at any point to be compared to a gold set, that set must be associated with a stage in the rules file and a column in the lexicon file, with its gold forms for each etymon in the lexicon file. 
Without exception, each column specified must uniquely correspond to a diachronic stage that is declared in the rules file (see rules.README.txt -- a stage should be on a line declared immediately after the last rule that applied to its forms). In the rules file, a proper diachronic stage that will be compared to gold must be flagged with a '~' character, as demonstrated below: 

```
~Middle French
```

Stages where the forms of etyma are just to be recorded, not compared, are instead flagged with a "=" character.

DiaSim will automatically associate stages in the rules file, in the order they are declared, to columns other than the input column, from left to right. 
If there is exactly one more column in use after all declared stages are associated with columns to its left, then the system will assume that this is the specification of the final output.
If there are still columns to the right of this final column, then DiaSim will throw an error. 
Likewise, if there are too few columns so that not every stage declared in the rules file is associated with a column, then DiaSim will also throw an error. 

Here is a sample row, showing the diachronic trajectory of Classical Latin *sagmarium* into French *sommier*, via Popular Latin, Old French, Later Old French, Middle French and Modern French. 

```
s ˌɑ ɡ m ˈɑː r i u m	s ˌɑ w m ˈɑ r ʝ o	s ˌo m ˈi ɛ̯ r	s ˌũ m j ˈe r	s ˌũ m j ˈe	s ɔ m j e $sommier. Pope s674. 	
```

# MORPHOLOGICAL PARADIGM INFORMATION 

As of June 2023, expansion of DiaSim to handle paradigmatic relations is underway. (TODO update when complete) 

For how to access automated paradigm based analyses making use of this info, this is handled in the *Paradigmatic analysis* section of README.suite.md. Note that inclusion of paradigmatic info in a line of the lexicon file is optional, and that the statistical analyses performed after and during a CFR run will only take into account items for which paradigmatic information is marked. 

Once the expansion of DiaSim to handle such paradigmatic info is completed and released, morphological info will be marked in lexicon files as follows: 

* '%' will be the flag for paradigmatic info within a line in the lexicon. Inclusion will be optional. The paradigmatic information will be placed between the diachronic phonology part of the line (i.e. input and then gold forms for stages and final output if applicable), and the comment clause flagged by '$'. Thus it will look as follows: 

```
d̪ ˈo r m i t̪ , d̪ ɔ ʁ %VERB<DORMIRE> {POS=VERB | PERSON=3 | NUMBER=SG} $dort.
ˈo k u l u m , œ j %NOUN<OCVLVM> {POS=NOUN | GENDER=MASC} $œil.  
ˈo k u l oː s , j ø %NOUN<OCVLVM> {GENDER=MASC | NUMBER=PL} $yeux.  
d̪ ˌo r m ˈiː r e , d̪ ɔ ʁ m i ʁ %VERB<DORMIRE> {POS=INF} $dort.
s ˌɑ p i ˈɑː t i s , s a ʃ j e %VERB<SAPERE> {MOOD=SUBJ | PERSON=2 | NUMBER=PL} $sachiez 
s ˌɑ p ˈuː t ɑː s , s y %VERB<SAPERE> {POS=ADJ | GENDER=FEM | NUMBER=PL} $sues. 
```

In each line above, again, paradigmatic info is found after the paradigm flag '%' and before the comment flag '$'. 
The paradigmatic info here consists of two components, in the following order: (1) the *lemma ID*, placed between '<' and '>' and flagged beforehand by the lemma's morpho-lexical category and (2) the *morphosyntactic feature-value clause*. 

## LEMMA ID 
The *lemma ID*, found between '<' and '>', contains a string that is a unique lemma for all forms that are considered to belong to the same paradigm -- those that are to be considered forms of the "same word" by DiaSim. 
It is flagged beforehand by the lemma's morpho-lexical category, which is potentially distinct from its morpho-*syntactic* category (see below, in next section). 
Abbreviations are enabled in the paradigm shape file (see two sections below), so either V or VERB can be used for verbs, and either N or NOMINAL can be used for nominals, a term that unites nouns and adjectives as (in the case of French) they share paradigm shape. 
Thus, as seen above, the second line (for *œil*) ad the third (*yeux*) share the same lemma ID (N<OCVLVM>), but this ID is not shared with the first line (*dort*), as that is not a form of the same word; instead the lemma ID of *dort* would probably be shared with lines for *dormir*, *dors*, et cetera. 

Note: it is advisable to avoid giving to give homophonous lemmata with the same morpholexical category the same lemma ID -- for example, Latin liber "free" and liber "book" are best given different lemma IDs like "LIBER1" and "LIBER2" (for the same reason, Latin based lemma IDs are preferred for Romance CFRs, because sound change can regularly create new homophony, but homophone splits do not regularly occur). This important because otherwise it will lead to unintended side effects in how paradigmatic stats are calculated: the erroneously conflated paradigms with homophonous lemmata will be considered the same, so any items with the same morphosyntactic feature specifications will be treated as being in *overabundant* cells within the same paradigm (erroneously increasing cell-wise rates of overabundance) and the two paradigms together with all their cells) will only contribute with a weight of one paradigm to statistical calculations performed for automated analyses by DiaSim. It is preferable to avoid giving the same lemma ID to distinct lemmata with different morpholexical categories but homophonous citation forms, but DiaSim will still handle it differently since it internally appends each lemma ID with its morpholexical category. 

A lemma's morpholexical category describes the lexical category of its root, and thus the shape of its paradigm. This is usually equivalent to its morphosyntactic category, which governs the word's syntactic distribution and agreement behavior, but there are cases where the two are not equivalent. For example, the French (morpholexical) verb paradigm is generally considered to include past participles (like *sues*, the last line) and infinitives (*dormir*, the fourth line), but past participles are morphosyntactically most like adjectives (inflecting as such as well), while the morphosyntactic behavior of infinitives is rather distinct and lacks the agreement marking (person, number) seen on morphosyntactic verbal items, as well as lacking other features present in other verbal forms like tense. As will be explained in the following section, the calculation of stats for feature value combinations will take such factors of morphosyntactically distinct parts of morpholexical paradigms into account.  



## MORPHOSYNTACTIC FEATURE-VALUE CLAUSE
TODO -- features defined wrt morphosyntactic classes? 

The *morphosyntactic feature-value clause*, bounded by '{' and '}', is placed between the lemma ID and the comment clause (if present, otherwise the end of the line), and consists of assignments of morphosyntactic values to morphosyntactic feature.
Each morphosyntactic feature is delimited from its value by '='; different feature-value assignments are delimited by '|'. The features here are morphosyntactic features, not semantic ones (i.e. a grammatically singular collective noun is thus singular, not plural; German *mädchen" is neuter, not feminine). 

The examples from the top of this section are reiterated here for easier reading: 

```
d̪ ˈo r m i t̪ , d̪ ɔ ʁ %VERB<DORMIRE> {POS=VERB | PERSON=3 | NUMBER=SG} $dort.
ˈo k u l u m , œ j %NOMINAL<OCVLVM> {POS=NOUN | GENDER=MASC} $œil.  
ˈo k u l oː s , j ø %N<OCVLVM> {GENDER=MASC | NUMBER=PL} $yeux.  
d̪ ˌo r m ˈiː r e , d̪ ɔ ʁ m i ʁ %V<DORMIRE> {POS=INF} $dort.
s ˌɑ p i ˈɑː t i s , s a ʃ j e %V<SAPERE> {MOOD=SUBJ | PERSON=2 | NUMBER=PL} $sachiez 
s ˌɑ p ˈuː t ɑː s , s y %V<SAPERE> {TENSE=PAST | POS=ADJ | GENDER=FEM | NUMBER=PL} $sues. 
```

As can be seen for the first line of the example above, *dort* is marked as having the value "VERB" for the feature "POS", "3" for "PERSON", and "SG" for "NUMBER". There is no marking for the fact that it is also present tense, and indicative mood: these are unnecessary as they are the default values for the features 'TENSE' and 'MOOD', which are assigned by default if they are not marked within the line of the lexicon file. Furthermore, note that in the second line of the example given at the top of this section, notice that *œil* is not marked as SG -- SG is actually the default for NUMBER, and doesn't need to be marked, though it *can* be if seen as desirable, as it is above for *dort*. However, the PL value for NUMBER must always be marked for this is a marked (non-default) value for the feature NUMBER. The usage of marked and unmarked here does not reflect a theoretical position, but rather a computational description of the behavior of DiaSim, not the behavior of language:  features that are computationally treated as default are thus functionally treated as *unmarked* by DiaSim not needing to be *marked*, and the rest thus need to be "*marked*" in the lexicon. 

Regarding "POS" (for *part of speech*), as noted in the section above, this is part of how DiaSim treats cells that differ morphosyntactically from their lemma's morpholexical class.
A form having a different morphosyntactic behavior from its morpholexical class is considered *marked*-- it must be specified as the value for the feature "POS" in the morphosyntactic feature-value clause. 
For example, this is necessary for French past participles (as in *sues*, last line of the examples) as they are morphosyntactically (most like) adjectives, and for infinitives (*dormir*, fourth line) which are morphosyntactically distinct. 
Meanwhile, having behavior falling within that which is typical of the lemma's morpholexical class is treated as having the same morphosyntactic and morpholexical classes, and is essentially *unmarked* -- it *can* be specified if desirable for notational reasons (as in the first and second lines of the example), but it can also be omitted (as in the third and fifth), as a morphosyntactic class identical to morpholexical class will be assigned by default -- more specifically, each morpholexical class has an *unmarked* (default) morphosyntactic case that will be automatically assigned unless otherwise specified: for the morpholexical class N(OMINAL) in the French paradigm shape file (see section below on how to read and write these files), the unarked morphosyntactic class is NOUN. 

Although notated like a morphosyntactic feature, morphosyntactic class stands out among other features in that it changes how other features are treated for purposes of paradigmatic analyses (see README.suite.md): for example, infinitives will *not* contribute to stats in terms of tense and person. 
They *can* however, contribute to such stats, if explicitly specified, as seen in the sixth line where *sues*, with POS set as ADJ, is still specified as PAST for TENSE. 
While the ordering of other feature specifications does not matter, here it does: feature specificitions placed before the marking of a (different) morphosyntactic category are counted for the paradigm of the morpholexical category, whereas those placed after are counted for the paradigm of hte morphosyntactic category. 
This prevents calculations for tense on French nouns, and also allows different treatment for cases where the same feature is being marked in different paradigms (e.g. Albanian *ynë*, a genitive (pro)noun, which is plural as a noun but singular as an adjective). 
Furthermore (as explained at greater length in README.suite.md), the user can filter their analysis in terms of POS or morpholexical category (as well as specifying a narrower scope by setting other features). 

## PARADIGM SHAPE INPUT FILE
 
 TODO example with French... (in which Old French paradigm shape used, not modern nor Latin) 

# CONTACT FOR ANY QUESTIONS 

Any questions or requests for clarification on how to use this system can be emailed to: marr.54@buckeyemail.osu.edu, or (after 2025) cl.st.marr@gmail.com.
