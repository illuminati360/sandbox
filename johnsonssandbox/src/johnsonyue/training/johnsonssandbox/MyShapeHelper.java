package johnsonyue.training.johnsonssandbox;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.DistanceJoint;
import org.jbox2d.dynamics.joints.DistanceJointDef;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.jbox2d.dynamics.joints.MouseJointDef;
import org.jbox2d.dynamics.joints.PrismaticJoint;
import org.jbox2d.dynamics.joints.PrismaticJointDef;
import org.jbox2d.dynamics.joints.PulleyJoint;
import org.jbox2d.dynamics.joints.PulleyJointDef;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class MyShapeHelper {
	
	private static final float RATE=MySurfaceView.RATE;
	private static final float RADIUS=MySurfaceView.RADIUS;
	
	/**
	 * @param world
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param isStatic
	 * @return
	 */
	public Body createPolyRect(World world,float x,float y,float width,float height,boolean isStatic){
		//有两处需要设置静态，density，BodyDef type.
		//Define shape.定义矩形轮廓。
		PolygonShape ps=new PolygonShape();
		//setAsBox必须设置为width/2,height/2。
		ps.setAsBox(width/2/RATE, height/2/RATE);
		
		//Define fixture.定义图形皮肤(固有属性:密度，摩擦力，回复力，轮廓)。
		FixtureDef fd=new FixtureDef();
		if(isStatic){
			fd.density=0;
		}
		else{
			fd.density=1;
		}
		fd.friction=1.0f;
		fd.restitution=0.4f;
		fd.shape=ps;
		
		//Define body.
		BodyDef bd=new BodyDef();
		bd.type=isStatic?BodyType.STATIC:BodyType.DYNAMIC;
		bd.position.set((x+width/2)/RATE,(y+height/2)/RATE);
		
		//Create body.
		Body body=world.createBody(bd);
		body.createFixture(fd);
		body.m_userData=new MyRect((x+width/2)/RATE,(y+height/2)/RATE,width/RATE,height/RATE,0);
		body.setSleepingAllowed(false);
		
		return body;
	}
	
	public Body createCircle(World world,float x,float y,float r,boolean isStatic){
		CircleShape cs=new CircleShape();
		cs.m_radius=r/RATE;
		
		FixtureDef fd=new FixtureDef();
		if(isStatic){
			fd.density=0;
		}
		else{
			fd.density=0.4f;
		}
		fd.shape=cs;
		fd.friction=1.0f;
		fd.restitution=1.0f;
		
		BodyDef bd=new BodyDef();
		bd.position.set(x/RATE,y/RATE);
		bd.type=isStatic?BodyType.STATIC:BodyType.DYNAMIC;
		
		Body body=world.createBody(bd);
		body.createFixture(fd);
		body.m_userData=new MyCircle(bd.position.x,bd.position.y,cs.m_radius,0);
		body.setSleepingAllowed(false);
		
		return body;
	}
	
	public class MyRect{
		private float x,y,width,height,degrees;
		
		MyRect(float x,float y,float width,float height,float degrees){
			this.x=x;
			this.y=y;
			this.width=width;
			this.height=height;
			this.degrees=degrees;
		}
		
		public void setX(float x){
			this.x=x;
		}
		
		public void setY(float y){
			this.y=y;
		}
		
		public float getX(){
			return x;
		}
		
		public float getY(){
			return y;
		}
		
		public void setDegrees(float d){
			degrees=d;
		}
		
		public void draw(Canvas canvas,Paint paint){
			canvas.save();
			canvas.rotate(degrees, x*RATE, y*RATE);
			canvas.drawRect((x-width/2)*RATE, (y-height/2)*RATE, (x+width/2)*RATE, (y+height/2)*RATE, paint);
			canvas.restore();
		}
	}
	
	public class MyCircle{
		private float x,y,r,degrees;
		
		MyCircle(float x,float y,float r,float degrees){
			this.x=x;
			this.y=y;
			this.r=r;
			this.degrees=degrees;
		}
		
		public void setX(float x){
			this.x=x;
		}
		
		public void setY(float y){
			this.y=y;
		}
		
		public void setDegrees(float d){
			this.degrees=d;
		}
		
		public float getX(){
			return x;
		}
		
		public float getY(){
			return y;
		}
		public void draw(Canvas canvas, Paint paint){
			canvas.save();
			canvas.rotate(degrees, x*RATE, y*RATE);
			canvas.drawCircle(x*RATE, y*RATE, r*RATE, paint);
			canvas.drawLine(x*RATE, y*RATE, x*RATE+RADIUS, y*RATE, paint);
			canvas.restore();
		}
	}

	//Create a point sensor with fixed x,y,r and set dynamic.
	public Body createPointSensor(World world){
		CircleShape cs=new CircleShape();
		float r=20/RATE;
		float x=-100,y=-100;
		cs.m_radius=r;
		
		FixtureDef fd=new FixtureDef();
		
		fd.density=0f;
		fd.shape=cs;
		fd.friction=1.0f;
		fd.restitution=0.1f;
		fd.isSensor=true;
		
		BodyDef bd=new BodyDef();
		bd.position.set(x,y);
		bd.type=BodyType.DYNAMIC;
		
		Body body=world.createBody(bd);
		body.createFixture(fd);
		body.m_userData=new MyCursor(bd.position.x,bd.position.y,cs.m_radius);
		
		return body;
	}
	
	public class MyCursor{
		private float x,y,r;
		
		MyCursor(float x,float y,float r){
			this.x=x;
			this.y=y;
			this.r=r;
		}
		
		public void setX(float x){
			this.x=x;
		}
		
		public void setY(float y){
			this.y=y;
		}
		
		public float getX(){
			return x;
		}
		
		public float getY(){
			return y;
		}
		public void draw(Canvas canvas, Paint paint){
			Style t=paint.getStyle();
			paint.setStyle(Style.FILL_AND_STROKE);
			
			canvas.drawCircle(x*RATE, y*RATE, r*RATE, paint);
			
			paint.setStyle(t);
		}
	}
	
	public DistanceJoint createDistanceJoint(World world, Body A, Body B){
		DistanceJointDef djd=new DistanceJointDef();
		djd.initialize(A, B, A.getWorldCenter(), B.getWorldCenter());
		djd.collideConnected=true;
		DistanceJoint dj=(DistanceJoint)world.createJoint(djd);
		dj.m_userData=new MyLine(A.getPosition().x,A.getPosition().y,B.getPosition().x,B.getPosition().y);
		return dj;
	}
	
	public class MyLine{
		private float startX,startY,stopX,stopY;
		
		MyLine(float startX,float startY,float stopX,float stopY){
			this.startX=startX;
			this.startY=startY;
			this.stopX=stopX;
			this.stopY=stopY;
		}
		
		public void setStartX(float startX) {
			this.startX = startX;
		}

		public void setStartY(float startY) {
			this.startY = startY;
		}

		public void setStopX(float stopX) {
			this.stopX = stopX;
		}

		public void setStopY(float stopY) {
			this.stopY = stopY;
		}
		
		public void draw(Canvas canvas, Paint paint){
			canvas.drawLine(startX*RATE, startY*RATE, stopX*RATE, stopY*RATE, paint);
		}
	}
	
	public MouseJoint createMouseJoint(World world,Body bodyA,Body bodyB){
		MouseJointDef mjd=new MouseJointDef();
		mjd.bodyA=bodyA;
		mjd.bodyB=bodyB;
		mjd.target.x=bodyA.getPosition().x;
		mjd.target.y=bodyA.getPosition().y;
		mjd.maxForce=100;
		mjd.frequencyHz=1.0f;
		MouseJoint mj=(MouseJoint)world.createJoint(mjd);
		
		return mj;
	}
	
	public RevoluteJoint createRevoluteJoint(World world, Body bodyA, Body bodyB){
		RevoluteJointDef rjd=new RevoluteJointDef();
		rjd.initialize(bodyA, bodyB, bodyA.getWorldCenter());
		rjd.maxMotorTorque=30;
		rjd.motorSpeed=60;
		rjd.enableMotor=true;
		RevoluteJoint rj=(RevoluteJoint)world.createJoint(rjd);
		rj.m_userData=new MyLine(bodyA.getPosition().x,bodyA.getPosition().y,bodyB.getPosition().x,bodyB.getPosition().y);
		return rj;
	}
	
	public PulleyJoint createPulleyJoint(World world, Vec2 anchorA, Vec2 anchorB, Body bodyA, Body bodyB){
		PulleyJointDef pjd=new PulleyJointDef();
		pjd.initialize(bodyA, bodyB, anchorA, anchorB, bodyA.getWorldCenter(), bodyB.getWorldCenter(), 1f);
		PulleyJoint pj=(PulleyJoint)world.createJoint(pjd);
		pj.m_userData=new MyPulley(anchorA,anchorB,bodyA.getPosition().x,bodyA.getPosition().y,bodyB.getPosition().x,bodyB.getPosition().y);
		
		return pj;
	}
	
	public class MyPulley{
		float AX,AY,BX,BY;
		Vec2 anchorA,anchorB;
		
		MyPulley(Vec2 anchorA, Vec2 anchorB, float AX, float AY, float BX, float BY){
			this.anchorA=anchorA;
			this.anchorB=anchorB;
			this.AX=AX;
			this.AY=AY;
			this.BX=BX;
			this.BY=BY;
		}

		public void setAX(float aX) {
			AX = aX;
		}

		public void setAY(float aY) {
			AY = aY;
		}

		public void setBX(float bX) {
			BX = bX;
		}

		public void setBY(float bY) {
			BY = bY;
		}
		
		public void draw(Canvas canvas, Paint paint){
			canvas.drawLine(anchorA.x*RATE, anchorA.y*RATE, anchorB.x*RATE, anchorB.y*RATE, paint);
			canvas.drawLine(AX*RATE, AY*RATE, anchorA.x*RATE, anchorA.y*RATE, paint);
			canvas.drawLine(BX*RATE, BY*RATE, anchorB.x*RATE, anchorB.y*RATE, paint);
			canvas.drawCircle(anchorA.x*RATE, anchorA.y*RATE, 5f, paint);
			canvas.drawCircle(anchorB.x*RATE, anchorB.y*RATE, 5f, paint);
		}
	}
	
	public PrismaticJoint createPrismaticJoint(World world, Body A, Body B){
		PrismaticJointDef pjd=new PrismaticJointDef();
		pjd.initialize(A, B, A.getWorldCenter(), new Vec2(0,1));
		pjd.maxMotorForce=0f;
		pjd.motorSpeed=0f;
		pjd.enableMotor=true;
		pjd.lowerTranslation=-30f;
		pjd.upperTranslation=30f;
		pjd.enableLimit=true;
		
		PrismaticJoint pj=(PrismaticJoint)world.createJoint(pjd);
		pj.m_userData=new MyLine(A.getPosition().x,A.getPosition().y,B.getPosition().x,B.getPosition().y);
		
		return pj;
	}
}
