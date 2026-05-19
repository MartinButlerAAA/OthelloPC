//************************************************************************************************************************
//* Game Othello			Martin Butler	May 2026
//*
//* Game class to play Othello against the computer (based on Wii U C version).
//*
//* The processing of moves and checking, along with logic to determine player counts and game end.
//* It now also includes the AI processing for the computer move.
//*
//* The machine learning to optimise the game weightings has not been ported into the java version.
//*
//************************************************************************************************************************
import java.util.Random;	// To support random first moves and random play in MEDIUM and EASY.

// Small class to implement an array of valid moves (replaces a C data-structure from the Wii U version).
class validMove_t {
	int x;		// x position on board 1-8 left to right.
	int y;		// y position on board 1-8 top to bottom.
	int score;	// Calculated score used to select valid moves.

	// Constructor to set the values to 0 (not needed in C).
	validMove_t() {
		x = 0;
		y = 0;
		score = 0;
	}
}

// The class for the Othello game.
public class Game {

	// Integers to replace the enum used in the C version. This sets the difficulty level.
	private static int EASY = 0;
	private static int MEDIUM = 1;
	private static int HARD = 2;
	private static int difficulty = HARD;

	// Integer constant weightings used for calculating computer moves. These are variable to support optimisation.
	// Note that these values could not be improved by a full optimisation run of 1000 x 10000 (10 million) games.
	private static int CNR =  10;	// Score for capturing a corner position.
	private static int CNO = -10;	// Score for possible corner for opponent.
	private static int CN2 =   3;	// Score for possible corner next move.
	private static int EG2 =   3;	// Score for edge next to a corner we have.
	private static int NCN =  -4;	// Score to avoid playing next to an available corner.
	private static int EDG =   3;	// Score for playing position two away from an available corner.
	private static int BTO =   2;	// Score for playing between own pieces to fill in the gaps.
	private static int ERL =   5;	// Score to stay in the middle early on in the game. 

	// The game board is 8x8. The game table has a border all the way round of 'B' for boundary.
	// This allows indices of 1 to 8 to be used as these are easier to understand and match the 8 by 8 board. 
	// Having edges allows logic to search all round each board position without exceeding the array size.
	private static char[][]   gameTable = new char[10][10];
	// Used as a working copy of the game board, so that move processing does not corrupt the actual game table.
	private static char[][]   workingTable = new char[10][10];

