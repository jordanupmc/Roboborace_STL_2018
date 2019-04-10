/*******************************************************************************
 *
 *   Copyright 2017 Mytech Ingenieria Aplicada <http://www.mytechia.com>
 *   Copyright 2017 Gervasio Varela <gervasio.varela@mytechia.com>
 *
 *   This file is part of Robobo project.
 *
 *   Robobo Ros Module is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Robobo Ros Module is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with Robobo Ros Module.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/
package robobo.example.com.roboboexample;

import android.util.Log;

import com.mytechia.commons.framework.exception.InternalErrorException;
import com.mytechia.commons.framework.simplemessageprotocol.exception.CommunicationException;
import com.mytechia.robobo.framework.behaviour.ABehaviourModule;
import com.mytechia.robobo.framework.hri.emotion.Emotion;
import com.mytechia.robobo.framework.hri.emotion.IEmotionModule;
import com.mytechia.robobo.framework.hri.sound.emotionSound.IEmotionSoundModule;
import com.mytechia.robobo.framework.hri.speech.production.ISpeechProductionModule;
import com.mytechia.robobo.framework.hri.touch.ITouchListener;
import com.mytechia.robobo.framework.hri.touch.ITouchModule;
import com.mytechia.robobo.framework.hri.touch.TouchGestureDirection;
import com.mytechia.robobo.rob.IRob;
import com.mytechia.robobo.rob.IRobInterfaceModule;

/**
 * Example of custom Robobo behaviour written in java and using the native Robobo Framework
 * <p>
 * This is just a simple example behaviour that subscribes a listener for touch events so that
 * it can react to taps and flings by changing the robot face and making some noises.
 * <p>
 * Furthermore a periodic behaviour is also programed in runStep() that makes the robot
 * react to obstacles by checking the value of the IR sensors and changing the face of the
 * robot when an obstacle is too close.
 *
 * @author Gervasio Varela | gervasio.varela@mytechia.com
 */
public class StartRobo extends ABehaviourModule {

    private IRob robModule = null;
    private IEmotionModule emotionModule = null;
    private ITouchModule touchModule = null;
    private ISpeechProductionModule ttsModule = null;
    private IEmotionSoundModule soundModule = null;

    private ITouchListener touchListener = null;

    boolean finish = false;
    private static int count = 0;
    private static int state = 0; //0 --> normal | 1 --> too close!
    private static volatile int parcourChoisie = 0;
    private static volatile int timing = 100;
    private volatile boolean parcourOneFinish = false;

    public static void setUpdates(int timer, int choise) {
        Log.d("eee", "setUpdates: >>>>>>>>>>>>>>>>>> setting " + timer + " " + choise);
        timing = timer;
        parcourChoisie = choise;
        state = 0;
        count = 0;
    }

    @Override
    protected void startBehaviour() throws InternalErrorException {

        robModule = getRobobo().getModuleInstance(IRobInterfaceModule.class).getRobInterface();

        //the emotion module allows us to change the face of the robot
        emotionModule = getRobobo().getModuleInstance(IEmotionModule.class);

        //the touch module allows the reaction to touch events like tap or fling
        touchModule = getRobobo().getModuleInstance(ITouchModule.class);

        //the TTS module allows the robot to speak
        ttsModule = getRobobo().getModuleInstance(ISpeechProductionModule.class);

        soundModule = getRobobo().getModuleInstance(IEmotionSoundModule.class);


        //startBehavour is the place to setup our modules and resources, like for example
        //suscribing some listeners to modules events
        //it's very important to perform only 'quick' operations in the listeners, if you need
        //to perform heavy processing, like for example image processing or network access, you
        //should execute the processing in a separate thread that you can start from the listener

        //lets make the robot react to taps by temporarily changing its face and make some 'noise'
        touchListener = new ITouchListener() {
            @Override
            public void tap(final Integer x, final Integer y) {
                //let's complain a little bit
                soundModule.playSound(IEmotionSoundModule.OUCH_SOUND);
                emotionModule.setTemporalEmotion(Emotion.ANGRY, 1500, Emotion.NORMAL);
                if (!parcourOneFinish) {
                    parcourOneFinish = true;
                    setUpdates(100, 1);
                }
            }

            @Override
            public void touch(final Integer x, final Integer y) {

            }

            @Override
            public void fling(final TouchGestureDirection dir, final double angle, final long time, final double distance) {
                //let's make a little noise
                soundModule.playSound(IEmotionSoundModule.PURR_SOUND);
                emotionModule.setTemporalEmotion(Emotion.LAUGHING, 15000, Emotion.NORMAL);
            }

            @Override
            public void caress(final TouchGestureDirection dir) {
                if (!parcourOneFinish) {
                    parcourOneFinish = true;
                    setUpdates(100, 2);
                }
            }
        };

        touchModule.suscribe(touchListener);


        //if needed you can change the default execution period of the runStep() code
        //by default it is 50 ms, and has a minimum allowed value of 10 ms
        setPeriod(100); //100ms --> 10 times per second

    }

    @Override
    protected void stopBehaviour() throws InternalErrorException {

        touchModule.unsuscribe(touchListener);

    }

