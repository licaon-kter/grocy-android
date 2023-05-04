/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class RecipeImportActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Check if the activity was started by a SEND intent
    Intent intent = getIntent();
    String action = intent.getAction();
    String type = intent.getType();
    String url = null;

    if (Intent.ACTION_SEND.equals(action) && type != null) {
      if ("text/plain".equals(type)) {
        url = intent.getStringExtra(Intent.EXTRA_TEXT);
      }
    }

    // Start MainActivity and pass the URL as an extra
    Intent mainActivityIntent = new Intent(this, MainActivity.class);
    mainActivityIntent.putExtra("url", url);
    startActivity(mainActivityIntent);

    // Finish RecipeImportActivity so that it's removed from the back stack
    finish();
  }

}
