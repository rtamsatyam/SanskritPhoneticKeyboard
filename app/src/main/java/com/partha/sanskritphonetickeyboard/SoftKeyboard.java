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

import android.app.Dialog;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener, SpellCheckerSession.SpellCheckerSessionListener {
    static final boolean DEBUG = false;

    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;

    private InputMethodManager mInputMethodManager;

    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;

    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;

    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;

    private LatinKeyboard mCurKeyboard;

    private String mWordSeparators;

    private SpellCheckerSession mScs;
    private List<String> mSuggestions;
    /*Partha*/
    private LatinKeyboard mUpperCaseKeyboard;
    ArrayList<String> mPreviousInput = new ArrayList<>();
    String mVirama = "्";
    String[] mVowelShortForms = new String[]{"ा", "ि", "ी", "ु", "ू", "ृ", "ॄ", "े", "ै", "ो", "ौ"};
    String[] mVowels = new String[]{"अ", "आ", "इ", "ई", "उ", "ऊ", "ऋ", "ॠ", "ऌ", "ॡ", "ए", "ऐ", "ओ", "औ", "ऽ"};
    String[] mSymbols = new String[]{
            "ॐ", "ं", "ः", "ँ", "ऽ",
            ",", "?", "-", "/", ".", "+", "=", "।",
            "१", "२", "३", "४", "५", "६", "७", "८", "९", "०"};
   /* String[] mConsonants = new String[]{"क", "ख", "ग", "घ", "ङ",
            "च", "छ", "ज", "झ", "ञ",
            "ट", "ठ", "ड", "ढ", "ण",
            "त", "थ", "द", "ध", "न",
            "प", "फ", "ब", "भ", "म",
            "य", "र", "ल", "ळ", "व",
            "श", "ष", "स", "ह"};
    */
    List<String> mVowelShortFormList = Arrays.asList(mVowelShortForms);
    List<String> mVowelList = Arrays.asList(mVowels);
    List<String> mSymbolList = Arrays.asList(mSymbols);
    //List<String> mConsonantList = Arrays.asList(mConsonants);


    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
        final TextServicesManager tsm = (TextServicesManager) getSystemService(
                Context.TEXT_SERVICES_MANAGER_SERVICE);
        mScs = tsm.newSpellCheckerSession(null, null, this, true);


    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
        /*Partha*/
        mUpperCaseKeyboard = new LatinKeyboard(this, R.xml.uppercase);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setPreviewEnabled(false);
        setLatinKeyboard(mQwertyKeyboard);
        return mInputView;
    }

    private void setLatinKeyboard(LatinKeyboard nextKeyboard) {
        final boolean shouldSupportLanguageSwitchKey =
                mInputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken());
        nextKeyboard.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey);
        mInputView.setKeyboard(nextKeyboard);
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override
    public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                //mPredictionOn = true;
                /*Partha*/
                //disable predictions
                mPredictionOn = false;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override
    public void onFinishInput() {
        super.onFinishInput();

        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        setLatinKeyboard(mCurKeyboard);
        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                getCurrentInputConnection().finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() - 1);
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length() - 1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:

                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                /*
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            getCurrentInputConnection().clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }*/
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }


        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }

    /**
     * Helper to determine if a given character code is alphabetgetCurrentInputConnection().
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
        Log.d("Test", "KEYCODE: " + primaryCode);
        if (isWordSeparator(primaryCode)) {
            // Handle separator
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
            /*Partha*/
            mPreviousInput.clear();

        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            mPreviousInput.clear();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
            mPreviousInput.clear();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                setLatinKeyboard(mQwertyKeyboard);
            } else {
                setLatinKeyboard(mSymbolsKeyboard);
                mSymbolsKeyboard.setShifted(false);
            }
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        getCurrentInputConnection().beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        getCurrentInputConnection().commitText(text, 0);
        getCurrentInputConnection().endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                //list.add(mComposing.toString());
                Log.d("SoftKeyboard", "REQUESTING: " + mComposing.toString());
                mScs.getSentenceSuggestions(new TextInfo[]{new TextInfo(mComposing.toString())}, 5);
                setSuggestions(list, true, true);
            } else {
                ArrayList<String> list = new ArrayList<String>();
                setSuggestions(list, false, false);
            }
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        mSuggestions = suggestions;
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    private void handleBackspace() {
        /*Partha*/
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (currentKeyboard == mUpperCaseKeyboard || currentKeyboard == mQwertyKeyboard) {
            int mCursorPos = getCurrentInputConnection().getTextBeforeCursor(Integer.MAX_VALUE, 0).length();
            if (mCursorPos > 0) {
                if (mPreviousInput.size() > 0) {
                    String mLastKey = mPreviousInput.get(mPreviousInput.size() - 1);
                    switch (mLastKey) {
                        // if virama is found, the previous letter has to be a consonant
                        // hence remove the virama and the corresponding consonant
                        case "्":
                            //getCurrentInputConnection().deleteSurroundingText(2, 0);
                            keyDownUp(KeyEvent.KEYCODE_DEL);
                            keyDownUp(KeyEvent.KEYCODE_DEL);
                            mPreviousInput.remove(mPreviousInput.size() - 1);
                            mPreviousInput.remove(mPreviousInput.size() - 1);
                            break;
                        // if the previous letter was "a", then it was just a
                        // negation of the virama. So virama is added back
                        case "a":
                            getCurrentInputConnection().commitText(mVirama, 1);
                            mPreviousInput.remove(mPreviousInput.size() - 1);
                            break;
                        //when we find an ा, we just want to remove it
                        case "ा":
                            keyDownUp(KeyEvent.KEYCODE_DEL);
                            mPreviousInput.remove(mPreviousInput.size() - 1);
                            break;
                        default:
                            // if a symbol or vowel then just remove it
                            if (mVowelList.contains(mLastKey) || mSymbolList.contains(mLastKey)) {
                                keyDownUp(KeyEvent.KEYCODE_DEL);
                                mPreviousInput.remove(mPreviousInput.size() - 1);
                            }
                            // in any other circumstance, it must be a vowel short form
                            // hence add back virama e.g. [व, ्, े]
                            else {
                                keyDownUp(KeyEvent.KEYCODE_DEL);
                                mPreviousInput.remove(mPreviousInput.size() - 1);
                                getCurrentInputConnection().commitText(mVirama, 1);
                            }

                    }
                } else {
                    // if there is still some text that is not tracked by mPreviousInput then
                    // delete it
                    keyDownUp(KeyEvent.KEYCODE_DEL);
                }
            }
        } else {
            final int length = mComposing.length();
            if (length > 1) {
                mComposing.delete(length - 1, length);
                getCurrentInputConnection().setComposingText(mComposing, 1);
                updateCandidates();
            } else if (length > 0) {
                mComposing.setLength(0);
                getCurrentInputConnection().commitText("", 0);
                updateCandidates();
            } else {
                keyDownUp(KeyEvent.KEYCODE_DEL);
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        }
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
            /*Partha*/
            setLatinKeyboard(mUpperCaseKeyboard);
        } else if (currentKeyboard == mUpperCaseKeyboard) {
            setLatinKeyboard(mQwertyKeyboard);
            mUpperCaseKeyboard.setShifted(false);
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            setLatinKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            setLatinKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {

        /*Partha*/
        /*
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (mPredictionOn) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates();
        } else {

            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
                    */

        //System.out.println("prev input " + mPreviousInput);
        //if uppercase, switch back to lowercase
        Keyboard currentKeyboard = mInputView.getKeyboard();
        int i = primaryCode;
        char code = (char) i;

        if (currentKeyboard == mUpperCaseKeyboard || currentKeyboard == mQwertyKeyboard) {

            //handling vowels
            if (mVowelList.contains(String.valueOf(code))) {
                // first check if something was typed previously
                if (mPreviousInput.size() > 0) {
                    String mLastEntry = mPreviousInput.get(mPreviousInput.size() - 1);
                    // if prev Input was a symbol then add vowel "as is"
                    //if जा + ओ then just add the vowel "AS IS" = जाओ
                    if (mVowelShortFormList.contains(mLastEntry) || mSymbolList.contains(mLastEntry)) {
                        getCurrentInputConnection().commitText(String.valueOf(code), 1);
                        mPreviousInput.add(String.valueOf(code));
                    }
                    // handling vowel symbols
                    else {
                        switch (i) {
                            //toughest handle अ
                            case 2309:
                                // if previous unit was a consonant e.g. ग (ग् + a), then result is गा
                                if (mLastEntry.equals("a")) {
                                    // send short form of अ
                                    code = (char) 2366;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } //sending आ
                                else if (mLastEntry.equals("अ")) {
                                    code = (char) 2366;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } // if previous entry was a vowel,like ए then add अ , एअ
                                else if (mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                // mostly a virama is found before, which is removed ग् + अ = ग
                                else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    mPreviousInput.add("a");
                                }
                                break;
                            // e, ए
                            case 2319:
                                if (mLastEntry.equals("a") || mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2375;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;
                            // i ,इ
                            case 2311:
                                //ai short form
                                if (mLastEntry.equals("a")) {
                                    code = (char) 2376;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));

                                } //ai ऐ in vowel format at beginning
                                else if (mLastEntry.equals("अ")) {
                                    //remove the a
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    //send ai
                                    code = (char) 2320;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                // if previous is a vowel, then print i इ as is
                                else if (mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                // short form of i
                                else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2367;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;

                            // I, ई
                            case 2312:
                                if (mLastEntry.equals("a") || mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2368;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;
                            // o, ऒ
                            case 2323:
                                if (mLastEntry.equals("a") || mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2379;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;
                            // u, उ
                            case 2313:
                                //au short form
                                if (mLastEntry.equals("a")) {

                                    code = (char) 2380;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                //au in vowel format at beginning
                                else if (mLastEntry.equals("अ")) {
                                    //remove the a
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    //send au
                                    code = (char) 2324;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    //Add value to array
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                // if previous is a vowel, then print u as is
                                else if (mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    //Add value to array
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                // short form of u
                                else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2369;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;
                            // U, ऊ
                            case 2314:
                                if (mLastEntry.equals("a") || mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2370;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;
                            // ऋ
                            case 2315:
                                if (mLastEntry.equals("a") || mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2371;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;
                            // ॠ
                            case 2400:
                                if (mLastEntry.equals("a") || mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2372;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;
                            // ऌ
                            case 2316:
                                if (mLastEntry.equals("a") || mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2402;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;

                            // ऌ
                            case 2401:
                                if (mLastEntry.equals("a") || mVowelList.contains(mLastEntry)) {
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                } else {
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    code = (char) 2403;
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
                                }
                                break;

                            default:
                                System.out.println("never come here, do we?");
/*
                                    keyDownUp(KeyEvent.KEYCODE_DEL);
                                    mPreviousInput.add("a");
                                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                                    mPreviousInput.add(String.valueOf(code));
*/
                        }
                    }
                }
                // handling vowels as is, since it is beginning of a new word, mPreviousInput.size() = 0
                else {
                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                    mPreviousInput.add(String.valueOf(code));
                }
            }
            //handling consonant and symbols
            else {
                // if values entered are symbols, just add them as is
                if (mSymbolList.contains(String.valueOf(code))) {
                    getCurrentInputConnection().commitText(String.valueOf(code), 1);
                    mPreviousInput.add(String.valueOf(code));

                } else {
                    // real consonants
                    String mCurrentEntry = String.valueOf(code);
                    // check if previous to previous symbol is a consonant.
                    // Because previous symbol is a virama and the one before that is a consonant
                    if (mPreviousInput.size() > 1) {
                        String mLastEntry = mPreviousInput.get(mPreviousInput.size() - 2);
                        if (mCurrentEntry.equals("ह")) {
                            switch (mLastEntry) {
                                case "क":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("ख", 1);
                                    mPreviousInput.add("ख");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "ग":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("घ", 1);
                                    mPreviousInput.add("घ");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "च":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("छ", 1);
                                    mPreviousInput.add("छ");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "ज":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("झ", 1);
                                    mPreviousInput.add("झ");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "ट":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("ठ", 1);
                                    mPreviousInput.add("ठ");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "ड":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("ढ", 1);
                                    mPreviousInput.add("ढ");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "त":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("थ", 1);
                                    mPreviousInput.add("थ");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "द":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("ध", 1);
                                    mPreviousInput.add("ध");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "प":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("फ", 1);
                                    mPreviousInput.add("फ");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "ब":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("भ", 1);
                                    mPreviousInput.add("भ");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "स":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("श", 1);
                                    mPreviousInput.add("श");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                case "श":
                                    getCurrentInputConnection().deleteSurroundingText(2, 0);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    mPreviousInput.remove(mPreviousInput.size() - 1);
                                    getCurrentInputConnection().commitText("ष", 1);
                                    mPreviousInput.add("ष");
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                                    break;
                                // when any other letter comes before h ह्, then just add ह् as is
                                default:
                                    getCurrentInputConnection().commitText(mCurrentEntry, 1);
                                    mPreviousInput.add(mCurrentEntry);
                                    getCurrentInputConnection().commitText(mVirama, 1);
                                    mPreviousInput.add(mVirama);
                            }

                        } // if current entry is not h,ह्  then add it as is and add a virama
                        else {
                            getCurrentInputConnection().commitText(mCurrentEntry, 1);
                            mPreviousInput.add(mCurrentEntry);
                            getCurrentInputConnection().commitText(mVirama, 1);
                            mPreviousInput.add(mVirama);
                        }
                    }
                    // if mPreviousInput = 1 or 0, then consonant coming alone for first time
                    // just add it as is
                    else {
                        getCurrentInputConnection().commitText(String.valueOf(code), 1);
                        mPreviousInput.add(String.valueOf(code));
                        getCurrentInputConnection().commitText(mVirama, 1);
                        mPreviousInput.add(mVirama);
                    }
                } // end of handling real consonants
            } // end of handling vowels and consonants


            // }
            //if uppercase, switch back to lowercase
            if (currentKeyboard == mUpperCaseKeyboard) {
                setLatinKeyboard(mQwertyKeyboard);
                mQwertyKeyboard.setShifted(false);
            }
        } else {
            getCurrentInputConnection().commitText(String.valueOf(code), 1);
            mPreviousInput.clear();
        }

    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    private void handleLanguageSwitch() {
        mInputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    private String getWordSeparators() {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char) code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {

            if (mPredictionOn && mSuggestions != null && index >= 0) {
                mComposing.replace(0, mComposing.length(), mSuggestions.get(index));
            }
            commitTyped(getCurrentInputConnection());

        }
    }

    public void swipeRight() {
        Log.d("SoftKeyboard", "Swipe right");
        if (mCompletionOn || mPredictionOn) {
            pickDefaultCandidate();
        }
    }

    public void swipeLeft() {
        Log.d("SoftKeyboard", "Swipe left");
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }

    public void onPress(int primaryCode) {

    }

    public void onRelease(int primaryCode) {

    }

    /**
     * http://www.tutorialspoint.com/android/android_spelling_checker.htm
     *
     * @param results results
     */
    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < results.length; ++i) {
            // Returned suggestions are contained in SuggestionsInfo
            final int len = results[i].getSuggestionsCount();
            sb.append('\n');

            for (int j = 0; j < len; ++j) {
                sb.append("," + results[i].getSuggestionAt(j));
            }

            sb.append(" (" + len + ")");
        }
        Log.d("SoftKeyboard", "SUGGESTIONS: " + sb.toString());
    }

    private static final int NOT_A_LENGTH = -1;

    private void dumpSuggestionsInfoInternal(
            final List<String> sb, final SuggestionsInfo si, final int length, final int offset) {
        // Returned suggestions are contained in SuggestionsInfo
        final int len = si.getSuggestionsCount();
        for (int j = 0; j < len; ++j) {
            sb.add(si.getSuggestionAt(j));
        }
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        Log.d("SoftKeyboard", "onGetSentenceSuggestions");
        final List<String> sb = new ArrayList<>();
        for (int i = 0; i < results.length; ++i) {
            final SentenceSuggestionsInfo ssi = results[i];
            for (int j = 0; j < ssi.getSuggestionsCount(); ++j) {
                dumpSuggestionsInfoInternal(
                        sb, ssi.getSuggestionsInfoAt(j), ssi.getOffsetAt(j), ssi.getLengthAt(j));
            }
        }
        Log.d("SoftKeyboard", "SUGGESTIONS: " + sb.toString());
        setSuggestions(sb, true, true);
    }
}
