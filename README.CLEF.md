# README.CLEF :

This file details the construction of the cascade files BaseCLEF, BaseCLEF*/BaseCLEFstar, DiaCLEF, and DiaCLEF2024. 
While BaseCLEF, BaseCLEF*, and DiaCLEF have been covered in existing published work (Marr & Mortensen 2020, 2023), this file is at the time of writing the only dedicated treatment pertaining to how DiaCLEF2024 was built. 
Inasmuch as each item in the list above from BaseCLEF to DiaCLEF2024 was constructed using the previous item as its starting point, this file will also proceed in that order, with the implied understanding that unchanged aspects are `inherited' by descendent files. 
For example, since the periodization scheme present in BaseCLEF (based on that of Pope 1934) was not changed, it applies to all cascades directly or indirectly built from BaseCLEF as the original starting point, all the way `down' to DiaCLEF2024. 

Note that at present, this file is a draft README and may have various aspects needing slight revision. 

# BaseCLEF 

BaseCLEF was constructed as a baseline to represent the relative chronology of regular sound change represented in the seminal work of Pope 1934, which continues to be used as the baseline to be built from among Anglophone work on French diachronic phonology (Short 2013), while it is to be acknowledged that equally suitable potential baselines could be found in works by other authors in other languages such as French and German (e.g. that of Fouché, Bourciez, Rhienfelder, Richter, etc.).
In constructing this cascade, the relative chronology of specific sound changes was inferred both by absolute dating as stated by Pope 1934 (e.g. "late 13th century"; "before 1690", etc.) and her statements about the ordering of specific developments relative to other developments (i.e. statements directly about relative chronology). 
Rules were included if they were presented by Pope 1934 as being phonological in nature -- those that would be expected to at least potentially be regular, Neogrammarian sound changes, within a Neogrammarian framework. 
Thus, rules that were presented as having been motivated by morphology (analogy etc.; for discussion of scholarly discourse on this matter, see Hill 2014), those by sociological forces such as prescriptivism, or by other non-phonological/phonetic factors, were not included as regular "sound changes" within the cascade, though many were remarked on in comments.
Furthermore, for sound changes that were presented by Pope as having been partially but not totally reversed by prescriptivist channels such as public education and spelling pronunciations, the original rule was included but not the (partial) reversal of it, as only the former would be expected to be a phonically motivated and therefore expectedly regular sound change. 
As is inevitable in works written by humans that did not necessarily intend for their work to be transformed into a strict relative chronology for application of rules by a computer, there are many points of the dating and conditioning of sound changes that Pope did not make explicit, such as (for example) the point at which R in French changed from an uvular trill to an uvular fricative. 
When possible, such matters were disambiguated in one or more of a few ways described below. 
Firstly, the input forms of phonic sequences for the application of rules were held to imply that any sound change necessary to reach that form must have already applied: so, for example, if the input form listed as just before the application of s-debuccalization in Old French has a nasalized vowel rather than a vowel-then-nasal-consonant sequence, the coalescence of VN to Ṽ at least for the particular vowel quality in the example form will be assumed to have already applied. (This is just a hypothetical example.))
Secondly, and conversely, if examples used for a specific rule have a sequence of phones that implies that a certain rule did *not* act yet, then it is assumed to have come after the rule being exemplified. 
Thirdly, if the rule is assigned to a specific period by Pope but there is no indication of its absolute or relative chronology within that period, it was simply listed added to the last sound change already established within that period. 
Many of the other ambiguities present in Pope 1934 were generously fixed in BaseCLEFstar, as described below. 

# BaseCLEFstar

BaseCLEFstar, with an accuracy nearly ten times that of BaseCLEF upon the dataset FLLex, was constructed so as to represent a "generous" interpretation of the spirit of Pope 1934's rules as represented in its parent cascade, BaseCLEF. 
This is to be distinguished from DiaCLEF, whose contents represent meaningful divergences in the understanding from that of Pope on the matter of what sound changes happened in French, when they occurred, how they were conditioned on phonetic context, and so on. 
Thus, the changes between BaseCLEF and BaseCLEFstar fundamentally represent a sort of taking Pope 1934's assertions to a logical destination, while generously resolving internal inconsistencies in favor of the cascade that was more accurate when there was a likely generalizable difference. 
Many of these changes also served to further disambiguate those remaining inexplicit aspects of French phonology as per Pope as described in the bottom of the section above. 
For example, one such change was the timing of the loss of phonemic vowel length differences: Pope 1934 does state that modern French did not inherit length differences, but never states when this distinction was lost. 
(This applies not only to secondary phonemic vowel length as developed in Middle French, but even to /i/ in open syllables where it lengthened in Proto-Gallo-Roman: while, for example, /a/ in these positions was explained as having gone from /aː/ to, ultimately, /e/, as concerns Gallo-Roman open-syllable-lengthened /i/, the loss of phonemic length was never explicated.)
While the effect of this omission on accuracy was quite significant, fixing it does not actually represent an improvement that was attained through using CFR via DiaSim as a hypothesis-evaluation tool, so these gains in accuracy were embedded in BaseCLEFstar, not DiaCLEF. 

