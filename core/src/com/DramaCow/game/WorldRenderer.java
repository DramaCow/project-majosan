package com.DramaCow.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import java.util.List;

public class WorldRenderer {
	
	private World world;
	private OrthographicCamera cam;
	private SpriteBatch batch;

	private Tileset tileset;

	public WorldRenderer(SpriteBatch batch, World world) {
		this.world = world;
	
		float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
		this.cam = new OrthographicCamera(16.0f * ((float) w/h), 16.0f);
		this.cam.position.set(cam.viewportWidth / 2.0f, cam.viewportHeight / 2.0f, 0.0f);
		this.cam.update();

		this.batch = batch;

		// Loading screen texture
		TextureManager.loadTexture("loading", XReader.getLoadingScreen(Terms.LEVEL_MASTER));

		loadNextLevelAssets();

		TextureManager.loadTexture("parasprite","parasprite.png");
		AnimationManager.loadAnimation("parasprite", 
			new Animation(0.0625f, (new Tileset(TextureManager.getTexture("parasprite"), 58, 44)).getTiles()));
		//TextureManager.loadTexture("tempEnemy", XReader.getEnemySprite("enemies.xml", "1"));
	}	

	private boolean loadNextLevelAssets(){
		// Boolean check needed to see if current biomeId matches next level biomeId to prevent reloading of the same assets

		String levelFilename = XReader.getFilenameOfLevel(Terms.LEVEL_MASTER, world.getNextBiome());
		
		TextureManager.loadTexture("tiles", XReader.getLevelTileset(levelFilename));
			tileset = new Tileset(TextureManager.getTexture("tiles"), 32, 32);
		TextureManager.loadTexture("background", XReader.getLevelBackground(levelFilename));
		SoundManager.loadMusic("bgm", XReader.getLevelBGM(levelFilename), true);

		return true;
	}
	
	public void render() {
		cam.update();
		batch.setProjectionMatrix(cam.combined);

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		switch (world.getState()) {
			case INIT:
				batch.disableBlending();
				batch.begin();
					batch.draw(TextureManager.getTexture("loading"), 0.0f, 0.0f, cam.viewportWidth, cam.viewportHeight);
				batch.end();
				break;

			case READY:
				// Display same stuff as transition state?
				break;

			case RUNNING:
				batch.disableBlending();
				batch.begin();
					renderLevelBackground();
				batch.end();

				batch.enableBlending();
				batch.begin();
					renderLevelTiles();
					renderLevelObjects();
				batch.end();
				break;

			case TRANSITION:
				// Render fade in/out sequence here (keep player animation on screen)
				break;

			case GAME_OVER:
				// Dispose of textures and sounds

				// Undecided, this state is currently going to be used for notifying game screen of a game over
				break;
		}
	}

	private void renderLevelBackground() {
		// TODO
		batch.draw(TextureManager.getTexture("background"), 0.0f, 0.0f, cam.viewportWidth, cam.viewportHeight);
	}

	private void renderLevelTiles() {
		// EXAMPLE CODE
		int tile = 0;
		float width = tileset.TILE_X / 32;		// Where 32px == 1.0m
		float height = tileset.TILE_Y / 32;

		for (int r = 0; r < world.getCurrentLevel().LEVEL_HEIGHT; r++) {
			for (int c = 0; c < world.getCurrentLevel().LEVEL_WIDTH; c++) {
				tile = world.getCurrentLevel().getMap()[r][c];
				if (tile != 0) {
					batch.draw(tileset.getTile(tile-1), c * width, r * height, 
						width, height);
				}
			}
		}
	}

	private void renderLevelObjects() {
		List<GameObject> objects = world.getCurrentLevel().getObjects();
		for(GameObject object: objects){
			batch.draw(AnimationManager.getAnimation(object.id).getKeyFrame(object.getTime(), 0), object.getX(), 
				object.getY(), object.getWidth(), object.getHeight());
		}
	}

	public void resize(int w, int h) {
		cam.viewportWidth = 16.0f * ((float) w/h);
		cam.viewportHeight = 16.0f;
		cam.position.set(cam.viewportWidth / 2.0f, cam.viewportHeight / 2.0f, 0.0f);
		cam.update();
	}
}
