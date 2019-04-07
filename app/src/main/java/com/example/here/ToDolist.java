package com.example.here;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;

public class ToDolist extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_dolist);
    }

    public void onChboxClicked(View view) {
        // Получаем флажок
        CheckBox language = (CheckBox) view;
        // Получаем, отмечен ли данный флажок
        boolean checked = language.isChecked();
        switch (view.getId()) {
            case R.id.checkBox:
                if (checked) {
                    //selection.setText("Покормите кота!");
                    Intent intent = new Intent(this, MapActivity.class);
                    //ХАВ ТУ ДУ ИТ?
                    //MapActivity.

                }
        }
    }
}