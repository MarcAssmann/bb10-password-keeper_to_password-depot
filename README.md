# bb10-password-keeper_to_password-depot
Convert BlackBerry 10 Password Keeper CSV to Password Depot XML format.

The following fields are mapped to to the target format:
* title
* username
* password
* url
* notes
* last modified timestamp

## Requirements
Tested with:
* Groovy Version: 2.4.7 under Windows 7
* BlackBerry Password Keeper 10.3.2.44
* Password Depot: 10.5.3


## Usage
1. Export passwords from BlackBerry 10 Password Keeper to csv format without password protection.
   In Password Keeper you find the export in the settings that can be accessed when you swipe down from the top of the screen.
2. Transfer the file to your PC
3. Convert using the script `bb10_2_pwdepot.groovy [source-path]  [target-path]`
4. Import the converted file into Password Keeper.
   It's probably a good idea to import into a new separate folder or a seperate/fresh database to inspect results and sync/check for duplicates.
       


