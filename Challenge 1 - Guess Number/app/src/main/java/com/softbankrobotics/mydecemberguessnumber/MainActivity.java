package com.softbankrobotics.mydecemberguessnumber;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.AutonomousReactionImportance;
import com.aldebaran.qi.sdk.object.conversation.AutonomousReactionValidity;
import com.aldebaran.qi.sdk.object.conversation.Bookmark;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.QiChatVariable;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Topic;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {
    Integer lowest = 0;
    Integer highest = 20;
    Integer current = 10;

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
                .withResource(R.raw.guessnumber)
                .build();

        QiChatbot chatbot = QiChatbotBuilder.with(qiContext)
                .withTopic(topic)
                .build();

        QiChatVariable guessVariable = chatbot.variable("currentGuess");
        guessVariable.setValue("10");

        Chat chat = ChatBuilder.with(qiContext)
                .withChatbot(chatbot)
                .build();
        Bookmark askBookmark = topic.getBookmarks().get("ASK");
        chatbot.addOnBookmarkReachedListener((Bookmark bookmark) -> {
            Boolean askAgain = false;
            if(bookmark.getName().equals("SMALLER")) {
                highest = current;
                askAgain = true;
            } else if(bookmark.getName().equals("LARGER")) {
                lowest = current;
                askAgain = true;
            } else if(bookmark.getName().equals("RESET")) {
                lowest = 0;
                highest = 20;
            }
            current = (lowest + highest) / 2;
            guessVariable.setValue(current.toString());
            if (askAgain) {
                chatbot.goToBookmark(askBookmark,
                        AutonomousReactionImportance.HIGH,
                        AutonomousReactionValidity.IMMEDIATE);
            }
        });
        Bookmark startBookmark = topic.getBookmarks().get("START");
        chat.addOnStartedListener(() -> {
            chatbot.goToBookmark(startBookmark,
                    AutonomousReactionImportance.HIGH,
                    AutonomousReactionValidity.IMMEDIATE);
        });

        chat.run();
    }

    @Override
    public void onRobotFocusLost() {

    }

    @Override
    public void onRobotFocusRefused(String reason) {

    }
}
