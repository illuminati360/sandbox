package johnsonyue.training.johnsonssandbox;

import java.util.ArrayList;

import johnsonyue.training.johnsonssandbox.MyShapeHelper.MyCircle;
import johnsonyue.training.johnsonssandbox.MyShapeHelper.MyCursor;
import johnsonyue.training.johnsonssandbox.MyShapeHelper.MyLine;
import johnsonyue.training.johnsonssandbox.MyShapeHelper.MyPulley;
import johnsonyue.training.johnsonssandbox.MyShapeHelper.MyRect;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.MouseJoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

//使用box2d注意事项：一定要在模拟之后再改变世界的物体，否则会出现不可预见的错误。方法：用容器传递。
public class MySurfaceView extends SurfaceView implements Runnable, Callback{
	//1 meter:30 pixels. 1米对应30像素。标准：在画的时候，用像素，用物理引擎的时候，转换到米即/30。
	//屏幕宽480p，长720p.所以宽16米，长24米。
	public static final float RATE=30.0f;
	private static final float TIME_STEP=1f/60f;
	private static final int VELOCITY_ITERATIONS=10;
	private static final int POSITION_ITERATIONS=8;
	private static final float BOUNDARY_WIDTH=20f;
	private static final float HUD_HEIGHT=140f;
	private static final float PADDING=30f;
	private static final float TEXTPADDINGTOP=20f;
	private static final float TEXTPADDINGLEFT=20f;
	public static final float RADIUS=30f;
	public static final float SIDE_LENGTH=60f;
	private static final int MAX_PROP_LIMIT=35;
	//Operation Types.
	public static final int SPAWN_CIRCLE=1;
	public static final int SPAWN_SQUARE=2;
	public static final int REMOVE_PROP=4;
	public static final int SELECT_PROP=5;
	public static final int SET_STATIC=6;
	public static final int FREEZE=7;
	public static final int UNFREEZE=8;
	public static final int PLACE_PROP=9;
	public static final int DISTANCE_JOINT=10;
	public static final int REVOLUTE_JOINT=11;
	public static final int PULLEY_JOINT=12;
	public static final int PRISMATIC_JOINT=13;
	//操作类型，开始默认生成圆。
	public int operationType=SPAWN_CIRCLE;
	private String hint=new String();
	private Context context;
	public boolean gravitySensorActive=false;
	public boolean prev=true;
	public boolean zeroGravity=true;
	public boolean hudVisible=true;
	
	private float screenW,screenH;
	private float hudWidth;
	
	private SurfaceHolder sfh;
	private Canvas canvas;
	private Paint paint;
	private MyShapeHelper helper;
	
	private boolean flag=true;
	private Thread thread;
	
	private World world;
	private Vec2 gravity;
	private ContactListener listener;
	private Body sensorBody;
	
	// Queues to pass Bodies between onTouch,OnContact and logic.
	//If change bodies before world.step, there will be some unpredictable problems.
	private ArrayList<Body> removeQueue;
	private float eventX=-100,eventY=-100;
	private enum ShapeType{
		CIRCLE,SQUARE,KINEMATIC_CIRCLE
	}
	private class ShapeInfo{
		float x,y;
		ShapeType shape;
		ShapeInfo(float x,float y,ShapeType shape){
			this.x=x;
			this.y=y;
			this.shape=shape;
		}
	}
	private ArrayList<ShapeInfo> spawnQueue;
	private ArrayList<Body> jointQueue;
	private ArrayList<Body> revoluteJointQueue;
	private ArrayList<Body> prismaticJointQueue;
	private ArrayList<Body> pulleyJointQueue;
	private boolean isUp=true;
	private ArrayList<Vec2> anchorQueue;
	private MouseJoint mj=null;
	private Body selectedBody=null;
	private Body placedBody=null;
	private Body staticBody=null;
	private Body frozenBody=null;
	private Body unfrozenBody=null;
	private ArrayList<Body> borderList;
	
	//Constructor.
	public MySurfaceView(Context context) {
		super(context);
		this.context=context;
		hint=context.getString(R.string.spawn_circle_hint);
		// TODO Auto-generated constructor stub
		init();
	}
	
	public void init(){
		sfh=this.getHolder();
		sfh.addCallback(this);
		
		paint=new Paint();
		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);
		
		removeQueue=new ArrayList<Body>();
		spawnQueue=new ArrayList<ShapeInfo>();
		jointQueue=new ArrayList<Body>();
		revoluteJointQueue=new ArrayList<Body>();
		prismaticJointQueue=new ArrayList<Body>();
		pulleyJointQueue=new ArrayList<Body>();
		anchorQueue=new ArrayList<Vec2>();
		borderList=new ArrayList<Body>();
		
