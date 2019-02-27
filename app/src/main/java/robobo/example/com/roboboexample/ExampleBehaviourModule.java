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

import com.mytechia.commons.framework.exception.InternalErrorException;
import com.mytechia.robobo.framework.IModule;
import com.mytechia.robobo.framework.RoboboManager;
import com.mytechia.robobo.framework.behaviour.ABehaviourModule;
import com.mytechia.robobo.framework.hri.emotion.Emotion;
import com.mytechia.robobo.framework.hri.emotion.IEmotionModule;
import com.mytechia.robobo.framework.hri.sound.emotionSound.IEmotionSoundModule;
import com.mytechia.robobo.framework.hri.speech.production.ISpeechProductionModule;
import com.mytechia.robobo.framework.hri.touch.ITouchListener;
import com.mytechia.robobo.framework.hri.touch.ITouchModule;
import com.mytechia.robobo.framework.hri.touch.TouchGestureDirection;
import com.mytechia.robobo.rob.IRSensorStatus;
import com.mytechia.robobo.rob.IRob;
import com.mytechia.robobo.rob.IRobInterfaceModule;

/** Example of custom Robobo behaviour written in java and using the native Robobo Framework
 *
 * This is just a simple example behaviour that subscribes a listener for touch events so that
 * it can react to taps and flings by changing the robot face and making some noises.
 *
 * Furthermore a periodic behaviour is also programed in runStep() that makes the robot
 * react to obstacles by checking the value of the IR sensors and changing the face of the
 * robot when an obstacle is too close.
 *
 * @author Gervasio Varela | gervasio.varela@mytechia.com
 *
 */
public class ExampleBehaviourModule extends ABehaviourModule {

    private IRob robModule = null;
    private IEmotionModule  emotionModule = null;
    private ITouchModule touchModule = null;
    private ISpeechProductionModule ttsModule = null;
    private IEmotionSoundModule soundModule = null;

    private ITouchListener touchListener = null;

    private int count = 0;

    private int state = 0; //0 --> normal | 1 --> too close!


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
            public void tap(Integer x, Integer y) {
                //let's complain a little bit
                soundModule.playSound(IEmotionSoundModule.OUCH_SOUND);
                emotionModule.setTemporalEmotion(Emotion.ANGRY, 1500, Emotion.NORMAL);
            }

            @Override
            public void touch(Integer x, Integer y) {

            }

            @Override
            public void fling(TouchGestureDirection dir, double angle, long time, double distance) {
                //let's make a little noise
                soundModule.playSound(IEmotionSoundModule.PURR_SOUND);
                emotionModule.setTemporalEmotion(Emotion.LAUGHING, 15000, Emotion.NORMAL);
            }

            @Override
            public void caress(TouchGestureDirection dir) {

            }
        };

        touchModule.suscribe(touchListener);


        //if needed you can change the default execution period of the runStep() code
        //by default it is 50 ms, and has a minimum allowed value of 10 ms
        this.setPeriod(100); //100ms --> 10 times per second

    }

    @Override
    protected void stopBehaviour()  throws InternalErrorException{

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

        int newState = 0; //normal by default
        for(IRSensorStatus irSensor : robModule.getLastStatusIRs()) {
            if (irSensor.getDistance() < 1000) {
                newState = 1; //some obstacle is to close!
            }
        }

        if (newState != this.state) { //state changed since last time

            this.state = newState;

            switch(state) {
                case 0: //normal state
                    this.emotionModule.setCurrentEmotion(Emotion.NORMAL);
                    break;
                case 1: //too close to obstacle
                    this.soundModule.playSound(IEmotionSoundModule.ANGRY_SOUND);
                    this.emotionModule.setCurrentEmotion(Emotion.ANGRY);
                    break;

            }
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
