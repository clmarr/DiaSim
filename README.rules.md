# README.rules :

This file details how to properly construct a ruleset file so that it can be read by DiaSym.

The file should be a ".txt" file, with each non-blank and non-commented line consisting of either a rule or a stage flag. 

# MOTIVATIONS:
DiaSim operates in the Neogrammarian tradition.
Thus, it sees diachronic phonology as primarily consisting of an ordered set of algorithmic sound rules. 
The set is ordered in that each of its rules operates after the one before it and before the one after it. 
This fact is of special importance where bleeding, feeding, counterbleeding, and counterfeeding phenomena are concerned.
Each of these diachronic rules is considered by the Neogrammarian tradition (and thus DiaSim) to be regular.
This means that they effect a distinct segment in a predictable way, perhaps operating only in a certain context. 
When "rules" are not regular in ways not "regularizable" by specifying context, they are held to be due to various non-phonetic motivations, including syntactic interference, analogy, sociolinguistic prestige, restoration, hypercorrection, folk analogy, morphoological interference, and so forth.
These exceptional cases are not of interest for the use of DiaSim and are (unfortunately) impossible to model using only phonological means; DiaSim is a purely phonological model. 
The ruleset file is precisely this ordered set of rules, additionally with ordered declarations of "stages" where they exist among the rules optionally in use, if the user so desires.

# STAGE FLAGGING: 

# COMMENTING:
One flags a comment by using the dollar sign "$", just like in the lexicon file.
After seeing this flag, the system erases from its memory the character and everything on the same line to the right of it.
Hence a line that starts with the comment flag "$" will be considered blank by DiaSim.

# RULE FORMAT: 
All rules (to reiterate -- each being exactly one line) should be of the following format: 

```
Target Source > Destination / Prior Context __ Posterior Context
```

(elaborate...) 

## TRANSFORMATION FORMAT:

## CONTEXT FORMAT:

# TO MODIFY SYMBOLS USED:
