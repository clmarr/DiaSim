# Metrics used in DiaSim

## Accuracy report

After calculating forward-reconstructed etyma based on the input lexicon and the cascade you give it, DiaSim cross-checks the lexicon it has built against your lexicon of gold forms and provides the user with an accuracy report. This report provides the following statistics pertaining to the accuracy of your data:

- Overall Accuracy: reflects the number of correctly forward-reconstructed etyma (in other words, etyma where the **edit distance** relative to the gold set is 0), divided by the total number of present etyma. A higher value is better.

- Accuracy within 1 phone: reflects the number of forward-reconstructed etyma that have an **edit distance** of less than or equal to 1 phone relative to the gold set, divided by the total number of present etyma. A higher value is better.

- Accuracy within 2 phones: reflects the number of forward-reconstructed etyma that have an **edit distance** of less than or equal to 2 phones relative to the gold set, divided by the total number of present etyma. A higher value is better.

- Average edit distance from gold: The sum of all **edit distances** is taken and divided by the total number of present etyma. A lower number is better.

- Average feature edit distance from gold: The sum of all **feature edit distances** is taken and divided by the total number of present etyma. A lower number is better.

## Understanding edit distance

For every etymon in your lexicon, DiaSim calculates an **edit distance** value. This consists of a comparison between each forward-reconstructed etymon (computed by DiaSim) and its gold counterpart (provided in the lexicon by the user). The edit distance is the number of phones by which the two etyma differ (be it by mismatch, inaccurate deletion of phones, or inaccurate insertion of phones). A lower edit distance value is better; an edit distance of zero means the two etyma match.

In addition to edit distance according to phones, another edit distance metric called "**feature edit distance**" is calculated for each etymon. Feature edit distance sheds light on how many **features** off an etymon is. It is therefore a more fine-grained metric.
