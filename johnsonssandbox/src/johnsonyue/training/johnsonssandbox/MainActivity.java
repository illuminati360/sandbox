package johnsonyue.training.johnsonssandbox;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.app.Activity;

public class MainActivity extends Activity implements SensorEventListener{
	private static final int GROUP_GROUP_ID=0;
	private static final int SPAWN_GROUP_ID=1;
	private static final int CONTROL_GROUP_ID=2;
	private static final int GRAVITY_GROUP_ID=3;
	private static final int JOINT_GROUP_ID=4;
	private static final int TOGGLE_HUD_GROUP_ID=5;
	
	private static final int SPAWN_CIRCLE_ID=1;
	private static final int SPAWN_SQUARE_ID=2;
	
	private static final int REMOVE_PROP_ID=1;
	private static final int SELECT_PROP_ID=2;
	private static final int SET_STATIC_ID=3;
	private static final int FREEZE_ID=4;
	private static final int UNFREEZE_ID=5;
	private static final int PLACE_PROP_ID=6;
	
	
	private static final int TOGGLE_GRAVITY_SENSOR_ID=1;
	private static final int TOGGLE_ZERO_GRAVITY_ID=2;
	
	private static final int DISTANCE_JOINT_ID=1;
	private static final int REVOLUTE_JOINT_ID=2;
	private static final int PULLEY_JOINT_ID=3;
	private static final int PRISMATIC_JOINT_ID=4;
	
	private static final int TOGGLE_HUD_ID=1;
	
	private MySurfaceView mySurfaceView;
	private int soundId;
	private SoundPool sp;
	
	private SensorManager smr;
	private Sensor sr;
	private float GX,GY;
	
