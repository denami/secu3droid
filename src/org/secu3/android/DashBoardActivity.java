/* Secu3Droid - An open source, free manager for SECU-3 engine
 * control unit
 * Copyright (C) 2013 Maksim M. Levin. Russia, Voronezh
 * 
 * SECU-3  - An open source, free engine control unit
 * Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Gorlovka
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contacts:
 *            http://secu-3.org
 *            email: mmlevin@mail.ru
*/

package org.secu3.android;

import java.io.IOException;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.util.FPSLogger;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.secu3.android.api.io.ProtoFieldFloat;
import org.secu3.android.api.io.ProtoFieldInteger;
import org.secu3.android.api.io.Secu3Packet;
import org.secu3.android.api.io.Secu3Service;
import org.secu3.android.api.io.Secu3Manager.SECU3_TASK;
import org.secu3.android.api.utils.PacketUtils;
import org.secu3.android.api.utils.ResourcesUtils;
import org.secu3.android.gauges.DashBoard;
import org.secu3.android.gauges.GaugeAnalog;
import org.secu3.android.gauges.GaugeDigital;
import org.secu3.android.gauges.LedGauge;
import org.secu3.android.gauges.SpriteGauge;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.WindowManager;

public class DashBoardActivity extends SimpleBaseGameActivity {	
	private DashBoard dashBoard = null;

	private boolean isOnline;
	private int protocol_version = SettingsActivity.PROTOCOL_UNKNOWN;
	private PowerManager.WakeLock wakelock = null;
	private ReceiveMessages receiver = null;
	private PacketUtils packetUtils = null;
	private long time = 0;
	private float delta = 0;
	
	private float speedData = 0;
	private float odometerData = 0;
	private float pressureData = 0;
	private float tempData = 0;
	private float voltageData = 8;
	private float rpmData = 0;
	private float onlineData = 0;
	private float checkEngineData = 0;
	private float gasolineData = 0;
	private float ecoData = 0;
	private float powerData = 0;
	private float chokeData = 0;
	private float fanData = 0;
	
	public class ReceiveMessages extends BroadcastReceiver 
	{
		public final IntentFilter intentFilter;
		
		public ReceiveMessages() {
			intentFilter = new IntentFilter();
			intentFilter.addAction(Secu3Service.EVENT_SECU3_SERVICE_RECEIVE_PACKET);			
			intentFilter.addAction(Secu3Service.EVENT_SECU3_SERVICE_STATUS_ONLINE);
		}
		
		@Override
		public void onReceive(Context context, Intent intent) 
		{    
			update (intent); 
		}
	}
	
