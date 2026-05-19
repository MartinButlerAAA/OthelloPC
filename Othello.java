//************************************************************************************************************************
//* Othello			Martin Butler	May 2026
//*
//* Othello class to display the Othello game (based on Wii U C version).
//*
//* This uses the Game class for all game logic, including the computer AI moves.
//*
//************************************************************************************************************************
import java.awt.*;			// General Java
import java.awt.event.*;	// To support keyboard and windows inputs.
import javax.swing.*;		// Swing windows interface.

// Small class to implement an array of valid moves (replaces a C data-structure from Wii U).
class vMove_t {
	int x;		// x position on board 1-8 left to right.
	int y;		// y position on board 1-8 top to bottom.

	// Constructor to set the values to 0 (not needed in C).
	vMove_t() {
		x = 0;
		y = 0;
	}
}

// The main Othello class that includes all code to operate the Windows display and get user input.
// This calls functions from the Game class for game processing.
public class Othello extends JFrame implements KeyListener {

	// Constants for the states to control the game play state machine.
	private static int SETUP_ST = 0;
	private static int PLAYERMOVE_ST = 1;
	private static int PANIMATE_ST = 2;
	private static int AIMOVE_ST = 3;
	private static int AIANIMATE_ST = 4;
	private static int NEWGAME_ST = 5;
	private static int gameState = SETUP_ST;	// The state index for the state machine.
	private static int anm8 = 0;				// Count used for animation within game states.
	private static int key = '.';				// Key pressed by user (set to a invalid key so it is not used).

	private static vMove_t[] vMoves = new vMove_t[60];	// Array to store valid moves (60 is the maximum number of available spaces on the board at the start of the game).
	private static int nmoves = 0;		// Number of valid moves for a particular player or computer turn.
	private static int selMove = 0;		// Currently chosen move.

	// Global counts to determine and count games won.
	public static int red = 2, green = 2;			// Counts for how many pieces each player has.
	public static float rWin = 0.0f, gWin = 0.0f;	// Counts for each player game wins. Floating point is used so that draws can be awarded as 0.5 each.