	private int prevGroupId=0,prevId=0;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN,WindowManager.LayoutParams. FLAG_FULLSCREEN);
        mySurfaceView=new MySurfaceView(this);
        setContentView(mySurfaceView);
        
        sp=new SoundPool(4,AudioManager.STREAM_MUSIC,100);
        soundId=sp.load(this, R.raw.menu_item_click, 1);
        
        smr=(SensorManager)getSystemService(SENSOR_SERVICE);
        sr=smr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        smr.registerListener(this, sr,SensorManager.SENSOR_DELAY_NORMAL);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		SubMenu subMenu=menu.addSubMenu(GROUP_GROUP_ID, 1, 0, this.getString(R.string.spawn));
		subMenu.add(SPAWN_GROUP_ID, SPAWN_CIRCLE_ID, 0, this.getString(R.string.spawn_circle));
		subMenu.add(SPAWN_GROUP_ID, SPAWN_SQUARE_ID, 0, this.getString(R.string.spawn_square));
		
		SubMenu subMenu2=menu.addSubMenu(GROUP_GROUP_ID, 2, 0, this.getString(R.string.control));
		subMenu2.add(CONTROL_GROUP_ID, REMOVE_PROP_ID, 0, this.getString(R.string.remove_prop));
		subMenu2.add(CONTROL_GROUP_ID, SELECT_PROP_ID, 0, this.getString(R.string.move_prop));
		subMenu2.add(CONTROL_GROUP_ID, SET_STATIC_ID, 0, this.getString(R.string.stop_prop));
		subMenu2.add(CONTROL_GROUP_ID, FREEZE_ID, 0, this.getString(R.string.freeze_prop));
		subMenu2.add(CONTROL_GROUP_ID, UNFREEZE_ID, 0, this.getString(R.string.unfreeze_prop));
		subMenu2.add(CONTROL_GROUP_ID, PLACE_PROP_ID, 0, this.getString(R.string.place_prop));
		
		SubMenu subMenu3=menu.addSubMenu(GROUP_GROUP_ID, 3, 0, this.getString(R.string.gravity));
		subMenu3.add(GRAVITY_GROUP_ID, TOGGLE_GRAVITY_SENSOR_ID, 0, this.getString(R.string.toggle_gravity_sensor));
		subMenu3.add(GRAVITY_GROUP_ID, TOGGLE_ZERO_GRAVITY_ID, 0, this.getString(R.string.toggle_zero_gravity));
		
		SubMenu subMenu4=menu.addSubMenu(GROUP_GROUP_ID, 4, 0, this.getString(R.string.joint));
		subMenu4.add(JOINT_GROUP_ID, DISTANCE_JOINT_ID, 0, this.getString(R.string.distance_joint));
		subMenu4.add(JOINT_GROUP_ID, REVOLUTE_JOINT_ID, 0, this.getString(R.string.revolute_joint));
		subMenu4.add(JOINT_GROUP_ID, PULLEY_JOINT_ID, 0, this.getString(R.string.pulley_joint));
		subMenu4.add(JOINT_GROUP_ID, PRISMATIC_JOINT_ID, 0, this.getString(R.string.prismatic_joint));
		
		menu.add(TOGGLE_HUD_GROUP_ID, TOGGLE_HUD_ID, 0, this.getString(R.string.toggle_hud));
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int id=item.getItemId();
		int groupId=item.getGroupId();
		switch(groupId){
		case SPAWN_GROUP_ID:
			switch(id){
			case SPAWN_CIRCLE_ID:
				mySurfaceView.setOperationType(MySurfaceView.SPAWN_CIRCLE);
				break;
			case SPAWN_SQUARE_ID:
				mySurfaceView.setOperationType(MySurfaceView.SPAWN_SQUARE);
				break;
			}
			break;
		case CONTROL_GROUP_ID:
			switch(id){
			case REMOVE_PROP_ID:
				mySurfaceView.setOperationType(MySurfaceView.REMOVE_PROP);
				break;
			case SELECT_PROP_ID:
				mySurfaceView.setOperationType(MySurfaceView.SELECT_PROP);
				break;
			case SET_STATIC_ID:
				mySurfaceView.setOperationType(MySurfaceView.SET_STATIC);
				break;
			case FREEZE_ID:
				mySurfaceView.setOperationType(MySurfaceView.FREEZE);
				break;
			case UNFREEZE_ID:
				mySurfaceView.setOperationType(MySurfaceView.UNFREEZE);
				break;
			case PLACE_PROP_ID:
				mySurfaceView.setOperationType(MySurfaceView.PLACE_PROP);
				break;
			}
			break;
		case GRAVITY_GROUP_ID:
			switch(id){
			case TOGGLE_GRAVITY_SENSOR_ID:
				mySurfaceView.toggleGravitySensor();
				break;
			case TOGGLE_ZERO_GRAVITY_ID:
				mySurfaceView.toggleZeroGravity();
				break;
			}
			break;
		case JOINT_GROUP_ID:
			switch(id){
			case DISTANCE_JOINT_ID:
				mySurfaceView.setOperationType(MySurfaceView.DISTANCE_JOINT);
				break;
			case REVOLUTE_JOINT_ID:
				mySurfaceView.setOperationType(MySurfaceView.REVOLUTE_JOINT);
				break;
			case PULLEY_JOINT_ID:
				mySurfaceView.setOperationType(MySurfaceView.PULLEY_JOINT);
				break;
			case PRISMATIC_JOINT_ID:
				mySurfaceView.setOperationType(MySurfaceView.PRISMATIC_JOINT);
				break;
			}
			break;
		case TOGGLE_HUD_GROUP_ID:
			mySurfaceView.toggleHud();
			break;
		case GROUP_GROUP_ID:
			break;
		}
		
		if(groupId!=prevGroupId||id!=prevId){
			mySurfaceView.clearAll();
			mySurfaceView.resetHint();
		}
		prevGroupId=groupId;
		prevId=id;
		sp.play(soundId, 1, 1, 0, 0, 1);
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		GX=-event.values[SensorManager.DATA_X];
		GY=-event.values[SensorManager.DATA_Y];
		mySurfaceView.setGravity(GX, GY);
	}
}
