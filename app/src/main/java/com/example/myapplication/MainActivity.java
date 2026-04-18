package com.example.myapplication;

import android.app.*;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	DatabaseHelper db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final EditText firstname = findViewById(R.id.firstname);
		final EditText lastname = findViewById(R.id.lastname);
		final EditText contactnumber = findViewById(R.id.contactnumber);
		final EditText email = findViewById(R.id.email);
		final EditText password = findViewById(R.id.password);
		final Button submit = findViewById(R.id.submit);
		Toast.makeText(getApplicationContext(), "Safe!", Toast.LENGTH_SHORT).show();
		db = new DatabaseHelper(this);
		submit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String fname = firstname.getText().toString();
				String lname = lastname.getText().toString();
				String cnumber = contactnumber.getText().toString();
				String emailadd = email.getText().toString();
				String pw = password.getText().toString();
				if(fname.isEmpty() && lname.isEmpty() && cnumber.isEmpty() && emailadd.isEmpty() && pw.isEmpty()) {
					Toast.makeText(getApplicationContext(), "Please fill out all the fields", Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(getApplicationContext(), "Before saving to DB", Toast.LENGTH_SHORT).show();
					boolean success = db.addUser(fname, lname, cnumber, emailadd, pw, "user");
					Toast.makeText(getApplicationContext(), "After DB", Toast.LENGTH_SHORT).show();
					if(success) {
						Toast.makeText(getApplicationContext(), "Registration successfull!", Toast.LENGTH_SHORT).show();	
					}
					else {
						Toast.makeText(getApplicationContext(), "Registration failed!", Toast.LENGTH_SHORT).show();	
					}
				}
			}
		});
	}
}
