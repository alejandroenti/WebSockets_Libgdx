package io.github.alejandroieti.websockets;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketListener;
import com.github.czyzby.websocket.WebSockets;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private final int FRAME_COLS = 8;

    private SpriteBatch batch;
    private FitViewport viewport;

    private Texture backgroundTexture;
    private Texture walkingAnimationTexture;
    private TextureRegion currentFrame;
    private TextureRegion[][] walkingFrames;

    private Animation<TextureRegion> walkingAnimation;

    private float stateTime;

    private float speed;
    private float posX;
    private float lastPosXSend;
    private float posXdelta;
    private int dir;
    private boolean isFlipped;
    private int spriteSize;

    private Stage uiStage;
    private Touchpad touchpad;
    private Texture touchpadBgTexture;
    private Texture touchpadKnobTexture;

    WebSocket socket;
    String address = "localhost";
    int port = 8888;

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(8, 5);

        backgroundTexture = new Texture(Gdx.files.internal("images/background.png"));
        walkingAnimationTexture = new Texture(Gdx.files.internal("images/walking.png"));

        walkingFrames = TextureRegion.split(walkingAnimationTexture,
            walkingAnimationTexture.getWidth() / FRAME_COLS,
            walkingAnimationTexture.getHeight()
        );

        walkingAnimation = new Animation<>(0.1f, walkingFrames[0]);
        stateTime = 0f;

        posX = 0f;
        lastPosXSend = posX;
        posXdelta = 0.1f;
        dir = 1;
        speed = 1.2f;
        spriteSize = 2;
        isFlipped = false;

        // Create touchpad textures programmatically
        touchpadBgTexture = createCircleTexture(200, new Color(0.3f, 0.3f, 0.3f, 0.5f));
        touchpadKnobTexture = createCircleTexture(80, new Color(0.7f, 0.7f, 0.7f, 0.8f));

        Drawable touchpadBg = new TextureRegionDrawable(new TextureRegion(touchpadBgTexture));
        Drawable touchpadKnob = new TextureRegionDrawable(new TextureRegion(touchpadKnobTexture));

        Touchpad.TouchpadStyle touchpadStyle = new Touchpad.TouchpadStyle();
        touchpadStyle.background = touchpadBg;
        touchpadStyle.knob = touchpadKnob;

        touchpad = new Touchpad(10, touchpadStyle);
        touchpad.setBounds(15, 15, 200, 200);

        uiStage = new Stage(new ScreenViewport());
        uiStage.addActor(touchpad);
        Gdx.input.setInputProcessor(uiStage);

        initializeWebSocketServer();
    }

    private Texture createCircleTexture(int diameter, Color color) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillCircle(diameter / 2, diameter / 2, diameter / 2 - 1);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
        uiStage.act(Gdx.graphics.getDeltaTime());
        uiStage.draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        backgroundTexture.dispose();
        walkingAnimationTexture.dispose();
        uiStage.dispose();
        touchpadBgTexture.dispose();
        touchpadKnobTexture.dispose();
    }

    private void input() {
        // Read from touchpad (knobPercentX: -1 left, +1 right, 0 center)
        float knobX = touchpad.getKnobPercentX();

        // Also keep keyboard support for desktop
        boolean rightPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean leftPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT);

        if (knobX > 0.2f || rightPressed) {
            if (dir != 1) {
                dir = 1;
                if (isFlipped) {
                    flipAnimationFrames(walkingFrames[0]);
                }
            }
        } else if (knobX < -0.2f || leftPressed) {
            if (dir != -1) {
                dir = -1;
                if (!isFlipped) {
                    flipAnimationFrames(walkingFrames[0]);
                }
            }
        } else {
            dir = 0;
        }
    }

    private void logic() {
        stateTime = dir == 0 ?
            0 :
            stateTime + Gdx.graphics.getDeltaTime();
        posX += speed * dir * Gdx.graphics.getDeltaTime();

        if (posX - lastPosXSend >= posXdelta || lastPosXSend - posX >= posXdelta) {
            lastPosXSend = posX;
            socket.send("Nom: Hero, Posx: " + posX);
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        currentFrame = walkingAnimation.getKeyFrame(stateTime, true);

        batch.begin();

        batch.draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.draw(currentFrame, posX, 0.2f, spriteSize, spriteSize);

        batch.end();
    }

    private void flipAnimationFrames(TextureRegion[] frames) {
        for (TextureRegion frame : frames) {
            frame.flip(true, false);
        }
        isFlipped = !isFlipped;
    }

    private void initializeWebSocketServer() {
        if (Gdx.app.getType() == Application.ApplicationType.Android)
            // en Android el host és accessible per 10.0.2.2
            address = "10.0.2.2";
        socket = WebSockets.newSocket(WebSockets.toWebSocketUrl(address, port));
        // ULL: si és a traves de HTTPS , el protocol seria wss enlloc de ws
        //socket = WebSockets.newSocket(WebSockets.toSecureWebSocketUrl(address, port));
        socket.setSendGracefully(false);
        socket.addListener((WebSocketListener) new MyWSListener());
        socket.connect();
    }
}


/////////////////////////////////////////////
// COMUNICACIONS (rebuda de missatges)
/////////////////////////////////////////////
class MyWSListener implements WebSocketListener {

    @Override
    public boolean onOpen(WebSocket webSocket) {
        System.out.println("Opening...");
        return false;
    }

    @Override
    public boolean onClose(WebSocket webSocket, int closeCode, String reason) {
        System.out.println("Closing...");
        return false;
    }

    @Override
    public boolean onMessage(WebSocket webSocket, String packet) {
        System.out.println("Message: " + packet);
        return false;
    }

    @Override
    public boolean onMessage(WebSocket webSocket, byte[] packet) {
        System.out.println("Message: " + packet);
        return false;
    }

    @Override
    public boolean onError(WebSocket webSocket, Throwable error) {
        System.out.println("ERROR: " + error.toString());
        return false;
    }
}
