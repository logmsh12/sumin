package test; // 이클립스 test 패키지용 선언 추가

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class Memo1 extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private GameBoard gameBoard;
	private MainMenu mainMenu;
	private CardLayout cardLayout;

	private int[][] board = new int[20][10]; 
	private int score = 0; 
	private boolean comboActive = false; 

	private int curX = 0;                    
	private int curY = 4;                    

	private int[][] curBlock;
	private Timer timer; 
	
	private int gameMode = 0;        // 0: 클래식 모드, 1: 트리키 타워 모드
	private int magicCount = 3;      // 트리키 타워 모드 남은 마법 횟수
	private boolean gameOver = false;
	private boolean gameWon = false;
	private final int TARGET_HEIGHT = 14; 
	private final int TARGET_ROW_INDEX = 20 - TARGET_HEIGHT; 

	private final int[][][] TETROMINOS = {
		{{1, 1}, {1, 1}},             // O 모양
		{{1, 1, 1, 1}},               // I 모양
		{{0, 1, 0}, {1, 1, 1}},       // T 모양
		{{1, 0, 0}, {1, 1, 1}},       // L 모양
		{{0, 0, 1}, {1, 1, 1}},       // J 모양
		{{0, 1, 1}, {1, 1, 0}},       // S 모양
		{{1, 1, 0}, {0, 1, 1}}        // Z 모양
	};

	private Random random = new Random();

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Memo1 frame = new Memo1();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Memo1() {
		setTitle("모바일 테트리스 & 트리키 타워");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 390, 750); 
		
		cardLayout = new CardLayout();
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0)); 
		contentPane.setLayout(cardLayout);
		setContentPane(contentPane);
		
		mainMenu = new MainMenu();
		contentPane.add(mainMenu, "Menu");
		
		gameBoard = new GameBoard();
		gameBoard.setBackground(new Color(46, 78, 142)); 
		contentPane.add(gameBoard, "Game");
		
		cardLayout.show(contentPane, "Menu");
		setFocusable(true);
		
		timer = new Timer(500, e -> {
			if (gameOver || gameWon) return;
			
			if (canMove(1, 0)) {
				curX++;
			} else {
				fixBlock();
				checkLines();
				if (!gameWon) {
					createNewBlock();
				}
			}
			gameBoard.repaint();
		});
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					timer.stop();
					cardLayout.show(contentPane, "Menu");
					return;
				}

				if (!timer.isRunning() || gameOver || gameWon) return;
				
				switch (e.getKeyCode()) {
					case KeyEvent.VK_UP:     
						rotateBlock();
						break;
					case KeyEvent.VK_LEFT:
						moveBlock(0, -1);
						break;
					case KeyEvent.VK_RIGHT:
						moveBlock(0, 1);
						break;
					case KeyEvent.VK_DOWN:   
						moveBlock(1, 0);
						break;
					case KeyEvent.VK_ENTER:
					case KeyEvent.VK_SPACE:
						while (canMove(1, 0)) {
							curX++;
						}
						fixBlock();
						checkLines();
						if (!gameWon) {
							createNewBlock();
						}
						break;
					case KeyEvent.VK_M: 
						if (gameMode == 1 && magicCount > 0) {
							useMagic();
						}
						break;
				}
				gameBoard.repaint(); 
			}
		});
	}

	private void startGame(int mode) {
		gameMode = mode;
		score = 0;
		magicCount = 3;
		comboActive = false;
		gameOver = false;
		gameWon = false;
		
		for(int i = 0; i < 20; i++) {
			for(int j = 0; j < 10; j++) {
				board[i][j] = 0;
			}
		}
		
		createNewBlock();
		cardLayout.show(contentPane, "Game");
		timer.start(); 
		requestFocusInWindow();
	}

	private void useMagic() {
		for (int i = 0; i < 20; i++) {
			boolean hasBlock = false;
			for (int j = 0; j < 10; j++) {
				if (board[i][j] != 0) {
					hasBlock = true;
					break;
				}
			}
			if (hasBlock) {
				for (int j = 0; j < 10; j++) {
					board[i][j] = 0;
				}
				magicCount--;
				break;
			}
		}
	}

	private int getCurrentHeight() {
		for (int i = 0; i < 20; i++) {
			for (int j = 0; j < 10; j++) {
				if (board[i][j] != 0) {
					return 20 - i; 
				}
			}
		}
		return 0;
	}

	private void rotateBlock() {
		int rows = curBlock.length;
		int cols = curBlock[0].length;
		int[][] rotated = new int[cols][rows];
		
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				rotated[j][rows - 1 - i] = curBlock[i][j];
			}
		}
		
		int[][] temp = curBlock;
		curBlock = rotated;
		
		if (!canMove(0, 0)) {
			curBlock = temp;
		}
	}

	private boolean canMove(int dx, int dy) {
		int nextX = curX + dx;
		int nextY = curY + dy;
		
		for (int i = 0; i < curBlock.length; i++) {
			for (int j = 0; j < curBlock[i].length; j++) {
				if (curBlock[i][j] != 0) {
					int boardX = nextX + i;
					int boardY = nextY + j;
					
					if (boardX < 0 || boardX >= 20 || boardY < 0 || boardY >= 10) {
						return false;
					}
					if (board[boardX][boardY] != 0) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void moveBlock(int dx, int dy) {
		if (canMove(dx, dy)) {
			curX += dx;
			curY += dy;
		}
	}

	private void fixBlock() {
		for (int i = 0; i < curBlock.length; i++) {
			for (int j = 0; j < curBlock[i].length; j++) {
				if (curBlock[i][j] != 0) {
					board[curX + i][curY + j] = curBlock[i][j];
				}
			}
		}
	}

	private void checkLines() {
		if (gameMode == 0) {
			boolean clearedAny = false;
			for (int i = 19; i >= 0; i--) {
				boolean isLineFull = true;
				for (int j = 0; j < 10; j++) {
					if (board[i][j] == 0) {
						isLineFull = false;
						break;
					}
				}
				if (isLineFull) {
					clearedAny = true;
					score += 100; 
					for (int k = i; k > 0; k--) {
						for (int j = 0; j < 10; j++) {
							board[k][j] = board[k-1][j];
						}
					}
					for (int j = 0; j < 10; j++) {
						board[0][j] = 0;
					}
					i++; 
				}
			}
			comboActive = clearedAny;
		} else {
			if (getCurrentHeight() >= TARGET_HEIGHT) {
				gameWon = true;
				timer.stop();
			}
		}
	}

	private void createNewBlock() {
		curX = 0; 
		curY = 3; 
		
		int shapeIdx = random.nextInt(TETROMINOS.length);
		int[][] shape = TETROMINOS[shapeIdx];
		int randomColor = random.nextInt(4) + 1;
		
		curBlock = new int[shape.length][shape[0].length];
		for (int i = 0; i < shape.length; i++) {
			for (int j = 0; j < shape[i].length; j++) {
				if (shape[i][j] != 0) {
					curBlock[i][j] = randomColor; 
				} else {
					curBlock[i][j] = 0;
				}
			}
		}
		
		if (!canMove(0, 0)) {
			timer.stop();
			gameOver = true;
		} else {
			comboActive = false; 
		}
	}

	class MainMenu extends JPanel {
		private static final long serialVersionUID = 1L;

		public MainMenu() {
			setBackground(new Color(28, 44, 77));
			setLayout(null); 
			
			JLabel titleLabel = new JLabel("TETRIS");
			titleLabel.setForeground(Color.WHITE);
			titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
			titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
			titleLabel.setBounds(0, 180, 375, 60);
			add(titleLabel);
			
			JButton startButton = new JButton("클래식 모드");
			startButton.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
			startButton.setBackground(new Color(241, 196, 15));
			startButton.setForeground(Color.BLACK);
			startButton.setFocusPainted(false); 
			startButton.setBorder(new LineBorder(Color.WHITE, 2));
			startButton.setBounds(95, 360, 190, 50);
			startButton.addActionListener(e -> startGame(0));
			add(startButton);

			JButton trickyButton = new JButton("트리키 타워 모드");
			trickyButton.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
			trickyButton.setBackground(new Color(155, 89, 182)); 
			trickyButton.setForeground(Color.WHITE);
			trickyButton.setFocusPainted(false); 
			trickyButton.setBorder(new LineBorder(Color.WHITE, 2));
			trickyButton.setBounds(95, 430, 190, 50);
			trickyButton.addActionListener(e -> startGame(1));
			add(trickyButton);
		}
	}

	class GameBoard extends JPanel {
		private static final long serialVersionUID = 1L;

		Color boardBg = new Color(28, 44, 77);
		Color gridColor = new Color(40, 58, 96);
		Color[] blockColors = {
			boardBg,
			new Color(220, 53, 69),   // 1: 빨간색
			new Color(91, 192, 222),  // 2: 하늘색
			new Color(240, 173, 78),  // 3: 주황색
			new Color(149, 117, 205)  // 4: 보라색
		};

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			int startX = 30;
			int startY = 70;
			int cellSize = 30;

			if (gameMode == 0) {
				g.setColor(new Color(241, 196, 15)); 
				g.setFont(new Font("Arial", Font.BOLD, 16));
				g.drawString("👑 CLASSIC TOP", 25, 40); 

				g.setColor(Color.WHITE);
				g.setFont(new Font("Arial", Font.BOLD, 28));
				g.drawString(String.valueOf(score), 165, 50);
			} else {
				g.setColor(new Color(155, 89, 182)); 
				g.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
				g.drawString("🧱 높이: " + getCurrentHeight() + " / " + TARGET_HEIGHT, 25, 40); 

				g.setColor(Color.CYAN);
				g.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
				String magicStars = "★".repeat(magicCount) + "☆".repeat(3 - magicCount);
				g.drawString("✨ 마법(M): " + magicStars, 230, 40);
			}

			g.setColor(boardBg);
			g.fillRect(startX, startY, 10 * cellSize, 20 * cellSize);

			for (int i = 0; i < 20; i++) {
				for (int j = 0; j < 10; j++) {
					int blockType = board[i][j];

					if (curBlock != null && i >= curX && i < curX + curBlock.length && j >= curY && j < curY + curBlock[0].length) {
						int localX = i - curX;
						int localY = j - curY;
						if (curBlock[localX][localY] != 0) {
							blockType = curBlock[localX][localY];
						}
					}

					if (blockType != 0) {
						g.setColor(blockColors[blockType]);
						g.fillRect(startX + j * cellSize, startY + i * cellSize, cellSize - 1, cellSize - 1);
					} else {
						g.setColor(gridColor);
						g.drawRect(startX + j * cellSize, startY + i * cellSize, cellSize, cellSize);
					}
				}
			}

			if (gameMode == 1) {
				int goalY = startY + TARGET_ROW_INDEX * cellSize;
				g.setColor(Color.GREEN);
				for (int i = startX; i < startX + 10 * cellSize; i += 10) {
					g.drawLine(i, goalY, i + 5, goalY);
				}
				g.setFont(new Font("Arial", Font.BOLD, 11));
				g.drawString("GOAL LINE (14)", startX + 210, goalY - 5);
			}

			if (gameMode == 0 && comboActive) {
				g.setColor(Color.WHITE);
				g.setFont(new Font("Arial", Font.BOLD, 22));
				g.drawString("+100", 160, 340); 
				g.setColor(new Color(155, 89, 182));
				g.drawString("Good!", 140, 365);
			}

			if (gameOver || gameWon) {
				g.setColor(new Color(0, 0, 0, 180));
				g.fillRect(startX, startY, 10 * cellSize, 20 * cellSize);

				g.setFont(new Font("Malgun Gothic", Font.BOLD, 32));
				if (gameWon) {
					g.setColor(Color.GREEN);
					g.drawString("VICTORY!", startX + 75, startY + 260);
				} else {
					g.setColor(Color.RED);
					g.drawString("GAME OVER", startX + 55, startY + 260);
				}

				g.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
				g.setColor(Color.WHITE);
				g.drawString("ESC 키를 누르면 메뉴로 이동합니다.", startX + 40, startY + 310);
			}

			g.setColor(Color.WHITE);
			g.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
			g.drawString("트리키 타워 & 테트리스 하이브리드", 90, 695);
		}
	}
}