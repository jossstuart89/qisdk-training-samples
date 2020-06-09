package com.softbankrobotics.mydecembercarsapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.BaseQiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.QiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Topic;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QiSDK.register(this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        QiSDK.unregister(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Topic topic = TopicBuilder.with(qiContext)
                .withResource(R.raw.cars)
                .build();
        QiChatbot chatbot = QiChatbotBuilder.with(qiContext)
                .withTopic(topic)
                .build();

        HashMap<String, QiChatExecutor> executors = new HashMap<>();
        executors.put("showCar", new ShowCarExecutor(qiContext));
        chatbot.setExecutors(executors);

        Chat chat = ChatBuilder.with(qiContext)
                .withChatbot(chatbot)
                .build();

        chat.run();

    }

    @Override
    public void onRobotFocusLost() {

    }

    @Override
    public void onRobotFocusRefused(String reason) {

    }

    private class ShowCarExecutor extends BaseQiChatExecutor {
        public ShowCarExecutor(QiContext qiContext) {
            super(qiContext);
        }

        @Override
        public void runWith(List<String> params) {
            String color = "red";
            if (params.size() > 0) {
                color = params.get(0);
            }
            String imageName = color + "_car";
            int imageId = getResources().getIdentifier(imageName,
                    "drawable",
                    getPackageName());
            runOnUiThread(() -> {
                ImageView splashScreen = findViewById(R.id.splashscreen);
                splashScreen.setImageResource(imageId);
                splashScreen.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void stop() {

        }
    }
}
