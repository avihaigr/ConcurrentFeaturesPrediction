# ConcurrentFeaturesPrediction
Elaborate the FeaturesPrediction Prom plugin to support also concuurent instances

The main changes to the origin plugin are theses two augmentatoin  classes:

ConcurrentInstances - support the concurrent features based on concurrent instances
ConcurrentNumberExecution - support the concurrent features based on concurrent events

These two classes works similar - they calculate all the available concurrent features as they first initilized,
and in the augentation phase of the plugin they supply the specific feature that the user select.
This is why they take long run time in the initiation.

Many other change made in the origin plugin to support the use of these classes.
Other changes fixed some bugs, while other changes elabore the standart abilities of the plugin

In order to run this code one have to use java IDE (eclipse) and run the "ProM with UITopia" launcher
