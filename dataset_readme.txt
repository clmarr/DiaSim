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
* Words effected by syntactically-motivated phenomena that are NOT phonologically mediated
* Place names in dialectal or foreign language regions. 

The following are NOT valid reasons for exclusion, even though they do lead to irregularity: 
* Words effected by sporadic metathesis. 
* Words effected by assimilation or dissimilation (including long-distance assimilation and dissimilation)-- these are 
	still phonologically motivated 
* Words effected by syntactically motivated but phonologically mediated shifts -- this has a special bearing on changes
	in stress as determined by syntactic position. Thankfully these are mostly occurred in Latin and Vulgar Latin, so we 
	realize them lexically in this dataset. 
* Any and all semantic shifts -- this is irrelevant to the phonological development. However, gender shifts may be grounds
	for exclusion if and only if they involve changes in lexical morphology to match the new gender, as this is analogy.

Policies regarding forms of words in the datasets:

Latin forms:

French forms:


Sources: 
As of present 