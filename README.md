# DiaSim

This package, or rather earlier stages of it, are covered broadly in Marr & Mortensen 2020 (preprint:https://www.academia.edu/71888418/Computerized_Forward_Reconstruction_for_Analysis_in_Diachronic_Phonology_and_Latin_to_French_Reflex_Prediction) , and Marr & Mortensen 2022 (preprint: https://www.academia.edu/94911785/Large_scale_computerized_forward_reconstruction_yields_new_perspectives_in_French_diachronic_phonology) 

For a run with default configurations, in command line call: bash std_derive.sh

To specify run name, ruleset, and lexicon file: bash derive.sh <RUN_NAME> <CASCADE_FILE> <LEXFILE_FILE>

    <RUN_NAME> is the name you want the folder with all resulting derivations and analysis files to be placed (without < and >) 
    
    <CASCADE_FILE> is the location of the file containing the set of sound changes you want to implement, in SPE format, with $ flagging comments (see README.cascade.md for more details)
    
    <LEXICON_FILE> is the location of the file containing the etyma you want to operate these ordered sound changes upon, with $ flagging comments, and stages with observed outcomes in consistent columns delimited by ',' from the input forms (see README.lexicon.md for details).

You may also edit derive.sh and std_derive.sh to designate targets of your choosing.

To run within Eclipse or another programming interface, or to run it in terminal, usage is as follows.

Before July 1 2023, it was necessary to specify a run name using -out: however, as of July 2023, it now defaults to a run name based on the date.

Run configurations include: 

  -out <run_name>, where <run_name> is the name you want the folder with all resulting derivations and analysis files to be placed.   

  -symbols <symbol_file>  -- allows you to use a symbol definitions file other than symbolDefs.csv (on how to make these, you can follow the rubric of that file and/or consult README.representations.md)
  
  -rules <cascade_file> -- sets the file with the ordered sound changes to realize upon the lexicon 

  -lex <filename> -- sets the file with the etyma to implment sound hcanges on (see README.lexicon.md)
  
  -impl <filename> -- allows you to use a feature implications file other than the default FeatureImplications (cf. README.representations.md) 
  
  -diacrit <filename> -- allows you to use a custom diacritics file (cf. README.representations.md) 
  
  -idcost <a number> -- sets the cost of insertion and deletion for computing edit distances (cf. README.metrics.md) 

There are also the following command line flags, which are put together after a single hyphen (eg. "-ph")
  
  -p -- print changes mode -- prints words changed by each rule to console as they are changed. 
  
  -h -- halt mode --- halts at all intermediate stages, not just those associated with observed outcomes to test against (gold stages) 
  
  -e -- explicit mode -- ignores feature implications
  
  -v -- verbose mode -- prints out more information about file locations and other variables set at the command line call. 

This file will be expanded with usage basics  in 2023 and 2024. 

The other README files have the following coverage: (please be patient as we are trying our best to complete them while attending to other pressing issues!) 
	
	README.lexicon.md: covers how to build your lexicon file, the set of etyma to realize sound changes upon, including how to make a lexicon file with columns for ('gold') stages with observed forms to compare with reconstructed outcomes, and how include paradigmatic information and token frequencies. 

	README.cascade.md: covers how to make your cascade file, the set of ordered sound changes to realize upon your lexicon, according to SPE format with a couple added gimmicks, the use of alpha features, the placement of gold and black boxes in the cascade, and so forth (most sections complete but considerable material still under construction)
		
	README.representations.md: covers the handling of features and the phone symbols defined in terms of them, including feature implications and translations. 

	README.metrics.md: covers how metrics provided whenever DiaSim evaluates reconstructed outputs against observed forms are computed, and how to modify their computation	(coming soon)
	
	README.suite.md: covers the diagnostics offered by DiaSim whenever it *halts*, and how to use the so-called "halt menu" to debug your cascade! (coming soon)
	