	// Toolkit and Resource bit gets the image into the jar file, to make jar file run standalone.
	// Images for the Othello game pieces, including animation of pieces flipping.
	private static Image  tBlank = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Blank.jpg"));
	private static Image  tAllowed = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Allowed.jpg"));
	private static Image  tSelected = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Selected.jpg"));
	private static Image  tGreen1 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Green1.jpg"));
	private static Image  tGreen2 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Green2.jpg"));
	private static Image  tGreen3 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Green3.jpg"));
	private static Image  tGreen4 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Green4.jpg"));
	private static Image  tGreen5 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Green5.jpg"));
	private static Image  tMiddle = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Middle.jpg"));
	private static Image  tRed1 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Red1.jpg"));
	private static Image  tRed2 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Red2.jpg"));
	private static Image  tRed3 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Red3.jpg"));
	private static Image  tRed4 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Red4.jpg"));
	private static Image  tRed5 = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Red5.jpg"));
	private static Image  tMissTurn = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("MissTurn.jpg"));
	private static Image  tEmpty = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("Empty.jpg"));
	private static Image  tRedWin = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("RedWin.jpg"));
	private static Image  tGreenWin = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("GreenWin.jpg"));
	private static ImageIcon iBlank = new ImageIcon(tBlank);
	private static ImageIcon iAllowed = new ImageIcon(tAllowed);
	private static ImageIcon iSelected = new ImageIcon(tSelected);
	private static ImageIcon iGreen1 = new ImageIcon(tGreen1);
	private static ImageIcon iGreen2 = new ImageIcon(tGreen2);
	private static ImageIcon iGreen3 = new ImageIcon(tGreen3);
	private static ImageIcon iGreen4 = new ImageIcon(tGreen4);
	private static ImageIcon iGreen5 = new ImageIcon(tGreen5);
	private static ImageIcon iMiddle = new ImageIcon(tMiddle);
	private static ImageIcon iRed1 = new ImageIcon(tRed1);
	private static ImageIcon iRed2 = new ImageIcon(tRed2);
	private static ImageIcon iRed3 = new ImageIcon(tRed3);
	private static ImageIcon iRed4 = new ImageIcon(tRed4);
	private static ImageIcon iRed5 = new ImageIcon(tRed5);
	private static ImageIcon iMissTurn = new ImageIcon(tMissTurn);
	private static ImageIcon iEmpty = new ImageIcon(tEmpty);
	private static ImageIcon iRedWin = new ImageIcon(tRedWin);
	private static ImageIcon iGreenWin = new ImageIcon(tGreenWin);

	// An 8x8 array of labels to make up the game board display.
	private static JLabel[][] gameBoard = new JLabel[8][8];

	// Labesl to display the game scores and space these out at the bottom of the window.
	private static Label lWinner = new Label("Winner:");
	private static JLabel lWin = new JLabel();
	private static Label lpad1 = new Label(" ");
	private static Label lRedWin = new Label("  Red: 0");
	private static Label lGreenWin = new Label(" Yellow: 0");
	private static Label lpad2 = new Label(" ");
	private static JLabel lMissTurn = new JLabel();		// Show if player of computer had to miss a turn.

	// Get key input to be processed by humanMove.
	public void keyPressed(KeyEvent input) {
		key = input.getKeyCode();
	}

	// Capture key inputs, released and typed are not necessary for this game but
	// must be declared for implementing keyListener
	public void keyReleased(KeyEvent input) {
	}

	public void keyTyped(KeyEvent input) {
	}

	// Constructor for the Connect 4 game window.
	Othello() {
        Container pane = getContentPane();
		// Grid layout is used with an extra row at the bottom for scores.
		GridLayout layout = new GridLayout(9,8,0,0);
		pane.setLayout(layout);

		// The window is sized for the images used which are 80x80 pixels.
		// Some padding is allowed either side and bottom for the edges of the window and at the top for the window banner.
		setSize((80*8)+20,(80*9)+40);
		setLocation (10,10);
		setTitle("Othello      Use left and right keys to select move and space to play");

		// Add the board positions to the window in the correct order for display using grid layout.
		for (int y = 0; y < 8; y++) {
			for(int x = 0; x < 8; x++) {
				gameBoard[x][y] = new JLabel();
				pane.add(gameBoard[x][y]);
			}
		}

		// Initialise the valid move array.
		for (int a = 0; a < 60; a++) {
			vMoves[a] = new vMove_t();
		}

		// Put the scores and miss turn display at the bottom of the window.
		pane.add(lWinner);
		pane.add(lWin);
		pane.add(lpad1);
		pane.add(lRedWin);
		pane.add(lGreenWin);
		pane.add(lpad2);
		pane.add(lMissTurn);

 		// This is just to close down the window properly. 
		addWindowListener(new WindowAdapter () {
			public void windowClosing(WindowEvent e) {
					System.exit(0);
			}
		});

		// Add key listener fot game control and make sure game is in windows focus so keys are received.
		addKeyListener(this);
		setFocusable(true);

		// Layout may get screwed up if the window can be re-sized so disable it.
		setResizable(false);
		setVisible(true);	
	}

	// Function to display the board.
	public static void displayBoard(int anm8) {
		// The game board piece counts are sent to the standard output to support development.
		System.out.printf("Red Pieces: %2d  Green Pieces: %2d\n", red, green);

		// Display the Othello board.
		for (int y = 0; y < 8; y++) {
			for(int x = 0; x < 8; x++) {
				// Game table is 1 to 8 in each direction but display array starts at 0s.
				if (Game.getGameTable(x+1,y+1) == 'G') { gameBoard[x][y].setIcon(iGreen1); }
				// The anm8 pass parameter is used to sequence the flip animation.
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  0)) { gameBoard[x][y].setIcon(iGreen1); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  1)) { gameBoard[x][y].setIcon(iGreen2); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  2)) { gameBoard[x][y].setIcon(iGreen3); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  3)) { gameBoard[x][y].setIcon(iGreen4); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  4)) { gameBoard[x][y].setIcon(iGreen5); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  5)) { gameBoard[x][y].setIcon(iMiddle); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  6)) { gameBoard[x][y].setIcon(iRed1); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  7)) { gameBoard[x][y].setIcon(iRed2); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  8)) { gameBoard[x][y].setIcon(iRed3); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 ==  9)) { gameBoard[x][y].setIcon(iRed4); }
				if ((Game.getGameTable(x+1,y+1) == 'g') && (anm8 == 10)) { gameBoard[x][y].setIcon(iRed5); }
				if (Game.getGameTable(x+1,y+1) == 'R') { gameBoard[x][y].setIcon(iRed5); }
				// The anm8 pass parameter is used to sequence the flip animation.
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  0)) { gameBoard[x][y].setIcon(iRed5); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  1)) { gameBoard[x][y].setIcon(iRed4); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  2)) { gameBoard[x][y].setIcon(iRed3); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  3)) { gameBoard[x][y].setIcon(iRed2); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  4)) { gameBoard[x][y].setIcon(iRed1); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  5)) { gameBoard[x][y].setIcon(iMiddle); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  6)) { gameBoard[x][y].setIcon(iGreen5); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  7)) { gameBoard[x][y].setIcon(iGreen4); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  8)) { gameBoard[x][y].setIcon(iGreen3); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 ==  9)) { gameBoard[x][y].setIcon(iGreen2); }
				if ((Game.getGameTable(x+1,y+1) == 'r') && (anm8 == 10)) { gameBoard[x][y].setIcon(iGreen1); }
				if (Game.getGameTable(x+1,y+1) == 'V') { gameBoard[x][y].setIcon(iAllowed); }
				if (Game.getGameTable(x+1,y+1) == ' ') { gameBoard[x][y].setIcon(iBlank); }
			}
		}
		// The currently selected valid move is overwritten with the seleted icon.
		if ((vMoves[selMove].x > 0) && (vMoves[selMove].y > 0)) {
			gameBoard[vMoves[selMove].x-1][vMoves[selMove].y-1].setIcon(iSelected);
		}
		return;
	}

	// This function prompts the human player for their move. 
	// The Game API is called to check if the player move is a valid move, if not they are prompted again.
	public static boolean humanMove() {
		if (key == KeyEvent.VK_LEFT) { selMove--; }						// Move backwards through the valid moves.
		if (key == KeyEvent.VK_RIGHT) { selMove++; }					// Move forwards through the valid moves.
		if ((selMove < 0) && (nmoves >= 1)) { selMove = nmoves - 1; }	// Wrap to the end of the valid moves.
		if (selMove >= nmoves) { selMove = 0; }							// Wrap to the start of the valid moves.

		// Error trap in case there are no valid moves, to stop selected move going negative.
		if (selMove < 0) { selMove = 0; }

		// If the space bar is presses pass the move to the game logic to try it out.
		// If the move worked return true.
		// As these are all valid moves there should never be a problem, this is just an extra error check.
		if (key == ' ') {
			if (Game.putMove(vMoves[selMove].x, vMoves[selMove].y) == true ) {
				key = '.'; // Once key used set it to an invalid key.
				return true;
			}
		}
		key = '.'; // Once key used set it to an invalid key.
		return false;
	}

	// Find an array of the valid moves, which is used by the logic to allow the player to use arrow keys to select their move.
	private static void findValidMoves()
	{
		nmoves = 0;	// Number of valid moves found.
		for (int x = 1; x <= 8; x++) {
			for (int y = 1; y <= 8; y++) {
				if (Game.getGameTable(x, y) == 'V') { vMoves[nmoves].x = x; vMoves[nmoves].y = y; nmoves++; }
			}
		}
		if (nmoves > 0) {
			selMove = nmoves - 1;	// Set the first valid move to the last valid move found.
		}
		else {
			selMove = 0;	// Avoid setting selected move outside the array.
		}
	}

	// The main program always starts at main. This just runs Othello.
	public static void main(String[] args) {
		Othello oth = new Othello();	// Create the window for the game.

		Game.clearGameTable();			// Set up the game table.
		lWin.setIcon(iEmpty);			// Show no winner at the start.
		lMissTurn.setIcon(iEmpty);		// Show no missed turn.
		Game.checkBoard('R');		// Check the board to get the pieces counts before the first display.

		// Play the game indefinitely. Alternating human then computer to play first.
		// Note that human always plays R red and computer G green.
		for (;;) {
			if (gameState == SETUP_ST) {
				Game.clearGameTable();		// Set up the game table.
				Game.checkBoard('R');	// Check the board to get the pieces counts before the first display.
				Game.validRedMoves();		// Identify all of the valid moves that the human player can make.
				findValidMoves();			// Get list of valid moves to support human move selection.
				gameState = PLAYERMOVE_ST;	// Move on to the player move.
			}
			else if (gameState == PLAYERMOVE_ST) {
				// Check if there is a valid move available.
				if (Game.checkBoard('R') != 'M') {
					// If there are valid moves let the player select one.
					if (humanMove() == true) { 
						selMove = 59;		// Setting the selected move to the end of the table means x and y are 0, so it is no longer displayed.
						gameState = PANIMATE_ST;
						anm8 = 10;			// Set the animation count down to sequence the flip animation.
					}
				}
				// Otherwise go to the computer move.
				else {
					selMove = 59;					// Setting the selected move to the end of the table means x and y are 0, so it is no longer displayed.
					gameState = PANIMATE_ST;		// Move to on to animating the player move.
					lMissTurn.setIcon(iMissTurn);	// Show player had to miss a turn.
					anm8 = 10;						// Allow a delay to see the missed turn.
				}
			}
			else if (gameState == PANIMATE_ST) {				
				if (anm8 <= 0) {					// Wait for animation to complete before moving on.
					lMissTurn.setIcon(iEmpty);		// Clear out any miss turn.
					// Check if the game is over.
					if (Game.checkBoard('B') != ' ') { 
						gameState = NEWGAME_ST; 
						anm8 = 10;					// Allow time to see the end of game.
					}
					else {
						Game.validGreenMoves();		// Identify the valid moves for the computer.
						gameState = AIMOVE_ST; 
						anm8 = 10;					//Allow time.
					}
				}
				anm8--;
			}
			else if (gameState == AIMOVE_ST) {		// Do the AI move.
				if (anm8 <= 0) {					// Allow time for valid options to be seen before making move. 
					// Check if there is a valid move available and make it
					if (Game.checkBoard('G') != 'M') {
						Game.computerMove();
					}
					else { // Otherwise show the computer had to miss a go.
						lMissTurn.setIcon(iMissTurn);
					}
					gameState = AIANIMATE_ST;
					anm8 = 10;						// Allow time for flip animation.
				}
				anm8--;
			}
			else if (gameState == AIANIMATE_ST) {
				if (anm8 <= 0) {					// Wait for flip animation to complete before moving on.
					lMissTurn.setIcon(iEmpty);		// Clear out any miss turn.
					// Check if the game is over.
					if (Game.checkBoard('B') != ' ') {
						gameState = NEWGAME_ST;
						anm8 = 10;					// Allow time for end game to be seen.
					}
					else {
						Game.validRedMoves();	// Identify all of the valid moves that the human player can make.
						findValidMoves();	// Get list of valid moves to support human move.
						gameState = PLAYERMOVE_ST;
						// No need for delay to see player move as nothing happens until button pressed.
					}
				}
				anm8--;
			}
			else if (gameState == NEWGAME_ST) {
				if (anm8 <= 0) {					// Wait to show game complete before starting a new one.
					// Work out who won, update counts and display icon of winner.
					if (red > green) { rWin = rWin + 1.0f; lWin.setIcon(iRedWin); }
					else if (red < green) { gWin = gWin + 1.0f; lWin.setIcon(iGreenWin); }
					else { rWin = rWin + 0.5f; gWin = gWin + 0.5f;  lWin.setIcon(iEmpty); }	// Need to consider the players can draw.
					lRedWin.setText("  Red: " + rWin);
					lGreenWin.setText("  Green: " + gWin);
					Game.clearGameTable();		// Set up the game table ready for the next game.
					Game.checkBoard('B');	// Check the board to get the pieces counts before the first display.

					// Even games have human first, odd games have computer first.
					if ((((int)(rWin + gWin + 0.05f)) % 2) == 0) {
						Game.validRedMoves();				// Identify all of the valid moves that the human player can make.
						findValidMoves();					// Get list of valid moves to support selection of human move.
						gameState = PLAYERMOVE_ST;			// Human player first.
					}
					else {
						Game.validGreenMoves();				// Identify the valid moves for the computer.
						gameState = AIMOVE_ST;				// Computer first.
					}
				}
				anm8--;
			}

			displayBoard(anm8);	// Show the board which will include the valid moves available.
			oth.repaint();		// Repaint to make sure window is up to date.			

			// Sleep that loop isn't hogging processing time and to allow player to see what is going on.
			try {
				Thread.sleep(60);
			}
			catch (InterruptedException e) {}
		}
	}	
}
