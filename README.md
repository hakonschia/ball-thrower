# ball-thrower
Lab 3 - IMT 3673 Mobile Programming

# Misc
I did not use the sliding window technique for finding the highest acceleration. I found it easier use a timer that waits a certain amount of time (250 ms in this case) from the first acceleration above the threshold. The app gathers accelerations during that time and uses the highest found to move the ball. The code for this is fairly trivial, and easy to tweak the value for how long it should look for accelerations. If implemented, the user could easily change this value to customize the app to each users liking. It is also not dependant on how often a sensor event is triggered (this is definitely changeable by code, and probably hardware).

## Extras
The app provides haptic feedback for when the ball is thrown and lands.

Long clicking on the high score text at the top resets/removes the highest throw.

## Build
The app is built with minSdkVersion 26, and targetSdkVersion 28. It is tested on a physical device with API 26, and emulator (as far is possible) with API 28.

## Problems with the app
I'm not sure why this is happening (it might be intended behaviour in Android, as far as I know I haven't done anything that would cause it to happen) but if you go out of the app without closing it from the multitasking it will continue to look for accelerations and possibly start playing sounds. For the people testing the app, you should probably delete the app from your phones afterwards (especially because the highest point reached sound is kind of loud and to be honest kind of obnoxious).

## User testing
Tested by:
1. Magnus Holm-Hagen
![Magnus Holm-Hagen](https://i.imgur.com/0GpqAhK.png)

2. Erik Mohn
![Erik Mohn](https://i.imgur.com/xJosaQk.png)
