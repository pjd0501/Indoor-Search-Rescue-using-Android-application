package edu.group9.group9diorama;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/*
*  ModeSelector is the activity launched when the app starts.
* It's purpose is to allow the user to enter a server IP address for sending/receiving triage data to/from.
*/

public class ModeSelector extends Activity implements OnClickListener {
	private EditText ipText;
	private Button goButton;
	private Toast toast;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.modeselect);
		getActionBar().hide();
		ipText = (EditText)findViewById(R.id.ipAddressText);
		
		goButton = (Button)findViewById(R.id.goButton);
		goButton.setOnClickListener(this);
	}
	
	void showToast(String text)
	{
	    if(toast != null)
	    {
	        toast.cancel();
	    }
	    toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
	    toast.show();
	}
	

	/*
	* There's only one button in this activity, the "Go" button.
	* When clicked, the IP address in the text box is stored as a string and bundled with the intent.
	* The intent is for the MainActivity activity, which has the actual functionality of the app.
	*/
	@Override
	public void onClick(View v) {
		String ip = ipText.getText().toString();
		if(ip.equals("")){
			showToast("Please enter server IP address");
			return;
		}

		Intent intent = new Intent(getApplicationContext(), MainActivity.class);

		switch(v.getId()){
		case R.id.goButton:
			break;
		default:
			showToast("Unchecked click event: view id = " + v.getId());
			return;
		}

		intent.putExtra("IP", ip);
		startActivity(intent);
	}
}




