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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetZxingPromptBinding;
import xyz.zedler.patrick.grocy.util.ResUtil;

public class ZXingPromptBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = "PurchasePromptBottomSheet";

  private FragmentBottomsheetZxingPromptBinding binding;
  private SharedPreferences sharedPrefs;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetZxingPromptBinding.inflate(
        getLayoutInflater(), container, false
    );

    MainActivity activity = (MainActivity) getActivity();
    assert activity != null;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

    binding.text.setText(ResUtil.getRawText(requireContext(), R.raw.zxing_prompt));

    binding.buttonIgnore.setOnClickListener(v -> dismiss());

    binding.buttonSettings.setOnClickListener(v -> {
      activity.navigateDeepLink(R.string.deep_link_settingsCatScannerToolFragment);
      dismiss();
    });

    setCancelable(false);
    setSkipCollapsedInPortrait();

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    sharedPrefs.edit().putInt(PREF.ZXING_PROMPT, 0).apply();
    super.onDismiss(dialog);
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.linearZxingPromptScroll.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
