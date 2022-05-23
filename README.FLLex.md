Readme for dataset policies. 


Rationale: 
The FFLex dataset is meant to be used for the simulation of diachronic phonological rules 
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
* Words affected by hypercorrection. 
* Words that became obselete. This applies also for words that fell out of use, and were then revived. This is 
	significant because it cannot be guaranteed that they would be affected by regular shifts during their period 
	wihthout usage. 
	* However, some obselete words have been included for the purposes of testing certain phenomena. These are marked
		with comments (starting with $) specifying that they are obselete and used for testing, and will be removed 
		once this purposes is deemed to be sufficiently fulfilled. 
* Words that have been effected by lexicon-motivated processes like contamination
	* These also include bilingual contamination -- i.e. Pope s750, common for both Gaulish and Frankish.
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
* Words effected by syntactically-motivated phenomena that are NOT phonologically mediated. However, when these phenomena
	occurred during the Latin stage and were mediated through stress in a predictable way, as described by Pope, they 
	may still be included simply by changing the stress values of the Latin citation forms. Almost all cases of this
	are function words. 
* Place names in dialectal or foreign language regions. Like stipulated above, the region that is considered "safe" is 
	the Central "oil" region including Ile-de-France, Orleans, Berry, and Western Champagne. 
* Onomatapoeia -- because it is semantically rather than phonologically motivated. 

The following are NOT valid reasons for exclusion, even though they do lead to irregularity: 
* Words effected by sporadic metathesis. 
* Words effected by assimilation or dissimilation (including long-distance assimilation and dissimilation)-- these are 
	still phonologically motivated 
* Words effected by syntactically motivated but phonologically mediated shifts -- this has a special bearing on changes
	in stress as determined by syntactic position. Thankfully these are mostly occurred in Latin and Vulgar Latin, so we 
	realize them lexically in this dataset. 
* Any and all semantic shifts that did not cause phonological change -- this is irrelevant to the phonological 
	development. However, gender shifts may be grounds for exclusion if and only if they involve changes in 
	lexical morphology to match the new gender, as this is analogy.
* Words that were loaned into Latin from other languages. A word entering Latin from Greek, Etruscan or any other language 
	during the classical period is acceptable. Loans from Germanic languages are included if and only if they were realized
	in most or all branches of Romance. Celtic loans are a bit trickier as it can be unclear when the loan happened as 
	Celtic may have survived in a bilingual situation with Latin for quite awhile. All of these are marked for future
	decisions to exclude or include as a group. Currently they are being included. 
 * For the most part, loanwords which preserve phonological elements that were absent in Latin are still incorporated, 
 	as how these segments developed has been theorized to occur in regular ways. This particularly applies to Greek 
 	loanwords (where /y/ was unrounded, aspirated stops deaspirated, ɸ > f later, etc), Celtic loanwords (in which French 
 	very often simply incorporated Celtic phonology including antipenultimate stress -- and indeed modified many 
 	Latin words so that their phonology was "more Celtic" such as enforcing Celtic lenition and sandhi phenomena, and shifts like kt > xt) and 
 	the earlier Germanic loanwords (where hl > fl, etc) -- while the later ones cannot be considered for a 
 	Latin-to-French dataset as they were borrowed after the "Latin" stage was over. For all of these, the 
 	phonological forms posited by Pope for Latin etymologies of French words take precedence. 
 * Euphony. Although perhaps not strictly phonologically motivated, this is nevertheless a purely phonologically mediated process that
 	should not be used as an excuse to excuse words from analysis (especially as it is hard to verify).
 
Policies regarding forms of words in the datasets:

Latin forms:
* When Classical and Popular Latin forms are both available, Classical forms are used by default
	* When the Popular Latin form that the French descends from has a morphological change using a common morpheme
		(ex: flagrum >flagello > fle'aux), then we list the word as descended from what the Classical Latin would be with that
		morphological form (in this case, flagellum)
	* With the exception of a few cases, French nouns derive from their Latin accusative forms, for both plurals and singulars. 
* Lexicon forms cite the older form of Classical Latin, before the length distinction became a quality one (i.e. before 
	short i e o u became laxened to ɪ ɛ ɔ ʊ). These transitions are represented as rules in the ruleset, rather than in 
	the lexicon. 
* Regular rules such as the various forms of Latin l (lʲ, dark l, etc) are not represented in the lexicon, but rather 
	represented as regular rules in the ruleset. The same applies to the mutations of Latin n, by which it became m or 
	ŋ in various contexts. 
* Latin uses dental d̪, t̪ and n̪, not their alveolar counterparts (like modern French, unlike modern English)
* Latin /r/ is always represented as a trill here. 
* Words with foreign (non-Latin) pronunciations in Latin, i.e. with phonemes not native to Latin including pʰ, kʰ, t̪ʰ,
	ɸ, x, θ, y, and ts, are included with those phonemes in the lexicon-- their mutations are included instead in the ruleset.
	An exception is where these were loaned early into Latin, and had non-typical results (for example, y loaned early from
	Greek became Latin u, while later loans of Greek y became Latin i -- the former have /u/ in this lexicon, while the 
	latter "preserve" the Greek /y/).  
* Unless otherwise specified, French nouns are held to have descended from Latin accusative case forms. The exceptions to
	this are a few that came from the dative, and a somewhat larger but still small category that came from the nominative
	(ex. French fils /fis/ from Latin filius, not filium). Other cases are used when Pope specifies so, otherwise the 
	accusative case is assumed to be the ancestor. 

French forms:
* Like Latin, French uses dental forms for its dental stops : d̪, t̪, n̪ , not d t n
* Cited forms are not effected by some very recent shifts. These include: 
	* the merger of ɑ and a
	* the merger of œ̃ and ɛ̃
	* the breaking of ɲ into a n̪j segment 
	* deletion of ə everywhere followed by voicing assimilation of resulting clusters (example : cheval,
	 	formerly /ʃəval/ has become [ʃfal] )
* Citation forms DO reflect the following recent shifts 
	* deletion of final /ə/ 
	* the 18th and 19th century shift of [wɛ] to [wa]
	* ʎ > j 
	* the loss of liaison, effectively effacing most final consonants. 
	* the establishment of ʁ as the correct pronunciation of <<r>> (formerly [ʀ], before that [r]) 
	* the loss of almost all phonetically lengthened vowels. 
* Currently, the lexicon's citation forms EXCLUDE phonological realizations of liaison. This is because (a) 
	liaison is highly idiosyncratic in realization, (b) its conditioning factors are often sociolinguistic and syntactic,
	and (c) some French speakers are increasingly losing it entirely. As a result of the above, a consistent treatment
	on which words have liaison in a standardized lexicon would be very difficult to adhere to, and for consistency,
	it appears best to exclude liaison entirely. 
* French r is phonologically represented as ʁ, which is the standard pronunciation, even though dialectal realizations 
	are widely varied, including ʀ, r, ɾ, χ, ɣ and x. It is represented as a voiced fricative, not an approximant or a trill,
	but one may note that according to some analysis it nevertheless has "sonorant" syllabic properties (i.e. in words
	like "quatre" /kat̪ʁ/) 
