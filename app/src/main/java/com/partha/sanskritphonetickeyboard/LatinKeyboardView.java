/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.partha.sanskritphonetickeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;

public class LatinKeyboardView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;

    static final int KEYCODE_LANGUAGE_SWITCH = -101;

    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        /*} else if (key.codes[0] == 113) {

            return true; */
        } else {
            //Log.d("LatinKeyboardView", "KEY: " + key.codes[0]);
            return super.onLongPress(key);
        }
    }

    void setSubtypeOnSpaceKey(final InputMethodSubtype subtype) {
        final LatinKeyboard keyboard = (LatinKeyboard)getKeyboard();
        //keyboard.setSpaceIcon(getResources().getDrawable(subtype.getIconResId()));
        invalidateAllKeys();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(28);
        paint.setColor(Color.LTGRAY);

        List<Key> keys = getKeyboard().getKeys();
        for(Key key: keys) {
            if(key.label != null) {
                if (key.label.equals("lu")) {
                    canvas.drawText("१", key.x + (key.width - 25), key.y + 40, paint);
                } else if (key.label.equals("Ru")) {
                    canvas.drawText("२", key.x + (key.width - 25), key.y + 40, paint);
                } else if (key.label.equals("e")) {
                    canvas.drawText("३", key.x + (key.width - 25), key.y + 40, paint);
                } else if (key.label.equals("r")) {
                    canvas.drawText("४", key.x + (key.width - 25), key.y + 40, paint);
                } else if (key.label.equals("t")) {
                    canvas.drawText("५", key.x + (key.width - 25), key.y + 40, paint);
                }else if (key.label.equals("y")) {
                    canvas.drawText("६", key.x + (key.width - 25), key.y + 40, paint);
                }else if (key.label.equals("u")) {
                    canvas.drawText("७", key.x + (key.width - 25), key.y + 40, paint);
                }else if (key.label.equals("i")) {
                    canvas.drawText("८", key.x + (key.width - 25), key.y + 40, paint);
                }else if (key.label.equals("o")) {
                    canvas.drawText("९", key.x + (key.width - 25), key.y + 40, paint);
                }else if (key.label.equals("p")) {
                    canvas.drawText("०", key.x + (key.width - 25), key.y + 40, paint);
                }
                else if (key.label.equals("m")) {
                    canvas.drawText("०ं", key.x + (key.width - 25), key.y + 40, paint);
                }
                else if (key.label.equals("jn")) {
                    canvas.drawText("ँ", key.x + (key.width - 25), key.y + 40, paint);
                }
                else if (key.label.equals("h")) {
                    canvas.drawText("ः", key.x + (key.width - 25), key.y + 40, paint);
                }
            }

        }
    }
}