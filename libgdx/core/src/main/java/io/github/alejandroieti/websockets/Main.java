package io.github.alejandroieti.websockets;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

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
    private int dir;
    private boolean isFlipped;
    private int spriteSize;

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

        walkingAnimation = new Animation<TextureRegion>(0.1f, walkingFrames[0]);
        stateTime = 0f;

        posX = 0f;
        dir = 1;
        speed = 1.2f;
        spriteSize = 2;
        isFlipped = false;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        backgroundTexture.dispose();
        walkingAnimationTexture.dispose();
    }

    private void input() {
        boolean rightPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean leftPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT);

        if (rightPressed) {
            if (dir != 1) {
                dir = 1;
                if (isFlipped) {
                    flipAnimationFrames(walkingFrames[0]);
                }
            }
        } else if (leftPressed) {
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
}
