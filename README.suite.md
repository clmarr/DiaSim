# README.suite.md

This file (as of summer 2023, in progress) covers how to use DiaSim to analyze inaccuracies in cascades. This means fixing the order and formulation of rules, and checking where errors lie in how they are realized upon etyma in the lexicon.

## "Debugging" rule inaccuracies

After computing forward-reconstructed etyma based on the input lexicon and the cascade you give it, DiaSim provides the user with a menu through which detailed analytics may be performed. This menu, referred to as the "suite menu," is a powerful tool for determining what rules in your cascade are producing reconstructive errors and should be examined.

Note that in DiaSim, the term "confusion" refers to a mismatch at the level of phones, while "error" refers to a mismatch at the level of etyma.

## The suite menu

DiaSim's suite menu looks like this:

```text
What would you like to do? Please enter the appropriate number below:
| 0 : Set evaluation point ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|
| 1 : Set pivot point (upon which actions are conditioned, incl. filtering [2])       |
| 2 : Set filter sequence                                                             |
| 3 : Query                                                                           |
| 4 : Confusion diagnosis at evaluation point                                         |
| 5 : Run autopsy for (at evaluation point) (for subset lexicon if specified)         |
| 6 : Review filtered results or analyze them (stats, errors) at eval point (submenu) |
| 7 : Test full effects of a proposed change to the cascade                           |
| 9 : End this analysis.______________________________________________________________|
```

The user may interact with this menu in the console by inputting the number corresponding to their desired action and pressing enter. Actions are detailed below.

### `0`: Setting an evaluation point

With this command you may set the gold stage at which you desire the **confusion diagnosis**, **autopsy**, and **filtered result review / analysis** to happen (see below).

### `1`: Setting a pivot point

Setting a pivot point is a prerequisite for filtering. Pivot points can be set anywhere -- any stage, the input point, the output point, or a specific rule. Setting a pivot point is as simple as choosing this option in the suite menu and entering a string from the pivot point options shown.

### `2`: Setting a filter sequence

Filter sequences determine the scope of other operations in the suite menu.

Setting a filter sequence requires first setting a pivot point (see **Setting a pivot point** above).

Once you have set a pivot point, selecting this option will prompt you to input a filter consisting of a sequence of phonemes. Phones must be separated by the phone delimiter, which is currently hardcoded to "space".

### `3`: Querying

In the query submenu, you can:

- `0`: See the ID number of an etymon given its phonetic form at input

- `1`: See the phonetic input form of an etymon given its ID number

- `2`: Print a list of all etyma in your lexicon with the ID number, input form, and final-stage gold form of each

- `3`: See a step-by-step forward reconstruction of a specific etymon by ID number

- `4`: See a rule given its rule ID number

- `5`: See rules (and their ID numbers) containing a certain string

- `6`: Print a list of all rules with ID number

### `4`: Running a confusion diagnosis

The confusion diagnosis tool is used to show the most common 'confusions,' that is to say, which phones are most frequently confused for other phones, and the rates at which these confusions occur.

For each listed phone-to-phone confusion, the percentage of errant words comprised by this confusion is printed, along with contextual predictors of the confusion, in terms of both features and phones.

### `5`: Running an autopsy

*A detailed explanation of this submenu is in progress. Thank you for your patience.*

### `6`: Reviewing / analyzing filtered results

In the filter analysis submenu, you canː

- `0`ː Print an accuracy report (see **README.metrics.md**) on filtered data

- `1`ː Print etyma containing your chosen filter

- `2`ː Print erroneously constructed etyma containing your chosen filter

- `3`ː Print erroneously constructed etyma containing your chosen filter as they appeared at your chosen evaluation point (see **Setting an evaluation point** above)

### `7`: Testing effects of a proposed cascade edit

*A detailed explanation of this submenu is in progress. Thank you for your patience.*
