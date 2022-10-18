
# react-native-gizwits-widget

## Getting started

`$ npm install react-native-gizwits-widget --save`

### Mostly automatic installation

`$ react-native link react-native-gizwits-widget`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-gizwits-widget` and add `RNGizwitsWidget.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNGizwitsWidget.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.gizwitswidget.RNGizwitsWidgetPackage;` to the imports at the top of the file
  - Add `new RNGizwitsWidgetPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-gizwits-widget'
  	project(':react-native-gizwits-widget').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-gizwits-widget/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      implementation project(':react-native-gizwits-widget')
  	```

## Usage
```javascript
import RNGizwitsWidget from 'react-native-gizwits-widget';

// TODO: What to do with the module?
RNGizwitsWidget;
```
  