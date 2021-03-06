package com.DramaCow.game;

import com.DramaCow.maths.Vector2D;
import com.DramaCow.maths.Contact;
import com.DramaCow.maths.CollisionObject;
import com.DramaCow.maths.Collision;
import com.DramaCow.maths.Rect;
import com.DramaCow.maths.AABB;

import java.util.List;
import java.util.ArrayList;

abstract public class DynamicGameObject extends GameObject {
	protected final Vector2D velocity;
	protected final Vector2D acceleration;

	protected static final Vector2D down_dir 	= new Vector2D(0.0f, -1.0f); // Do not set these
	protected static final Vector2D up_dir 		= new Vector2D(0.0f,  1.0f);
	protected static final Vector2D no_dir 		= new Vector2D(0.0f,  0.0f);
	
	protected final Vector2D g_dir = new Vector2D(0.0f, -1.0f);

	protected boolean collidable = true;
	protected boolean grounded = false;

	protected Level level;
	
	public DynamicGameObject (String id, float x, float y, float width, float height, final Level level) {
		super(id, x, y, width, height);
		velocity = new Vector2D();
		acceleration = new Vector2D();
		this.level = level;
	}

	// COLLISION CODE

	@Override
	public void update(float dt){
		super.update(dt);

		if (!collidable) {
			velocity.add(acceleration.x * dt, acceleration.y * dt);
			position.add(velocity.x * dt, velocity.y * dt);
			box.position.x = position.x + box.halfExtents.x;
			box.position.y = position.y + box.halfExtents.y;
		}

		// Pre-collision
		acceleration.add( Vector2D.scalar(level.G_MAG, g_dir) );
		velocity.add( Vector2D.scalar(dt, acceleration) );

		// Turn DynamicObject into CollisionObject
		CollisionObject object = new CollisionObject(new AABB(box), new Vector2D(velocity), 
			new Vector2D(acceleration), new Vector2D(0.0f, 0.0f));
		CollisionDGO dgo = new CollisionDGO(object, level.G_MAG, new Vector2D(g_dir), collidable, grounded);

		// Position after one frame if no collision were to occur
		Vector2D nextPosition = Vector2D.add( object.box.position, Vector2D.scalar(dt, object.velocity) );

		// Calculate what tiles need to be checked for collision
		Vector2D min = Vector2D.min( Vector2D.sub( object.box.position, object.box.halfExtents ), 
			Vector2D.sub( nextPosition, object.box.halfExtents ) );
		Vector2D max = Vector2D.max( Vector2D.add( object.box.position, object.box.halfExtents ), 
			Vector2D.add( nextPosition, object.box.halfExtents ) );

		// Bounds rectangle
		Rect bounds = new Rect(min, max);

		int c0 = min.x >= 0.0f ? (int) min.x : 0; 
		int cmax = (int) Math.ceil(max.x) <= level.LEVEL_WIDTH - 1 ? (int) Math.ceil(max.x) : level.LEVEL_WIDTH - 1;

		int r0 = min.y >= 0.0f ? (int) min.y : 0;
		int rmax = (int) Math.ceil(max.y) <= level.LEVEL_HEIGHT - 1 ? (int) Math.ceil(max.y) : level.LEVEL_HEIGHT - 1;

		boolean north = false;
		boolean south = false;
		boolean east = false;
		boolean west = false;

		// Check for collision on each tile
		for (int r = r0; r < rmax; r++) {
			for (int c = c0; c < cmax; c++) {
				if (level.getMap()[r][c] == 1) {
					AABB tileAabb = new AABB(new Rect(c, r, 1.0f, 1.0f));
					//System.out.println(tileAabb);

					// Contact plane represents collision data
					Contact contact = Collision.AABBvsAABB(object.box, tileAabb);

					if (!internalTileCheck(r, c, object.box, contact.normal)) {
						//System.out.println("collision");
						CollisionObject response = Collision.collisionResponse(object, contact, dt);
						object.velocity = response.velocity; object.posCorrect = response.posCorrect;

						if (object.north) north = true;
						if (object.south) south = true;
						if (object.east) east = true;
						if (object.west) west = true;
					}
				}
			}
		}

		// Check collision on surrounding objects
		List<GameObject> gos = level.getAllObjects();
		gos.remove(this);

		List<GameObject> remove = new ArrayList<GameObject>();

		for (int i = 0; i < gos.size(); i++) {
			GameObject go = gos.get(i);
			if (go.box.toRect().overlaps(bounds)) {
				if (go instanceof Coin || go instanceof Heart) {
					remove.add(go); continue;
				}
				Contact contact = Collision.AABBvsAABB(object.box, go.box);
				CollisionObject response = Collision.collisionResponse(object, contact, dt);
				object.velocity = response.velocity; object.posCorrect = response.posCorrect;
			}
		}

		level.getObjects().removeAll(remove);

		// Set which faces of the object are in contact with a surface
		northCollision(north); 
		southCollision(south);
		eastCollision(east);
		westCollision(west);

		// Post-collision
		velocity.set(object.velocity.x, object.velocity.y);
		g_dir.set(dgo.g_dir.x, dgo.g_dir.y);

		// Add immoveable flag
		if (this instanceof Enemy) {
			if ( ((Enemy)this).getAiID().equals("static") ) {
				object.posCorrect.set(0.0f, 0.0f);
			}
		}

		// Correct the velocity
		velocity.add( Vector2D.scalar( /*1.0f?*/ dt, object.posCorrect) );

		position.add( Vector2D.scalar(dt, velocity) );
		box.position.x = position.x + box.halfExtents.x;
		box.position.y = position.y + box.halfExtents.y;
	}

