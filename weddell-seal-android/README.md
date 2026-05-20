# weddell-seal-mark-recap-app
Next generation Android app to support data capture in the field

November 26, 2023
Test Version: app-pup-entry-patch
Home Screen
-Bug fix: moved start census and start observation to bottom navigation bar on home screen

November 19, 2023
Test Version: app-pup-entry
Home Screen
-Bug fix: Replaced the OutlinedTextField to ensure that input field behavior is similar between the Home and Observation Entry Screens
Observation Screen
-Updated view for adding a pup to the observation and viewing summary data when the user saves an observation
-Added bottom bar navigation to Home, View Pup, View Adult, Save
-Bug fix: Condition field no longer shifts its position left when the dropdown is selected

November 8, 2023
Test Version: app-debug-sealCard
Added fields to Observation Screen, add pup button (currently not active)
Looking for feedback on layout, colors, and general field usability
Added new screen to view recent observations
Updates to home screen to add high level metadata for each observation, add cute seal photo

October 27, 2023
Test Version: app-debug-db-view
Addition of scrollable database view to the home page that will populate after observations are saved
Build CSV File displays a file picker, allowing the user to create a new file in Internal Storage on the Android device. A database query to select all records is executed once a file is created and the results are written to the file (currently in BIN format - known bug).

October 18, 2023
Test Version : app-debug-csvWriter-obsScreen
Home screen displays two action options, add observation and write to csv
Write to csv capability is still being tested with File Picker functionality coming soon to allow for a user to select the desired location for the csv file
Add observation takes the user to an updated screen that has interactive fields that reflect the user entered values in addition to metadata fields such as date and gps
Save observation functionality is still being tested and will result in writing a record to a local database to persist all observations collected during an app’s session

October 9, 2023
Test Version: app-debug-nav-gps-screen
App installs and loads an initial screen with the option to click the plus sign to navigate to a screen that displays gps fields and datetime

October 2, 2023
Test Version: app-debug-gps (not committed)
App will not start if the location services are disabled

September 26, 2023
Test Version: app-debug-field-to-csv (not committed)
Expect to see the following upon successful installation and opening of the apk file
Fields should accept values and button should populate a csv file
Testing steps:
Please enter data in one or both fields then select “Save to CSV”.  
Take photos of any errors and provide details on whether you can locate a file called data.csv on the tablet.

September 21, 2023
Test Version: app-debug.apk (not committed)
Expect to see the following upon successful installation and opening of the apk file
