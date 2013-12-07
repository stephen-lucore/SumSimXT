/**
 * Stephen LuCore and Chad Stapes
 * Intro to Software Design: Group Project
 * 12-2-13
 */
package sumsimxt;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author slucore
 */
public class SumSimXT extends Canvas implements Runnable {
    
    // Graphics
    private JFrame mainFrame = new JFrame("SumSimXT");
    private JPanel mainPanel = (JPanel) mainFrame.getContentPane();
    private BufferStrategy bufferStrategy;
    private Graphics2D graphics;
    private static final int STANDARD_WIDTH = 1000;
    private static final int STANDARD_HEIGHT = 700;
    private static int gameWidth = STANDARD_WIDTH;
    private static int gameHeight = STANDARD_HEIGHT;
    private boolean fullscreen = false;
    
    // Threading and Networking
    private Executor executor = Executors.newCachedThreadPool();
    
    // Game variables
    private String classPath = SumSimXT.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static PlayerObject player;
    private HashMap<String,Integer> playerLoadout = new HashMap<>();
    private Level currentLevel;
    private MenuListener menuListener = new MenuListener();
    private FlightListener flightListener = new FlightListener();
    private HangarListener hangarListener = new HangarListener();
    boolean startGame = false;
    boolean pauseRequested = false;
    private int movementRequestedX = 0;
    private int hangarRequested = 0;
    private boolean goToHangar = false;
    private boolean embark = false;
    private boolean shotRequested = false;
    private List<ShotObject> shots = new ArrayList<>();
    private Random rand = new Random();
    
    public SumSimXT() {
        Sprite.setSpriteDir(classPath + "../../images/sprites/");
        Level.setBackgroundDir(classPath + "../../images/backgrounds/");
        this.setIgnoreRepaint(true);
        // fullscreen
        switchFullscreen();
        mainPanel.setPreferredSize(new Dimension(gameWidth,gameHeight));
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
        mainFrame.setVisible(true);
    }
    
    public void run() {
        // Finish initializing stuff
        mainFrame.getContentPane().add(this);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        this.createBufferStrategy(2);
        bufferStrategy = this.getBufferStrategy();
        graphics = (Graphics2D) bufferStrategy.getDrawGraphics();
        this.setFocusable(true);
        this.requestFocus();
        player = new PlayerObject(Sprite.getSprite("SHIP_TITAN"), new Point((gameWidth / 2) - 40, gameHeight - 110), new Dimension(75,100), 5);
        
        // Show the Menu
        currentLevel = new Level("Main Menu");
        this.addKeyListener(menuListener);
        int alpha = 0;
        int delta_alpha = 10;
        while (!startGame) {
            Image bg = currentLevel.getBackgroundA();
            graphics.drawImage(bg, 0, 0, gameWidth, gameHeight, null);
            graphics.setColor(new Color(0, 255, 0, alpha));         // green text with variable transparency
            graphics.setFont(new Font("Consolas", Font.BOLD, 72));
            graphics.drawString("Press Z to Start", Level.MainMenu.TEXT_POSITION.getX(), Level.MainMenu.TEXT_POSITION.getY());
            alpha += delta_alpha;   // fades text in and out
            if (alpha >= 255) {
                alpha = 255;
                delta_alpha = -delta_alpha;
            } else if (alpha <= 0) {
                alpha = 0;
                delta_alpha = -delta_alpha;
            }
            bufferStrategy.show();
            pause(10);  // fps limiter
        }
        this.removeKeyListener(menuListener);
        
        // Main Game Loop
        currentLevel = new Level("Level 1");
        this.addKeyListener(flightListener);
        Image bgA = currentLevel.getBackgroundA();
        Image bgB = currentLevel.getBackgroundB();
        int bgScrollerA = currentLevel.getBackgroundA().getHeight(null);
        int bgScrollerB = currentLevel.getBackgroundB().getHeight(null);
        AlphaComposite parallax = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) 0.75);    // 75% transparency for parallaxed background
        Composite original = graphics.getComposite();
        long passed, last = System.currentTimeMillis();     
        boolean levelRunning = true;
        int renderingPass = 0;      // for control of startup lag
