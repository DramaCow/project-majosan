package com.DramaCow.game;

import com.DramaCow.maths.Vector2D;

public class Enemy extends DynamicGameObject{

	private Ai ai;

	public Enemy(String id, float x, float y, float width, float height, final Level level, Ai ai) {
		super(id, x, y, width, height, level);
		this.ai = ai;
		this.ai.create(this);
	}

	public Enemy(String id, float x, float y, float width, float height, Ai ai) {
		super(id, x, y, width, height, null);
		this.ai = ai;
		this.ai.create(this);
	}

	@Override
	public void update(float dt){
		acceleration.set(0.0f,0.0f);
		ai.update(this, dt);
		super.update(dt);
	}

	// Allow repositioning cloned enemy
	public Enemy(Enemy that, float x, float y, final Level level, float gdirx, float gdiry) {
		super(that.id, x, y, that.box.halfExtents.x * 2, that.box.halfExtents.y * 2, level);
		this.g_dir.set(gdirx,gdiry);
		this.ai = Ai.getAI(that.ai.ID(), that.ai.difficulty);
		this.ai.create(this);
	}

	public String getAiID(){
		return ai.ID();
	}

	@Override 
	public String toString() {
		return this.getAiID() + ": " + Float.toString(position.x); 
	}
}