    @Override
    protected void runStep() {

        //as you can see in the documentation the majority of the Robobo framework works by
        //using listeners to events, the best place to setup the listeners is the startBehaviour()
        //method

        //anyway, if need to execute some periodic task like checking the state of a sensor
        //or something like that, runStep() is the place to put that code

        //below you can seen an example where we are checking the values of the IR sensors
        //and changing the face of the robot if there is an obstacle to close to the robot
        try {
        if (parcourChoisie == 0) {
            Log.d("runStep", "runStep: >>>>>>>>>>>>> 0");
            return;
        } else if (parcourChoisie == 1) {
            count++;
            switch (state) {
                case 0: //normal state
                    emotionModule.setCurrentEmotion(Emotion.NORMAL);

                    robModule.moveMT(40, 90, 40, 90);

                    Log.d("Parcours1", count + " COUNT");
                    if (count == 75) {
                        state = 1;
                        count = 0;
                    }
                    break;
                case 1:
                    soundModule.playSound(IEmotionSoundModule.ANGRY_SOUND);
                    emotionModule.setCurrentEmotion(Emotion.ANGRY);

                    robModule.moveMT(40, 90, 10, 90);
                    Log.d("Parcours1", "STATE 1");

                    if (finish) {
                        state = 6;
                    }
                    if (count >= 15) {
                        state = 2;
                        count = 0;
                    }
                    break;
                case 2:
                    robModule.moveMT(0, 90, 0, 90);
                    Log.d("Parcours1", "STATE 2");

                    if (count == 2) {

                        state = 3;
                        count = 0;
                    }
                    break;
                case 3:
                    robModule.moveMT(40, 90, 40, 90);
                    Log.d("Parcours1", "STATE 3");

                    if (count == 16) {
                        state = 4;
                        count = 0;
                    }
                    break;
                case 4:
                    robModule.moveMT(40, 90, 5, 90);
                    Log.d("Parcours1", "STATE 4");

                    if (count >= 19) {
                        finish = true;
                        state = 5;
                        count = 0;
                    }
                    break;
                case 5:
                    Log.d("Parcours1", "STATE 5");
                    robModule.moveMT(70, 90, 70, 90);
                    if (count == 55) {
                        state = 6;
                        count = 0;
                    }

                    break;
                case 6:
                    parcourOneFinish = false;
                    break;
            }
        } else if (parcourChoisie == 2) {
            count++;
                switch (state) {
                    case 0: //normal state
                        emotionModule.setCurrentEmotion(Emotion.NORMAL);

                        robModule.moveMT(40, 90, 40, 90);

                        Log.d("Parcours2", count + " COUNT");
                        if (count == 75) {
                            state = 1;
                            count = 0;
                        }
                        break;
                    case 1:
                        soundModule.playSound(IEmotionSoundModule.ANGRY_SOUND);
                        emotionModule.setCurrentEmotion(Emotion.ANGRY);

                        robModule.moveMT(40, 90, 10, 90);
                        Log.d("Parcours2", "STATE 1");

                        if (finish) {
                            state = 6;
                        }
                        if (count >= 15) {
                            state = 2;
                            count = 0;
                        }
                        break;
                    case 2:
                        robModule.moveMT(0, 90, 0, 90);
                        Log.d("Parcours2", "STATE 2");

                        if (count == 2) {

                            state = 3;
                            count = 0;
                        }
                        break;
                    case 3:
                        robModule.moveMT(40, 90, 40, 90);
                        Log.d("Parcours2", "STATE 3");

                        if (count == 16) {
                            state = 4;
                            count = 0;
                        }
                        break;
                    case 4:
                        robModule.moveMT(40, 90, 5, 90);
                        Log.d("Parcours2", "STATE 4");

                        if (count >= 19) {
                            finish = true;
                            state = 5;
                            count = 0;
                        }
                        break;
                    case 5:
                        Log.d("Parcours2", "STATE 5");
                        robModule.moveMT(40, 90, 40, 90);
                        if (count == 20) {
                            state = 6;
                            count = 0;
                        }
                        break;
                    case 6:
                        Log.d("Parcours2", "STATE 6");
                        robModule.moveMT(40, 90, 10, 90);
                        if (count == 20) {
                            state = 7;
                            count = 0;
                        }
                        break;
                    case 7:
                        Log.d("Parcours2", "STATE 7");
                        robModule.moveMT(40, 90, 40, 90);
                        if (count == 20) {
                            state = 8;
                            count = 0;
                        }
                        break;

                    case 8:
                        Log.d("Parcours2", "STATE 8");
                        robModule.moveMT(10, 90, 40, 90);
                        if (count == 20) {
                            state = 9;
                            count = 0;
                        }
                        break;
                    case 9:
                        Log.d("Parcours2", "STATE 9");
                        robModule.moveMT(40, 90, 40, 90);
                        if (count == 5) {
                            state = 10;
                            count = 0;
                        }
                        break;

                    case 10:
                        Log.d("Parcours2", "STATE 10");
                        robModule.moveMT(10, 90, 40, 90);
                        if (count == 10) {
                            state = 11;
                            count = 0;
                        }
                        break;

                    case 11:
                        Log.d("Parcours2", "STATE 11");
                        robModule.moveMT(40, 90, 40, 90);
                        if (count == 50) {
                            state = 12;
                            count = 0;
                        }
                        break;
                    case 12 :
                        parcourOneFinish = false;
                        break;

                    default: break;
                }
        }
        } catch (final CommunicationException e) {
            e.printStackTrace();
        }

    }


    @Override
    public String getModuleInfo() {
        return "Example behaviour";
    }

    @Override
    public String getModuleVersion() {
        return "0.1";
    }

}