//        for (MobObject mob : currentLevel.getMobs()) {
//            System.out.format("%s %d %d\n", mob.getName(), mob.getPoint().x, mob.getPoint().y);
//        }
        shots = new ArrayList<>();
        Timer timer = new Timer();
        Image heart = Sprite.getSprite("HEART").getImage();
        Image bigCoin = Sprite.getSprite("BIG_COIN").getImage();
        Font coinFont = new Font("Helvetica", Font.BOLD, 36);
        graphics.setFont(coinFont);
        
        while (levelRunning) {
            while (pauseRequested) {}
            if (goToHangar) {
                hangarRequested = 0;
                this.removeKeyListener(flightListener);
                hangar();
                this.addKeyListener(flightListener);
                bgA = currentLevel.getBackgroundA();
                bgB = currentLevel.getBackgroundB();
                bgScrollerA = currentLevel.getBackgroundA().getHeight(null);
                bgScrollerB = currentLevel.getBackgroundB().getHeight(null);
                player.setPoint(new Point((gameWidth / 2) - 40, gameHeight - 125));
                player.setCurrentHP(player.getTotalHP());
                shots = new ArrayList<>();
                passed = last = System.currentTimeMillis();
            }
            passed = System.currentTimeMillis() - last;
            last = System.currentTimeMillis();
            graphics.setColor(currentLevel.getBackgroundColor());
            graphics.fillRect(0,0,gameWidth,gameHeight);
            graphics.drawImage(bgB, 0, 0, gameWidth, gameHeight, 0, bgScrollerB - gameHeight, gameWidth, bgScrollerB, null);
            graphics.setComposite(parallax);
            graphics.drawImage(bgA, 0, 0, gameWidth, gameHeight, 0, bgScrollerA - gameHeight, gameWidth, bgScrollerA, null);
            graphics.setComposite(original);
            bgScrollerA += currentLevel.getBackgroundAScrollRate();
            bgScrollerB += currentLevel.getBackgroundBScrollRate();
            if (bgScrollerA - gameHeight < 0) {
                bgScrollerA = bgA.getHeight(null);
            }
            if (bgScrollerB - gameHeight < 0) {
                bgScrollerB = bgB.getHeight(null);
            }
            for (int i = 0; i < player.getCurrentHP(); i++) {
                graphics.drawImage(heart, (heart.getWidth(null) + 5) * i, gameHeight - heart.getHeight(null), heart.getWidth(null), heart.getHeight(null), null);
            }
            if (shotRequested && player.shotCooled()) {
                Sprite shotSprite = Sprite.getSprite("BULLET");
                Point shotPoint = new Point(player.getPoint());
                shotPoint.translate((player.getSprite().getImage().getWidth(null) / 2) - (shotSprite.getImage().getWidth(null) / 2), 0);
//                shotPoint.translate(0, -shotSprite.getImage().getHeight(null));
                player.startShotCooldown();
                shots.add(new ShotObject(shotSprite, shotPoint, new Dimension(shotSprite.getImage().getWidth(null), shotSprite.getImage().getHeight(null)),
                        0, -player.getShotVelocity(), 50, player));
            }
            for (int i = 0; i < shots.size(); i++) {
                ShotObject shot = shots.get(i);
                shot.move(passed);
                if (shot.isAlive()) {
                    if (shot.getSource().equals(player)) {
                        for (int j = 0; j < currentLevel.getMobs().size(); j++) {
                            MobObject mob = currentLevel.getMobs().get(j);
                            if (mob.isAlive() && detectCollision(shot, mob)) {
                                shot.setSprite(Sprite.getSprite("SMALL_EXPLOSION"));
                                shot.hitSomething();
                                new Animation(shot, Sprite.getBulletExplosion(), 50);
                                shot.moveX(-(Sprite.getSprite("SMALL_EXPLOSION").getImage().getWidth(null) / 2));
                                timer.schedule(new ObjectRemover(shot, shots), 300);
                                mob.takeDamage(shot.getDamage());
                                if (!mob.isAlive()) {
                                    timer.schedule(new ObjectRemover(mob, currentLevel.getMobs()), 500);
                                    if (rand.nextInt(10) < 2) {
                                        Sprite shotSprite = Sprite.getSprite("COIN");
                                        Point shotPoint = new Point(mob.getPoint());
                                        shotPoint.translate((mob.getSprite().getImage().getWidth(null) / 2) - (shotSprite.getImage().getWidth(null) / 2), 0);
                                        shots.add(new ShotObject(shotSprite, shotPoint, new Dimension(shotSprite.getImage().getWidth(null), shotSprite.getImage().getHeight(null)),
                                                0, mob.getShotVelocity()*2, -10, mob));
                                    }
                                }
                            }
                        }
                    } else {
                        if (detectCollision(shot, player)) {
                            shot.hitSomething();
                            if (shot.getDamage() > 0) {
                                shot.setSprite(Sprite.getSprite("SMALL_EXPLOSION"));
                                Animation animation = new Animation(shot, Sprite.getBulletExplosion(), 50);
                                shot.moveX(-(Sprite.getSprite("SMALL_EXPLOSION").getImage().getWidth(null) / 2));
                            } else {
                                shot.setSprite(Sprite.getSprite("VANISH"));
                            }
                            timer.schedule(new ObjectRemover(shot, shots), 250);
                            player.takeDamage(shot.getDamage());
                            if (!player.isAlive()) {
                                this.removeKeyListener(flightListener);
                                player.setXVelocity(0);
                                player.setYVelocity(0);
                                new Animation(player, Sprite.getDeathExplosion(), 50, 3);
                                timer.schedule(new TimerTask() {
                                    public void run() {
                                        goToHangar = true;
                                    }
                                }, 1500);
                            }
                        }
                    }
                }
                Image shotImage = shot.getSprite().getImage();
                graphics.drawImage(shotImage, shot.getPoint().x, shot.getPoint().y, shotImage.getWidth(null), shotImage.getHeight(null), null);
            }
            if (movementRequestedX != 0) {
                player.setXVelocity(player.getXVelocity() + (movementRequestedX * player.getHorizontalAcceleration()));
            } else {
                player.decelerateX(player.getHorizontalDecelerationFactor());
            }
            player.move(passed);
            Image playerImage = player.getSprite().getImage();
            graphics.drawImage(playerImage, player.getPoint().x, player.getPoint().y, playerImage.getWidth(null), playerImage.getHeight(null), null);
            if (renderingPass > 5) {
                for (int i = 0; i < currentLevel.getMobs().size(); i++) {
                    MobObject mob = currentLevel.getMobs().get(i);
                    mob.move(passed);
                    Image mobImage = mob.getSprite().getImage();
                    graphics.drawImage(mobImage, mob.getPoint().x, mob.getPoint().y, mobImage.getWidth(null), mobImage.getHeight(null), null);
                    if (mob.shotCooled() && mob.wantsToShoot()) {
                        Sprite shotSprite = Sprite.getSprite("MOB_LASER");
                        Point shotPoint = new Point(mob.getPoint());
                        shotPoint.translate((mob.getSprite().getImage().getWidth(null) / 2) - (shotSprite.getImage().getWidth(null) / 2), 0);
                        mob.startShotCooldown();
                        shots.add(new ShotObject(shotSprite, shotPoint, new Dimension(shotSprite.getImage().getWidth(null), shotSprite.getImage().getHeight(null)),
                                0, mob.getShotVelocity(), 1, mob));
                    }
                }
            } else {
                renderingPass++;
            }
            graphics.drawImage(bigCoin, 0, 0, bigCoin.getWidth(null), bigCoin.getHeight(null), null);
            graphics.setColor(Color.YELLOW);
            graphics.drawString(String.format("%d", player.getGold()), bigCoin.getWidth(null) + 5, graphics.getFont().getSize() - 5);
            bufferStrategy.show();
            pause(10);      // fps limiter
        }
    }
    
    private class ObjectRemover extends TimerTask {
        private GameObject thing;
        private List list;
        public ObjectRemover(GameObject thing, List list) { this.thing = thing; this.list = list; }
        public void run() {
            list.remove(thing);
        }
    }
    
    private boolean detectCollision(GameObject a, GameObject b) {
        int ax = a.getPoint().x;
        int ay = a.getPoint().y;
        int bx = b.getPoint().x;
        int by = b.getPoint().y;
        if ((ay + a.getHeight()) >= by && ay <= (by + b.getHeight())
                && (ax + a.getWidth()) >= bx && ax <= (bx + b.getWidth())) {
            return true;
        }
        return false;
    }
    
    private void initLevel(Level level) {
        
    }
    
    private void hangar() {
        goToHangar = false;
        embark = false;
        this.addMouseListener(hangarListener);
        currentLevel = new Level("Hangar");
        Image bg = currentLevel.getBackgroundA();
        graphics.drawImage(bg, 0, 0, gameWidth, gameHeight, null);
        bufferStrategy.show();
        while (!embark) {
            pause(10);
        }
        this.removeMouseListener(hangarListener);
        currentLevel = new Level("Level 1");
        player.revive();
    }

    private void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            System.err.format("Interrupted exception!\n");
            ex.printStackTrace();
        }
    }
    
    private void switchFullscreen() {
        if (fullscreen) {
            GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            device.setFullScreenWindow(mainFrame);
            gameWidth = device.getFullScreenWindow().getWidth();
            gameHeight = device.getFullScreenWindow().getHeight();
            DisplayMode displayMode = new DisplayMode(gameWidth, gameHeight, 32, DisplayMode.REFRESH_RATE_UNKNOWN);
            if (device.isDisplayChangeSupported()) {
                device.setDisplayMode(displayMode);
            }
        } else {
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
            gameHeight = STANDARD_HEIGHT;
            gameWidth = STANDARD_WIDTH;
        }
        this.setBounds(0,0,gameWidth,gameHeight);
        mainPanel.setPreferredSize(new Dimension(gameWidth,gameHeight));
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
    }
    
    public void shop() {
        // TODO
    }
    
    public void sendHighScore() {
        // TODO
    }
    
    private class FullScreenListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_F5) {
                fullscreen = !fullscreen;
                switchFullscreen();
            }
        }
        public void keyReleased(KeyEvent e) {}
        public void keyTyped(KeyEvent e) {}
    }
    
    private class HangarListener implements MouseListener {
        public void mouseExited(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {}
        public void mouseClicked(MouseEvent e) {
            double x = ((double) e.getX()) / gameWidth;
            double y = ((double) e.getY()) / gameHeight;
//            System.out.format("%d, %d, %f\n%d, %d, %f\n", e.getX(), WIDTH, x, e.getY(), HEIGHT, y);
            if (y > (300.0/800) && y < (430.0/800) && x > (280.0/1450) && x < (440.0/1450)) {
//                System.out.format("SHOP\n");
                shop();
            } else if (y > (280.0/800) && y < (380.0/800) && x > (800.0/1450) && x < (1280.0/1450)) {
//                System.out.format("EMBARK\n");
                embark = true;
            } else if (y > (710.0/800) && x > (1270.0/1450)) {
//                System.out.format("EXIT\n");
                sendHighScore();
                System.exit(0);
            }
        }
    }
    
    private class FlightListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_F5:
                    switchFullscreen();
                case KeyEvent.VK_Z:
                    // fire primary
                    shotRequested = true;
                    break;
                case KeyEvent.VK_X:
                    // fire secondary
                    break;
                case KeyEvent.VK_SPACE:
                    // fire bomb
                    break;
                case KeyEvent.VK_P:
                case KeyEvent.VK_ENTER:
                    // pause
                    pauseRequested = !pauseRequested;
                    break;
                case KeyEvent.VK_LEFT:
                    // move left
                    movementRequestedX = -1;
//                    if (player.getXVelocity() > 0) {
//                        player.setXVelocity(0);
//                    } else {
//                        player.setXVelocity(-1 * player.getHorizontalAcceleration());
//                    }
                    //player.setXVelocity(player.getXVelocity() - player.getHorizontalAcceleration());
                    break;
                case KeyEvent.VK_RIGHT:
                    // move right
                    movementRequestedX = 1;
//                    if (player.getXVelocity() < 0) {
//                        player.setXVelocity(0);
//                    } else {
//                        player.setXVelocity(player.getHorizontalAcceleration());
//                    }
                    //player.setXVelocity(player.getXVelocity() + player.getHorizontalAcceleration());
                    break;
                case KeyEvent.VK_Q:
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_H:
                    goToHangar = true;
                    break;
                case KeyEvent.VK_DOWN:
                    // if held, return to hangar
                    hangarRequested++;
                    if (hangarRequested > 10) {
                        goToHangar = true;
                    }
                    break;
                default:
            }
        }
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_RIGHT:
                    movementRequestedX = 0;
                    break;
                case KeyEvent.VK_Z:
                    shotRequested = false;
                    break;
                case KeyEvent.VK_DOWN:
                    hangarRequested = 0;
                    break;
                default:
            }
        }
        public void keyTyped(KeyEvent e) {}
    }
    
    private class MenuListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_Z) {
                startGame = true;
                graphics.drawImage(currentLevel.getBackgroundA(), 0, 0, gameWidth, gameHeight, null);
                graphics.setColor(new Color(0, 255, 0, 255));
                graphics.setFont(new Font("Consolas", Font.BOLD, 72));
                graphics.drawString("... LOADING ...", Level.MainMenu.TEXT_POSITION.getX(), Level.MainMenu.TEXT_POSITION.getY());
                bufferStrategy.show();
            }
        }
        public void keyReleased(KeyEvent e) {}
        public void keyTyped(KeyEvent e) {}
    }
    
    public static int getGameWidth() {
        return gameWidth;
    }
    
    public static int getGameHeight() {
        return gameHeight;
    }
    
    public static PlayerObject getPlayer() {
        return player;
    }
    
    public static void main(String[] args) {
        SumSimXT main = new SumSimXT();
        Executors.newFixedThreadPool(1).execute(main);
    }
}
