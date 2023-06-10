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
d̪ ˈo r m i t̪ , d̪ ɔ ʁ %<DORMIRE> {CLASS=VERB | PERSON=3 | NUMBER=SG} $dort.
ˈo k u l u m , œ j %<OCULUM> {CLASS=NOUN | GENDER=MASC} $œil.  
ˈo k u l oː s , j ø %<OCULUM> {CLASS=NOUN | GENDER=MASC | NUMBER=PL} $yeux.  
```

In each line above, again, paradigmatic info is found after the paradigm flag '%' and before the comment flag '$'. 
The paradigmatic info here consists of two components, in the following order: (1) the *lemma ID*, placed between '<' and '>" and (2) the *morphosyntactic feature-value clause*. 

## LEMMA ID 
The *lemma ID*, found between '<' and '>', contains a string that is a unique lemma for all forms that are considered to belong to the same paradigm -- those that are to be considered forms of the "same word" by DiaSim. Thus, as seen above, the second line (for *œil*) ad the third (*yeux*) share the same lemma ID, but this ID is not shared with the first line (*dort*), as that is not a form of the same word; instead the lemma ID of *dort* would probably be shared with lines for *dormir*, *dors*, et cetera. 

Note: it is advisable to avoid giving to give homophonous lemmata the same lemma ID -- for example, Latin liber "free" and liber "book" are best given different lemma IDs like "LIBER1" and "LIBER2" (for the same reason, Latin based lemma IDs are preferred for Romance CFRs, because sound change can  regularly create new homophony, but homophone splits do not regularly occur). This is important because otherwise it will lead to unintended sidde effects in how paradigmatic stats are calculated: the erroneously conflated paradigms with homophonous lemmata will be considered the same, so any items with the same morphosyntactic feature specifications will be treated as being in *overabundant* cells within the same paradigm (erroneously increasing cell-wise rates of overabundance) and the two paradigms together with all their cells) will only contribute with a weight of one paradigm to statistical calculations performed for automated analyses by DiaSim.

## MORPHOSYNTACTIC FEATURE-VALUE CLAUSE
The *

   	 
TODO 'unmarked values' 
TODO define input file and discuss


# CONTACT FOR ANY QUESTIONS 

Any questions or requests for clarification on how to use this system can be emailed to: marr.54@buckeyemail.osu.edu, or (after 2025) cl.st.marr@gmail.com.