	public void createDashboardFromXml (int xmlId)
	{
		String attr, attrValue;
		try {
			XmlPullParser xpp = getResources().getXml(xmlId);

			while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
				switch (xpp.getEventType()){
				case XmlPullParser.START_TAG:
					String name = xpp.getName();
					int count;
					int color;
					int id;
					float x,y;
					String textureName;
					switch (name) {
						case "DashBoard":
							String width = null;
							String height = null;
							textureName = null;
							color = 0;

							count = xpp.getAttributeCount();
							for (int i = 0; i != count; i++) {
								attr = xpp.getAttributeName(i);
								attrValue = xpp.getAttributeValue(i);
								switch (attr) {
									case "width":
										width = attrValue;
										break;
									case "height":
										height = attrValue;
										break;
									case "texture":
										textureName = attrValue;
										break;
									case "color":
										if (attrValue.startsWith("#")) {
											color = Color.parseColor(attrValue);
										} else {
											color = Integer.parseInt(attrValue);
										}
										break;
								}
							}
							dashBoard = new DashBoard(Float.parseFloat(width), Float.parseFloat(height), textureName, color);
							break;
						case "GaugeAnalog":
							String scaleTexture = null;
							String labelsTexture = null;
							String arrowTexture = null;
							String degreesPerUnit = null;
							String beginAngle = null;
							String scaleX = null;
							String scaleY = null;
							String labelsX = null;
							String labelsY = null;
							String arrowX = null;
							String arrowY = null;
							String arrowAnchorX = null;
							String arrowAnchorY = null;
							String minValue = null;
							String maxValue = null;

							id = 0;

							count = xpp.getAttributeCount();
							for (int i = 0; i != count; i++) {
								attr = xpp.getAttributeName(i);
								attrValue = xpp.getAttributeValue(i);
								switch (attr) {
									case "id":
										if (ResourcesUtils.isResource(attrValue))
											id = ResourcesUtils.referenceToInt(attrValue);
										break;
									case "scale_texture":
										scaleTexture = attrValue;
										break;
									case "labels_texture":
										labelsTexture = attrValue;
										break;
									case "arrow_texture":
										arrowTexture = attrValue;
										break;
									case "degrees_per_unit":
										degreesPerUnit = attrValue;
										break;
									case "begin_angle":
										beginAngle = attrValue;
										break;
									case "scale_x":
										scaleX = attrValue;
										break;
									case "scale_y":
										scaleY = attrValue;
										break;
									case "labels_x":
										labelsX = attrValue;
										break;
									case "labels_y":
										labelsY = attrValue;
										break;
									case "arrow_x":
										arrowX = attrValue;
										break;
									case "arrow_y":
										arrowY = attrValue;
										break;
									case "arrow_anchor_x":
										arrowAnchorX = attrValue;
										break;
									case "arrow_anchor_y":
										arrowAnchorY = attrValue;
										break;
									case "min_value":
										minValue = attrValue;
										break;
									case "max_value":
										maxValue = attrValue;
										break;
								}
							}
							dashBoard.addGauge(
									new GaugeAnalog(id, Float.parseFloat(degreesPerUnit), Float.parseFloat(beginAngle), Float.parseFloat(minValue), Float.parseFloat(maxValue),
											scaleTexture, labelsTexture, arrowTexture,
											Float.parseFloat(scaleX), Float.parseFloat(scaleY), Float.parseFloat(labelsX), Float.parseFloat(labelsY), Float.parseFloat(arrowX), Float.parseFloat(arrowY), Float.parseFloat(arrowAnchorX), Float.parseFloat(arrowAnchorY))
							);
							break;
						case "GaugeDigital":
							String font = null;
							String format = null;
							String size = null;
							x = y = 0;
							color = 0;

							id = 0;

							count = xpp.getAttributeCount();
							for (int i = 0; i != count; i++) {
								attr = xpp.getAttributeName(i);
								attrValue = xpp.getAttributeValue(i);
								switch (attr) {
									case "id":
										if (ResourcesUtils.isResource(attrValue))
											id = ResourcesUtils.referenceToInt(attrValue);
										break;
									case "x":
										x = Float.parseFloat(attrValue);
										break;
									case "y":
										y = Float.parseFloat(attrValue);
										break;
									case "size":
										size = attrValue;
										break;
									case "color":
										if (attrValue.startsWith("#")) {
											color = Color.parseColor(attrValue);
										} else {
											color = Integer.parseInt(attrValue);
										}
										break;
									case "font":
										font = attrValue;
										break;
									case "format":
										format = attrValue;
										break;
								}
							}
							dashBoard.addGauge(
									new GaugeDigital(id, x, y, font, Float.parseFloat(size), format, color)
							);
							break;
						case "Sprite":
							textureName = null;
							x = y = 0;
							id = 0;

							count = xpp.getAttributeCount();
							for (int i = 0; i != count; i++) {
								attr = xpp.getAttributeName(i);
								attrValue = xpp.getAttributeValue(i);
								switch (attr) {
									case "id":
										if (ResourcesUtils.isResource(attrValue))
											id = ResourcesUtils.referenceToInt(attrValue);
										break;
									case "texture":
										textureName = attrValue;
										break;
									case "x":
										x = Float.parseFloat(attrValue);
										break;
									case "y":
										y = Float.parseFloat(attrValue);
										break;
								}
							}
							dashBoard.addGauge(new SpriteGauge(id, textureName, x, y));
							break;
						case "Led":
							textureName = null;
							x = y = 0;
							id = 0;

							count = xpp.getAttributeCount();
							for (int i = 0; i != count; i++) {
								attr = xpp.getAttributeName(i);
								attrValue = xpp.getAttributeValue(i);
								switch (attr) {
									case "id":
										if (ResourcesUtils.isResource(attrValue))
											id = ResourcesUtils.referenceToInt(attrValue);
										break;
									case "texture":
										textureName = attrValue;
										break;
									case "x":
										x = Float.parseFloat(attrValue);
										break;
									case "y":
										y = Float.parseFloat(attrValue);
										break;
								}
							}
							dashBoard.addGauge(new LedGauge(id, textureName, x, y));
							break;
						}
				}
				xpp.next();				
			}
		} catch (XmlPullParserException|IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onCreate(Bundle pSavedInstanceState) {
		packetUtils = new PacketUtils(this);
		receiver = new ReceiveMessages();				
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Secu3Droid:wakelock");
		
		super.onCreate(pSavedInstanceState);
	}

	@Override
	public EngineOptions onCreateEngineOptions() {
		createDashboardFromXml(R.xml.nfs_portrait);		
		Camera camera = new Camera(0, 0, dashBoard.getWidth(), dashBoard.getHeight());
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_SENSOR, new RatioResolutionPolicy(dashBoard.getWidth(), dashBoard.getHeight()),camera);
	}

