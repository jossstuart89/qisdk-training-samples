package com.softbankrobotics.mydecemberasyncdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.human.Human;
import com.aldebaran.qi.sdk.object.humanawareness.EngageHuman;
import com.aldebaran.qi.sdk.object.humanawareness.HumanAwareness;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "MainActivityFollow";
    private QiContext qiContext;
    private Say say;
    private Future<Void> sayFuture;
    private Animate animate;

    final int STATE_INITIALIZING = 0;
    final int STATE_ALONE = 1;
    final int STATE_ENGAGING = 2;
    final int STATE_MOVING = 3;
    final int STATE_ARRIVED = 4;
    final int STATE_PAUSED = 5;
    private int state = STATE_INITIALIZING;
    private HumanAwareness humanAwareness = null;
    private Human engagedHuman;
    private Human nextHuman;
    private Say sayWait;
    private Future<Void> goToFuture;
    private Say sayComing;

    private void showToast(String msg) {
        Log.i(TAG, "Toast: " + msg);
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QiSDK.register(this, this);
        Button button = findViewById(R.id.sayhello);
        button.setOnClickListener((View v) -> {
            if (state == STATE_PAUSED) {
                setState(STATE_ALONE);
                onRecommendedHuman(nextHuman);
            } else {
                setState(STATE_PAUSED);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        QiSDK.unregister(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        sayWait = SayBuilder.with(qiContext)
                .withText("Wait for me!")
                .build();
        sayComing = SayBuilder.with(qiContext)
                .withText("I'm coming!")
                .build();
        //say.async().run();
        //animate.run();
        state = STATE_ALONE;
        humanAwareness = qiContext.getHumanAwareness();
        humanAwareness.addOnRecommendedHumanToEngageChangedListener(this::onRecommendedHuman);
        onRecommendedHuman(humanAwareness.getRecommendedHumanToEngage()); // init
    }

    public void onRecommendedHuman(Human human) {
        if ( (human!=null) && (state == STATE_ALONE)) {
            setState(STATE_ENGAGING);
            engagedHuman = human;
            EngageHuman engage = humanAwareness.makeEngageHuman(qiContext.getRobotContext(), human);
            engage.addOnHumanIsEngagedListener(this::onEngaged);
            engage.addOnHumanIsDisengagingListener(() -> sayWait.run());
            Log.i(TAG, "Engage started.");
            engage.async().run().thenConsume((engageFuture) -> {
                Log.i(TAG, "Engage finished.");
                if (engageFuture.isCancelled()) {
                    showToast("Engage was cancelled ??");
                } else if (engageFuture.hasError()) {
                    showToast("Engage has error: " + engageFuture.getErrorMessage());
                } else {
                    showToast("Engage is done");
                }
                engagedHuman = null;
                setState(STATE_ALONE);
                onRecommendedHuman(nextHuman);
            });
        } else {
            nextHuman = human; // Who do I talk to if this guy leaves?
        }
    }

    private void onEngaged() {
        if (state == STATE_ENGAGING) {
            sayComing.async().run();
            setState(STATE_MOVING);
            goToEngaged();
        }
    }

    private void goToEngaged() {
        // Try again
        if ((state == STATE_MOVING) && (engagedHuman != null)) {
            GoTo goTo = GoToBuilder.with(qiContext).withFrame(engagedHuman.getHeadFrame()).build();
            goToFuture = goTo.async().run();
            goToFuture.thenConsume(this::goToDone);
        }
    }

    private void goToDone(Future<Void> future) {
        if (future.hasError()) {
            Log.d("Joss", "" +future.getErrorMessage());
            showToast("Move failed, let's try again");
            goToEngaged();
        } else if (future.isCancelled()) {
            showToast("Move Cancelled");
        } else {
            showToast("Move Succeeded (now what?)");
            setState(STATE_ARRIVED);
        }
    }


    @Override
    public void onRobotFocusLost() {
        humanAwareness.removeAllOnRecommendedHumanToEngageChangedListeners();
    }

    public void setState(int newState) {
        // Stop old state
        if ((state == STATE_MOVING) && (this.goToFuture != null)) {
            this.goToFuture.requestCancellation();
        }
        // Set state
        state = newState;
        String stateName = "UNKNOWN " + newState;
        switch(state) {
            case STATE_ALONE:
                stateName = "Alone";
                break;
            case STATE_ARRIVED:
                stateName = "Arrived";
                break;
            case STATE_ENGAGING:
                stateName = "Engaging";
                break;
            case STATE_INITIALIZING:
                stateName = "Initializing";
                break;
            case STATE_MOVING:
                stateName = "Moving";
                break;
            case STATE_PAUSED:
                stateName = "Paused";
                break;
        }
        Log.i(TAG, "New state: " + stateName);
        String finalStateName = stateName;
        runOnUiThread(() -> {
            Button button = findViewById(R.id.sayhello);
            button.setText(finalStateName);
        });

    }

    @Override
    public void onRobotFocusRefused(String reason) {
        showToast("Focus refused");

    }
}
