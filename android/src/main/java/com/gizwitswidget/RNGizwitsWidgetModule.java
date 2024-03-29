
package com.gizwitswidget;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.gizwitswidget.AppWidgetController;
import com.gizwitswidget.model.WidgetResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class RNGizwitsWidgetModule extends ReactContextBaseJavaModule {

    private static final String TAG = RNGizwitsWidgetModule.class.getSimpleName();

    private final ReactApplicationContext reactContext;

    private Gson gsonParser = new GsonBuilder().create();

    public RNGizwitsWidgetModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        // 激活小组件
        AppWidgetController.INSTANCE.activateAppWidget(reactContext);
    }

    @Override
    public String getName() {
        return "RNGizWidgetManager";
    }

    @ReactMethod
    public void setUpAppInfo(ReadableMap readableMap) {
        JSONObject jsonObject = new JSONObject();
        try {
            Iterator<Map.Entry<String, Object>> entryIterator = readableMap.getEntryIterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, Object> next = entryIterator.next();
                jsonObject.put(next.getKey(), next.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        Log.d(TAG, jsonObject.toString());

        AppWidgetController.INSTANCE.setCommonWidgetConfiguration(reactContext, jsonObject.toString());
    }

    @ReactMethod
    public void saveSceneList(ReadableArray data, Callback callback) {
        String sceneConfiguration = gsonParser.toJson(data.toArrayList());
        AppWidgetController.INSTANCE.setSceneWidgetConfiguration(reactContext, sceneConfiguration);

        String successJson = createJsonResponse("", "");
        callback.invoke(successJson);
    }

    @ReactMethod
    public void getSceneList(Promise promise) {
        AppWidgetController.INSTANCE.getSceneWidgetConfiguration(reactContext, configuration -> {
            try {
                JSONObject configurationJson = new JSONObject();
                if (!configuration.isEmpty()) {
                    JSONArray sceneArray = new JSONArray(configuration);
                    configurationJson.put("data", sceneArray);
                } else {
                    configurationJson.put("error", "Not Set Up");
                }
                WritableMap map = jsonObject2WriteableMap(configurationJson);
                promise.resolve(map);
            } catch (Exception e) {
                // 构建Json对象中出现未知异常
                WritableMap map = Arguments.createMap();
                map.putString("err","JSON ERROR");
                promise.resolve(map);
            }
            return null;
        });
    }

    @ReactMethod
    public void saveControlDeviceList(ReadableArray data, Callback callback) {
        String controlConfiguration = gsonParser.toJson(data.toArrayList());
        AppWidgetController.INSTANCE
                .setControlWidgetConfiguration(reactContext, controlConfiguration);

        String successJson = createJsonResponse("", "");
        callback.invoke(successJson);
    }

    @ReactMethod
    public void getControlDeviceList(Promise promise) {
        AppWidgetController.INSTANCE.getControlWidgetConfiguration(reactContext, configuration -> {
            try {
                JSONObject configurationJson = new JSONObject();
                if (!configuration.isEmpty()) {
                    JSONArray controlArray = new JSONArray(configuration);
                    configurationJson.put("data", controlArray);
                } else {
                    configurationJson.put("error", "Not Set Up");
                }
                WritableMap map = jsonObject2WriteableMap(configurationJson);
                promise.resolve(map);
            } catch (Exception e) {
                // 构建Json对象中出现未知异常
                WritableMap map = Arguments.createMap();
                map.putString("err","JSON ERROR");
                promise.resolve(map);
            }
            return null;
        });
    }

    @ReactMethod
    public void saveStateDeviceList(ReadableArray data, Callback callback) {
        String stateConfiguration = gsonParser.toJson(data.toArrayList());
        AppWidgetController.INSTANCE.setStateWidgetConfiguration(reactContext, stateConfiguration);

        String successJson = createJsonResponse("", "");
        callback.invoke(successJson);
    }

    @ReactMethod
    public void getStateDeviceList(Promise promise) {
        AppWidgetController.INSTANCE.getStateWidgetConfiguration(reactContext, configuration -> {
            try {
                JSONObject configurationJson = new JSONObject();
                if (!configuration.isEmpty()) {
                    JSONArray stateArray = new JSONArray(configuration);
                    configurationJson.put("data", stateArray);
                } else {
                    configurationJson.put("error", "Not Set Up");
                }
                WritableMap map = jsonObject2WriteableMap(configurationJson);
                promise.resolve(map);
            } catch (Exception e) {
                // 构建Json对象中出现未知异常
                WritableMap map = Arguments.createMap();
                map.putString("err","JSON ERROR");
                promise.resolve(map);
            }
            return null;
        });
    }

    @ReactMethod
    public void clearAllData(Callback callback) {
        // 清空小组件的配置信息
        AppWidgetController.INSTANCE.clearWidgetConfiguration(reactContext);
        // 返回成功
        String successJson = createJsonResponse("", "");
        callback.invoke(successJson);
    }

    private String createJsonResponse(String data, String error) {
        return gsonParser.toJson(new WidgetResponse(data, error));
    }

    private WritableMap jsonObject2WriteableMap(JSONObject jsonObject) {
        try {
            WritableMap writableMap = Arguments.createMap();
            Iterator iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                Object object = jsonObject.get(key);
                if (object instanceof String) {
                    writableMap.putString(key, jsonObject.getString(key));
                } else if (object instanceof Boolean) {
                    writableMap.putBoolean(key, jsonObject.getBoolean(key));
                } else if (object instanceof Integer) {
                    writableMap.putInt(key, jsonObject.getInt(key));
                } else if (object instanceof Double) {
                    writableMap.putDouble(key, jsonObject.getDouble(key));
                } else if (object instanceof JSONObject) {
                    writableMap.putMap(key, jsonObject2WriteableMap(jsonObject.getJSONObject(key)));
                } else if (object instanceof JSONArray) {
                    writableMap.putArray(key, jsonArray2WriteableArray(jsonObject.getJSONArray(key)));
                } else {
                    writableMap.putNull(key);
                }
            }
            return writableMap;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private WritableArray jsonArray2WriteableArray(JSONArray jsonArray) {
        try {
            WritableArray writableArray = Arguments.createArray();
            for (int i = 0; i < jsonArray.length(); i++) {
                Object object = jsonArray.get(i);
                if (object instanceof String) {
                    writableArray.pushString(jsonArray.getString(i));
                } else if (object instanceof Boolean) {
                    writableArray.pushBoolean(jsonArray.getBoolean(i));
                } else if (object instanceof Integer) {
                    writableArray.pushInt(jsonArray.getInt(i));
                } else if (object instanceof Double) {
                    writableArray.pushDouble(jsonArray.getDouble(i));
                } else if (object instanceof JSONObject) {
                    writableArray.pushMap(jsonObject2WriteableMap(jsonArray.getJSONObject(i)));
                } else if (object instanceof JSONArray) {
                    writableArray.pushArray(jsonArray2WriteableArray(jsonArray.getJSONArray(i)));
                } else {
                    writableArray.pushNull();
                }
            }
            return writableArray;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}