	@Override
	protected void onCreateResources() {
		try {
			dashBoard.load(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());
		final Scene scene = new Scene();				
		dashBoard.attach(scene, this.getVertexBufferObjectManager());
				
		scene.registerUpdateHandler(new TimerHandler(1 / 10.0f, true, new ITimerCallback() {
			@Override
			public void onTimePassed(final TimerHandler pTimerHandler) {
				if (delta <= 0.1) delta = 0.1f;
				dashBoard.setGaugeValue(R.id.GaugeOdometer, odometerData,delta);
				dashBoard.setGaugeValue(R.id.GaugeSpeedometer, speedData,delta);
				dashBoard.setGaugeValue(R.id.GaugeManometer, pressureData,delta);
				dashBoard.setGaugeValue(R.id.GaugeThermometer, tempData,delta);
				dashBoard.setGaugeValue(R.id.GaugeVoltmeter, voltageData,delta);
				dashBoard.setGaugeValue(R.id.GaugeTachometer, rpmData,delta);			
				
				dashBoard.setGaugeValue(R.id.LedOnline, onlineData, 0.3f);
				dashBoard.setGaugeValue(R.id.LedCheckEngine, checkEngineData, 0.3f);
				dashBoard.setGaugeValue(R.id.LedGasoline, gasolineData, 0.3f);
				dashBoard.setGaugeValue(R.id.LedEco, ecoData, 0.3f);
				dashBoard.setGaugeValue(R.id.LedPower, powerData, 0.3f);
				dashBoard.setGaugeValue(R.id.LedChoke, chokeData, 0.3f);
				dashBoard.setGaugeValue(R.id.LedFan, fanData, 0.3f);
			}
		}));
					
		return scene;
	}
	
	@Override
	protected synchronized void onResume() {
		if (SettingsActivity.isKeepScreenAliveActive(this)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		if ((wakelock != null)&&(SettingsActivity.isWakeLockEnabled(this))) {
			wakelock.acquire();
		} else if ((wakelock != null)&&(wakelock.isHeld())) {
			wakelock.release();
		}
		registerReceiver(receiver, receiver.intentFilter);
		startService(new Intent (Secu3Service.ACTION_SECU3_SERVICE_START,Uri.EMPTY,this,Secu3Service.class));
		protocol_version = SettingsActivity.getProtocolVersion(getBaseContext());
		SECU3_TASK task = SECU3_TASK.SECU3_READ_SENSORS;
		startService(new Intent (Secu3Service.ACTION_SECU3_SERVICE_SET_TASK,Uri.EMPTY,this,Secu3Service.class).putExtra(Secu3Service.ACTION_SECU3_SERVICE_SET_TASK_PARAM, task.ordinal()));		
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		if ((wakelock != null)&&(wakelock.isHeld())) {
			wakelock.release();
		}
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {	
		if (SettingsActivity.isKeepScreenAliveActive(this)) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		unregisterReceiver(receiver);
		super.onPause();		
	}
	
	private void update(Intent intent) {
		if (Secu3Service.EVENT_SECU3_SERVICE_STATUS_ONLINE.equals(intent.getAction())) {				
			boolean isOnline = intent.getBooleanExtra(Secu3Service.EVENT_SECU3_SERVICE_STATUS,false);
			onlineData = isOnline?1:0;
			if (isOnline && !this.isOnline) {
				this.isOnline = true;						

			}						
		} else if (Secu3Service.EVENT_SECU3_SERVICE_RECEIVE_PACKET.equals(intent.getAction()))
		{
			Secu3Packet packet = intent.getParcelableExtra(Secu3Service.EVENT_SECU3_SERVICE_RECEIVE_PARAM_PACKET);
			if (packet != null) {
				if (time != 0) {
					delta = (float) ((System.currentTimeMillis() - time) / 1000.0);
				}
				time = System.currentTimeMillis();
				
				switch (packet.getPacketIdResId()) {
				case R.string.packet_type_sensor_dat:
					int bitfield = ((ProtoFieldInteger) packet.getField(R.string.sensor_dat_bitfield_title)).getValue();
					rpmData = ((ProtoFieldInteger) packet.getField(R.string.sensor_dat_rpm_title)).getValue();
					pressureData = ((ProtoFieldFloat) packet.getField(R.string.sensor_dat_map_title)).getValue();
					voltageData = ((ProtoFieldFloat) packet.getField(R.string.sensor_dat_voltage_title)).getValue();
					tempData = ((ProtoFieldFloat) packet.getField(R.string.sensor_dat_temperature_title)).getValue(); 
					checkEngineData = (((ProtoFieldInteger) packet.getField(R.string.sensor_dat_errors_title)).getValue() != 0)?1:0;
					gasolineData = (Secu3Packet.bitTest(bitfield, Secu3Packet.BITNUMBER_GAS) != 0) ? 0:1;
					ecoData = (Secu3Packet.bitTest(bitfield, Secu3Packet.BITNUMBER_EPHH_VALVE) != 0)?0:1;
					powerData = (Secu3Packet.bitTest(bitfield, Secu3Packet.BITNUMBER_EPM_VALVE) != 0)?1:0;
					fanData = (Secu3Packet.bitTest(bitfield, Secu3Packet.BITNUMBER_COOL_FAN) != 0)?1:0;
					chokeData = (((ProtoFieldFloat) packet.getField(R.string.sensor_dat_choke_position_title)).getValue()>=95.0)?1:0;
					

					/*		((ProtoFieldFloat) packet.getField(R.string.sensor_dat_angle_correction_title)).getValue(),
							((ProtoFieldFloat) packet.getField(R.string.sensor_dat_knock_title)).getValue(),
							((ProtoFieldFloat) packet.getField(R.string.sensor_dat_knock_retard_title)).getValue(),
							((ProtoFieldInteger) packet.getField(R.string.sensor_dat_air_flow_title)).getValue(),
							Secu3Packet.bitTest(bitfield, Secu3Packet.BITNUMBER_CARB),							
							Secu3Packet.bitTest(bitfield, Secu3Packet.BITNUMBER_ST_BLOCK),
							((ProtoFieldFloat) packet.getField(R.string.sensor_dat_addi1_voltage_title)).getValue(),
							((ProtoFieldFloat) packet.getField(R.string.sensor_dat_addi2_voltage_title)).getValue(),
							((ProtoFieldFloat) packet.getField(R.string.sensor_dat_tps_title)).getValue() */													
					
					if (protocol_version >= SettingsActivity.PROTOCOL_28082013_SUMMER_RELEASE) {
						speedData = packetUtils.calcSpeed(((ProtoFieldInteger) packet.getField(R.string.sensor_dat_speed_title)).getValue());
						odometerData = packetUtils.calcDistance(((ProtoFieldInteger) packet.getField(R.string.sensor_dat_distance_title)).getValue()); 		
					}			
					break;
				default:
					break;
				}
			}
		}		
	}
	
}
