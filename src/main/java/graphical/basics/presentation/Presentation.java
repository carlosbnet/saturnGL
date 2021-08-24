package graphical.basics.presentation;

import codec.JCodec;
import codec.RawImageCodec;
import codec.VideoCodec;
import graphical.basics.BackGround;
import graphical.basics.behavior.Behavior;
import graphical.basics.gobject.Camera;
import graphical.basics.gobject.struct.Gobject;
import graphical.basics.task.EndLessParallelTask;
import graphical.basics.task.ParalelTask;
import graphical.basics.task.Task;
import graphical.basics.task.WaitTask;
import graphical.basics.task.transformation.gobject.ColorTranform;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Presentation extends JFrame {

    public static Presentation staticReference;

    private boolean switchProcessing = true;
    private boolean disableCodec;

    public static int FRAME_RATE = 60;


    public BufferedImage bufferedImage;
    public Graphics bufferedGraphics;

    VideoCodec videoCodec;

    List<Gobject> gobjects = new ArrayList<>();
    List<Behavior> behaviors = new ArrayList<>();

    //FpsControler fpsControler = new FpsControler();

    int clipCounter = 0;
    int frameCounter = 0;

    long lastMesure = System.currentTimeMillis();


    private final BackGround backGround = new BackGround();
    private final Camera camera = new Camera();

    public final EndLessParallelTask backGroundTask = new EndLessParallelTask();

    public Presentation() {


        PresentationConfig presentationConfig = new PresentationConfig();
        setup(presentationConfig);
        applyConfigs(presentationConfig);


        //dirs
        File videoDir= new File("video");
        videoDir.mkdir();

        File rawDir= new File("video\\raw");
        rawDir.mkdir();

        videoCodec.startNewVideo("video/", "mv" + clipCounter + ".mov", FRAME_RATE);


        Graphics2D g2 = (Graphics2D) bufferedGraphics;

        RenderingHints rh = new RenderingHints(

                new HashMap<>() {
                    {
                        put(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                    }
                });

        g2.setRenderingHints(rh);

        //preview window

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);


        staticReference = this;


    }

    public abstract void setup(PresentationConfig presentationConfig);

    private void applyConfigs(PresentationConfig presentationConfig) {
        if (presentationConfig.getFramerate() != null) {
            FRAME_RATE = presentationConfig.getFramerate();
        } else {
            FRAME_RATE = 30;
        }

        if (presentationConfig.isDisableCodec() != null) {
            disableCodec = presentationConfig.isDisableCodec();
        } else {
            disableCodec = false;
        }

        if (presentationConfig.getCodec() != null) {
            switch (presentationConfig.getCodec()) {
                case JCODEC:
                    this.videoCodec = new JCodec();
                    break;
                case RAW_IMAGE:
                    this.videoCodec = new RawImageCodec();
                    break;
            }

        } else {
            this.videoCodec = new JCodec();
        }

        //preview windowSize
        this.setUndecorated(presentationConfig.isPreviewWindowBarVisible());
        this.setSize(presentationConfig.getWidth(), presentationConfig.getHeight());

        // framesize
        this.bufferedImage = new BufferedImage(presentationConfig.getWidth(), presentationConfig.getHeight(), BufferedImage.TYPE_INT_ARGB);
        bufferedGraphics = bufferedImage.getGraphics();

    }

    protected abstract void buildPresentation();

    public void build(){
        buildPresentation();
        if(backGroundTask.hasTasks()){
            // consome as coisas que estejam ainda em background
            var remainingTaks=backGroundTask.getResumedTask();
            backGroundTask.clear();
            remainingTaks.execute();
        }
        execute(wait(1));
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(bufferedImage, 0, 0, null);
        g.setColor(Color.green);
        g.drawString("" + frameCounter, 900, 100);
        g.drawString((System.currentTimeMillis() - lastMesure) + " ms", 900, 150);
        lastMesure = System.currentTimeMillis();
    }

    public void processFrame() {
        runBehaviors();
        frameCounter++;
        System.out.println("COUNTER(" + frameCounter + ")");
        var before1 = System.currentTimeMillis();
        paintComponent(bufferedGraphics);
        repaint();
        System.out.println((System.currentTimeMillis() - before1) + "ms processando quadro");
        var before2 = System.currentTimeMillis();
        if (!disableCodec) videoCodec.addFrame(bufferedImage);
        System.out.println((System.currentTimeMillis() - before1) + "ms de codec");

        if (disableCodec) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void paintComponent(Graphics g) {

        backGround.paint(g);
        var g2d = (Graphics2D) g;
        var oldT = (AffineTransform) g2d.getTransform().clone();
        camera.applyView(g);

        for (int i = 0; i < gobjects.size(); i++) {
            gobjects.get(i).paint(g, true);
        }

        g2d.setTransform(oldT);
    }

    public void runBehaviors() {
        for (Behavior behavior : behaviors) {
            behavior.update();
        }
    }

    public void add(Gobject gobject) {
        gobjects.add(gobject);
    }

    public void add(Behavior behavior) {
        behaviors.add(behavior);
    }

    public void remove(Gobject gobject) {
        gobjects.remove(gobject);
    }

    public void execute(Task task) {

        task.setup();
        while (!task.isDone()) {
            task.step();
            backGroundTask.step();
            if (switchProcessing)
                processFrame();
        }
        //cut();
    }

    public void execute(Task... tasks) {
        execute(new ParalelTask(tasks));
    }

    public void cut() {
        videoCodec.saveVideo();
        clipCounter++;
        videoCodec.startNewVideo("video/", "mv" + clipCounter + ".mov", FRAME_RATE);
    }

    public int seconds(double seconds) {
        return (int) (seconds * FRAME_RATE);
    }

    public Task paralel(Task... tasks) {
        return new ParalelTask(tasks);
    }

    public Task fadeOut(Gobject gobject, int steps) {
        return new ColorTranform(gobject, new Color(0, 0, 0, 0), steps);
    }

    public Task fadeOut(Gobject gobject) {
        return new ColorTranform(gobject, new Color(0, 0, 0, 0), seconds(1));
    }

    public Task wait(int steps) {
        return new WaitTask(steps);
    }


    public void switchOff() {
        switchProcessing = false;
    }

    public void switchOn() {
        switchProcessing = true;
    }


    public BackGround getBackGround() {
        return backGround;
    }

    public Camera getCamera() {
        return camera;
    }
}
