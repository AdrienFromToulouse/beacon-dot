package com.example.beacon_dot;



import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class LoginActivity extends Activity {
 
	private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText usernameField = (EditText) findViewById(R.id.editText1);


        Button startButton = (Button) findViewById(R.id.button1);
        startButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                username = usernameField.getText().toString();
                Intent myIntent = new Intent(view.getContext(), MainActivity.class);
                myIntent.putExtra("username", username);
                startActivityForResult(myIntent, 0);
            }
        });
    }
} 