Concerning onomastics, "BaseCLEF*" may sometimes be used to refer to BaseCLEFstar. 
This is not just a shorthand, but also the former name of the file, which was changed for the banal reason of avoiding possible character encoding issues in various computational contexts. 
BaseCLEF* has always been pronounced as "BaseCLEFstar" in speech, in this file continues to prescribe this pronunciation. 


# DiaCLEF

Changes that constituted the construction of DiaCLEF were made by iteratively using computerized forward reconstruction via DiaSim as a diagnostic tool, as well as other diagnostics provided by DiaSim. 
Forward reconstruction applies a hypothesized chronology of sound changes in a language to its parent language to see if the predicted results for each word match the actual observed words in the language of interest (the descendent); 
computerized forward reconstruction, or CFR (Sims-Williams 2018) recruits computers to perform this task so as to bypass the limitations and hazards of human working memory, in a drastically more efficient manner. 
DiaSim (Marr & Mortensen 2020) is a CFR system based in Java which treats lexical phonologies as consisting of sequences of phonological segments, themselves defined as bundles of features, with the set of features used being flexible and redefinable by the user. 
It not only performs CFR and provides access to matters of interest such as individual change-by-change lexical histories but also provides accuracy metrics and statistically-based diagnostic tools that can be used to deduce systematic patterning of error, which could suggest that the "baseline" cascade (hypothesized relative chronology of sound change for the language of interest) has either a wrong rule, a wrongly-formulated or wrongly-timed rule, or is missing a rule. 
Using these diagnostics, as described in Marr & Mortensen 2020, a procedure aiming to pinpoint, amend, test and then choose modifications to create an improved cascade by thus `debugging' the baseline was carried out, and these changes are what constituted the construction of DiaCLEF, which ultimately reached upward of 84% accuracy on the data (FLLex, as described in README.FLLex.md). 
As it turned out, many of these changes ended up independently producing conclusions that had been argued for by topical literature subsequent to Pope 1934's baseline (Marr & Mortensen 2023) while at least one newly discovered sound change in the history of French emerged (Marr 2024). 
The largest type of change by far consisted of reformulating and recalibrating the application of phenomena that were already part of the baseline in some way, the second largest type of changes that constituted DiaCLEF were indeed the "discovery" of phenomena that were new relative to Pope's baseline, and the third largest type consisted of reassigning existing phenomena, typically by moving them to later points in the cascade (Marr & Mortensen 2020: 32-3).  

In terms of which modifications are present, one may treat DiaCLEF as the granddaughter of BaseCLEF with BaseCLEFstar as the mother of DiaCLEF and daughter of BaseCLEF -- all the changes that constitute the difference between BaseCLEF and BaseCLEFstar effectively being `inherited' by DiaCLEF although potentially subject to further adjustment as motivated by the same principles as other DiaCLEF changes. 
However, in terms of how the two were actually made, it is not strictly true that the adjustments that constitute the divergence of BaseCLEFstar from BaseCLEF -- essentially, the construction of BaseCLEFstar itself -- was completed and then, discretely, the construction of DiaCLEF began with BaseCLEFstar now as the baseline. 
Rather, in many cases, it would be detected during the debugging procedure that a particular modification would result in a meaningful increase and accuracy, but, upon consideration, it was realized that this modification was actually a possible interpretation of Pope 1934's rules, and, "giving Pope the benefit of the doubt", it was handled as one of the constitutive modifications of BaseCLEFstar, and only as an inherited modification within DiaCLEF.
In this way, the gain in accuracy was not presumptively credited to our debugging procedure unless it was absolutely clear that they represented a veritable departure from the views of Pope 1934.

However, despite the power of CFR augmented by diagnostic tools as seen in DiaSim, there were still important assumptions and limitations of this process.
In part for this reason, neither DiaCLEF nor its descendent DiaCLEF2024 are intended to represent a hard assertion about the "truth" of French diachrony, but rather a model built through computer-assisted hypothesis testing via CFR and diagnostics. 
Firstly, it should be noted that DiaCLEF was built from the baseline of BaseCLEF and also within its framework, as reflected in its periodization, for example. 
Changes were only made if they would lead to imporvements, with the rest of the cascade held constant, and given that the rest of the cascade was inevitably a modified or unmodified part of Pope 1934's hypothesized relative chronology, it is quite reasonable to assume that this process would be predisposed to remain within certain areas within the hypothesis space of possible relative chronologies for the development of regional Latin into French, without a specific impetus to explore many other areas that nevertheless may have merited exploration. 
Secondly, and perhaps more importantly, is the fact that the datasets (FLLex and FLLAPS) used to evaluate output accuracy in the process of constructing DiaCLEF were themselves largely drawn from the work of Pope 1934 and skewed toward certain parts of the lexicon, favoring nouns (or nominal inflectional forms), adjectives, prepositions, and function words, with fewer verbal forms included -- a fact that also meant that items with more syllables were systematically underrepresented. 
This also meant that certain sorts of secondary clusters never had the chance to arise, and without a need to handle them, there was no coverage in the baseline, and no impetus to add coverage during the debugging process 
To address this, for the purposes of applying a cascade to represent "regular phonological development" to provide for downstream *en masse* analysis of analogical developments in verbal inflection (Herce & Marr forthcoming), a cascade that was further modified in principled ways was constructed: DiaCLEF2024, as described below. 


