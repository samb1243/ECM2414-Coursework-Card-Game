# ECM2414-Coursework-Card-game

## Table of Contents
- [Overview](#overview)
- [Requirments](#requirements)
- [Compilation](#compilation)
- [Running the Test Suite](#running-the-test-Suite)
- [Project Structure](#project-structure)
- [Extra information](#extra-information)

## [Overview](#table-of-contents)
<br>We have created a multi-threaded card game playing simulation, where up to n players compete to hold exactly 4 cards of the same value.
<br>The players are positioned in a ring with a deck in between each pair of adjacent players.
<br>Initially, each player is dealt 4 cards and then each deck is dealt 4 cards from a pack containing 8n cards.
<br>If any players are dealt 4 cards with the same value, they immediately win.
<br>Whilst there is no winner, players simultaneously draw from the top of the deck to their left, and discard a card to the bottom of the deck to their right.
<br>Each player prefers a value equal to their index and discards the card they have held for the longest not matching their preferred value.
<br>All players will repeat this process until any player holds 4 cards of the same value; at this point any players with 4 cards of the same value win.
<br>For each player, a file is updated recording which cards they hold, draw and discard after each turn.

## [Requirements](#table-of-contents)
- Java Development Kit 27 (JDK27) installed
- A terminal
- Version: Java 17
- Dependencies: all imports come from Java’s standard libraries, no external libraries are required

## [Compilation](#table-of-contents)
- open a new terminal
- navigate to ECM2414-Coursework-Card-Game\src
- run the command javac *.java
- run the command java CardGAme.java
- you will be prompted to enter the number of players, until you enter a positive integer greater than 0
- you will then be prompted to enter the path to a pack for those players to use,
<br>until you enter the path to a pack that contains exactly 8n rows and at least 4 cards of the same value (n is the number of players)
- the text "Player {x} wins" will be printed to the terminal {x} being the wining players ID

## [Running the Test Suite](#table-of-contents)
- Dependencies JDK27
- open a new terminal
- navigate to ECM2414-Coursework-Card-Game\test

## [Project Structure](#table-of-contents)

<br>ECM2414-Coursework-Card-Game/
<br>├── Packs/
<br>│   ├── eight.txt
<br>│   ├── four.txt
<br>├── Bin/
<br>├── Lib/
<br>├── src/
<br>│   ├── Card.java
<br>│   ├── CardDeck.java
<br>│   ├── CardGame.java
<br>│   ├── GameState.java
<br>│   ├── InvalidPackException.java
<br>│   ├── Pack.java
<br>│   ├── Player.java
<br>├── test/
<br>│   ├── CardDeckTest.java
<br>│   ├── GameGameTest.java
<br>│   ├── CardTest.java
<br>│   ├── PackTest.java
<br>│   ├── PlayerTest.java
<br>│   ├── TestRunner.java
<br>└── README.md

## [Extra information](#table-of-contents)
- Project name: ECM2414-Coursework-Card-game
- Project goal: A fully working card game simulation
- Language: Java
- Authors: Samuel Bartle and Arlo Risby
