import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class MemoryGame extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/memorygameleaderboard";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "Leahparap7";

    private JPanel cardPanel;
    private List<CardButton> cards;
    private List<CardButton> faceUpCards = new ArrayList<>(); // To keep track of face-up cards
    private int pairsFound = 0;

    private JLabel scoreLabel; // Label to display the player's score
    private Timer gameTimer;
    private int elapsedTime = 0; // Time elapsed in seconds

    private int lowestTime = Integer.MAX_VALUE; // Initialize to a large value

    private String[] symbols = {"A", "B", "C", "D", "E", "F", "G", "H"};

    public MemoryGame() {
        setTitle("Memory Game");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardPanel = new JPanel(new GridLayout(4, 4, 5, 5)); // 4x4 grid
        cards = new ArrayList<>();

        List<String> symbolsList = new ArrayList<>();
        for (String symbol : symbols) {
            symbolsList.add(symbol);
            symbolsList.add(symbol);
        }
        Collections.shuffle(symbolsList);

        for (String symbol : symbolsList) {
            CardButton card = new CardButton(symbol);
            card.addActionListener(new CardClickListener());
            cards.add(card);
            cardPanel.add(card);
        }

        scoreLabel = new JLabel("Time: 0 seconds"); // Initialize the score label as a timer
        add(scoreLabel, BorderLayout.NORTH); // Add the label to the top of the panel

        add(cardPanel, BorderLayout.CENTER);

        // Start the game timer
        gameTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                elapsedTime++;
                scoreLabel.setText("Time: " + elapsedTime + " seconds");
            }
        });
        gameTimer.start();
    }

    // Database connection and insertScore methods
    private Connection establishConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            System.out.println("Connected to the database");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    private void insertScore(Connection connection, String playerName, int score) {
        try {
            // Assuming you have a 'leaderboard' table with columns 'player_name' and 'score'
        	
            String sql = "INSERT INTO leaderboard (player_name, score) VALUES (?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, playerName);
            preparedStatement.setInt(2, score);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Save the player's score in the database and check for lowest time
    private void saveScore(int timeInSeconds) {
        Connection connection = establishConnection();
        if (connection != null) {
            insertScore(connection, "Player", timeInSeconds);
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Check if the player's time is the lowest
        if (timeInSeconds < lowestTime) {
            lowestTime = timeInSeconds;
        }
    }

    private class CardButton extends JButton {
        private String symbol;
        private boolean isFaceUp;

        public CardButton(String symbol) {
            this.symbol = symbol;
            this.isFaceUp = false;
            setText("");
        }

        public String getSymbol() {
            return symbol;
        }

        public boolean isFaceUp() {
            return isFaceUp;
        }

        public void flip() {
            if (!isFaceUp) {
                setText(symbol);
            } else {
                setText("");
            }
            isFaceUp = !isFaceUp;
        }
    }

    private class CardClickListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            CardButton clickedCard = (CardButton) e.getSource();

            if (clickedCard.isFaceUp() || faceUpCards.size() >= 2) {
                return; // Ignore clicks on face-up cards or if two cards are already face-up
            }

            clickedCard.flip();
            faceUpCards.add(clickedCard);

            if (faceUpCards.size() == 2) {
                if (faceUpCards.get(0).getSymbol().equals(faceUpCards.get(1).getSymbol())) {
                    // Match found
                    pairsFound++;
                    faceUpCards.clear();
                    if (pairsFound == symbols.length) {
                        gameTimer.stop(); // Stop the timer
                        String congratulatoryMessage = "Congratulations! You've won. Time taken: " + elapsedTime + " seconds";

                        // Check if it's the lowest time
                        if (elapsedTime == lowestTime) {
                            congratulatoryMessage += "\nThis is your lowest time!";
                        }

                        JOptionPane.showMessageDialog(MemoryGame.this, congratulatoryMessage);
                        saveScore(elapsedTime);
                        System.exit(0);
                    }
                } else {
                    // No match, flip cards back
                    Timer timer = new Timer(1000, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            for (CardButton card : faceUpCards) {
                                card.flip();
                            }
                            faceUpCards.clear();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MemoryGame game = new MemoryGame();
            game.setVisible(true);
        });
    }
}