# DiaCLEF2024

As described above,
DiaCLEF was made by iteratively using DiaSim’s diagnostics to debug BaseCLEF, which was a computerization of the relative chronology of Latin to French sound change represented in Pope 1934, evaluated over a dataset of mostly nominal etyma that comprised items referenced by Pope 1934 herself with additions made to cover underrepresented input sequences. 
Corrections were made to improve performance over the data considered, not any external data, such as the verbal forms that would end up considered for this work. 
However, these verbal forms are often longer, and had strings of syllables that did not enjoy primary stress. 
Due to the extent and variety of interacting elision, lenition, assimilation, and epenthetic phenomena in the history of French, this means that unforeseen secondary sequences emerged, typically in the Gallo-Roman or Later Old French periods, and if the rules did not generalize in a way to handle these unforeseen secondary clusters right, a cascading error effect emerged as tertiary phenomena errantly applied or did not apply. 
Furthermore, since phenomena that Pope 1934 had described with the clear (and often stated) intention that they would collectively explain the loss of certain phonemes did not account for some contexts that emerged for the predicted reflexes of verbal forms in this study, this meant that these phonemes, which included distinctively palatalized consonants and non-strident spirants (/ð/,/ɣ/…) actually ended up “surviving” into Modern French for predicted forms, an eccentric and unexpected outcome argued for by no scholar of French diachronic or modern phonology. 

In order to make a cascade that would not generate such implausible outcomes for verbal forms for the downstream purposes of statistically analogical analyzing patterns in verbal inflection (Herce & Marr forthcoming), during February 2024, a new cascade was built with DiaCLEF now serving as the baseline: DiaCLEF2024.
To fix the matters described above, thankfully, in many cases, it often sufficed to simply extend the application rules that already existed in DiaCLEF; in other cases, adjustments (added rules, reorderings) were made by optimizing performance over additional nominal forms recruited from the *FEW* (von Wartburg et al 1922-2002) that had the patterns of interest or something close to them (occasionally taking into account other Gallo-Romance varieties, for phenomena datable to Proto-Gallo-Romance).
Curiously but reassuringly, many of these fixes essentially boiled down to taking existing acknowledged phenomena and just making them apply more broadly. 

A summarized list of changes included will be coming soon. 


# Cited works

Herce & Marr forthcoming = Herce, B., and Marr, C. G. S. Forthcoming. "The effects of sound change versus analogy on paradigm complexity". 

Hill 2014 = Hill, N. W. 2014. "Grammatically conditioned sound change". *Language and Linguistics Compass* 8(6): 211-229.

Marr & Mortensen 2020 = Marr, C., and Mortensen, D. 2020. [**"Computerized forward reconstruction for analysis in diachronic phonology, and Latin to French reflex prediction"**](https://aclanthology.org/2020.lt4hala-1.5/). *Proceedings of LT4HALA 2020-1st workshop on language technologies for historical and ancient languages*: 28-36. 

Marr & Mortensen 2023 = Marr, C., and Mortensen, D. 2023. [**"Large-scale computerized forward reconstruction yields new perspectives in French diachronic phonology"**](https://www.jbe-platform.com/content/journals/10.1075/dia.20027.mar). *Diachronica* 40 (2): 238-285. 

Marr 2024 = Marr, C. G. S. 2024. [**"A missed regular sound change between Latin and French: velar onset voicing"**](https://www.researchgate.net/profile/Clayton_Marr2/publication/377266441_A_missed_regular_sound_change_between_Latin_and_French_velar_onset_voicing_pre-proofs/links/659dca020bb2c7472b3f13e7/A-missed-regular-sound-change-between-Latin-and-French-velar-onset-voicing-pre-proofs.pdf). *Indogermanische Forschungen* 129(1): 7-43. 

Pope 1934 = Pope, M. K. 1934. *From Latin to Modern French with especial consideration of Anglo-Norman: Phonology and morphology*. Manchester University Press. 

Short 2013 = Short, I. R. 2013. *Manual of Anglo-Norman*, volume 8. Anglo-Norman Text Society. 

Sims-Williams 2018 = Sims-Williams, P. 2018. "Mechanising historical phonology". *Transactions of the Philological Society*. 116 (3): 555-573. 

von Wartburg 1922-2002 = von Wartburg, Walther et al. 1922–2002. *Französisches Etymologisches Wörterbuch*. Eine darstellung des galloromanischen sprachschatzes. Klopp/Winter/Teubner/Zbinden. 25 vols. 