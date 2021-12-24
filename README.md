# **Trello Project**
![](https://img.shields.io/badge/license-yilin-blue) ![](https://img.shields.io/badge/JAVA-1.8-red) ![](https://img.shields.io/badge/SpringBoot-2.1.4.RELEASE-green)

## How to set up the properties
Step 1. get your board id and copy the id to the trello.properties in `trello.board`

Step 2. get your key and copy the key to the trello.properties in `trello.key`

Step 3. get your token and copy the token to the trello.properties in `trello.token`

Step 4. get all member full name in the board and copy the full name to the trello.properties in `trello.fullName`

> * annotation: get all member full name can use trello api
> `https://api.trello.com/1/boards/`Step1`/members?key=`Step2`&token=`Step3

## Run the spring boot server
Step 1. you need check your computer have Java8 

Step 2. double click `run.bat` 

Step 3. Open browser and goto 127.0.0.1:1087