	// This is a small function to clear all places in the game table ready for a new game.
	public static void clearGameTable() {
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y++) {
				gameTable[x][y] = ' ';
			}
		}
		// Mark a border round the outside 
		for (int x = 0; x < 10; x++) {
			gameTable[x][0] = 'B';
			gameTable[x][9] = 'B';
			gameTable[0][x] = 'B';
			gameTable[9][x] = 'B';
		}

		// The starting four pieces are placed in the middle of the board.
		gameTable[4][4] = 'R';
		gameTable[4][5] = 'G';
		gameTable[5][5] = 'R';
		gameTable[5][4] = 'G';

		return;
	}

	// Copy the current gameTable to the working table.
	private static void tableToWorking() {
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y++) {
				workingTable[x][y] = gameTable[x][y];
			}
		}
	}

	// Get the specified contents from the game table to support board display.
	public static char getGameTable(int x, int y) {
		// If the pass parameters are in range of the gameTable, return the corresponding character for the game content.
		if ((x >= 1) && (x <= 8) && (y >= 1) && (y <= 8)) {
			return (gameTable[x][y]);
		}
		return (' ');	// Otherwise return blank.
	}

	// Flipped counters are shown in lowercase to identify the pieces that have been captured by a move.
	// Once the captured pieces have been displayed these need to be replaced with uppercase to simplify the rest of game processing.
	private static void clearFlips() {
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y++) {
				// Replace lower case red or green pieces with upper case, in both the game table and working copy.
				if (gameTable[x][y] == 'g') gameTable[x][y] = 'G';
				if (gameTable[x][y] == 'r') gameTable[x][y] = 'R';
				if (workingTable[x][y] == 'g') workingTable[x][y] = 'G';
				if (workingTable[x][y] == 'r') workingTable[x][y] = 'R';
			}
		}
	}

	// Clear out valid move markers (once the move has been selected and played).
	private static void clearValid() {
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y++) {
				if (gameTable[x][y] == 'V') gameTable[x][y] = ' ';
			}
		}
	}

	// Clear out valid move markers from the working copy of the game table.
	private static void clearValidWorking() {
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y++) {
				if (workingTable[x][y] == 'V') workingTable[x][y] = ' ';
			}
		}
	}

	// This function identifies all of the green pieces that have been flipped by a red move, marking them as lower case r.
	// This function relies on clearValid being used to get rid of any V markers before it is run.
	// The processing is similar to searching for valid moves. The function searches in all directions from the position selected.
	private static void captureGreen(int xi, int yi) {
		int xe, ye;		// Local processing indices.

		// Search right from the new piece to see if there is another Red 'R' piece.
		xe = 0;
		for (int x = xi + 1; x < 9; x++) { 
			if (gameTable[x][yi] == ' ') { break; }		// If there is a gap stop searching.
			// If another Red piece is found capture it's position.
			if (gameTable[x][yi] == 'R') { xe = x; break; }
		}
		// If the search found another Red piece then go through the pieces in between and mark them to be flipped.
		if (xe != 0) {
			for (int x = xi + 1; x < xe; x++) {
				if (gameTable[x][yi] == 'G') { gameTable[x][yi] = 'r'; }
			}
		}
		// Search left from the moved piece to see if there is another Red 'R' piece.
		xe = 0;
		for (int x = xi - 1; x > 0; x--) {
			if (gameTable[x][yi] == ' ') { break; }		// If there is a gap stop searching.
			// If another Red piece is found capture it's position.
			if (gameTable[x][yi] == 'R') { xe = x; break; }
		}
		// If the search found another Red piece then go through the pieces in between and mark them to be flipped.
		if (xe != 0) {
			for (int x = xi - 1; x > xe; x--) {
				if (gameTable[x][yi] == 'G') { gameTable[x][yi] = 'r'; }
			}
		}
		// Search down from the moved piece to see if there is another Red 'R' piece.
		ye = 0;
		for (int y = yi + 1; y < 9; y++) {
			if (gameTable[xi][y] == ' ') { break; }		// If there is a gap stop searching.
			// If another Red piece is found capture it's position.
			if (gameTable[xi][y] == 'R') { ye = y; break; }
		}
		// If the search found another Red piece then go through the pieces in between and mark them to be flipped.
		if (ye != 0) {
			for (int y = yi + 1; y < ye; y++) {
				if (gameTable[xi][y] == 'G') { gameTable[xi][y] = 'r'; }
			}
		}
		// Search up from the moved piece to see if there is another Red 'R' piece.
		ye = 0;
		for (int y = yi - 1; y > 0; y--) {
		if (gameTable[xi][y] == ' ') { break; }		// If there is a gap stop searching.
		// If another Red piece is found capture it's position.
		if (gameTable[xi][y] == 'R') { ye = y; break; }
		}
		// If the search found another Red piece then go through the pieces in between and mark them to be flipped.
		if (ye != 0) {
			for (int y = yi - 1; y > ye; y--) {
				if (gameTable[xi][y] == 'G') { gameTable[xi][y] = 'r'; }
			}
		}
		// Search diagonal down/right from the moved piece to see if there is another Red 'R' piece.
		xe = 0;
		ye = 0;
		for (int a = 1; a < 8; a++) {
			// If x or y has got to the end of the board stop searching.
			if (((xi + a) > 8) || ((yi + a) > 8)) { break; }
			if (gameTable[xi + a][yi + a] == ' ') { break; }		// If there is a gap stop searching.
			// If another Red piece is found capture it's position.
			if (gameTable[xi + a][yi + a] == 'R') { xe = xi + a; ye = yi + a; break; }
		}
		// If the search found another Red piece then go through the pieces in between and mark them to be flipped.
		// Note xe and ye are set at the same time so only xe is checked.
		if (xe != 0) {
			// Count along to change captured pieces.
			for (int a = 1; a < 8; a++) {
				// If x or y has got to the end of the line stop capturing.
				if (((xi + a) >= xe) || ((yi + a) >= ye)) { break; }
				// change the opponent pieces.
				if (gameTable[xi + a][yi + a] == 'G') { gameTable[xi + a][yi + a] = 'r'; }
			}
		}
		// Search diagonal up/left from the moved piece to see if there is another Red 'R' piece.
		xe = 0;
		ye = 0;
		for (int a = 1; a < 8; a++) {
			// If x or y has got to the end of the board stop searching.
			if (((xi - a) < 1) || ((yi - a) < 1)) { break; }
			if (gameTable[xi - a][yi - a] == ' ') { break; }		// If there is a gap stop searching.
			// If another Red piece is found capture it's position.
			if (gameTable[xi - a][yi - a] == 'R') { xe = xi - a; ye = yi - a; break; }
		}
		// If the search found another Red piece then go through the pieces in between and mark them to be flipped.
		// Note xe and ye are set at the same time so only xe is checked.
		if (xe != 0) {
			// Count along to change captured pieces.
			for (int a = 1; a < 8; a++) {
				// If x or y has got to the end of the line stop capturing.
				if (((xi - a) <= xe) || ((yi - a) <= ye)) { break; }
				// change the opponent pieces.
				if (gameTable[xi - a][yi - a] == 'G') { gameTable[xi - a][yi - a] = 'r'; }
			}
		}
		// Search diagonal down/left from the moved piece to see if there is another Red 'R' piece.
		xe = 0;
		ye = 0;
		for (int a = 1; a < 8; a++) {
			// If x or y has got to the end of the board stop searching.
			if (((xi - a) < 1) || ((yi + a) > 8)) { break; }
			if (gameTable[xi - a][yi + a] == ' ') { break; }		// If there is a gap stop searching.
			// If another Red piece is found capture it's position.
			if (gameTable[xi - a][yi + a] == 'R') { xe = xi - a; ye = yi + a; break; }
		}
		// If the search found another Red piece then go through the pieces in between and mark them to be flipped.
		// Note xe and ye are set at the same time so only xe is checked.
		if (xe != 0) {
			// Count along to change captured pieces.
			for (int a = 1; a < 8; a++) {
				// If x or y has got to the end of the line stop capturing.
				if (((xi - a) <= xe) || ((yi + a) >= ye)) { break; }
				// change the opponent pieces.
				if (gameTable[xi - a][yi + a] == 'G') { gameTable[xi - a][yi + a] = 'r'; }
			}
		}
		// Search diagonal up/right from the moved piece to see if there is another Red 'R' piece.
		xe = 0;
		ye = 0;
		for (int a = 1; a < 8; a++) {
			// If x or y has got to the end of the board stop searching.
			if (((xi + a) > 8) || ((yi - a) < 1)) { break; }
			if (gameTable[xi + a][yi - a] == ' ') { break; }		// If there is a gap stop searching.
			// If another Red piece is found capture it's position.
			if (gameTable[xi + a][yi - a] == 'R') { xe = xi + a; ye = yi - a; break; }
		}
		// If the search found another Red piece then go through the pieces in between and mark them to be flipped.
		// Note xe and ye are set at the same time so only xe is checked.
		if (xe != 0) {
			// Count along to change captured pieces.
			for (int a = 1; a < 8; a++) {
				// If x or y has got to the end of the line stop capturing.
				if (((xi + a) >= xe) || ((yi - a) <= ye)) { break; }
				// change the opponent pieces.
				if (gameTable[xi + a][yi - a] == 'G') { gameTable[xi + a][yi - a] = 'r'; }
			}
		}
		return;
	}

	// This function checks the move entered and puts it into gameTable if it is a valid move.
	public static boolean putMove(int xi, int yi) {
		// Check the move entered is in range.
		if ((xi >= 1) && (xi <= 8) && (yi >= 1) && (yi <= 8)) {
			// If the move selected is valid make that move.
			if (gameTable[xi][yi] == 'V') {
				// Make the move.
				gameTable[xi][yi] = 'R';
				clearValid();	// Get rid of potential move markers

				// Call function to capture all of the pieces to be flipped by the valid red move.
				captureGreen(xi, yi);
				return true;	// Valid move.
			}
		}
		return false;	// Not a valid move.
	}

	// Identify valid red moves in the table passed in to the function.
	// The table is passed in so that the function can be used working copies of the game table.
	private static void validRedMovesWork(char table[][]) {
		clearFlips();	// Tidy up flipped pieces.
		// Go through each of the 64 positions looking for the possible start of a valid move (adjacent to a green piece).
		for (int x = 1; x <= 8; x++) {
			for (int y = 1; y <= 8; y++) {
				// If the place is empty it is a possible valid move.
				if (table[x][y] == ' ') {
					// Check each of the 8 valid directions to see if the position is next to an opponent piece.
					// If left is next to an opponent piece search for a player piece in that direction.
					if (table[x - 1][y] == 'G') {
						// If a player piece is found on that line it is a valid move.
						for (int a = x - 2; a > 0; a--) {
							if ((table[a][y] == ' ') || (table[a][y] == 'V')) { break; } // If there is a gap it doesn't count.
							if (table[a][y] == 'R') { table[x][y] = 'V'; break; }
						}
					}
					// If right is next to an opponent piece search for a player piece in that direction.
					if (table[x + 1][y] == 'G') {
						for (int a = x + 2; a < 10; a++) {
							if ((table[a][y] == ' ') || (table[a][y] == 'V')) { break; } // If there is a gap it doesn't count.
							if (table[a][y] == 'R') { table[x][y] = 'V'; break; }
						}
					}
					// If up is next to an opponent piece search for a player piece in that direction.
					if (table[x][y - 1] == 'G') {
						for (int a = y - 2; a > 0; a--) {
							if ((table[x][a] == ' ') || (table[x][a] == 'V')) { break; } // If there is a gap it doesn't count.
							if (table[x][a] == 'R') { table[x][y] = 'V'; break; }
						}
					}
					// If down is next to an opponent piece search for a player piece in that direction.
					if (table[x][y + 1] == 'G') {
						for (int a = y + 2; a < 10; a++) {
							if ((table[x][a] == ' ') || (table[x][a] == 'V')) { break; } // If there is a gap it doesn't count.
							if (table[x][a] == 'R') { table[x][y] = 'V'; break; }
						}
					}
					// If diagonal left and up is next to an opponent piece search for a player piece in that direction.
					if (table[x - 1][y - 1] == 'G') {
						int x1 = x - 1;
						int y1 = y - 1;
						// If a player piece is found on that line it is a valid move.
						for (int a = 1; a < 8; a++) {
							x1 = x1 - 1;
							y1 = y1 - 1;
							// If the search has run off the end of the board stop searching.
							if ((x1 <= 0) || (y1 <= 0)) { break; }
							else if ((table[x1][y1] == ' ') || (table[x1][y1] == 'V')) { break; } // If there is a gap it doesn't count.
							else if (table[x1][y1] == 'R') { table[x][y] = 'V'; break; }
						}
					}
					// If diagonal down and right is next to an opponent piece search for a player piece in that direction.
					if (table[x + 1][y + 1] == 'G') {
						int x1 = x + 1;
						int y1 = y + 1;
						// If a player piece is found on that line it is a valid move.
						for (int a = 1; a < 8; a++) {
							x1 = x1 + 1;
							y1 = y1 + 1;
							// If the search has run off the end of the board stop searching.
							if ((x1 >= 9) || (y1 >= 9)) { break; }
							else if ((table[x1][y1] == ' ') || (table[x1][y1] == 'V')) { break; } // If there is a gap it doesn't count.
							else if (table[x1][y1] == 'R') { table[x][y] = 'V'; break; }
						}
					}
					// If diagonal down and left is next to an opponent piece search for a player piece in that direction.
					if (table[x - 1][y + 1] == 'G') {
						int x1 = x - 1;
						int y1 = y + 1;
						// If a player piece is found on that line it is a valid move.
						for (int a = 1; a < 8; a++) {
							x1 = x1 - 1;
							y1 = y1 + 1;
							// If the search has run off the end of the board stop searching.
							if ((x1 <= 0) || (y1 >= 9)) { break; }
							else if ((table[x1][y1] == ' ') || (table[x1][y1] == 'V')) { break; } // If there is a gap it doesn't count.
							else if (table[x1][y1] == 'R') { table[x][y] = 'V'; break; }
						}
					}
					// If diagonal up and right is next to an opponent piece search for a player piece in that direction.
					if (table[x + 1][y - 1] == 'G') {
						int x1 = x + 1;
						int y1 = y - 1;
						// If a player piece is found on that line it is a valid move.
						for (int a = 1; a < 8; a++) {
							x1 = x1 + 1;
							y1 = y1 - 1;
							// If the search has run off the end of the board stop searching.
							if ((x1 >= 9) || (y1 <= 0)) { break; }
							else if ((table[x1][y1] == ' ') || (table[x1][y1] == 'V')) { break; } // If there is a gap it doesn't count.
							else if (table[x1][y1] == 'R') { table[x][y] = 'V'; break; }
						}
					}
				}
			}
		}
		return;
	}

	// Identify valid moves for Red.
	public static void validRedMoves() {
		// Call the function to indentify the valid moves on the main game table so that these can be displayed.
		validRedMovesWork(gameTable);
	}

	// Identify valid green moves in the table passed in to the function.
	// The table is passed in so that the function can be used working copies of the game table.
	// This is identical to validRedMovesWork, I felt it was better to have separate functions for red and green, rather than one function with more complex logic.
	private static void validGreenMovesWork(char table[][]) {
		clearFlips();	// Tidy up flipped pieces.
		// Go through each of the 64 positions looking for the possible start of a valid move (adjacent to a red piece).
		for (int x = 1; x <= 8; x++) {
			for (int y = 1; y <= 8; y++) {
				// If the place is empty it is a possible valid move.
				if (table[x][y] == ' ') {
					// Check each of the 8 valid directions to see if the position is next to an opponent piece.
					// If left is next to an opponent piece search for a player piece in that direction.
					if (table[x - 1][y] == 'R') {
						// If a player piece is found on that line it is a valid move.
						for (int a = x - 2; a > 0; a--) {
							if ((table[a][y] == ' ') || (table[a][y] == 'V')) { break; } // If there is a gap it doesn't count.
							if (table[a][y] == 'G') { table[x][y] = 'V'; break; }
						}
					}
					// If right is next to an opponent piece search for a player piece in that direction.
					if (table[x + 1][y] == 'R') {
						for (int a = x + 2; a < 10; a++) {
							if ((table[a][y] == ' ') || (table[a][y] == 'V')) { break; } // If there is a gap it doesn't count.
							if (table[a][y] == 'G') { table[x][y] = 'V'; break; }
						}
					}
					// If up is next to an opponent piece search for a player piece in that direction.
					if (table[x][y - 1] == 'R') {
						for (int a = y - 2; a > 0; a--) {
							if ((table[x][a] == ' ') || (table[x][a] == 'V')) { break; } // If there is a gap it doesn't count.
							if (table[x][a] == 'G') { table[x][y] = 'V'; break; }
						}
					}
					// If down is next to an opponent piece search for a player piece in that direction.
					if (table[x][y + 1] == 'R') {
						for (int a = y + 2; a < 10; a++) {
							if ((table[x][a] == ' ') || (table[x][a] == 'V')) { break; } // If there is a gap it doesn't count.
							if (table[x][a] == 'G') { table[x][y] = 'V'; break; }
						}
					}
					// If diagonal left and up is next to an opponent piece search for a player piece in that direction.
					if (table[x - 1][y - 1] == 'R') {
						int x1 = x - 1;
						int y1 = y - 1;
						// If a player piece is found on that line it is a valid move.
						for (int a = 1; a < 8; a++) {
							x1 = x1 - 1;
							y1 = y1 - 1;
							// If the search has run off the end of the board stop searching.
							if ((x1 <= 0) || (y1 <= 0)) { break; }
							else if ((table[x1][y1] == ' ') || (table[x1][y1] == 'V')) { break; } // If there is a gap it doesn't count.
							else if (table[x1][y1] == 'G') { table[x][y] = 'V'; break; }
						}
					}
					// If diagonal down and right is next to an opponent piece search for a player piece in that direction.
					if (table[x + 1][y + 1] == 'R') {
						int x1 = x + 1;
						int y1 = y + 1;
						// If a player piece is found on that line it is a valid move.
						for (int a = 1; a < 8; a++) {
							x1 = x1 + 1;
							y1 = y1 + 1;
							// If the search has run off the end of the board stop searching.
							if ((x1 >= 9) || (y1 >= 9)) { break; }
							else if ((table[x1][y1] == ' ') || (table[x1][y1] == 'V')) { break; } // If there is a gap it doesn't count.
							else if (table[x1][y1] == 'G') { table[x][y] = 'V'; break; }
						}
					}
					// If diagonal down and left is next to an opponent piece search for a player piece in that direction.
					if (table[x - 1][y + 1] == 'R') {
						int x1 = x - 1;
						int y1 = y + 1;
						// If a player piece is found on that line it is a valid move.
						for (int a = 1; a < 8; a++) {
							x1 = x1 - 1;
							y1 = y1 + 1;
							// If the search has run off the end of the board stop searching.
							if ((x1 <= 0) || (y1 >= 9)) { break; }
							else if ((table[x1][y1] == ' ') || (table[x1][y1] == 'V')) { break; } // If there is a gap it doesn't count.
							else if (table[x1][y1] == 'G') { table[x][y] = 'V'; break; }
						}
					}
					// If diagonal up and right is next to an opponent piece search for a player piece in that direction.
					if (table[x + 1][y - 1] == 'R') {
						int x1 = x + 1;
						int y1 = y - 1;
						// If a player piece is found on that line it is a valid move.
						for (int a = 1; a < 8; a++) {
							x1 = x1 + 1;
							y1 = y1 - 1;
							// If the search has run off the end of the board stop searching.
							if ((x1 >= 9) || (y1 <= 0)) { break; }
							else if ((table[x1][y1] == ' ') || (table[x1][y1] == 'V')) { break; } // If there is a gap it doesn't count.
							else if (table[x1][y1] == 'G') { table[x][y] = 'V'; break; }
						}
					}
				}
			}
		}
		return;
	}

	// Identify valid moves for Green.
	public static void validGreenMoves() {
		// Call the function to indentify the valid moves on the main game table so that these can be displayed.
		validGreenMovesWork(gameTable);
	}

	// This function identifies all of the red pieces that have been flipped by green move, marking them as lower case g.
	// This function relies on clearValid being used to get rid of any V markers before it is run.
	// This is identical to captureGreenWork. It was deemed more maintainable to have two functions than one more complex function for both players.
	// The table is passed in so that it can be used a working copy for compuer move processing.
	private static void captureRedWork(int xi, int yi, char table[][]) {
		int xe, ye;

		// Search right from the moved piece to see if there is another Green piece.
		xe = 0;
		for (int x = xi + 1; x < 9; x++) {
			if (table[x][yi] == ' ') { break; }		// If there is a gap stop searching.
			// If another Green piece is found capture it's position.
			if (table[x][yi] == 'G') { xe = x; break; }
		}
		// If the search found another Green piece then go through the pieces in between and mark them to be flipped.
		if (xe != 0) {
			for (int x = xi + 1; x < xe; x++) {
				if (table[x][yi] == 'R') { table[x][yi] = 'g'; }
			}
		}
		// Search left from the moved piece to see if there is another Green piece.
		xe = 0;
		for (int x = xi - 1; x > 0; x--) {
			if (table[x][yi] == ' ') { break; }		// If there is a gap stop searching.
			// If another Green piece is found capture it's position.
			if (table[x][yi] == 'G') { xe = x; break; }
		}
		// If the search found another Green piece then go through the pieces in between and mark them to be flipped.
		if (xe != 0) {
			for (int x = xi - 1; x > xe; x--) {
				if (table[x][yi] == 'R') { table[x][yi] = 'g'; }
			}
		}
		// Search down from the moved piece to see if there is another Green piece.
		ye = 0;
		for (int y = yi + 1; y < 9; y++) {
			if (table[xi][y] == ' ') { break; }		// If there is a gap stop searching.
			// If another Green piece is found capture it's position.
			if (table[xi][y] == 'G') { ye = y; break; }
		}
		// If the search found another Green piece then go through the pieces in between and mark them to be flipped.
		if (ye != 0) {
			for (int y = yi + 1; y < ye; y++) {
				if (table[xi][y] == 'R') { table[xi][y] = 'g'; }
			}
		}
		// Search up from the moved piece to see if there is another Green piece.
		ye = 0;
		for (int y = yi - 1; y > 0; y--) {
			if (table[xi][y] == ' ') { break; }		// If there is a gap stop searching.
			// If another Green piece is found capture it's position.
			if (table[xi][y] == 'G') { ye = y; break; }
		}
		// If the search found another Green piece then go through the pieces in between and mark them to be flipped.
		if (ye != 0) {
			for (int y = yi - 1; y > ye; y--) {
				if (table[xi][y] == 'R') { table[xi][y] = 'g'; }
			}
		}
		// Search diagonal down/right from the moved piece to see if there is another Green piece.
		xe = 0;
		ye = 0;
		for (int a = 1; a < 8; a++) {
			// If x or y has got to the end board stop searching.
			if (((xi + a) > 8) || ((yi + a) > 8)) { break; }
			if (table[xi + a][yi + a] == ' ') { break; }		// If there is a gap stop searching.
			// If another Green piece is found capture it's position.
			if (table[xi + a][yi + a] == 'G') { xe = xi + a; ye = yi + a; break; }
		}
		// If the search found another Green piece then go through the pieces in between and mark them to be flipped.
		// Note xe and ye are set at the same time so only xe is checked.
		if (xe != 0) {
			// Count along to change captured pieces.
			for (int a = 1; a < 8; a++) {
				// If x or y has got to the end of the line stop capturing.
				if (((xi + a) >= xe) || ((yi + a) >= ye)) { break; }
				// change the opponent pieces.
				if (table[xi + a][yi + a] == 'R') { table[xi + a][yi + a] = 'g'; }
			}
		}
		// Search diagonal up/left from the moved piece to see if there is another Green piece.
		xe = 0;
		ye = 0;
		for (int a = 1; a < 8; a++) {
			// If x or y has got to the end board stop searching.
			if (((xi - a) < 1) || ((yi - a) < 1)) { break; }
			if (table[xi - a][yi - a] == ' ') { break; }		// If there is a gap stop searching.
			// If another Green piece is found capture it's position.
			if (table[xi - a][yi - a] == 'G') { xe = xi - a; ye = yi - a; break; }
		}
		// If the search found another Green piece then go through the pieces in between and mark them to be flipped.
		// Note xe and ye are set at the same time so only xe is checked.
		if (xe != 0) {
			// Count along to change captured pieces.
			for (int a = 1; a < 8; a++) {
				// If x or y has got to the end of the line stop capturing.
				if (((xi - a) <= xe) || ((yi - a) <= ye)) { break; }
				// change the opponent pieces.
				if (table[xi - a][yi - a] == 'R') { table[xi - a][yi - a] = 'g'; }
			}
		}
		// Search diagonal down/left from the moved piece to see if there is another Green piece.
		xe = 0;
		ye = 0;
		for (int a = 1; a < 8; a++) {
			// If x or y has got to the end board stop searching.
			if (((xi - a) < 1) || ((yi + a) > 8)) { break; }
			if (table[xi - a][yi + a] == ' ') { break; }		// If there is a gap stop searching.
			// If another Green piece is found capture it's position.
			if (table[xi - a][yi + a] == 'G') { xe = xi - a; ye = yi + a; break; }
		}
		// If the search found another Green piece then go through the pieces in between and mark them to be flipped.
		// Note xe and ye are set at the same time so only xe is checked.
		if (xe != 0) {
			// Count along to change captured pieces.
			for (int a = 1; a < 8; a++) {
				// If x or y has got to the end of the line stop capturing.
				if (((xi - a) <= xe) || ((yi + a) >= ye)) { break; }
				// change the opponent pieces.
				if (table[xi - a][yi + a] == 'R') { table[xi - a][yi + a] = 'g'; }
			}
		}
		// Search diagonal up/right from the moved piece to see if there is another Green piece.
		xe = 0;
		ye = 0;
		for (int a = 1; a < 8; a++) {
			// If x or y has got to the end board stop searching.
			if (((xi + a) > 8) || ((yi - a) < 1)) { break; }
			if (table[xi + a][yi - a] == ' ') { break; }		// If there is a gap stop searching.
			// If another Green piece is found capture it's position.
			if (table[xi + a][yi - a] == 'G') { xe = xi + a; ye = yi - a; break; }
		}
		// If the search found another Green piece then go through the pieces in between and mark them to be flipped.
		// Note xe and ye are set at the same time so only xe is checked.
		if (xe != 0) {
			// Count along to change captured pieces.
			for (int a = 1; a < 8; a++) {
				// If x or y has got to the end of the line stop capturing.
				if (((xi + a) >= xe) || ((yi - a) <= ye)) { break; }
				// change the opponent pieces.
				if (table[xi + a][yi - a] == 'R') { table[xi + a][yi - a] = 'g'; }
			}
		}
		return;
	}

	// Version of captureRed used by the main game processing which uses the actual game table.
	private static void captureRed(int xi, int yi) {
		captureRedWork(xi, yi, gameTable);
	}

	// Check the state of the board 'B' is for both players, 'R' for red and 'G' for green.
	// For both players the function returns whether the game has ended 'E' or ' ' for not ended.
	// If 'R' or 'G' is selected it returns 'M' for miss a turn if there are no valid moves for that player, or ' ' if there are valid moves.
	public static char checkBoard(char pl) {
		// Copy the current gameTable to the working copy so that it can be used for game processing.
		tableToWorking();

		int r = 0, g = 0;	// Working red and green counts.
		char ret = 'M';		// If there are no valid moves the player has to miss a turn.

		// If looking for game ended, need to identify all possible valid moves for either player.
		if (pl == 'B') {
			ret = 'E'; //Return value is changed to 'E' for ended when checking for both players.
			// Use the working table to avoid corrupting game table.
			// Mark all valid moves for both players.
			validRedMovesWork(workingTable);
			validGreenMovesWork(workingTable);
		}

		// If we are checking whether a player can make a move any valid moves will already be identified in the game board.
		// If any valid move is found the game is still going.
		// While checking for valid moves also count up the green and red pieces.
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y++) {
				if ((workingTable[x][y] == 'R') || (workingTable[x][y] == 'r')) { r++; }
				if ((workingTable[x][y] == 'G') || (workingTable[x][y] == 'g')) { g++; }
				if (workingTable[x][y] == 'V') { ret = ' '; }
			}
		}

		// Pass back the counts of red and green pieces and return parameter to the Othello class.
		Othello.red = r;
		Othello.green = g;
		return ret;
	}

	// Calculate the computer move AI. Find all valid moves for the current play and assess each move to give it a score.
	// The select the move with the highest score. Various aspects of the move are considered such as on an edge or corner.
	public static void computerMove() {
		Random rand = new Random();	// To use random numbers.

		validMove_t[] validMoves = new validMove_t[60];	// Array to store valid moves (60 is the maximum number of available spaces on the board at the start of the game).
		int moveN = 0;		// Valid Move count for validMoves array.
		int selN = 0;		// Selected valid move.
		int captN = 0;		// Used to record the highest score to select the best move.
		int pcnt = 0;		// Count of all pieces on the board.

		// Initialise the valid moves array ready to use.
		for (int a = 0; a < 60; a++) { validMoves[a] = new validMove_t(); }

		// Count up the pieces played.
		// The number of pieces played is used to adjust tactics during the game.
		pcnt = 0;
		for (int xi = 1; xi <= 8; xi++) {
			for (int yi = 1; yi <= 8; yi++) {
				if ((workingTable[xi][yi] != ' ') && (workingTable[xi][yi] != 'V')) { pcnt++; }
			}
		}

		// Go through the entire board looking for valid moves 'V's and log the board positions in the validMoves array.
		// Note board positions are labelled 1-8 left to right and 1-8 top to bottom, 1,1 is top left.
		// Then process move to determine a score for how good that move is.
		for (int x = 1; x <= 8; x++) {
			for (int y = 1; y <= 8; y++) {
				// If a valid move is found capture the position.
				if (gameTable[x][y] == 'V') {
					validMoves[moveN].x = x;
					validMoves[moveN].y = y;
					validMoves[moveN].score = 0;

					// Favour staying closer to the centre earlier in the game.
					if (pcnt <= 20) {
						if ((x == 4) && (y == 2)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 5) && (y == 2)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 4) && (y == 3)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 5) && (y == 3)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 2) && (y == 4)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 3) && (y == 4)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 6) && (y == 4)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 7) && (y == 4)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 2) && (y == 5)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 3) && (y == 5)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 6) && (y == 5)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 7) && (y == 5)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 4) && (y == 6)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 5) && (y == 6)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 4) && (y == 7)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
						if ((x == 5) && (y == 7)) { validMoves[moveN].score = validMoves[moveN].score + ERL; }
					}

					// Places next to C and X positions (next to a free corner) should be played to try to force to opponent to play C and X positions.
					if ((pcnt > 10) && (pcnt <= 50)) {
						if ((gameTable[1][1] == ' ') && (x == 3) && (y == 1)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][1] == ' ') && (x == 3) && (y == 2)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][1] == ' ') && (x == 3) && (y == 3)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][1] == ' ') && (x == 2) && (y == 3)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][1] == ' ') && (x == 1) && (y == 3)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][1] == ' ') && (x == 6) && (y == 1)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][1] == ' ') && (x == 6) && (y == 2)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][1] == ' ') && (x == 6) && (y == 3)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][1] == ' ') && (x == 7) && (y == 3)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][1] == ' ') && (x == 8) && (y == 3)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][8] == ' ') && (x == 1) && (y == 6)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][8] == ' ') && (x == 2) && (y == 6)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][8] == ' ') && (x == 3) && (y == 6)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][8] == ' ') && (x == 3) && (y == 7)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[1][8] == ' ') && (x == 3) && (y == 8)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][8] == ' ') && (x == 6) && (y == 6)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][8] == ' ') && (x == 7) && (y == 6)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][8] == ' ') && (x == 8) && (y == 6)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][8] == ' ') && (x == 6) && (y == 7)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
						if ((gameTable[8][8] == ' ') && (x == 6) && (y == 8)) { validMoves[moveN].score = validMoves[moveN].score + EDG; }
					}

					// If we have a corner, favour moves along edges next to the corner.
					if ((gameTable[1][1] == 'G') && ((x == 1) || (y == 1))) { validMoves[moveN].score = validMoves[moveN].score + EG2; }
					if ((gameTable[1][8] == 'G') && ((x == 1) || (y == 8))) { validMoves[moveN].score = validMoves[moveN].score + EG2; }
					if ((gameTable[8][1] == 'G') && ((x == 8) || (y == 1))) { validMoves[moveN].score = validMoves[moveN].score + EG2; }
					if ((gameTable[8][8] == 'G') && ((x == 8) || (y == 8))) { validMoves[moveN].score = validMoves[moveN].score + EG2; }

					// If the valid move is one of the four corners increase score as corners cannot be flipped.
					if ((x == 1) && (y == 1)) { validMoves[moveN].score = validMoves[moveN].score + CNR; }
					if ((x == 1) && (y == 8)) { validMoves[moveN].score = validMoves[moveN].score + CNR; }
					if ((x == 8) && (y == 1)) { validMoves[moveN].score = validMoves[moveN].score + CNR; }
					if ((x == 8) && (y == 8)) { validMoves[moveN].score = validMoves[moveN].score + CNR; }

					// Avoid giving away a corner, by playing a position next to an available corner until late in the game.
					if (pcnt <= 50) {
						if ((x == 2) && (y == 2) && (gameTable[1][1] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 1) && (y == 2) && (gameTable[1][1] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 2) && (y == 1) && (gameTable[1][1] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 7) && (y == 7) && (gameTable[8][8] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 7) && (y == 8) && (gameTable[8][8] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 8) && (y == 7) && (gameTable[8][8] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 7) && (y == 2) && (gameTable[8][1] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 7) && (y == 1) && (gameTable[8][1] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 8) && (y == 2) && (gameTable[8][1] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 2) && (y == 7) && (gameTable[1][8] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 1) && (y == 7) && (gameTable[1][8] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
						if ((x == 2) && (y == 8) && (gameTable[1][8] == ' ')) { validMoves[moveN].score = validMoves[moveN].score + NCN; }
					}

					// Favour positions that are already surrounded by your own pieces.
					if ((gameTable[x - 1][y] == 'G') && (gameTable[x + 1][y] == 'G')) { validMoves[moveN].score = validMoves[moveN].score + BTO; }
					if ((gameTable[x][y - 1] == 'G') && (gameTable[x][y + 1] == 'G')) { validMoves[moveN].score = validMoves[moveN].score + BTO; }
					if ((gameTable[x - 1][y - 1] == 'G') && (gameTable[x + 1][y + 1] == 'G')) { validMoves[moveN].score = validMoves[moveN].score + BTO; }
					if ((gameTable[x + 1][y - 1] == 'G') && (gameTable[x - 1][y + 1] == 'G')) { validMoves[moveN].score = validMoves[moveN].score + BTO; }

					// Try out the move.
					tableToWorking();					// Copy the current game to the working table for processing.	
					clearValidWorking();				// Clear valid moves as these will get in the way of working out best move.
					workingTable[x][y] = 'G';			// Try the move in the working table.
					captureRedWork(x, y, workingTable);	// Capture the pieces on the working table.

					clearFlips();	// Clear flips now they have been counted, so that the don't impact opponent valid move processing.
					validRedMovesWork(workingTable);	// See what valid moves this gives to the opponent.

					// If the move means the opponent can get a corner, reduce the score.
					if (workingTable[1][1] == 'V') { validMoves[moveN].score = validMoves[moveN].score + CNO; }
					if (workingTable[1][8] == 'V') { validMoves[moveN].score = validMoves[moveN].score + CNO; }
					if (workingTable[8][1] == 'V') { validMoves[moveN].score = validMoves[moveN].score + CNO; }
					if (workingTable[8][8] == 'V') { validMoves[moveN].score = validMoves[moveN].score + CNO; }

					// Subtracrt from the score for each valid move the opponent has, aiming to minimise their options.
					for (int xi = 1; xi <= 8; xi++) {
						for (int yi = 1; yi <= 8; yi++) {
							if (workingTable[xi][yi] == 'V') { validMoves[moveN].score--; }
						}
					}

					clearValidWorking();				// Clear the possible red moves to check the possible green moves.
					validGreenMovesWork(workingTable);	// See what valid moves this gives us.

					// If the move means that we have a chance to capture a corner increase the score.
					if (workingTable[1][1] == 'V') { validMoves[moveN].score = validMoves[moveN].score + CN2; }
					if (workingTable[1][8] == 'V') { validMoves[moveN].score = validMoves[moveN].score + CN2; }
					if (workingTable[8][1] == 'V') { validMoves[moveN].score = validMoves[moveN].score + CN2; }
					if (workingTable[8][8] == 'V') { validMoves[moveN].score = validMoves[moveN].score + CN2; }

					moveN++;	// Go on to the next valid move.
				}
			}
		}

		// Now that all valid moves have been analysed, select the valid move with the highest score.
		captN = -1000;
		for (int a = 0; a < moveN; a++) {
			if (validMoves[a].score > captN) {
				captN = validMoves[a].score;
				selN = a;
			}
		}

		// Make a random move for the first computer move, to avoid playing the same every time.
		// Less than 6 allows to play random move if computer plays second as well.
		if (pcnt < 6) { selN = rand.nextInt(moveN); }
		// Depending on difficuty level replace the calculated move with a random move.
		else if (difficulty == EASY) { if (rand.nextInt(2) == 0) { rand.nextInt(moveN); } } // 50% random moves.
		else if (difficulty == MEDIUM)	{ if (rand.nextInt(5) == 0) { rand.nextInt(moveN); } } // 20% random moves.
		// For HARD the computer calculated move is always used.

		// Play the selected valid move.
		clearValid();	// Get rid of potential move markers now move has been chosen.
		gameTable[validMoves[selN].x][validMoves[selN].y] = 'G';	// Play the selected move.
		captureRed(validMoves[selN].x, validMoves[selN].y);			// Capture the pieces.
		return;
	}
}