Readme for dataset policies. 


Rationale: 
The LatinToFrenchPope (LTFP) dataset is meant to be used for the simulation of diachronic phonological rules 
which collectively constitute the diachronic transformation of (popular) Latin to French. For the purposes of the
initial paper introducing the DiaSim system, it is used for demonstrating how DiaSim can be used to "test" a "hypothesis"
rule set, namely the example of the Latin-to-French diachronic transformational rule set as described by Mildred K Pope
in her 1934 foundational treatise, "From Latin to Modern French with Especial Consideration of Anglo-Norman: Phonology
and Morphology". However, more generally, this dataset is also intended to enable testing of any "hypothesis" of a set
of diachronic transformation rules for the Latin-to-French scenario. 

In order to enable this dataset to be of such use, we must be clear of what specifically it is to be used to test. 
Specifically, this dataset is constructed to test PHONOLOGICAL developments. It is not intended to test developments that
arise from non-phonological motivations. In order to properly test diachronic phonological theories, we must declare
and enforce consistent adherence to a set of principles in constructing the dataset. These principles are listed below. 

Principles for exclusion and inclusion: 

As this dataset is intended for use in simulating the continuous development of etymons from Latin to French, etymons 
are consistently excluded if they fall under any of the following categories:
* Words that have been effected by morphologically motivated processes like analogy.
* Words that have been effected by lexicon-motivated processes like contamination
* Words that have been effected by sociolinguistically motivated processes including spelling pronunciation, prestige-mediated 
 restoration, folk etymology
* Words resulting from loans. Loans from other Romance languages are considered loans, and this includes loans from 
	Occitan, and highly divergent "langue d'oil" idioms including Norman, Picard, Champenois, Walloon, Burgundian, Gallo, 
	and Saintongeais. However, loans from dialects within teh central 'oil' zone cannot be considered loans. Loans from
	the parent language Latin are of course still loans. 
* Any results from interference from other languages. 
* Innovations in any period except Latin. 
* Any and all innovations based on building off of previous words. These include but are not limited to deverbalizations,
	suffixations, prefixations, blends/portmanteaus, compounds, and other similar sorts of derivations. 
* "Refections" as per Rey, i.e. where sounds were reinserted under the influence of the (known or percieved, correctly or not) etymological origin form. 
* Words effected by syntactically-motivated phenomena that are NOT phonologically mediated
* Place names in dialectal or foreign language regions. 
* Onomatapoeia -- as semantically rather than phonologically motivated. 

The following are NOT valid reasons for exclusion, even though they do lead to irregularity: 
* Words effected by sporadic metathesis. 
* Words effected by assimilation or dissimilation (including long-distance assimilation and dissimilation)-- these are 
	still phonologically motivated 
* Words effected by syntactically motivated but phonologically mediated shifts -- this has a special bearing on changes
	in stress as determined by syntactic position. Thankfully these are mostly occurred in Latin and Vulgar Latin, so we 
	realize them lexically in this dataset. 
* Any and all semantic shifts -- this is irrelevant to the phonological development. However, gender shifts may be grounds
	for exclusion if and only if they involve changes in lexical morphology to match the new gender, as this is analogy.
* Words that were loaned into Latin from other languages. A word entering Latin from Greek, Etruscan or any other language 
	during the classical period is acceptable. Loans from Germanic languages are included if and only if they were realized
	in most or all branches of Romance. Celtic loans are a bit trickier as it can be unclear when the loan happened as 
	Celtic may have survived in a bilingual situation with Latin for quite awhile. All of these are marked for future
	decisions to exclude or include as a group. Currently they are being included. 
 * Euphony. 


Policies regarding forms of words in the datasets:

Latin forms:
* When Classical and Popular Latin forms are both available, Classical forms are used by default
	* When the Popular Latin form that the French descends from has a morphological change using a common morpheme
		(ex: flagrum >flagello > fle'aux), then we list the word as descended from what the Classical Latin would be with that
		morphological form (in this case, flagellum)
	* With the exception of a few cases, French nouns derive from their Latin accusative forms, for both plurals and singulars. 

French forms:
* Cited forms are not effected by some very recent shifts (for example, the merger of ɑ and a, and of œ̃ and ɛ̃)
* In order to represent liaison, liaison consonants ARE included in the citation rules. Additionally, in order 
	* to differentiate words with consonants that only appear with liaison from those that appear always, ə is 
	* inserted at the end of the former, and it is assumed to be deleted by a SYNCHRONIC rule. Likewise, 
	* liaison-only final consonants are held to be deleted by a SYNCHRONIC rule in non-liaison environments.
	* Vowel initial words that do not trigger liaison are held to have a silent phonemic /h/, which is also 
	* deleted by a SYNCHRONIC rule in all scenarios after liaison would have occurred. Likewise it is a synchronic 
	* rule that deletes the word boundary to enable liaison. Words ending in phonemic -f or -s which become -z or -s 
	* in cases of liaison are not written with a final ə, instead they are written with citation forms lacking a final #. 

Sources: 
As of present 