	public boolean internalTileCheck(int r, int c, AABB box, Vector2D normal) {
		// CHECK FOR OPTIMISATIONS OR METHOD IMPROVEMENTS

		// Get bounds of player in terms of tiles
		Vector2D roundedAbsExtents = Vector2D.mult( Vector2D.scalar(2, box.halfExtents), normal ).abs();
				 roundedAbsExtents.set( (float) Math.ceil(roundedAbsExtents.x), (float) Math.ceil(roundedAbsExtents.y) );
		
		// Check if any tiles exist within extent of player in direction of collision normal
		// If so, disregard collision (this includes internal tiles)
		int i = 1;
		for ( Vector2D v = Vector2D.scalar(i, normal); 
			  Math.abs(v.x) <= roundedAbsExtents.x && Math.abs(v.y) <= roundedAbsExtents.y;
			  v = Vector2D.scalar(i, normal) ) 
		{
			int tileX = c + (int) v.x;
			int tileY = r + (int) v.y;

			if (tileX < 0 || tileY < 0) break;
			if (level.getMap()[tileY][tileX] == 1) return true;

			i++;
		}

		return false;
	}

	protected void northCollision(boolean touching) {}
	protected void southCollision(boolean touching) { this.grounded = touching; }
	protected void eastCollision(boolean touching)  {}
	protected void westCollision(boolean touching)  {}

	// /COLLISION CODE

	public void setVelocity(Vector2D v){
		velocity.set(v);
	}

	public Vector2D getVelocity(){
		return velocity;
	}

	public void setAcceleration(Vector2D a){
		acceleration.set(a);
	}

	public Vector2D getAcceleration(){
		return acceleration;
	}

	public void setPosition(Vector2D p){
		position.set(p);
		box.position.x = position.x + box.halfExtents.x;
		box.position.y = position.y + box.halfExtents.y;
	}

	public void setPosition(float x, float y) {
		position.set(x,y);
		box.position.x = x + box.halfExtents.x;
		box.position.y = y + box.halfExtents.y;
	}

	// Interface class
	public static class CollisionDGO {
		
		public CollisionObject collisionObject;

		public final float G_MAG;
		public Vector2D g_dir;

		public boolean collidable;
		public boolean grounded;

		public CollisionDGO(CollisionObject collisionObject, final float G_MAG, Vector2D g_dir, 
							boolean collidable, boolean grounded) {
			this.collisionObject = collisionObject;
			this.G_MAG = G_MAG;
			this.g_dir = g_dir;
			this.collidable = collidable;
			this.grounded = grounded;
		}
	}
}