		gravity=new Vec2(0,20);
		world=new World(gravity,true);
		listener=initContactListener();
		world.setContactListener(listener);

		helper=new MyShapeHelper();
	}
	
	public ContactListener initContactListener(){
		return listener=new ContactListener(){
			Body body1,body2;
			@Override
			public void beginContact(Contact arg0) {
				// TODO Auto-generated method stub
				switch(operationType){
				case REMOVE_PROP:
					if(removeQueue.isEmpty()){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)){
							removeQueue.add(body2);
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)){
							removeQueue.add(body1);
						}
					}
					break;
				case SELECT_PROP:
					if(selectedBody==null){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)&&body2.getType()==BodyType.DYNAMIC){
							selectedBody=body2;
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)&&body1.getType()==BodyType.DYNAMIC){
							selectedBody=body1;
						}
					}
					break;
				case PLACE_PROP:
					if(placedBody==null){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)&&body2.getType()!=BodyType.STATIC){
							placedBody=body2;
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)&&body1.getType()!=BodyType.STATIC){
							placedBody=body1;
						}
					}
					break;
				case SET_STATIC:
					if(staticBody==null){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)){
							staticBody=body2;
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)){
							staticBody=body1;
						}
					}
					break;
				case FREEZE:
					if(frozenBody==null){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)){
							frozenBody=body2;
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)){
							frozenBody=body1;
						}
					}
					break;
				case UNFREEZE:
					if(unfrozenBody==null){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)){
							unfrozenBody=body2;
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)){
							unfrozenBody=body1;
						}
					}
					break;
				case DISTANCE_JOINT:
					if(jointQueue.size()<2){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)){
							if(jointQueue.size()==0||(jointQueue.size()==1&&!body2.equals(jointQueue.get(0)))){
								jointQueue.add(body2);
							}
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)){
							if(jointQueue.size()==0||(jointQueue.size()==1&&!body1.equals(jointQueue.get(0)))){
								jointQueue.add(body1);
							}
						}
					}
					break;
				case REVOLUTE_JOINT:
					if(revoluteJointQueue.size()<2){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)){
							if(revoluteJointQueue.size()==0||(revoluteJointQueue.size()==1&&!body2.equals(revoluteJointQueue.get(0)))){
								revoluteJointQueue.add(body2);
							}
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)){
							if(revoluteJointQueue.size()==0||(revoluteJointQueue.size()==1&&!body1.equals(revoluteJointQueue.get(0)))){
								revoluteJointQueue.add(body1);
							}
						}
					}
					break;
				case PULLEY_JOINT:
					if(anchorQueue.size()==2&&pulleyJointQueue.size()<2){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)){
							if(!alreadyExists(pulleyJointQueue,body2)){
								pulleyJointQueue.add(body2);
							}
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)){
							if(!alreadyExists(pulleyJointQueue,body1)){
								pulleyJointQueue.add(body1);
							}
						}
					}				
					break;
				case PRISMATIC_JOINT:
					if(prismaticJointQueue.size()<2){
						body1=arg0.getFixtureA().getBody();
						body2=arg0.getFixtureB().getBody();
						if(body1.equals(sensorBody)&&!isBorder(body2)){
							if(prismaticJointQueue.size()==0||(prismaticJointQueue.size()==1&&!body2.equals(prismaticJointQueue.get(0)))){
								prismaticJointQueue.add(body2);
							}
						}
						else if(body2.equals(sensorBody)&&!isBorder(body1)){
							if(prismaticJointQueue.size()==0||(prismaticJointQueue.size()==1&&!body1.equals(prismaticJointQueue.get(0)))){
								prismaticJointQueue.add(body1);
							}
						}
					}
					break;
				}
			}

			@Override
			public void endContact(Contact arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void postSolve(Contact arg0, ContactImpulse arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void preSolve(Contact arg0, Manifold arg1) {
				// TODO Auto-generated method stub
				
			}
		};
	}
	
	public boolean alreadyExists(ArrayList<Body> list, Body body){
		for(int i=0;i<list.size();i++){
			if(list.get(i).equals(body)){
				return true;
			}
		}
		return false;
	}
	
	private void drawStatistics(Canvas canvas, Paint paint){
		canvas.drawRect(PADDING+BOUNDARY_WIDTH,PADDING+BOUNDARY_WIDTH,PADDING+BOUNDARY_WIDTH+hudWidth,BOUNDARY_WIDTH+HUD_HEIGHT,paint);
		canvas.drawText(context.getString(R.string.body_number)+": "+(world.getBodyCount()-5), TEXTPADDINGLEFT+PADDING+BOUNDARY_WIDTH, PADDING+BOUNDARY_WIDTH+TEXTPADDINGTOP, paint);
		canvas.drawText(context.getString(R.string.joint_number)+": "+world.getJointCount(), TEXTPADDINGLEFT+PADDING+BOUNDARY_WIDTH, PADDING+BOUNDARY_WIDTH+TEXTPADDINGTOP+20, paint);
		canvas.drawText(context.getString(R.string.hint)+": "+hint, TEXTPADDINGLEFT+PADDING+BOUNDARY_WIDTH, PADDING+BOUNDARY_WIDTH+TEXTPADDINGTOP+40, paint);
		canvas.drawText(context.getString(R.string.gravity)+": "+world.getGravity().x+", "+world.getGravity().y, TEXTPADDINGLEFT+PADDING+BOUNDARY_WIDTH, PADDING+BOUNDARY_WIDTH+TEXTPADDINGTOP+60, paint);
		canvas.drawText(context.getString(R.string.contacts)+": "+world.getContactCount(), TEXTPADDINGLEFT+PADDING+BOUNDARY_WIDTH, PADDING+BOUNDARY_WIDTH+TEXTPADDINGTOP+80, paint);
	}
	
	public void draw(){
		try{
			canvas=sfh.lockCanvas();
			if(canvas!=null){
				canvas.drawColor(Color.WHITE);
				if(hudVisible){
					drawStatistics(canvas,paint);
				}
				Body body=world.getBodyList();
				for(int i=0,count=world.getBodyCount();i<count;i++){
					if(body.m_userData instanceof MyRect){
						((MyRect)body.m_userData).draw(canvas,paint);
					}
					else if(body.m_userData instanceof MyCircle){
						((MyCircle)body.m_userData).draw(canvas,paint);
					}
					else if(body.m_userData instanceof MyCursor){
						((MyCursor)body.m_userData).draw(canvas, paint);
					}
					body=body.m_next;
				}
				
				Joint joint=world.getJointList();
				for(int j=0,count=world.getJointCount();j<count;j++){
					if(joint.m_userData instanceof MyLine){
						((MyLine)joint.m_userData).draw(canvas, paint);
					}
					else if(joint.m_userData instanceof MyPulley){
						((MyPulley)joint.m_userData).draw(canvas, paint);
					}
					joint=joint.m_next;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(canvas!=null){
				sfh.unlockCanvasAndPost(canvas);
			}
		}
	}
	
	public void logic(){
		world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
		
		Body body=world.getBodyList();
		MyRect rect;
		MyCircle circle;
		MyCursor cursor;
		
		//Update sensor position.
		sensorBody.setTransform(new Vec2(eventX/RATE,eventY/RATE), 0);
		
		//Update position.
		for(int i=0,count=world.getBodyCount();i<count;i++){
			if(body.m_userData instanceof MyRect){
				rect=(MyRect) body.m_userData;
				rect.setX(body.getPosition().x);
				rect.setY(body.getPosition().y);
				rect.setDegrees((float) ((body.getAngle()*180/Math.PI)%360));
			}
			else if(body.m_userData instanceof MyCircle){
				circle=(MyCircle) body.m_userData;
				circle.setX(body.getPosition().x);
				circle.setY(body.getPosition().y);
				circle.setDegrees((float)((body.getAngle()*180/Math.PI)%360));
			}
			else if(body.m_userData instanceof MyCursor){
				cursor=(MyCursor) body.m_userData;
				cursor.setX(body.getPosition().x);
				cursor.setY(body.getPosition().y);
			}
			body=body.m_next;
		}
		
		Joint joint=world.getJointList();
		MyLine line;
		MyPulley pulley;
		for(int j=0,count=world.getJointCount();j<count;j++){
			if(joint.m_userData instanceof MyLine){
				line=(MyLine) joint.m_userData;
				line.setStartX(joint.getBodyA().getPosition().x);
				line.setStartY(joint.getBodyA().getPosition().y);
				line.setStopX(joint.getBodyB().getPosition().x);
				line.setStopY(joint.getBodyB().getPosition().y);
			}
			else if(joint.m_userData instanceof MyPulley){
				pulley=(MyPulley) joint.m_userData;
				pulley.setAX(joint.getBodyA().getPosition().x);
				pulley.setBX(joint.getBodyB().getPosition().x);
				pulley.setAY(joint.getBodyA().getPosition().y);
				pulley.setBY(joint.getBodyB().getPosition().y);
			}
			joint=joint.m_next;
		}
		
		//Update gravity.
		if(zeroGravity){
			world.setGravity(new Vec2(0f,0f));
		}
		if(gravitySensorActive){
			world.setGravity(gravity);
		}
		
		//Select.
		if(selectedBody!=null){
			if(mj==null){
				mj=helper.createMouseJoint(world, sensorBody, selectedBody);
			}
			else{
				mj.setTarget(new Vec2(eventX/RATE,eventY/RATE));
			}
		}
		else if(mj!=null){
			world.destroyJoint(mj);
			mj=null;
		}
		
		//Place.
		if(placedBody!=null){
			placedBody.setTransform(new Vec2(eventX/RATE,eventY/RATE), 0);
		}
		
		//Set static.
		if(staticBody!=null){
			staticBody.setAwake(false);
			staticBody=null;
		}
		
		//Freeze.
		if(frozenBody!=null){
			frozenBody.setType(BodyType.STATIC);
			frozenBody=null;
		}
		
		//UnFreeze.
		if(unfrozenBody!=null){
			unfrozenBody.setType(BodyType.DYNAMIC);
			unfrozenBody=null;
		}
		
		//Spawn.
		for(int j=0;j<spawnQueue.size();j++){
			ShapeType t=spawnQueue.get(j).shape;
			float x=spawnQueue.get(j).x;
			float y=spawnQueue.get(j).y;
			switch(t){
				case CIRCLE:
					helper.createCircle(world,x,y,RADIUS,false);
					break;
				case SQUARE:
					helper.createPolyRect(world,x,y,SIDE_LENGTH,SIDE_LENGTH,false);
					break;
			}
		}
		spawnQueue.clear();
		
		//Remove.
		if(!removeQueue.isEmpty()){
			world.destroyBody(removeQueue.get(0));
			removeQueue.clear();
		}
		
		//Add joint.
		if(jointQueue.size()==2){
			helper.createDistanceJoint(world,jointQueue.get(0),jointQueue.get(1));
			jointQueue.clear();
		}
		
		//Add revolute joint.
		if(revoluteJointQueue.size()==2){
			helper.createRevoluteJoint(world,revoluteJointQueue.get(0),revoluteJointQueue.get(1));
			revoluteJointQueue.clear();
		}
		
		//Add prismatic joint.
		if(prismaticJointQueue.size()==2){
			helper.createPrismaticJoint(world, prismaticJointQueue.get(0), prismaticJointQueue.get(1));
			prismaticJointQueue.clear();
		}
		
		//Add pulley joint.
		if(pulleyJointQueue.size()==2&&isUp){
			helper.createPulleyJoint(world, anchorQueue.get(0), anchorQueue.get(1), pulleyJointQueue.get(0) ,pulleyJointQueue.get(1));
			pulleyJointQueue.clear();
			anchorQueue.clear();
		}
	}
	
	public void createBoundaries(){
		borderList.add(helper.createPolyRect(world, 0, -BOUNDARY_WIDTH,screenW,BOUNDARY_WIDTH*2,true));
		borderList.add(helper.createPolyRect(world, 0, screenH-BOUNDARY_WIDTH,screenW,BOUNDARY_WIDTH*2,true));
		borderList.add(helper.createPolyRect(world, -BOUNDARY_WIDTH, 0,BOUNDARY_WIDTH*2,screenH,true));
		borderList.add(helper.createPolyRect(world, screenW-BOUNDARY_WIDTH,0, BOUNDARY_WIDTH*2,screenH,true));
		
		sensorBody=helper.createPointSensor(world);
	}
	
	public boolean isBorder(Body body){
		for(int i=0,count=borderList.size();i<count;i++){
			if(body.equals(borderList.get(i))){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(flag){
			long start=System.currentTimeMillis();
			draw();
			logic();
			long end=System.currentTimeMillis();
			
			try{
				if((end-start)<25){
					Thread.sleep(25-(end-start));
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		//Must initialize screenW,screenH after the surface has been created.
		screenW=this.getWidth();
		screenH=this.getHeight();
		hudWidth=screenW-BOUNDARY_WIDTH*2-PADDING*2;
		
		createBoundaries();
		
		flag=true;
		thread=new Thread(this);
		thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		flag=false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		float x,y;
		x=event.getX();
		y=event.getY();
		
		switch(operationType){
		case SPAWN_CIRCLE:
			if(event.getAction()==MotionEvent.ACTION_UP){
				if(world.getBodyCount()>MAX_PROP_LIMIT){
					canvas.drawText("You've reached max prop limit", 20, 30, paint);
				}
				else{
					try{
						spawnQueue.add(new ShapeInfo(x,y,ShapeType.CIRCLE));
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			break;
		case SPAWN_SQUARE:
			if(event.getAction()==MotionEvent.ACTION_UP){
				if(world.getBodyCount()>MAX_PROP_LIMIT){
					canvas.drawText("You've reached max prop limit", 20, 30, paint);
				}
				else{
					try{
						spawnQueue.add(new ShapeInfo(x,y,ShapeType.SQUARE));
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			break;
		case REMOVE_PROP:
		case SET_STATIC:
		case FREEZE:
		case UNFREEZE:
		case DISTANCE_JOINT:
		case REVOLUTE_JOINT:
		case PRISMATIC_JOINT:
			if(event.getAction()==MotionEvent.ACTION_DOWN||event.getAction()==MotionEvent.ACTION_MOVE){
				eventX=x;
				eventY=y;
			}
			else if(event.getAction()==MotionEvent.ACTION_UP){
				eventX=-100;
				eventY=-100;
			}
			break;
		case PULLEY_JOINT:
			if(event.getAction()==MotionEvent.ACTION_DOWN||event.getAction()==MotionEvent.ACTION_MOVE){
				eventX=x;
				eventY=y;
				isUp=false;
			}
			else if(event.getAction()==MotionEvent.ACTION_UP){
				eventX=-100;
				eventY=-100;
				if(anchorQueue.size()<2&&pulleyJointQueue.isEmpty()){
					anchorQueue.add(new Vec2(event.getX()/RATE,event.getY()/RATE));
				}
				isUp=true;
			}
			break;
		case SELECT_PROP:
		case PLACE_PROP:
			if(event.getAction()==MotionEvent.ACTION_DOWN||event.getAction()==MotionEvent.ACTION_MOVE){
				eventX=x;
				eventY=y;
			}
			else if(event.getAction()==MotionEvent.ACTION_UP){
				eventX=-100;
				eventY=-100;
				selectedBody=null;
				placedBody=null;
			}
			break;
		}
		return true;
	}
	
	public void setOperationType(int op){
		operationType=op;
	}
	
	public void toggleGravitySensor(){
		if(!zeroGravity){
			gravitySensorActive=!gravitySensorActive;
		}
	}
	
	public void toggleZeroGravity(){
		if(zeroGravity==false){
			prev=gravitySensorActive;
			zeroGravity=true;
			gravitySensorActive=false;
		}
		else{
			zeroGravity=false;
			gravitySensorActive=prev;
		}
	}
	
	public void toggleHud(){
		hudVisible=!hudVisible;
	}
	
	public void setGravity(float x,float y){
		gravity=new Vec2(x*2,-y*2);
	}
	
	public void clearAll(){
		removeQueue.clear();
		spawnQueue.clear();
		jointQueue.clear();
		revoluteJointQueue.clear();
		prismaticJointQueue.clear();
		pulleyJointQueue.clear();
		anchorQueue.clear();
		selectedBody=null;
	}
	
	public void resetHint(){
		switch(operationType){
		case SPAWN_CIRCLE:
			hint=context.getString(R.string.spawn_circle_hint);
			break;
		case SPAWN_SQUARE:
			hint=context.getString(R.string.spawn_sqaure_hint);
			break;
		case REMOVE_PROP:
			hint=context.getString(R.string.remove_prop_hint);
			break;
		case SELECT_PROP:
			hint=context.getString(R.string.move_prop_hint);
			break;
		case SET_STATIC:
			hint=context.getString(R.string.stop_prop_hint);
			break;
		case FREEZE:
			hint=context.getString(R.string.freeze_prop_hint);
			break;
		case UNFREEZE:
			hint=context.getString(R.string.unfreeze_prop_hint);
			break;
		case PLACE_PROP:
			hint=context.getString(R.string.place_prop_hint);
			break;
		case DISTANCE_JOINT:
			hint=context.getString(R.string.distance_joint_hint);
			break;
		case REVOLUTE_JOINT:
			hint=context.getString(R.string.revolute_joint_hint);
			break;
		case PULLEY_JOINT:
			hint=context.getString(R.string.pulley_joint_hint);
			break;
		case PRISMATIC_JOINT:
			hint=context.getString(R.string.prismatic_joint_hint);
			break;
		}
	}
}
