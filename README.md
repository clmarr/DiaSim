# DiaSim

This package, or rather earlier stages of it, are covered broadly in Marr & Mortensen 2020 (preprint: <https://www.academia.edu/71888418/Computerized_Forward_Reconstruction_for_Analysis_in_Diachronic_Phonology_and_Latin_to_French_Reflex_Prediction>), and Marr & Mortensen 2022 (preprint: <https://www.academia.edu/94911785/Large_scale_computerized_forward_reconstruction_yields_new_perspectives_in_French_diachronic_phonology>)

## Wiki pages

This README file covers basic technical information, such as how to get DiaSim up and running. For detailed information on usage of DiaSim, see the following pages in this repository's wiki:

- [Lexicon](https://github.com/clmarr/DiaSim/wiki/Lexicon): covers how to build your lexicon file, the set of etyma to realize sound changes upon, including how to make a lexicon file with columns for ('gold') stages with observed forms to compare with reconstructed outcomes, and how include paradigmatic information and token frequencies.

- [Cascade](https://github.com/clmarr/DiaSim/wiki/Cascade): covers how to make your cascade file, the set of ordered sound changes to realize upon your lexicon, according to SPE format with a couple added gimmicks, the use of alpha features, the placement of gold and black stages in the cascade, and so forth.
  
- [Representations](https://github.com/clmarr/DiaSim/wiki/Representations): covers the handling of features and the phone symbols defined in terms of them, including feature implications and translations.

- [Metrics](https://github.com/clmarr/DiaSim/wiki/Metrics): covers how metrics provided whenever DiaSim evaluates reconstructed outputs against observed forms are computed, and how to modify their computation.

- [Suite](https://github.com/clmarr/DiaSim/wiki/Suite): covers the diagnostics offered by DiaSim whenever it reaches a halting point, and how to use the suite menu to "debug" your cascade!

## System requirements

In order to run DiaSim, you need Java installed on your computer. DiaSim has been successfully run with extensive usage with Java SE 8, 11, 17, 21, and 25; it is written to avoid differences between these. However, it is probably best to use SE 21, which has been used the most. At time of writing, Java SE 27 had just been released the previous day, so any possible issues are likely stil undetected.

## Running DiaSim

There are two ways DiaSim is run. 

The first operates via a command shell (Terminal, or Command Prompt on Windows). 
The second is to operate within the programming console Eclipse (you can download it [here (click)](https://eclipseide.org/)). 
It is expected DiaSim will be more commonly used on a shell, and what follows concerns that usage case. 
Some pointers for operating DiaSim within Eclipse are given in its respective section. 

After opening your shell, you should navigate to where DiaSim is located. 

If it is a folder on your Desktop, after opening your shell, your command to get there on Windows might look like this: 

```text
 chdir "Desktop\DiaSim"
```

For a Linux shell (including one on a Mac), one would use `cd` instead of `chdir`. You may need to replace `Desktop\DiaSim` with whatever the path to DiaSim is on your system. 

### Windows Command Line

On Windows, DiaSim can be run with the batch script `derive.bat`.

In the command line, navigate to the DiaSim directory and run derive.bat by using the following command:

```text
 ./derive.bat
```

Command line arguments can be included here; see the "Command line arguments" section below.

### Bash (Linux / Mac OS)

On Linux and Mac OS, DiaSim can be run with the bash script `derive.sh`.

In the command line, navigate to the DiaSim directory and run derive.sh by using the following command:

```text
 ./derive.sh
```

By default, without any further arguments, this will operate DiaCLEF, the French cascade used for Marr & Mortensen 2020 and 2023, upon the Latin to French lexicon FLLex, as it existed before 2024. 
This can be changed by using command line arguments; see the "Command line arguments" section below.

## Command line arguments

You may include these command line arguments by adding them to your run command. If you run the script without any arguments, DiaSim will default to using FLLex for its lexicon and DiaCLEF for its cascade, and the output will go to a folder with the name `unnamed_run_<datetime>`.

Specifying a lexicon, cascade, and run name:

- `-out <run_name>`, where <run_name> is the name you want the folder with all resulting forward-reconstructions and analysis files to be placed

- `-lex <filename>` -- sets the file with the etyma to implement sound changes on (see the [**Lexicon**](https://github.com/clmarr/DiaSim/wiki/Lexicon) page of the wiki). You may also use ``-words`` (and actually, any flag starting in `-lex` will be treated as this). 
  
- `-rules <cascade_file>` -- sets the file with the ordered sound changes to realize upon the lexicon. Instead of ``-rules`` you may instead use ``-cascade``. 


The correct file path, with DiaSim as the working directory, is necessary to avoid errors. 
For example, if you are operating on a cascade called "OldNorseToIcelandic.txt" in a folder "norse-cfr" in the same directory as DiaSim, you would have: 

``-rules ../norse-cfr/OldNorseToIcelandic.txt``

for the part of the function call indicating the cascade file (".." means "go to parent directory" on most systems). 
It is recommended that you store files that DiaSim will operate on in a folder that has the same parent directory as DiaSim, to make file path referencing easier. 

Additional options:

- `-diacrit <filename>` -- allows you to use a custom diacritics file (cf. [**Representations**](https://github.com/clmarr/DiaSim/wiki/Representations) on the wiki). If you use just `-diacrit`, the standard diacritics file will be used. It is recommended to use diacritics if you are using features in rule outputs, as otherwise if your rules end up producing a feature combination without a symbol explicitly dedicated to it in the symbol definitions file (see below), it will appear as a question mark followed by a number. 
 
- `-symbols <symbol_file>`  -- allows you to use a symbol definitions file other than symbolDefs.csv (on how to make these, you can follow the rubric of that file and/or consult the [**Representations**](https://github.com/clmarr/DiaSim/wiki/Representations) page of the wiki)

- `-shorthands <filename.tsv>` -- allows you to use a shorthands file,  to use shorthands for phonological classes other than the default set stored in `phonClassShortHands.tsv` (e.g. C = [+cons], W (glide) = [-cons,-syl], etc.). This must be .tsv file with two columns: the shorthands in the first (left) column, and the features they correspond to in the second (right) column. The features must also exist in your symbol definitions file (see above). 

- `-impl <filename>` -- allows you to use a feature implications file other than the default FeatureImplications (cf. [**Representations**](https://github.com/clmarr/DiaSim/wiki/Representations) on the wiki)
  
- `-idcost <a number>` -- sets the cost of insertion and deletion for computing edit distances (cf. [**Metrics**](https://github.com/clmarr/DiaSim/wiki/Metrics) on the wiki)

- `-files_only' -- run to create derivation and accuracy report files only. Will not stop. Intended for use in command line or as part of external workflows. 

- `-verbose` -- verbose mode -- prints out more information about file locations and other variables set at the command line call.

There are also the following command line flags, which are put together after a single hyphen (eg. "`-ph`")
  
- `-p` -- print changes mode -- prints words changed by each rule to console as they are changed.
  
- `-h` -- halt mode --- halts at all intermediate stages, not just those associated with observed outcomes to test against (gold stages)
  
- `-e` -- explicit mode -- ignores feature implications

- `-s` -- skip file creation -- runs without creating a run output folder

### Composite cascade call

Instead of ``-rules`` or ``-cascade``, you may use ``-cascades`` (plural!) or ``-composite``, DiaSim will understand this to mean you want to operate on a *sequence* of cascades, each starting where the last one ends (e.g. a Latin to Old French cascade, then an Old French to modern French cascade). 

The file after it *MUST* contain the following: filepath locations for each cascade in their historical order, each on their own line, with a line between each consecutive cascades stating the stage at which the one above transitions to the one below (starting with `~` if it's a gold stage, `=` if it's a black stage. Input stage should be a black stage but it is not necessary to list.). Do NOT list the final input and output stages. 
These should all be listed in the historical order they occur, as that is how DiaSim will assemble them into a composite cascade. The composite cascade file will be deleted after the run, but you can see the rules in order in the rules log file, which will be your run name (flagged by `-out`, see above) with the suffix `_rules_log.txt`. 

Example contents of the composite cascade listing file (Proto-Gallo-Romance, which doesn't have attested forms to compare to, is a black stage and thus flagged with `=`. Old French, an attested stage of French, has forms that can be compared to, and is thus flagged as a gold stage with `~`):

```LatinToProtoGalloRomanceCascade.txt
	=Proto-Gallo-Romance
	ProtoGalloRomanceToOldFrenchCascade.txt
	~Old French
	OldFrenchToModernFrenchCascade.txt
```

### Example configuration

Suppose you have created a lexicon named `my_lexicon` and a cascade named `my_cascade`. You have put these files in the DiaSim directory. Suppose you also want to run DiaSim and have it make an output folder called `my_run`. Additionally, you want DiaSim to print to the console every sound change and affected etymon as it runs. 

Once you have navigated to the DiaSim directory, you will run a shell command that looks like this:

Windows command line:

```text
./derive.bat -lex my_lexicon -rules my_cascade -out my_run -diacrit
```

Bash:

```text
./derive.sh -lex my_lexicon -rules my_cascade -out my_run -diacrit
```

To operate a run named "lastIrishRun" that will produce output report files for an Irish cascade "irish-casc.txt" operated upon a lexicon "OldToModernIrish.txt" in a sibling directory to DiaSim named "irish-cfr", your command could look like this: 

Windows command line:

```text
./derive.bat -out ../irish-cfr/lastIrishRun -lex ../irish-cfr/OldToModernIrish.txt -rules ../irish-cfr/irish-casc.txt -diacrit
```

Bash:

```text
./derive.sh -out ../irish-cfr/lastIrishRun -lex ../irish-cfr/OldToModernIrish.txt -rules ../irish-cfr/irish-casc.txt -diacrit
```

## Lexicon file

A lexicon file contains your collection of etyma to be processed. It contains a series of attested lexical items from a starting point ("input stage"), optionally followed by any number of series of attested forms of those words from later stages ("gold stages"). DiaSim iterates through your cascade of rules (see below), applies changes to this lexicon, and computes output forms, which can be compared to your gold stages. DiaSim can provide details on any discrepancies between computed results and what is actually observed in the gold stages.

More information on lexica can be found on the [**Lexicon**](https://github.com/clmarr/DiaSim/wiki/Lexicon) page of the wiki.

## Cascade file

A cascade file contains your ordered list of sound change rules that you desire DiaSim to apply to the lexicon. If your lexicon includes intermediate gold stages (see "Creating a lexicon file" above), then the point at which these stages occur must also be specified in the cascade. Additionally, stages without attested forms can be specified in the cascade; these are called "black stages." Since black stages, by definition, do not have corresponding attested forms which appear in the lexicon, black output forms cannot be cross-checked. They simply serve to let DiaSim know what other notable points in the cascade may be.

The rules that make up a cascade are in the conventional sound change notation, that is to say, `A > B / C __ D`.

More information on cascades can be found on the [**Cascade**](https://github.com/clmarr/DiaSim/wiki/Cascade) page of the wiki.

## Output data

DiaSim will populate your chosen output folder with files containing information on your results. These files include a log of the rules applied, a table of etyma in the state they appear at each stage, statistical analyses pertaining to phones, and a folder containing step-by-step forward-reconstructions for each etymon.

## Operating DiaSim from within Eclipse 

The Eclipse method has become somewhat of a tradition among the users of DiaSim, because it gives a `programmer` experience of `debugging` a language's phonological history, and because some users may be more comfortable within a console than using a shell.
For general issues installing Eclipse, please consult [Eclipse's own guide](https://eclipseide.org/getting-started/). 

Once Eclipse for Java development is successfully installed and runs on your computer, to operate DiaSim within it, you need to open it as a project. 
You can do this by going up to the command bar near the top of the window and clicking `File > Open Projects from File System...`.  
A window will open. 
Near the top, there is a bar with a file path. 
To its left lies the text `Import source:`. 
To its right, there is a button that says `Directory...`. 
Click it, navigate to the location of the project folder `DiaSim`, highlight the folder `DiaSim`, and click `Select Folder`. 

DiaSim should then appear on the `Package Explorer` panel on the left. 
If it is not already open it, go to the command bar near the top of Eclipse, click `Window > Show View > Package Explorer`, and it should appear, with DiaSim.
You may need to click on the package icon for `DiaSim [DiaSim gamma]` to open irs contents (`gamma` may be replaced with a later version name). 
Within DiaSim, click `src > (default package) > DiachronicSimulator.java`. 

With `DiachronicSimulator` open, go up to the top command bar, and click `Run > Run Configurations...`. 
Near the top of the winter that pops up, there should be a bar that says `DiachronicSimulator`, with the text `Name:` to its left. 
If it is not already there, there is a bar to the left that says `type filter text`. Type `Diachronic` in it and it should appear, under `Java Application`; click it, and you should be on track. 
Whether or not you had to fix that, there are a number of taps under the `Name:` bar. 
You want to click the one that says `Arguments`. 

Underneath the tabs, now, there should be a window titled `Program arguments:`. 
Here, you should put in everything after the batch/bash file, as discussed above (see the section `Command line arguments` above): 

```text
 -lex my_lexicon -rules my_cascade -out my_run -diacrit
```

## IPA character display 

On older systems especially, some IPA characters (phonetic symbols) may be replaced with "?" or a box. 
This is because the font of whatever console is displaying DiaSim's output does not support these characters. 

In the mid 2020s, using system fonts such as Consolas within Eclipse largely evades this problem. 
Nevertheless, it is common to have issues printing symbols if you are using DiaSim within Eclipse on certain operating systems. Adding the following VM argument within Run Configurations (under the Arguments tab) often helps:

```
-Dsun.stdout.encoding=UTF-8
```

It may help to download fonts that are designed to support IPA characters. 
I personally prefer [Gentium, which may be downloaded here (click).](https://software.sil.org/gentium/download/). 

