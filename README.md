# ECM2414-Coursework-Card-Game

## Table of Contents
- [Overview](#overview)
- [Requirements](#requirements)
- [Compiling and Running the Game](#compiling-and-running-the-game)
- [Running the Test Suite](#running-the-test-suite)
- [Project Structure](#project-structure)
- [Extra Information](#extra-information)

## Overview
We have created a multi-threaded card game playing simulation, where n players compete to hold exactly 4 cards of the same value.
<br>The players are positioned in a ring with a deck in between each pair of adjacent players.
<br>Initially, each player is dealt 4 cards and then each deck is dealt 4 cards from a pack containing 8n cards.
<br>If any players are dealt 4 cards with the same value, they immediately win.
<br>Whilst there is no winner, players simultaneously draw from the top of the deck to their left, and discard a card to the bottom of the deck to their right.
<br>Each player prefers a value equal to their index and discards the card they have held for the longest not matching their preferred value.
<br>All players repeat this process until any player holds 4 cards of the same value; at this point any players with 4 cards of the same value win.
<br>For each player, a file is updated recording which cards they hold, draw and discard after each turn.

## Requirements
- Java Development Kit (JDK) 17 or later
  - developed and tested with Eclipse Temurin JDK 25.0.3
- A terminal
- The game itself only uses the Java standard library and needs no external libraries
- The test suite uses **JUnit 5 (Jupiter 5.10.2, Platform 1.10.2)**. All required jars are included in `lib/`, so nothing needs to be downloaded:
  - `junit-jupiter-api-5.10.2.jar`
  - `junit-jupiter-engine-5.10.2.jar`
  - `junit-jupiter-params-5.10.2.jar`
  - `junit-platform-commons-1.10.2.jar`
  - `junit-platform-engine-1.10.2.jar`
  - `junit-platform-launcher-1.10.2.jar`
  - `opentest4j-1.3.0.jar`
  - `apiguardian-api-1.1.2.jar`

## Compiling and Running the Game
All commands are run from the project root (the folder containing this README). The game classes are in the package `src`, so they must be compiled and run from the project root, not from inside `src/`.

Run the executable jar:
```
java -jar cards.jar
```

Or compile and run from source:
```
javac src/*.java
java src.CardGame
```

- You will be prompted to enter the number of players, until you enter a positive integer
- You will then be prompted to enter the location of a pack to load, until you enter a valid pack
  - A valid pack is a plain text file with exactly 8n rows (n is the number of players), each row containing a single non-negative integer
  - If no card value appears four times in the pack, a warning is printed as nobody can win; press Ctrl + C to stop the game
- Two example packs are provided (paths are relative to the folder the game is run from):
  - 4 players: `Packs/four.txt`
  - 8 players: `Packs/eight.txt`
- The text `Player {x} wins` is printed to the terminal, {x} being the winning player's ID
- The output files `player{x}_output.txt` and `deck{x}_output.txt` are written to the folder the game is run from

## Running the Test Suite
From the project root, compile the game and test classes into `bin/`, then run the test suite:
```
javac -cp "lib/*" -d bin src/*.java test/*.java
java -cp "bin;lib/*" TestRunner
```
- On macOS/Linux, use `:` instead of `;` in the classpath: `java -cp "bin:lib/*" TestRunner`
- `TestRunner` runs every test class (`CardTest`, `CardDeckTest`, `PackTest`, `PlayerTest`, `CardGameTest`, `GameStateTest`) using the JUnit Platform launcher
- A summary of the results (tests found, succeeded and failed, plus details of any failures) is printed to the terminal
- The runner exits with status 0 if every test passes and 1 if any test fails
- The tests do not rely on any external testing files; any packs and output files they need are created in temporary directories

## Project Structure
```
ECM2414-Coursework-Card-Game/
├── Packs/
│   ├── eight.txt
│   └── four.txt
├── bin/                compiled classes (created by the test compile command)
├── lib/                JUnit 5 jars needed by the test suite
├── report/
│   ├── ECM2414 Coursework Report.docx
│   └── ECM2414 Pair Programming Development Process and Report.docx
├── src/
│   ├── Card.java
│   ├── CardDeck.java
│   ├── CardGame.java
│   ├── GameState.java
│   ├── InvalidPackException.java
│   ├── Pack.java
│   └── Player.java
├── test/
│   ├── CardDeckTest.java
│   ├── CardGameTest.java
│   ├── CardTest.java
│   ├── GameStateTest.java
│   ├── PackTest.java
│   ├── PlayerTest.java
│   └── TestRunner.java
└── README.md
```

## Extra Information
- Project name: ECM2414-Coursework-Card-Game
- Project goal: A fully working card game simulation
- Language: Java
- Authors: Samuel Bartle and Arlo Risby
